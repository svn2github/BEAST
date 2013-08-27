/*
 * LocalClockModel.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.evomodel.branchratemodel;

import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.util.TaxonList;
import dr.evomodel.tree.TreeModel;
import dr.evomodelxml.branchratemodel.LocalClockModelParser;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.inference.model.Variable;

import java.util.*;

/**
 * @author Andrew Rambaut
 * @version $Id: LocalClockModel.java,v 1.1 2005/04/05 09:27:48 rambaut Exp $
 */
public class LocalClockModel extends AbstractBranchRateModel {

    private TreeModel treeModel;
    protected Map<Integer, LocalClock> localTipClocks = new HashMap<Integer, LocalClock>();
    protected Map<BitSet, LocalClock> localCladeClocks = new HashMap<BitSet, LocalClock>();
    protected LocalClock trunkClock = null;

    private boolean updateNodeClocks = true;
    private Map<NodeRef, LocalClock> nodeClockMap = new HashMap<NodeRef, LocalClock>();
    private final Parameter globalRateParameter;

    public LocalClockModel(TreeModel treeModel, Parameter globalRateParameter) {

        super(LocalClockModelParser.LOCAL_CLOCK_MODEL);
        this.treeModel = treeModel;

        addModel(treeModel);

        this.globalRateParameter = globalRateParameter;
        addVariable(globalRateParameter);
    }

    public void addExternalBranchClock(TaxonList taxonList, Parameter rateParameter, boolean isRelativeRate) throws Tree.MissingTaxonException {
        Set<Integer> tips = Tree.Utils.getTipsForTaxa(treeModel, taxonList);
        LocalClock clock = new LocalClock(rateParameter, isRelativeRate, tips, ClockType.EXTERNAL);
        for (int i : tips) {
            localTipClocks.put(i, clock);
        }
        addVariable(rateParameter);
    }

    public void addCladeClock(TaxonList taxonList, Parameter rateParameter, boolean isRelativeRate, boolean includeStem, boolean excludeClade) throws Tree.MissingTaxonException {
        Set<Integer> tips = Tree.Utils.getTipsForTaxa(treeModel, taxonList);
        BitSet tipBitSet = Tree.Utils.getTipsBitSetForTaxa(treeModel, taxonList);
        LocalClock clock = new LocalClock(rateParameter, isRelativeRate, tips, includeStem, excludeClade);
        localCladeClocks.put(tipBitSet, clock);
        addVariable(rateParameter);
    }

    public void addTrunkClock(TaxonList taxonList, Parameter rateParameter, Parameter indexParameter, boolean isRelativeRate) throws Tree.MissingTaxonException {
        List<Integer> tipList = new ArrayList<Integer>(Tree.Utils.getTipsForTaxa(treeModel, taxonList));
        trunkClock = new LocalClock(rateParameter, indexParameter, isRelativeRate, tipList, ClockType.TRUNK);
        addVariable(rateParameter);
        if (indexParameter != null) {
            addVariable(indexParameter);
        }
    }

    public void handleModelChangedEvent(Model model, Object object, int index) {
        updateNodeClocks = true;
    }

    protected final void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        fireModelChanged();
    }

    protected void storeState() {
    }

    protected void restoreState() {
        updateNodeClocks = true;
    }

    protected void acceptState() {
    }


    // BranchRateModel implementation

    public double getBranchRate(final Tree tree, final NodeRef node) {

        if (tree.isRoot(node)) {
            throw new IllegalArgumentException("root node doesn't have a rate!");
        }

        if (updateNodeClocks) {
            nodeClockMap.clear();
            setupRateParameters(tree, tree.getRoot(), new BitSet());

            if (trunkClock != null) {
                // backbone will overwrite other local clocks
                setupTrunkRates(tree, tree.getRoot());
            }

            updateNodeClocks = false;
        }

        double rate = globalRateParameter.getParameterValue(0);

        LocalClock localClock = nodeClockMap.get(node);
        if (localClock != null) {
            if (localClock.isRelativeRate()) {
                rate *= localClock.getRateParameter().getParameterValue(0);
            } else {
                rate = localClock.getRateParameter().getParameterValue(0);
            }
        }

        return rate;
    }

    private void setupRateParameters(Tree tree, NodeRef node, BitSet tips) {
        LocalClock clock;

        if (tree.isExternal(node)) {
            tips.set(node.getNumber());
            clock = localTipClocks.get(node.getNumber());
        } else {
            for (int i = 0; i < tree.getChildCount(node); i++) {
                NodeRef child = tree.getChild(node, i);
                BitSet childTips = new BitSet();
                setupRateParameters(tree, child, childTips);

                tips.or(childTips);
            }
            clock = localCladeClocks.get(tips);
        }

        if (clock != null) {
            setNodeClock(tree, node, clock, clock.includeStem(), clock.excludeClade());
        }
    }

    private boolean setupTrunkRates(Tree tree, NodeRef node) {
        LocalClock clock = null;

        if (tree.isExternal(node)) {
            if (trunkClock.indexParameter != null) {
                if (trunkClock.tipList.get((int)trunkClock.indexParameter.getParameterValue(0)) == node.getNumber()) {
                    clock = trunkClock;
                }
            } else if (trunkClock.tips.contains(node.getNumber())) {
                clock = trunkClock;
            }
        } else {
            for (int i = 0; i < tree.getChildCount(node); i++) {
                NodeRef child = tree.getChild(node, i);
                if (setupTrunkRates(tree, child)) {
                    // if any of the desendents are back bone then this node is too
                    clock = trunkClock;
                }
            }
        }

        if (clock != null) {
            setNodeClock(tree, node, clock, clock.includeStem(), clock.excludeClade());
            return true;
        }

        return false;
    }

    private void setNodeClock(Tree tree, NodeRef node, LocalClock localClock, boolean includeStem, boolean excludeClade) {

        if (!tree.isExternal(node) && !excludeClade) {
            for (int i = 0; i < tree.getChildCount(node); i++) {
                NodeRef child = tree.getChild(node, i);
                setNodeClock(tree, child, localClock, true, false);
            }
        }

        if (includeStem && !nodeClockMap.containsKey(node)) {
            nodeClockMap.put(node, localClock);
        }
    }

    enum ClockType {
        CLADE,
        TRUNK,
        EXTERNAL
    }

    private class LocalClock {

        LocalClock(Parameter rateParameter, boolean isRelativeRate, Set<Integer> tipSet, ClockType type) {
            this.rateParameter = rateParameter;
            this.indexParameter = null;
            this.isRelativeRate = isRelativeRate;
            this.tips = tipSet;
            this.tipList = null;
            this.type = type;
            this.includeStem = true;
            this.excludeClade = true;
        }

        LocalClock(Parameter rateParameter, Parameter indexParameter, boolean isRelativeRate, List<Integer> tipList, ClockType type) {
            this.rateParameter = rateParameter;
            this.indexParameter = indexParameter;
            this.isRelativeRate = isRelativeRate;
            this.tips = null;
            this.tipList = tipList;
            this.type = type;
            this.includeStem = true;
            this.excludeClade = true;
        }

        LocalClock(Parameter rateParameter, boolean isRelativeRate, Set<Integer> tips, boolean includeStem, boolean excludeClade) {
            this.rateParameter = rateParameter;
            this.indexParameter = null;
            this.isRelativeRate = isRelativeRate;
            this.tips = tips;
            this.tipList = null;
            this.type = ClockType.CLADE;
            this.includeStem = includeStem;
            this.excludeClade = excludeClade;
        }

        boolean includeStem() {
            return this.includeStem;
        }

        boolean excludeClade() {
            return excludeClade;
        }

        ClockType getType() {
            return this.type;
        }

        boolean isRelativeRate() {
            return isRelativeRate;
        }

        Parameter getRateParameter() {
            return this.rateParameter;
        }

        private final Parameter rateParameter;
        private final Parameter indexParameter;
        private final boolean isRelativeRate;
        private final Set<Integer> tips;
        private final List<Integer> tipList;
        private final ClockType type;
        private final boolean includeStem;
        private final boolean excludeClade;
    }

}
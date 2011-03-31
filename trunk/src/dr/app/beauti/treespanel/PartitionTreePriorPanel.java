/*
 * PartitionTreePriorPanel.java
 *
 * Copyright (C) 2002-2009 Alexei Drummond and Andrew Rambaut
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
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.app.beauti.treespanel;

import dr.app.beauti.options.PartitionTreeModel;
import dr.app.beauti.options.PartitionTreePrior;
import dr.app.beauti.types.TreePriorParameterizationType;
import dr.app.beauti.types.TreePriorType;
import dr.app.beauti.util.PanelUtils;
import dr.app.gui.components.WholeNumberField;
import dr.app.util.OSType;
import dr.evomodel.coalescent.VariableDemographicModel;
import dr.evomodelxml.speciation.BirthDeathModelParser;
import dr.evomodelxml.speciation.BirthDeathSerialSamplingModelParser;
import jam.panels.OptionsPanel;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EnumSet;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author Walter Xie
 * @version $Id: PriorsPanel.java,v 1.9 2006/09/05 13:29:34 rambaut Exp $
 */
public class PartitionTreePriorPanel extends OptionsPanel {

    private static final long serialVersionUID = 5016996360264782252L;

    private JComboBox treePriorCombo = new JComboBox(EnumSet.range(TreePriorType.CONSTANT,
            TreePriorType.BIRTH_DEATH_SERI_SAMP_ESTIM).toArray());

    private JComboBox parameterizationCombo = new JComboBox(EnumSet.range(TreePriorParameterizationType.GROWTH_RATE,
            TreePriorParameterizationType.DOUBLING_TIME).toArray());
    //    private JComboBox parameterizationCombo1 = new JComboBox(EnumSet.of(TreePriorParameterizationType.DOUBLING_TIME).toArray());
    private JComboBox bayesianSkylineCombo = new JComboBox(EnumSet.range(TreePriorParameterizationType.CONSTANT_SKYLINE,
            TreePriorParameterizationType.LINEAR_SKYLINE).toArray());
    private WholeNumberField groupCountField = new WholeNumberField(2, Integer.MAX_VALUE);

    private JComboBox extendedBayesianSkylineCombo = new JComboBox(
            new VariableDemographicModel.Type[]{VariableDemographicModel.Type.LINEAR, VariableDemographicModel.Type.STEPWISE});

    private JComboBox gmrfBayesianSkyrideCombo = new JComboBox(EnumSet.range(TreePriorParameterizationType.UNIFORM_SKYRIDE,
            TreePriorParameterizationType.TIME_AWARE_SKYRIDE).toArray());

//    RealNumberField samplingProportionField = new RealNumberField(Double.MIN_VALUE, 1.0);

//	private BeautiFrame frame = null;
//	private BeautiOptions options = null;

    PartitionTreePrior partitionTreePrior;
    private final TreesPanel treesPanel;

    private boolean settingOptions = false;


    public PartitionTreePriorPanel(PartitionTreePrior parTreePrior, TreesPanel parent) {
        super(12, (OSType.isMac() ? 6 : 24));

        this.partitionTreePrior = parTreePrior;
        this.treesPanel = parent;

        PanelUtils.setupComponent(treePriorCombo);
        treePriorCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
                partitionTreePrior.setNodeHeightPrior((TreePriorType) treePriorCombo.getSelectedItem());
                setupPanel();
            }
        });

        PanelUtils.setupComponent(parameterizationCombo);
        parameterizationCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
                partitionTreePrior.setParameterization((TreePriorParameterizationType) parameterizationCombo.getSelectedItem());
            }
        });

//        PanelUtils.setupComponent(parameterizationCombo1);
//        parameterizationCombo1.addItemListener(new ItemListener() {
//            public void itemStateChanged(ItemEvent ev) {
//            	partitionTreePrior.setParameterization((TreePriorParameterizationType) parameterizationCombo1.getSelectedItem());
//            }
//        });

        PanelUtils.setupComponent(groupCountField);
        groupCountField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent ev) {
                // move to here?
            }
        });


        PanelUtils.setupComponent(bayesianSkylineCombo);
        bayesianSkylineCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
                partitionTreePrior.setSkylineModel((TreePriorParameterizationType) bayesianSkylineCombo.getSelectedItem());
            }
        });

        PanelUtils.setupComponent(extendedBayesianSkylineCombo);
        extendedBayesianSkylineCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
                partitionTreePrior.setExtendedSkylineModel(((VariableDemographicModel.Type)
                        extendedBayesianSkylineCombo.getSelectedItem()));
            }
        });

        PanelUtils.setupComponent(gmrfBayesianSkyrideCombo);
        gmrfBayesianSkyrideCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
                partitionTreePrior.setSkyrideSmoothing((TreePriorParameterizationType) gmrfBayesianSkyrideCombo.getSelectedItem());
            }
        });


//	        samplingProportionField.addKeyListener(keyListener);

        setupPanel();
    }

    private void setupPanel() {

        removeAll();

        JTextArea citationText = new JTextArea(1, 200);
        citationText.setLineWrap(true);
        citationText.setEditable(false);
        citationText.setFont(this.getFont());
//        JScrollPane scrollPane = new JScrollPane(citation, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
//                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//        scrollPane.setOpaque(true);

        String citation = null;

        addComponentWithLabel("Tree Prior:", treePriorCombo);

        if (treePriorCombo.getSelectedItem() == TreePriorType.EXPONENTIAL
                || treePriorCombo.getSelectedItem() == TreePriorType.LOGISTIC
                || treePriorCombo.getSelectedItem() == TreePriorType.EXPANSION) {
            addComponentWithLabel("Parameterization for growth:", parameterizationCombo);
            partitionTreePrior.setParameterization((TreePriorParameterizationType) parameterizationCombo.getSelectedItem());

//        } else if (treePriorCombo.getSelectedItem() == TreePriorType.LOGISTIC //) {//TODO Issue 93
//                || treePriorCombo.getSelectedItem() == TreePriorType.EXPANSION) { //TODO Issue 266
//        	addComponentWithLabel("Parameterization for growth:", parameterizationCombo1);
//        	partitionTreePrior.setParameterization((TreePriorParameterizationType) parameterizationCombo1.getSelectedItem());
//
        } else if (treePriorCombo.getSelectedItem() == TreePriorType.SKYLINE) {
            groupCountField.setColumns(6);
            addComponentWithLabel("Number of groups:", groupCountField);
            addComponentWithLabel("Skyline Model:", bayesianSkylineCombo);

            citation = "Drummond AJ, Rambaut A & Shapiro B and Pybus OG (2005) Mol Biol Evol 22, 1185-1192.";

        } else if (treePriorCombo.getSelectedItem() == TreePriorType.EXTENDED_SKYLINE) {
            addComponentWithLabel("Model Type:", extendedBayesianSkylineCombo);
            treesPanel.linkTreePriorCheck.setSelected(true);
            treesPanel.updateShareSameTreePriorChanged();

            citation = "Joseph Heled and Alexei J Drummond, Bayesian inference of population size history " +
                    "from multiple loci, BMC Evolutionary Biology 2008, 8:289";
//            treesPanel.getFrame().setupEBSP(); TODO

        } else if (treePriorCombo.getSelectedItem() == TreePriorType.GMRF_SKYRIDE) {
            addComponentWithLabel("Smoothing:", gmrfBayesianSkyrideCombo);
            //For GMRF, one tree prior has to be associated to one tree model. The validation is in BeastGenerator.checkOptions()
            addLabel("<html>For GMRF, tree model/tree prior combination not implemented by BEAST yet. "
                    + "It is only available for single tree model partition for this release.<br>"
                    + "Please go to Data Partition panel to link all tree models." + "</html>");

            citation = "Minin, Bloomquist and Suchard (2008) Mol Biol Evol, 25, 1459-1471.";

//            treesPanel.linkTreePriorCheck.setSelected(false);
//            treesPanel.linkTreePriorCheck.setEnabled(false);
//            treesPanel.linkTreeModel();
//            treesPanel.updateShareSameTreePriorChanged();

        } else if (treePriorCombo.getSelectedItem() == TreePriorType.BIRTH_DEATH) {
//            samplingProportionField.setColumns(8);
//            treePriorPanel.addComponentWithLabel("Proportion of taxa sampled:", samplingProportionField);
            citation = BirthDeathModelParser.getCitation();

        } else if (treePriorCombo.getSelectedItem() == TreePriorType.BIRTH_DEATH_INCOM_SAMP) {
            citation = BirthDeathModelParser.getCitationRHO();

        } else if (treePriorCombo.getSelectedItem() == TreePriorType.BIRTH_DEATH_SERI_SAMP) {
            citation = BirthDeathSerialSamplingModelParser.getCitationPsiOrg();

        } else if (treePriorCombo.getSelectedItem() == TreePriorType.BIRTH_DEATH_SERI_SAMP_ESTIM) {
            citation = BirthDeathSerialSamplingModelParser.getCitationRT();

        } else {
//            treesPanel.linkTreePriorCheck.setEnabled(true);
//            treesPanel.linkTreePriorCheck.setSelected(true);
//            treesPanel.updateShareSameTreePriorChanged();
        }
        if (citation != null) {
            addComponentWithLabel("Citation:", citationText);
            citationText.setText(citation);
        }

//        getOptions();
//
//        treesPanel.treeModelPanels.get(treesPanel.currentTreeModel).setOptions();
        for (PartitionTreeModel model : treesPanel.treeModelPanels.keySet()) {
            if (model != null) {
                treesPanel.treeModelPanels.get(model).setOptions();
            }
        }

//        createTreeAction.setEnabled(options != null && options.dataPartitions.size() > 0);

//        fireTableDataChanged();

        validate();
        repaint();
    }

    public void setOptions() {

        if (partitionTreePrior == null) {
            return;
        }

        settingOptions = true;

        treePriorCombo.setSelectedItem(partitionTreePrior.getNodeHeightPrior());

        groupCountField.setValue(partitionTreePrior.getSkylineGroupCount());
        //samplingProportionField.setValue(partitionTreePrior.birthDeathSamplingProportion);

        parameterizationCombo.setSelectedItem(partitionTreePrior.getParameterization());
        bayesianSkylineCombo.setSelectedItem(partitionTreePrior.getSkylineModel());

        extendedBayesianSkylineCombo.setSelectedItem(partitionTreePrior.getExtendedSkylineModel());

        gmrfBayesianSkyrideCombo.setSelectedItem(partitionTreePrior.getSkyrideSmoothing());

//        setupPanel();

        settingOptions = false;

        validate();
        repaint();
    }

    public void getOptions() {
        if (settingOptions) return;

//        partitionTreePrior.setNodeHeightPrior((TreePriorType) treePriorCombo.getSelectedItem());

        if (partitionTreePrior.getNodeHeightPrior() == TreePriorType.SKYLINE) {
            Integer groupCount = groupCountField.getValue();
            if (groupCount != null) {
                partitionTreePrior.setSkylineGroupCount(groupCount);
            } else {
                partitionTreePrior.setSkylineGroupCount(5);
            }
        } else if (partitionTreePrior.getNodeHeightPrior() == TreePriorType.BIRTH_DEATH) {
//            Double samplingProportion = samplingProportionField.getValue();
//            if (samplingProportion != null) {
//                partitionTreePrior.birthDeathSamplingProportion = samplingProportion;
//            } else {
//                partitionTreePrior.birthDeathSamplingProportion = 1.0;
//            }
        }

//        partitionTreePrior.setParameterization(parameterizationCombo.getSelectedIndex());
//        partitionTreePrior.setSkylineModel(bayesianSkylineCombo.getSelectedIndex());
//        partitionTreePrior.setExtendedSkylineModel(((VariableDemographicModel.Type) extendedBayesianSkylineCombo.getSelectedItem()).toString());
//
//        partitionTreePrior.setSkyrideSmoothing(gmrfBayesianSkyrideCombo.getSelectedIndex());
        // the taxon list may not exist yet... this should be set when generating...
//        partitionTreePrior.skyrideIntervalCount = partitionTreePrior.taxonList.getTaxonCount() - 1;

    }

//    public void setMicrosatelliteTreePrior() {
//        treePriorCombo.removeAllItems();
//        treePriorCombo.addItem(TreePriorType.CONSTANT);
//    }

    public void removeBayesianSkylineTreePrior() {
        treePriorCombo.removeItem(TreePriorType.SKYLINE);
    }

    public void removeCertainPriorFromTreePriorCombo() {
        treePriorCombo.removeItem(TreePriorType.YULE);
        treePriorCombo.removeItem(TreePriorType.BIRTH_DEATH);
        treePriorCombo.removeItem(TreePriorType.BIRTH_DEATH_INCOM_SAMP);
    }

    public void recoveryTreePriorCombo() {
        if (treePriorCombo.getItemCount() < EnumSet.range(TreePriorType.CONSTANT, TreePriorType.BIRTH_DEATH_SERI_SAMP_ESTIM).size()) {
            treePriorCombo.removeAllItems();
            for (TreePriorType tpt : EnumSet.range(TreePriorType.CONSTANT, TreePriorType.BIRTH_DEATH_SERI_SAMP_ESTIM)) {
                treePriorCombo.addItem(tpt);
            }
        }
    }
}
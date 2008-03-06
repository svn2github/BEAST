/*
 * TipBranchRateModel.java
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
import dr.inference.model.AbstractModel;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.xml.*;

/**
 * @author Andrew Rambaut
 *
 * @version $Id: TipBranchRateModel.java,v 1.1 2005/12/14 16:46:20 rambaut Exp $
 */
public class TipBranchRateModel extends AbstractModel implements BranchRateModel  {

    public static final String TIP_BRANCH_RATE_MODEL = "tipBranchRateModel";
    public static final String EXTERNAL_RATE = "externalRate";
    public static final String INTERNAL_RATE = "internalRate";

    private Parameter internalRateParameter;
    private Parameter externalRateParameter;

    public TipBranchRateModel(Parameter externalRateParameter, Parameter internalRateParameter) {

        super(TIP_BRANCH_RATE_MODEL);

        this.internalRateParameter = internalRateParameter;
        addParameter(internalRateParameter);

        this.externalRateParameter = externalRateParameter;
        addParameter(externalRateParameter);
    }

    public void handleModelChangedEvent(Model model, Object object, int index) {
        fireModelChanged();
    }

    protected void handleParameterChangedEvent(Parameter parameter, int index) {
        fireModelChanged();
    }

    protected void storeState() {
    }

    protected void restoreState() {
    }

    protected void acceptState() {}

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() { return TIP_BRANCH_RATE_MODEL; }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            Parameter externalRateParameter = (Parameter)xo.getSocketChild(EXTERNAL_RATE);
            Parameter internalRateParameter = (Parameter)xo.getSocketChild(INTERNAL_RATE);

            TipBranchRateModel tipBranchRateModel =  new TipBranchRateModel(externalRateParameter, internalRateParameter);

            System.out.println("Using tip branch rate model.");

            return tipBranchRateModel;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return
                    "This element returns a branch rate model that has a different rate for external branches than internal.";
        }

        public Class getReturnType() { return TipBranchRateModel.class; }

        public XMLSyntaxRule[] getSyntaxRules() { return rules; }

        private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
                new ElementRule(EXTERNAL_RATE, Parameter.class, "The molecular evolutionary rate parameter for external branches", false),
                new ElementRule(INTERNAL_RATE, Parameter.class, "The molecular evolutionary rate parameter for internal branches", false)
        };
    };

    public double getBranchRate(Tree tree, NodeRef node) {
        if (tree.isExternal(node)) {
            return externalRateParameter.getParameterValue(0);
        } else {
            return internalRateParameter.getParameterValue(0);

        }
    }

	public String getBranchAttributeLabel() {
		return "rate";
	}

	public String getAttributeForBranch(Tree tree, NodeRef node) {
		return Double.toString(getBranchRate(tree, node));
	}


}
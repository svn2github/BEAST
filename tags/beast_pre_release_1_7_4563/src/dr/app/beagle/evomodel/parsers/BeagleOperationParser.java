/*
 * BeagleOperationParser.java
 *
 * Copyright (c) 2002-2011 Alexei Drummond, Andrew Rambaut and Marc Suchard
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

package dr.app.beagle.evomodel.parsers;

import dr.app.beagle.evomodel.treelikelihood.BeagleOperationReport;
import dr.evolution.alignment.PatternList;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.tree.TreeModel;
import dr.xml.*;

/**
 * @author Marc Suchard
 * @version $Id$
 */
public class BeagleOperationParser extends AbstractXMLObjectParser {


    public static final String OPERATION_REPORT = "beagleOperationReport";

    public String getParserName() {
        return OPERATION_REPORT;
    }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        PatternList patternList = (PatternList) xo.getChild(PatternList.class);
        TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);

        BranchRateModel rateModel = (BranchRateModel) xo.getChild(BranchRateModel.class);

        return new BeagleOperationReport(treeModel, patternList, rateModel);
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "This element represents the likelihood of a patternlist on a tree given the site model.";
    }

    public Class getReturnType() {
        return BeagleOperationReport.class;
    }

    public static final XMLSyntaxRule[] rules = {
            new ElementRule(PatternList.class),
            new ElementRule(TreeModel.class),
            new ElementRule(BranchRateModel.class),
    };

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }
}

package dr.evomodel.MSSD;

import dr.evolution.tree.Tree;
import dr.evomodel.tree.TreeModel;
import dr.inference.model.*;
import dr.xml.*;

import java.util.logging.Logger;

/**
 * Package: CTMCScalePrior
 * Description:
 * <p/>
 * <p/>
 * Created by
 * Alexander V. Alekseyenko (alexander.alekseyenko@gmail.com)
 * Date: Aug 22, 2008
 * Time: 3:26:57 PM
 */
public class CTMCScalePrior extends AbstractModelLikelihood {
    Parameter ctmcScale;
    TreeModel treeModel;

    public static final String MODEL_NAME = "ctmcScalePrior";
    public static final String SCALEPARAMETER = "ctmcScale";
    public static final String TREEMODEL = "treeModel";

    /**
     * The XML parser
     */
    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return MODEL_NAME;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);
            Parameter ctmcScale = (Parameter) xo.getElementFirstChild(SCALEPARAMETER);

            Logger.getLogger("dr.evolution").info("\n ---------------------------------\nCreating ctmcScalePrior model.");
            Logger.getLogger("dr.evolution").info("\tIf you publish results using this prior, please reference:");
            Logger.getLogger("dr.evolution").info("\t\t 1. Ferreira and Suchard (in press) for the conditional reference prior on CTMC scale parameter prior;");

            return new CTMCScalePrior(MODEL_NAME, ctmcScale, treeModel);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "This element represents the prior for CTMC scale parameter.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(TreeModel.class),
                new ElementRule(SCALEPARAMETER, new XMLSyntaxRule[]{new ElementRule(Parameter.class)}),
        };
    };


    public CTMCScalePrior(String name, Parameter ctmcScale, TreeModel treeModel) {
        super(name);
        this.ctmcScale = ctmcScale;
        this.treeModel = treeModel;
    }

    protected void handleModelChangedEvent(Model model, Object object, int index) {
    }

    protected final void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
    }

    protected void storeState() {
    }

    protected void restoreState() {
    }

    protected void acceptState() {
    }

    public Model getModel() {
        return this;
    }

    public double getLogLikelihood() {
        double ab = ctmcScale.getParameterValue(0);
        double totalTreeTime = Tree.Utils.getTreeLength(treeModel, treeModel.getRoot());
        return -0.5 * Math.log(ab) - ab * totalTreeTime;
    }

    public void makeDirty() {
    }
}

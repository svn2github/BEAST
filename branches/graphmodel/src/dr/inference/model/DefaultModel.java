package dr.inference.model;

import dr.inference.loggers.LogColumn;
import dr.xml.*;

/**
 * @author Marc Suchard
 */
public class DefaultModel extends AbstractModelLikelihood {

    public static final String DUMMY_MODEL = "dummyModel";

    public DefaultModel() {
        super(DUMMY_MODEL);
    }

    public DefaultModel(Parameter parameter) {
        super(DUMMY_MODEL);
        addVariable(parameter);
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
        return 0;
    }

    public void makeDirty() {

    }

    public LogColumn[] getColumns() {
        return new LogColumn[0];
    }

    /**
     * Reads a distribution likelihood from a DOM Document element.
     */
    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName() {
            return DUMMY_MODEL;
        }

        public Object parseXMLObject(XMLObject xo) {

            DefaultModel likelihood = new DefaultModel();

            for (int i = 0; i < xo.getChildCount(); i++) {
                Parameter parameter = (Parameter) xo.getChild(i);
                likelihood.addVariable(parameter);
            }

            return likelihood;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "A function wraps a component model that would otherwise not be registered with the MCMC. Always returns a log likelihood of zero.";
        }

        public Class getReturnType() {
            return Likelihood.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                new ElementRule(Parameter.class, 1, Integer.MAX_VALUE)
        };

    };
}



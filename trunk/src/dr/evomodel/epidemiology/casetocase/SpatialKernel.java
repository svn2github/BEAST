package dr.evomodel.epidemiology.casetocase;

import dr.inference.model.AbstractModel;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.inference.model.Variable;
import dr.math.IntegrableUnivariateFunction;
import dr.math.RiemannApproximation;
import dr.xml.*;

/**
 * An abstract spatial kernel function class (and some examples) with single parameter aParam.
 *
 * @author Matthew Hall
 */

public abstract class SpatialKernel extends AbstractModel implements IntegrableUnivariateFunction {

    private Parameter aParam;
    private Parameter storeda;

    public SpatialKernel(String name, Parameter a){
        super(name);
        this.aParam = a;
        storeda = new Parameter.Default(a.getParameterValue(0));
        this.addVariable(a);
    }

    public Parameter geta(){
        return aParam;
    }

    public void seta(Parameter value){
        aParam = value;
    }

    public static final String SPATIAL_KERNEL_FUNCTION = "spatialKernelFunction";
    public static final String A = "a";
    public static final String KERNEL_TYPE = "kernelType";

    public enum Type{
        EXPONENTIAL ("exponential", Exponential.class),
        POWER_LAW ("powerLaw", PowerLaw.class),
        GAUSSIAN ("gaussian", Gaussian.class);

        private final String xmlName;
        private final Class kernelClass;

        String getXmlName(){
            return xmlName;
        }

        SpatialKernel makeKernelFunction() throws InstantiationException, IllegalAccessException{
            SpatialKernel out = (SpatialKernel)kernelClass.newInstance();
            return out;
        }

        Type(String xmlName, Class kernelClass){
            this.xmlName = xmlName;
            this.kernelClass = kernelClass;
        }
    }

    public static double EuclideanDistance(double[] point1, double[] point2){
        return Math.sqrt(Math.pow(point1[0]-point2[0],2) + Math.pow(point1[1]-point2[1], 2));
    }

    public double getUpperBound(){
        return Double.POSITIVE_INFINITY;
    }

    public double getLowerBound(){
        return 0;
    }

    protected void handleModelChangedEvent(Model model, Object object, int index){
        // nothing to do at present
    }

    protected void storeState(){
        storeda = new Parameter.Default(aParam.getParameterValue(0));
    }

    protected void restoreState(){
        aParam = storeda;
    }

    public double value(double[] point1, double[] point2){
        return evaluate(EuclideanDistance(point1, point2));
    }

    public double value(double[] point1, double[] point2, double alpha){
        return evaluate(EuclideanDistance(point1, point2), alpha);
    }

    protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type){
        // nothing to do?
    }

    protected void acceptState(){
        // nothing to do?
    }

    public double evaluate(double argument){
        return evaluate(argument, aParam.getParameterValue(0));
    }


    public abstract double evaluate(double argument, double alpha);

    public class Exponential extends SpatialKernel {

        public Exponential(String name, Parameter a){
            super(name, a);
        }

        public double evaluate(double argument, double alpha){
            return Math.exp(-argument * alpha);
        }

        public double evaluateIntegral(double a, double b){
            double aValue = aParam.getParameterValue(0);
            return -(1/aValue)*Math.exp(-aValue*b) + (1/aValue)*Math.exp(-aValue*a);
        }

    }

    public class PowerLaw extends SpatialKernel {

        public PowerLaw(String name, Parameter a){
            super(name, a);
        }

        public double value(double[] point1, double[] point2){
            return evaluate(EuclideanDistance(point1, point2));
        }

        public double evaluate(double argument, double alpha){
            return Math.pow(argument, alpha);
        }

        public double evaluateIntegral(double a, double b){
            double aValue = aParam.getParameterValue(0);
            return -aValue*Math.pow(b, -aValue-1) + -aValue*Math.pow(a, -aValue-1);
        }

    }

    public class Gaussian extends SpatialKernel {

        RiemannApproximation integrator;

        public Gaussian(String name, Parameter a){
            this(name, a, 25);
        }

        public Gaussian(String name, Parameter a, int steps){
            super(name, a);
            integrator = new RiemannApproximation(steps);
        }

        public double evaluate(double argument, double alpha){
            return Math.exp(-Math.pow(argument, 2) * alpha);
        }

        public double evaluateIntegral(double a, double b){
            return integrator.integrate(this, a, b);
        }



    }

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        public String getParserName(){
            return SPATIAL_KERNEL_FUNCTION;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {
            try{
                String type = (String)xo.getAttribute("type");
                SpatialKernel kernelFunction = null;
                for(Type value : Type.values()){
                    if(value.getXmlName().equals(type)){
                        kernelFunction = value.makeKernelFunction();
                    }
                }
                if(kernelFunction==null){
                    throw new XMLParseException("Unknown spatial kernel type");
                }
                Parameter a = new Parameter.Default((Double) xo.getAttribute(A));
                kernelFunction.seta(a);
                return kernelFunction;
            } catch(InstantiationException e){
                throw new XMLParseException("Failed to initiate spatial kernel");
            } catch(IllegalAccessException e){
                throw new XMLParseException("Failed to initiate spatial kernel");
            }
        }

        @Override
        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }


        public final XMLSyntaxRule[] rules;{
            rules = new XMLSyntaxRule[]{
                    AttributeRule.newDoubleRule(A),
                    AttributeRule.newStringRule(KERNEL_TYPE)
            };
        }

        public String getParserDescription() {
            return "This element represents a spatial kernel function with a single parameter.";
        }

        public Class getReturnType() {
            return SpatialKernel.class;
        }
    };



}

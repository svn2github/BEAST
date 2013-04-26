package dr.evomodel.epidemiology.casetocase;

import dr.evolution.tree.NodeRef;
import dr.evolution.util.Date;
import dr.evolution.util.Taxa;
import dr.evolution.util.Taxon;
import dr.evolution.util.Units;
import dr.evomodel.tree.TreeModel;
import dr.inference.distribution.GammaDistributionModel;
import dr.inference.distribution.ParametricDistributionModel;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.inference.model.ProductStatistic;
import dr.inference.model.Variable;
import dr.math.IntegrableUnivariateFunction;
import dr.math.RiemannApproximation;
import dr.math.UnivariateFunction;
import dr.xml.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Adaptation of the farm incubation and infectious period models from Morelli et al, PLoS Computational Biology, 2012
 * (10.1371/journal.pcbi.1002768.g001)
 *
 * Non-extended paintings for now
 *
 * User: Matthew Hall
 * Date: 07/09/2012
 */

public class Morelli12Outbreak extends AbstractOutbreak {

    public Morelli12Outbreak(String name, ParametricDistributionModel incubationPeriodDistribution, Parameter d,
                             ArrayList<AbstractCase> farms, Parameter riemannSampleSize){
        this(name, incubationPeriodDistribution, d ,riemannSampleSize);
        cases = farms;
        for(AbstractCase farm : farms){
            addModel(farm);
        }
    }

    public Morelli12Outbreak(ParametricDistributionModel incubationPeriodDistribution, Parameter d,
                             ArrayList<AbstractCase> farms, Parameter riemannSampleSize){
        this(MORELLI_12_OUTBREAK, incubationPeriodDistribution, d, farms, riemannSampleSize);
    }

    // with the inner class, initialisation has to take places without cases - add them later

    public Morelli12Outbreak(String name, ParametricDistributionModel incubationPeriodDistribution, Parameter d,
                             Parameter riemannSampleSize){
        super(name);
        this.latentPeriodDistribution = incubationPeriodDistribution;
        addModel(this.latentPeriodDistribution);
        this.d = d;
        numericalIntegrator = new RiemannApproximation((int)riemannSampleSize.getParameterValue(0));
        cases = new ArrayList<AbstractCase>();
    }

    public Morelli12Outbreak(ParametricDistributionModel incubationPeriodDistribution, Parameter d,
                             Parameter riemannSampleSize){
        this(MORELLI_12_OUTBREAK, incubationPeriodDistribution, d, riemannSampleSize);
    }


    private void addCase(String caseID, Date examDate, Date cullDate, Parameter oldestLesionAge, Taxa associatedTaxa){
        Morelli12Case thisCase = new Morelli12Case(caseID, examDate, cullDate, oldestLesionAge, associatedTaxa);
        cases.add(thisCase);
        addModel(thisCase);
    }




    //fingers crossed...

    //indices of paintings:
    //0 - node
    //1 - tree.getChild(node,0)
    //2 - tree.getChild(node,1)
    //
    // Similar for times
    // this ignores whether the tree will actually accept the paintings at this node (although it must be a
    // non-extended painting) and just calculates the probability of these timings

    public double localLogP(Morelli12Case parentPainting, Morelli12Case childPainting, double infectedAt,
                            double infectiousBy){
        if(parentPainting==childPainting){
            return 0;
        } else {
            //the event in question is that the case corresponding to the child whose painting is different infected
            // at the time of node and infectious by the time of that child.
            return Math.log(childPainting.infectedAtInfectiousBy(infectedAt, infectiousBy));
        }
    }
    public double localP(Morelli12Case parentPainting, Morelli12Case childPainting, double infectedAt,
                         double infectiousBy){
        return Math.exp(logP(parentPainting, childPainting, infectedAt, infectiousBy));
    }

    public double P(AbstractCase parentPainting, AbstractCase childPainting, double infectedAt,
                    double infectiousBy){
        return localP((Morelli12Case)parentPainting, (Morelli12Case)childPainting, infectedAt, infectiousBy);
    }

    public double logP(AbstractCase parentPainting, AbstractCase childPainting, double infectedAt,
                       double infectiousBy){
        return localLogP((Morelli12Case)parentPainting, (Morelli12Case)childPainting, infectedAt, infectiousBy);
    }

    public double localProbInfectiousBy(Morelli12Case painting, double time){
        return Math.exp(logProbInfectiousBy(painting, time));
    }

    public double localLogProbInfectiousBy(Morelli12Case painting, double time){
        return Math.log(1-painting.getInfectiousPeriodDistribution().cdf(painting.getEndOfInfectiousPeriod() - time));
    }

    public double probInfectiousBy(AbstractCase painting, double time){
        return localProbInfectiousBy((Morelli12Case)painting, time);
    }

    public double logProbInfectiousBy(AbstractCase painting, double time){
        return localLogProbInfectiousBy((Morelli12Case)painting, time);
    }


    /* Parser. */

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {
        //for the outbreak

        public static final String INCUBATION_PERIOD_DISTRIBUTION = "latentPeriodDistribution";
        public static final String RIEMANN_SAMPLE_SIZE = "riemannSampleSize";
        public static final String SQRT_INFECTIOUS_SCALE = "sqrtInfectiousScale";

        //for the cases

        public static final String CASE_ID = "caseID";
        public static final String CULL_DAY = "cullDay";
        public static final String EXAMINATION_DAY = "examinationDay";
        public static final String OLDEST_LESION_AGE = "oldestLesionAge";
        public static final String COORDINATES = "coordinates";

        @Override
        public Object parseXMLObject(XMLObject xo) throws XMLParseException {
            final ParametricDistributionModel incubationPeriodDistribution =
                    (ParametricDistributionModel) xo.getElementFirstChild(INCUBATION_PERIOD_DISTRIBUTION);
            final Parameter d = (Parameter) xo.getElementFirstChild(SQRT_INFECTIOUS_SCALE);
            final Parameter riemannSampleSize = (Parameter) xo.getElementFirstChild(RIEMANN_SAMPLE_SIZE);
            Morelli12Outbreak cases = new Morelli12Outbreak(incubationPeriodDistribution, d, riemannSampleSize);
            for(int i=0; i<xo.getChildCount(); i++){
                Object cxo = xo.getChild(i);
                if(cxo instanceof XMLObject && ((XMLObject)cxo).getName().equals(Morelli12Case.MORELLI_12_CASE)){
                    parseCase((XMLObject)cxo, cases);
                }
            }
            return cases;
        }

        public void parseCase(XMLObject xo, Morelli12Outbreak outbreak)
                throws XMLParseException {
            String farmID = (String) xo.getAttribute(CASE_ID);
            final Date cullDate = (Date) xo.getElementFirstChild(CULL_DAY);
            final Date examDate = (Date) xo.getElementFirstChild(EXAMINATION_DAY);
            final Parameter oldestLesionAge = (Parameter) xo.getElementFirstChild(OLDEST_LESION_AGE);
            Taxa taxa = new Taxa();
            for(int i=0; i<xo.getChildCount(); i++){
                if(xo.getChild(i) instanceof Taxon){
                    taxa.addTaxon((Taxon)xo.getChild(i));
                }
            }
            outbreak.addCase(farmID, examDate, cullDate, oldestLesionAge, taxa);
        }

        @Override
        public String getParserDescription(){
            return "Parses a set of Morelli 2012 farm cases and the information that they all share";
        }

        @Override
        public Class getReturnType(){
            return Morelli12Outbreak.class;
        }

        public String getParserName(){
            return MORELLI_12_OUTBREAK;
        }

        @Override
        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] caseRules = {
                new StringAttributeRule(CASE_ID, "The unique identifier for this farm"),
                new ElementRule(CULL_DAY, Date.class, "The date this farm was culled", false),
                new ElementRule(EXAMINATION_DAY, Date.class, "The date this farm was examined", false),
                new ElementRule(Taxon.class, 0, Integer.MAX_VALUE),
                new ElementRule(OLDEST_LESION_AGE, Parameter.class, "The estimated oldest lesion date as determined" +
                        "by investigating vets"),
                new ElementRule(COORDINATES, Parameter.class, "The spatial coordinates (x,y) of the farm", true),
        };

        private final XMLSyntaxRule[] rules = {
                new ElementRule(ProductStatistic.class, 0,2),
                new ElementRule(INCUBATION_PERIOD_DISTRIBUTION, ParametricDistributionModel.class, "The probability " +
                        "distribution of incubation periods (constructed in the XML so farm elements can inherit" +
                        "it).", false),
                new ElementRule(Morelli12Case.MORELLI_12_CASE, caseRules, 1, Integer.MAX_VALUE),
                new ElementRule(SQRT_INFECTIOUS_SCALE, Parameter.class, "The square root of the scale parameter of " +
                        "all infectiousness periods (variances are proportional to the square of this, see Morelli" +
                        "2012).", false),
                new ElementRule(RIEMANN_SAMPLE_SIZE, Parameter.class, "The sample size for the Riemann numerical" +
                        "integration method, used by all child cases.", true),
        };
    };

    public static final String MORELLI_12_OUTBREAK = "morelli12Outbreak";
    private ParametricDistributionModel latentPeriodDistribution;
    private final RiemannApproximation numericalIntegrator;
    private final Parameter d;

    @Override
    protected void handleModelChangedEvent(Model model, Object object, int index) {
        fireModelChanged();
    }

    @Override
    protected void handleVariableChangedEvent(Variable variable, int index, Variable.ChangeType type) {
        fireModelChanged();
    }

    @Override
    protected void storeState() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void restoreState() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void acceptState() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    //Case class.

    private class Morelli12Case extends AbstractCase {

        // geography version

        public Morelli12Case(String name, String caseID, Date examDate, Date cullDate, Parameter oldestLesionAge,
                             Taxa associatedTaxa){
            super(name);
            this.caseID = caseID;
            //The time value for end of these days is the numerical value of these dates plus 1.
            this.examDate = examDate;
            endOfInfectiousDate = cullDate;
            this.associatedTaxa = associatedTaxa;
            this.oldestLesionAge = oldestLesionAge;
            rebuildInfDistribution();
            this.addModel(infectiousPeriodDistribution);
            this.addModel(latentPeriodDistribution);
            this.addVariable(d);
        }



        public Morelli12Case(String caseID, Date examDate, Date cullDate, Parameter oldestLesionAge,
                             Taxa associatedTaxa){
            this(MORELLI_12_CASE, caseID, examDate, cullDate, oldestLesionAge, associatedTaxa);
        }



        private void rebuildInfDistribution(){
            Parameter infectious_shape = new Parameter.Default
                    (oldestLesionAge.getParameterValue(0)/Math.pow(d.getParameterValue(0),2));
            Parameter infectious_scale = new Parameter.Default(Math.pow(d.getParameterValue(0),2));
            infectiousPeriodDistribution = new GammaDistributionModel(infectious_shape, infectious_scale);
        }

        public Date getLatestPossibleInfectionDate() {
            Double doubleDate = examDate.getTimeValue();
            return Date.createTimeSinceOrigin(doubleDate, Units.Type.DAYS, examDate.getOrigin());
        }

        public Taxa getAssociatedTaxa() {
            return associatedTaxa;
        }

        public double getEndOfInfectiousPeriod(){
            return endOfInfectiousDate.getTimeValue();
        }

        public void setInfectiousPeriodDistribution(ParametricDistributionModel distribution){
            infectiousPeriodDistribution = distribution;
        }

        public ParametricDistributionModel getInfectiousPeriodDistribution(){
            return infectiousPeriodDistribution;
        }

        public boolean culledYet(int day) {
            return day>endOfInfectiousDate.getTimeValue()+1;
        }

        public Object getEndOfInfectiousDate() {
            return endOfInfectiousDate;
        }

        public double infectedAtInfectiousAt(double infected, double infectious){
            return infectiousPeriodDistribution.pdf(endOfInfectiousDate.getTimeValue()-infectious)
                    * latentPeriodDistribution.pdf(infectious-infected);
        }


        public double infectedAtInfectiousBy(double infected, double infectious){
            JointDistribution tempDist = new JointDistribution(endOfInfectiousDate.getTimeValue()-infected);
            return tempDist.evaluateIntegral(0, infectious-infected);
        }

        @Override
        protected void handleModelChangedEvent(Model model, Object object, int index) {
            rebuildInfDistribution();
            fireModelChanged();
        }

        @Override
        protected void handleVariableChangedEvent(Variable variable, int index, Variable.ChangeType type) {
            rebuildInfDistribution();
            fireModelChanged();
        }

        @Override
        protected void storeState() {
            storedInfectiousPeriodDistribution = infectiousPeriodDistribution;
        }

        @Override
        protected void restoreState() {
            infectiousPeriodDistribution = storedInfectiousPeriodDistribution;
        }

        @Override
        protected void acceptState() {
        }

    /* Probability distribution for the latent period being "argument" and the total time from infection to cull "t2".
    The integral of this function is over values of "argument" and is important in normalising the likelihood of the
    transmission tree given the phylogenetic tree*/

        public class JointDistribution implements IntegrableUnivariateFunction {

            public JointDistribution(double t2){
                this.t2=t2;
            }

            public double evaluateIntegral(double a, double b) {
                return numericalIntegrator.integrate(this, a, b);
            }

            public double evaluate(double argument) {
                return latentPeriodDistribution.pdf(argument)*infectiousPeriodDistribution.pdf(t2-argument);
            }

            public double getLowerBound() {
                return 0;
            }

            public double getUpperBound() {
                return t2;
            }

            private double t2;
        }

        public static final String MORELLI_12_CASE = "morelli12Case";
        private final Date examDate;
        private final Date endOfInfectiousDate;
        private final Parameter oldestLesionAge;
        private ParametricDistributionModel infectiousPeriodDistribution;
        private ParametricDistributionModel storedInfectiousPeriodDistribution;
    }
}

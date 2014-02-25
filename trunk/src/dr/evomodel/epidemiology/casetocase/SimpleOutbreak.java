package dr.evomodel.epidemiology.casetocase;

import dr.evolution.util.Date;
import dr.evolution.util.Taxa;
import dr.evolution.util.Taxon;
import dr.evolution.util.Units;
import dr.inference.distribution.GammaDistributionModel;
import dr.inference.distribution.ParametricDistributionModel;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.inference.model.ProductStatistic;
import dr.inference.model.Variable;
import dr.math.RiemannApproximation;
import dr.xml.*;

import java.util.ArrayList;

/**
 * This version has a separate prior distribution on each infection time and they are not assumed to be drawn from
 * the same distribution, so further infectious period parameters are not estimated. Latent periods could be handled,
 * but only if the time of infection is independent of the time of infectiousness.
 *
 * @todo implement latent periods
 *
 * Construct the prior probability distributions on infection dates using oldMacDonald. The XML expects each to be
 * stated up front.
 *
 * User: Matthew Hall
 * Date: 23/07/2012
 */

public class SimpleOutbreak extends AbstractOutbreak {

    public static final String SIMPLE_OUTBREAK = "simpleOutbreak";

    public SimpleOutbreak(String name, Taxa taxa, boolean hasGeography){
        super(name, taxa, false, hasGeography);
        cases = new ArrayList<AbstractCase>();
    }

    public SimpleOutbreak(String name, Taxa taxa, boolean hasGeography,
                          ArrayList<AbstractCase> cases){
        this(name, taxa, hasGeography);
        this.cases.addAll(cases);
    }

    private void addCase(String caseID, Date examDate, Date cullDate,
                         ParametricDistributionModel infectiousPeriodDistribution, Parameter coords,
                         Taxa associatedTaxa){
        SimpleCase thisCase = new SimpleCase(caseID, examDate, cullDate, infectiousPeriodDistribution, coords,
                associatedTaxa);
        cases.add(thisCase);
        addModel(thisCase);
    }

    public double getDistance(AbstractCase a, AbstractCase b) {
        return SpatialKernel.EuclideanDistance(a.getCoords(), b.getCoords());
    }

    // in all of the following infectiousness of the parent is assumed because there is no latent period, so Y is not
    // used

    public double probXInfectedByYAtTimeT(AbstractCase X, AbstractCase Y, double T) {
        return probXInfectedAtTimeT(X, T);
    }

    public double logProbXInfectedByYAtTimeT(AbstractCase X, AbstractCase Y, double T) {
        return logProbXInfectedAtTimeT(X, T);
    }

    public double probXInfectedByYBetweenTandU(AbstractCase X, AbstractCase Y, double T, double U) {
        return probXInfectedBetweenTandU(X, T, U);
    }

    public double logProbXInfectedByYBetweenTandU(AbstractCase X, AbstractCase Y, double T, double U) {
        return logProbXInfectedBetweenTandU(X, T, U);
    }

    public double probXInfectiousByTimeT(AbstractCase X, double T) {
        return ((SimpleCase)X).infectedBy(T);
    }

    public double logProbXInfectiousByTimeT(AbstractCase X, double T) {
        return Math.log(probXInfectiousByTimeT(X, T));
    }

    public double probXInfectedAtTimeT(AbstractCase X, double T) {
        return ((SimpleCase)X).infectedAt(T);
    }

    public double logProbXInfectedAtTimeT(AbstractCase X, double T) {
        return Math.log(probXInfectedAtTimeT(X,T));
    }

    public double probXInfectedBetweenTandU(AbstractCase X, double T, double U) {
        return ((SimpleCase)X).infectedBetween(T,U);
    }

    public double logProbXInfectedBetweenTandU(AbstractCase X, double T, double U) {
        return Math.log(probXInfectedBetweenTandU(X, T, U));
    }

    protected void handleModelChangedEvent(Model model, Object object, int index) {
        fireModelChanged();
    }

    protected void storeState() {
        //nothing to do
    }

    protected void restoreState() {
        //nothing to do
    }

    protected void acceptState() {
        //nothing to do
    }

    protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
        fireModelChanged();
    }

    private class SimpleCase extends AbstractCase{

        public static final String SIMPLE_CASE = "simpleCase";
        private final Parameter coords;
        //this is time from infection to cull, NOT time from infection to exam
        private ParametricDistributionModel infectiousPeriodDistribution;


        private SimpleCase(String name, String caseID, Date examDate, Date cullDate,
                           ParametricDistributionModel infectiousPeriodDistribution, Parameter coords,
                           Taxa associatedTaxa){
            super(name);
            this.caseID = caseID;
            this.examDate = examDate;
            endOfInfectiousDate = cullDate;
            this.associatedTaxa = associatedTaxa;
            this.coords = coords;
            this.infectiousPeriodDistribution = infectiousPeriodDistribution;
            this.addModel(infectiousPeriodDistribution);
        }

        private SimpleCase(String caseID, Date examDate, Date cullDate,
                           ParametricDistributionModel infectiousPeriodDistribution, Parameter coords,
                           Taxa associatedTaxa){
            this(SIMPLE_CASE, caseID, examDate, cullDate, infectiousPeriodDistribution, coords, associatedTaxa);
        }

        public Date getLatestPossibleInfectionDate() {
            Double doubleDate = examDate.getTimeValue();
            return Date.createTimeSinceOrigin(doubleDate, Units.Type.DAYS, examDate.getOrigin());
        }

        public double infectedAt(double infected){
            if(examinedYet(infected)){
                return 0;
            } else {
                return infectiousPeriodDistribution.pdf(endOfInfectiousDate.getTimeValue()-infected);
            }
        }

        public double infectedBetween(double start, double end){
            if(examinedYet(start)){
                return 0;
            } else {
                double endPoint = end<endOfInfectiousDate.getTimeValue() ? end : endOfInfectiousDate.getTimeValue();
                return infectiousPeriodDistribution.cdf(endOfInfectiousDate.getTimeValue()-start)
                        - infectiousPeriodDistribution.cdf(endOfInfectiousDate.getTimeValue()-endPoint);
            }

        }

        public double infectedBy(double time){
            if(examinedYet(time)){
                return 1;
            } else {
                return 1 - infectiousPeriodDistribution.cdf(endOfInfectiousDate.getTimeValue()-time);
            }
        }

        public double infectedAfter(double time){
            return 1 - infectedBy(time);
        }

        public boolean culledYet(double time) {
            return time>endOfInfectiousDate.getTimeValue();
        }

        public boolean examinedYet(double time) {
            return time>examDate.getTimeValue();
        }

        public double[] getCoords() {
            return new double[]{coords.getParameterValue(0), coords.getParameterValue(1)};
        }

        protected void handleModelChangedEvent(Model model, Object object, int index) {
            fireModelChanged();
        }

        protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
            fireModelChanged();
        }

        protected void storeState() {
            //nothing should ever change
        }

        protected void restoreState() {
            //nothing should ever change
        }

        protected void acceptState() {
            //nothing to do
        }

        public ParametricDistributionModel getInfectiousPeriodDistribution(){
            return infectiousPeriodDistribution;
        }
    }

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        //for the outbreak

        public static final String HAS_GEOGRAPHY = "hasGeography";

        //for the outbreak

        public static final String CASE_ID = "caseID";
        public static final String CULL_DAY = "cullDay";
        public static final String EXAMINATION_DAY = "examinationDay";
        public static final String INFECTIOUS_PERIOD_DISTRIBUTION = "estimatedInfectionDate";
        public static final String COORDINATES = "coordinates";

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {
            final boolean hasGeography = xo.hasAttribute(HAS_GEOGRAPHY)
                    ? (Boolean) xo.getAttribute(HAS_GEOGRAPHY) : false;
            final Taxa taxa = (Taxa) xo.getChild(Taxa.class);
            SimpleOutbreak cases = new SimpleOutbreak(null, taxa, hasGeography);
            for(int i=0; i<xo.getChildCount(); i++){
                Object cxo = xo.getChild(i);
                if(cxo instanceof XMLObject && ((XMLObject)cxo).getName().equals(SimpleCase.SIMPLE_CASE)){
                    parseCase((XMLObject)cxo, cases);
                }
            }
            return cases;
        }

        public void parseCase(XMLObject xo, SimpleOutbreak outbreak)
                throws XMLParseException {
            String farmID = (String) xo.getAttribute(CASE_ID);
            final Date cullDate = (Date) xo.getElementFirstChild(CULL_DAY);
            final Date examDate = (Date) xo.getElementFirstChild(EXAMINATION_DAY);
            final ParametricDistributionModel infDistribution
                    = (ParametricDistributionModel) xo.getElementFirstChild(INFECTIOUS_PERIOD_DISTRIBUTION);
            final Parameter coords = xo.hasChildNamed(COORDINATES) ?
                    (Parameter) xo.getElementFirstChild(COORDINATES) : null;
            Taxa taxa = new Taxa();
            for(int i=0; i<xo.getChildCount(); i++){
                if(xo.getChild(i) instanceof Taxon){
                    taxa.addTaxon((Taxon)xo.getChild(i));
                }
            }
            outbreak.addCase(farmID, examDate, cullDate, infDistribution, coords, taxa);
        }

        public String getParserDescription(){
            return "Parses a set of 'simple' farm outbreak and the information that they all share";
        }

        public Class getReturnType(){
            return SimpleOutbreak.class;
        }

        public String getParserName(){
            return SIMPLE_OUTBREAK;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] caseRules = {
                new StringAttributeRule(CASE_ID, "The unique identifier for this farm"),
                new ElementRule(CULL_DAY, Date.class, "The date this farm was culled", false),
                new ElementRule(EXAMINATION_DAY, Date.class, "The date this farm was examined", false),
                new ElementRule(Taxon.class, 0, Integer.MAX_VALUE),
                new ElementRule(INFECTIOUS_PERIOD_DISTRIBUTION, ParametricDistributionModel.class, "The prior " +
                        "probability distribution of the time from infection to becoming noninfectious"),
                new ElementRule(COORDINATES, Parameter.class, "The spatial coordinates of this case", true)
        };

        private final XMLSyntaxRule[] rules = {
                new ElementRule(ProductStatistic.class, 0,2),
                new ElementRule(SimpleCase.SIMPLE_CASE, caseRules, 1, Integer.MAX_VALUE),
                new ElementRule(Taxa.class),
                AttributeRule.newBooleanRule(HAS_GEOGRAPHY, true)
        };
    };


}

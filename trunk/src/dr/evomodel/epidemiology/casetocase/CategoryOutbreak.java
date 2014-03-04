package dr.evomodel.epidemiology.casetocase;

import dr.evolution.util.Date;
import dr.evolution.util.Taxa;
import dr.evolution.util.Taxon;
import dr.inference.model.*;
import dr.xml.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Outbreak class for within-case coalescent.
 *
 * Each case belongs to an infectious (and latent) category which corresponds to one of a list of probability
 * distributions (most likely gamma or exponential) for the length of the infectious (latent) period. The XML rules for
 * the outbreak class ask for at least one ParametricDistributionModel.
 * Assignment of outbreak to distributions should be handled in whatever script or GUI writes the XML.
 *
 * Intended for situations where no data on infection times exists.
 *
 * User: Matthew Hall
 * Date: 17/12/2013
 */

public class CategoryOutbreak extends AbstractOutbreak {

    public static final String CATEGORY_OUTBREAK = "categoryOutbreak";
    private final HashSet<String> latentCategories;
    private final HashSet<String> infectiousCategories;
    private final HashMap<String, AbstractPeriodPriorDistribution> latentMap;
    private final HashMap<String, AbstractPeriodPriorDistribution> infectiousMap;
    private double[][] distances;


    public CategoryOutbreak(String name, Taxa taxa, boolean hasGeography, boolean hasLatentPeriods,
                            HashMap<String, AbstractPeriodPriorDistribution> infectiousMap,
                            HashMap<String, AbstractPeriodPriorDistribution> latentMap){
        super(name, taxa, hasLatentPeriods, hasGeography);
        cases = new ArrayList<AbstractCase>();
        latentCategories = new HashSet<String>();
        infectiousCategories = new HashSet<String>();
        this.latentMap = latentMap;
        this.infectiousMap = infectiousMap;
    }


    private void addCase(String caseID, Date examDate, Date cullDate, Parameter coords, Taxa associatedTaxa,
                         String infectiousCategory, String latentCategory){
        CategoryCase thisCase;

        if(latentCategory==null){
            thisCase =  new CategoryCase(caseID, examDate, cullDate, coords, associatedTaxa,
                    infectiousCategory);
        } else {
            thisCase =
                    new CategoryCase(caseID, examDate, cullDate, coords, associatedTaxa, infectiousCategory,
                            latentCategory);
            latentCategories.add(latentCategory);
        }
        infectiousCategories.add(infectiousCategory);
        cases.add(thisCase);
        addModel(thisCase);
    }

    public HashSet<String> getLatentCategories(){
        return latentCategories;
    }

    public HashSet<String> getInfectiousCategories(){
        return infectiousCategories;
    }

    public int getLatentCategoryCount(){
        return latentCategories.size();
    }

    public int getInfectiousCategoryCount(){
        return infectiousCategories.size();
    }

    public HashMap<String, AbstractPeriodPriorDistribution> getLatentMap(){
        return latentMap;
    }

    public HashMap<String, AbstractPeriodPriorDistribution> getInfectiousMap(){
        return latentMap;
    }

    public AbstractPeriodPriorDistribution getLatentCategoryPrior(String category){
        return latentMap.get(category);
    }

    public AbstractPeriodPriorDistribution getInfectiousCategoryPrior(String category){
        return infectiousMap.get(category);
    }

    public String getInfectiousCategory(AbstractCase aCase){
        return ((CategoryCase)aCase).getInfectiousCategory();
    }

    public String getLatentCategory(AbstractCase aCase){
        return ((CategoryCase)aCase).getLatentCategory();
    }

    public double getDistance(AbstractCase a, AbstractCase b) {
        if(distances==null){
            throw new RuntimeException("Distance matrix has not been initialised");
        }
        return distances[getCaseIndex(a)][getCaseIndex(b)];
    }

    private void buildDistanceMatrix(){
        distances = new double[cases.size()][cases.size()];

        for(int i=0; i<cases.size(); i++){
            for(int j=0; j<cases.size(); j++){
                distances[i][j]= SpatialKernel.EuclideanDistance(getCase(i).getCoords(), getCase(j).getCoords());
            }
        }
    }

    protected void handleModelChangedEvent(Model model, Object object, int index) {
        fireModelChanged();
    }

    protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
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

    private class CategoryCase extends AbstractCase{

        public static final String CATEGORY_CASE = "categoryCase";
        private String infectiousCategory;
        private String latentCategory;
        private Parameter coords;

        private CategoryCase(String name, String caseID, Date examDate, Date cullDate, Parameter coords,
                             Taxa associatedTaxa, String infectiousCategory){
            super(name);
            this.caseID = caseID;
            this.infectiousCategory = infectiousCategory;
            this.examDate = examDate;
            endOfInfectiousDate = cullDate;
            this.associatedTaxa = associatedTaxa;
            this.coords = coords;
            latentCategory = null;
        }


        private CategoryCase(String name, String caseID, Date examDate, Date cullDate, Parameter coords,
                             Taxa associatedTaxa, String infectiousCategory, String latentCategory){
            this(name, caseID, examDate, cullDate, coords, associatedTaxa, infectiousCategory);
            this.latentCategory = latentCategory;
        }


        private CategoryCase(String caseID, Date examDate, Date cullDate, Parameter coords,
                             Taxa associatedTaxa, String infectiousCategory){
            this(CATEGORY_CASE, caseID, examDate, cullDate, coords, associatedTaxa, infectiousCategory);
        }


        private CategoryCase(String caseID, Date examDate, Date cullDate, Parameter coords,
                             Taxa associatedTaxa, String infectiousCategory, String latentCategory){
            this(CATEGORY_CASE, caseID, examDate, cullDate, coords, associatedTaxa, infectiousCategory,
                    latentCategory);
        }

        public String getLatentCategory(){
            return latentCategory;
        }

        public String getInfectiousCategory(){
            return infectiousCategory;
        }


        public boolean culledYet(double time) {
            return time>endOfInfectiousDate.getTimeValue();
        }

        public boolean examinedYet(double time) {
            return time>examDate.getTimeValue();
        }

        protected void handleModelChangedEvent(Model model, Object object, int index) {
            // @todo to have all the outbreak listening seems excessive and I'm not sure it's necessary - maybe only the outbreak need listen
            fireModelChanged();
        }

        protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
            fireModelChanged();
        }

        protected void storeState() {
            // nothing to do?
        }

        protected void restoreState() {
            // nothing to do?
        }

        protected void acceptState() {
            //nothing to do?
        }

        public double[] getCoords() {
            return new double[]{coords.getParameterValue(0), coords.getParameterValue(1)};
        }
    }

// for integrating out infectiousness dates, if this is preferred:

//    private class CombinedPeriodFunction implements IntegrableUnivariateFunction {
//
//        private ParametricDistributionModel infectious;
//        private ParametricDistributionModel latent;
//        private Integral numIntergrator;
//        private PdfByPdf pdfByPdf;
//        private CdfByPdf cdfByPdf;
//
//        private CombinedPeriodFunction(ParametricDistributionModel infectious, ParametricDistributionModel latent,
//                                       int numSteps){
//            this.infectious = infectious;
//            this.latent = latent;
//            this.numIntergrator = new RiemannApproximation(numSteps);
//            cdfByPdf = new CdfByPdf(1);
//            pdfByPdf = new PdfByPdf(1);
//        }
//
//        public double evaluateIntegral(double a, double b) {
//            cdfByPdf.setTotal(b);
//            double out = numIntergrator.integrate(cdfByPdf, 0, b);
//            if(a>0){
//                cdfByPdf.setTotal(a);
//                out -= numIntergrator.integrate(cdfByPdf, 0, a);
//            }
//            return out;
//        }
//
//        public double evaluateIntegral(double a, double b, double maxLatent){
//            double out;
//            cdfByPdf.setTotal(b);
//            if(maxLatent > b){
//                out = numIntergrator.integrate(cdfByPdf, 0, b);
//                out /= latent.cdf(maxLatent);
//            } else {
//                out = numIntergrator.integrate(cdfByPdf, 0, maxLatent);
//                out /= latent.cdf(maxLatent);
//            }
//            if(a>0){
//                cdfByPdf.setTotal(a);
//                if(maxLatent > a){
//                    out -= numIntergrator.integrate(cdfByPdf, 0, a)/latent.cdf(maxLatent);
//                } else {
//                    out -= numIntergrator.integrate(cdfByPdf, 0, maxLatent)/latent.cdf(maxLatent);
//                }
//
//            }
//            return out;
//        }
//
//        public double evaluate(double argument) {
//            pdfByPdf.setTotal(argument);
//            return numIntergrator.integrate(pdfByPdf, 0, argument);
//        }
//
//        public double evaluate(double argument, double maxLatent){
//            if(maxLatent>argument){
//                return evaluate(argument)/latent.cdf(maxLatent);
//            }
//            pdfByPdf.setTotal(argument);
//            return numIntergrator.integrate(pdfByPdf, 0, maxLatent)/latent.cdf(maxLatent);
//        }
//
//
//        public double getLowerBound() {
//            return 0;
//        }
//
//        public double getUpperBound() {
//            return Double.POSITIVE_INFINITY;
//        }
//
//        private class PdfByPdf implements UnivariateFunction {
//
//            double total;
//
//            private PdfByPdf(double total){
//                this.total = total;
//            }
//
//            public double evaluate(double argument) {
//                return infectious.pdf(total-argument)*latent.pdf(argument);
//            }
//
//            public double getLowerBound() {
//                return 0;
//            }
//
//            public double getUpperBound() {
//                return Double.POSITIVE_INFINITY;
//            }
//
//            public void setTotal(double total){
//                this.total = total;
//            }
//
//            public double getTotal(){
//                return total;
//            }
//
//        }
//
//        private class CdfByPdf implements UnivariateFunction{
//
//            double total;
//
//            private CdfByPdf(double total){
//                this.total = total;
//            }
//
//            public double evaluate(double argument) {
//                return infectious.cdf(total-argument)*latent.pdf(argument);
//            }
//
//            public double getLowerBound() {
//                return 0;
//            }
//
//            public double getUpperBound() {
//                return Double.POSITIVE_INFINITY;
//            }
//
//            public void setTotal(double total){
//                this.total = total;
//            }
//
//            public double getTotal(){
//                return total;
//            }
//
//        }
//    }

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

        //for the outbreak

        public static final String HAS_GEOGRAPHY = "hasGeography";
        public static final String HAS_LATENT_PERIODS = "hasLatentPeriods";
        public static final String INFECTIOUS_PERIOD_PRIOR = "infectiousPeriodPrior";
        public static final String LATENT_PERIOD_PRIOR = "latentPeriodPrior";

        //for the outbreak

        public static final String CASE_ID = "caseID";
        public static final String CULL_DAY = "cullDay";
        public static final String EXAMINATION_DAY = "examinationDay";
        public static final String COORDINATES = "spatialCoordinates";
        public static final String INFECTION_TIME_BRANCH_POSITION = "infectionTimeBranchPosition";
        public static final String INFECTIOUS_TIME_POSITION = "infectiousTimePosition";
        public static final String LATENT_CATEGORY = "latentCategory";
        public static final String INFECTIOUS_CATEGORY = "infectiousCategory";

        //for the normal-gamma priors

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {
            final boolean hasGeography = xo.hasAttribute(HAS_GEOGRAPHY) && xo.getBooleanAttribute(HAS_GEOGRAPHY);
            final boolean hasLatentPeriods = Boolean.parseBoolean((String)xo.getAttribute(HAS_LATENT_PERIODS));

            final Taxa taxa = (Taxa) xo.getChild(Taxa.class);

            HashMap<String, AbstractPeriodPriorDistribution> infMap
                    = new HashMap<String, AbstractPeriodPriorDistribution>();

            HashMap<String, AbstractPeriodPriorDistribution> latMap
                    = new HashMap<String, AbstractPeriodPriorDistribution>();

            for(int i=0; i<xo.getChildCount(); i++){
                Object cxo = xo.getChild(i);
                if(cxo instanceof XMLObject){
                    if (((XMLObject)cxo).getName().equals(INFECTIOUS_PERIOD_PRIOR)){
                        AbstractPeriodPriorDistribution hyperprior
                                = (AbstractPeriodPriorDistribution)
                                ((XMLObject)cxo).getChild(AbstractPeriodPriorDistribution.class);
                        infMap.put(hyperprior.getModelName(), hyperprior);
                    } else if ((((XMLObject)cxo).getName().equals(LATENT_PERIOD_PRIOR))){
                        AbstractPeriodPriorDistribution hyperprior
                                = (AbstractPeriodPriorDistribution)
                                ((XMLObject)cxo).getChild(AbstractPeriodPriorDistribution.class);
                        latMap.put(hyperprior.getModelName(), hyperprior);
                    }
                }
            }

            CategoryOutbreak cases = new CategoryOutbreak(null, taxa, hasGeography,
                    hasLatentPeriods, infMap, latMap);
            for(int i=0; i<xo.getChildCount(); i++){
                Object cxo = xo.getChild(i);
                if(cxo instanceof XMLObject && ((XMLObject)cxo).getName()
                        .equals(CategoryCase.CATEGORY_CASE)){
                    parseCase((XMLObject)cxo, cases, hasLatentPeriods);
                }
            }
            cases.buildDistanceMatrix();
            return cases;
        }

        public void parseCase(XMLObject xo, CategoryOutbreak outbreak, boolean expectLatentPeriods)
                throws XMLParseException {
            String farmID = (String) xo.getAttribute(CASE_ID);
            String infectiousCategory = (String) xo.getAttribute(INFECTIOUS_CATEGORY);
            final Date cullDate = (Date) xo.getElementFirstChild(CULL_DAY);
            final Date examDate = (Date) xo.getElementFirstChild(EXAMINATION_DAY);
            String latentCategory = null;
            if(xo.hasAttribute(LATENT_CATEGORY)){
                latentCategory = (String) xo.getAttribute(LATENT_CATEGORY);
            } else if(expectLatentPeriods){
                throw new XMLParseException("Case "+farmID+" not assigned a latent periods distribution");
            }
            if(expectLatentPeriods && !xo.hasChildNamed(INFECTIOUS_TIME_POSITION)){
                throw new XMLParseException("Latent periods specified, but case "+farmID+" not assigned a time of " +
                        "infectiousness");
            }

            final Parameter coords = xo.hasChildNamed(COORDINATES) ?
                    (Parameter) xo.getElementFirstChild(COORDINATES) : null;
            Taxa taxa = new Taxa();
            for(int i=0; i<xo.getChildCount(); i++){
                if(xo.getChild(i) instanceof Taxon){
                    taxa.addTaxon((Taxon)xo.getChild(i));
                }
            }
            outbreak.addCase(farmID, examDate, cullDate, coords, taxa, infectiousCategory, latentCategory);
        }


        public String getParserDescription(){
            return "Parses a set of 'category' farm outbreak and the information that they all share";
        }

        public Class getReturnType(){
            return SimpleOutbreak.class;
        }

        public String getParserName(){
            return CATEGORY_OUTBREAK;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] caseRules = {
                new StringAttributeRule(CASE_ID, "The unique identifier for this farm"),
                new ElementRule(CULL_DAY, Date.class, "The date this farm was culled", false),
                new ElementRule(EXAMINATION_DAY, Date.class, "The date this farm was examined", false),
                new ElementRule(Taxon.class, 0, Integer.MAX_VALUE),
                new ElementRule(INFECTION_TIME_BRANCH_POSITION, Parameter.class, "The exact position on the branch" +
                        " along which the infection of this case occurs that it actually does occur"),
                new ElementRule(INFECTIOUS_TIME_POSITION, Parameter.class, "Parameter taking a value between 0 and" +
                        "1, indicating when from infection (0) to first caused infection (or cull if the outbreak" +
                        "causes no infections) (1) the case became infectious", true),
                new ElementRule(COORDINATES, Parameter.class, "The spatial coordinates of this case", true),
                new StringAttributeRule(LATENT_CATEGORY, "The category of latent period", true),
                new StringAttributeRule(INFECTIOUS_CATEGORY, "The category of infectious period")
        };


        private final XMLSyntaxRule[] rules = {
                new StringAttributeRule(HAS_LATENT_PERIODS, "Whether to include a latent period in the model"),
                new ElementRule(ProductStatistic.class, 0,2),
                new ElementRule(CategoryCase.CATEGORY_CASE, caseRules, 1, Integer.MAX_VALUE),
                new ElementRule(Taxa.class),
                new ElementRule(INFECTIOUS_PERIOD_PRIOR, AbstractPeriodPriorDistribution.class, "blah", 1,
                        Integer.MAX_VALUE),
                new ElementRule(LATENT_PERIOD_PRIOR, AbstractPeriodPriorDistribution.class, "blah", 0,
                        Integer.MAX_VALUE),
                AttributeRule.newBooleanRule(HAS_GEOGRAPHY, true)
        };
    };

}

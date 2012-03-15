package dr.evomodel.antigenic;

import dr.evolution.util.*;
import dr.inference.model.*;
import dr.math.MathUtils;
import dr.math.distributions.NormalDistribution;
import dr.util.*;
import dr.xml.*;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Andrew Rambaut
 * @author Trevor Bedford
 * @author Marc Suchard
 * @version $Id$
 */
public class AntigenicLikelihood extends AbstractModelLikelihood implements Citable {
    public final static boolean CHECK_INFINITE = false;

    public final static String ANTIGENIC_LIKELIHOOD = "antigenicLikelihood";

    // column indices in table
    private static final int COLUMN_LABEL = 0;
    private static final int SERUM_STRAIN = 2;
    private static final int ROW_LABEL = 1;
    private static final int VIRUS_STRAIN = 3;
    private static final int SERUM_DATE = 4;
    private static final int VIRUS_DATE = 5;
    private static final int RAW_TITRE = 6;
    private static final int MIN_TITRE = 7;
    private static final int MAX_TITRE = 8;

    public enum MeasurementType {
        INTERVAL,
        POINT,
        UPPER_BOUND,
        LOWER_BOUND,
        MISSING
    }

    public AntigenicLikelihood(
            int mdsDimension,
            Parameter mdsPrecisionParameter,
            TaxonList strainTaxa,
            MatrixParameter locationsParameter,
            Parameter datesParameter,
            Parameter columnParameter,
            Parameter rowParameter,
            DataTable<String[]> dataTable,
            List<String> virusLocationStatisticList) {

        super(ANTIGENIC_LIKELIHOOD);

        List<String> strainNames = new ArrayList<String>();
        Map<String, Double> strainDateMap = new HashMap<String, Double>();

        for (int i = 0; i < dataTable.getRowCount(); i++) {
            String[] values = dataTable.getRow(i);
            int column = columnLabels.indexOf(values[COLUMN_LABEL]);
            if (column == -1) {
                columnLabels.add(values[0]);
                column = columnLabels.size() - 1;
            }

            int columnStrain = -1;
            if (strainTaxa != null) {
                columnStrain = strainTaxa.getTaxonIndex(values[SERUM_STRAIN]);
            } else {
                columnStrain = strainNames.indexOf(values[SERUM_STRAIN]);
                if (columnStrain == -1) {
                    strainNames.add(values[SERUM_STRAIN]);

                    Double date = Double.parseDouble(values[VIRUS_DATE]);
                    strainDateMap.put(values[SERUM_STRAIN], date);

                    columnStrain = strainNames.size() - 1;
                }
            }

            if (columnStrain == -1) {
                throw new IllegalArgumentException("Error reading data table: Unrecognized serum strain name, " + values[SERUM_STRAIN] + ", in row " + (i+1));
            }

            int row = rowLabels.indexOf(values[ROW_LABEL]);
            if (row == -1) {
                rowLabels.add(values[ROW_LABEL]);
                row = rowLabels.size() - 1;
            }

            int rowStrain = -1;
            if (strainTaxa != null) {
                rowStrain = strainTaxa.getTaxonIndex(values[VIRUS_STRAIN]);
            } else {
                rowStrain = strainNames.indexOf(values[VIRUS_STRAIN]);
                if (rowStrain == -1) {
                    strainNames.add(values[VIRUS_STRAIN]);

                    Double date = Double.parseDouble(values[VIRUS_DATE]);
                    strainDateMap.put(values[VIRUS_STRAIN], date);

                    rowStrain = strainNames.size() - 1;
                }
            }
            if (rowStrain == -1) {
                throw new IllegalArgumentException("Error reading data table: Unrecognized virus strain name, " + values[VIRUS_STRAIN] + ", in row " + (i+1));
            }

            double minTitre = Double.NaN;
            try {
                if (values[MIN_TITRE].length() > 0) {
                    try {
                        minTitre = Double.parseDouble(values[MIN_TITRE]);
                    } catch (NumberFormatException nfe) {
                        // do nothing
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e)  {
                 // do nothing
            }

            double maxTitre = Double.NaN;
            try {
                if (values[MAX_TITRE].length() > 0) {
                    try {
                        maxTitre = Double.parseDouble(values[MAX_TITRE]);
                    } catch (NumberFormatException nfe) {
                        // do nothing
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e)  {
                 // do nothing
            }

            // use this if minTitre and maxTitre are not defined in HI file
            double rawTitre = Double.NaN;
            if (Double.isNaN(minTitre) && Double.isNaN(maxTitre)) {
                if (values[RAW_TITRE].length() > 0) {
                    try {
                        rawTitre = Double.parseDouble(values[RAW_TITRE]);
                        maxTitre = rawTitre;
                        minTitre = rawTitre;
                    } catch (NumberFormatException nfe) {
                        // check if threshold below
                        if (values[RAW_TITRE].contains("<")) {
                            rawTitre = Double.parseDouble(values[RAW_TITRE].replace("<",""));
                            maxTitre = rawTitre;
                            minTitre = 0.0;
                        }
                        // check if threshold above
                        if (values[RAW_TITRE].contains(">")) {
                            rawTitre = Double.parseDouble(values[RAW_TITRE].replace(">",""));
                            minTitre = rawTitre;
                            maxTitre = Double.NaN;
                        }
                    }
                }
            }

            MeasurementType type = MeasurementType.INTERVAL;

            if (minTitre == maxTitre) {
                type = MeasurementType.POINT;
            }

            if (Double.isNaN(minTitre) || minTitre == 0.0) {
                if (Double.isNaN(maxTitre)) {
                    throw new IllegalArgumentException("Error in measurement: both min and max titre are at bounds in row " + (i+1));
                }
                type = MeasurementType.UPPER_BOUND;
            } else if (Double.isNaN(maxTitre)) {
                type = MeasurementType.LOWER_BOUND;
            }

            Measurement measurement = new Measurement(column, columnStrain, row, rowStrain, type, minTitre, maxTitre);
            measurements.add(measurement);
        }

        double[] maxColumnTitre = new double[columnLabels.size()];
        double[] maxRowTitre = new double[rowLabels.size()];
        for (Measurement measurement : measurements) {
            double titre = measurement.maxTitre;
            if (Double.isNaN(titre)) {
                titre = measurement.minTitre;
            }
            if (titre > maxColumnTitre[measurement.column]) {
                maxColumnTitre[measurement.column] = titre;
            }
            if (titre > maxRowTitre[measurement.row]) {
                maxRowTitre[measurement.row] = titre;
            }
        }

        if (strainTaxa != null) {
            this.strains = strainTaxa;

            // fill in the strain name array for local use
            for (int i = 0; i < strains.getTaxonCount(); i++) {
                strainNames.add(strains.getTaxon(i).getId());
            }

        } else {
            Taxa taxa = new Taxa();
            for (String strain : strainNames) {
                taxa.addTaxon(new Taxon(strain));
            }
            this.strains = taxa;
        }

        this.mdsDimension = mdsDimension;
        this.mdsPrecisionParameter = mdsPrecisionParameter;
        addVariable(mdsPrecisionParameter);

        this.locationsParameter = locationsParameter;
        setupLocationsParameter(this.locationsParameter, strainNames);
        addVariable(this.locationsParameter);

        if (datesParameter != null) {
            // this parameter is not used in this class but is setup to be used in other classes
            datesParameter.setDimension(strainNames.size());
            String[] labelArray = new String[strainNames.size()];
            strainNames.toArray(labelArray);
            datesParameter.setDimensionNames(labelArray);
            for (int i = 0; i < strainNames.size(); i++) {
                Double date = strainDateMap.get(strainNames.get(i));
                if (date == null) {
                    throw new IllegalArgumentException("Date missing for strain: " + strainNames.get(i));
                }
                datesParameter.setParameterValue(i, date);
            }
        }

        if (columnParameter == null) {
            this.columnEffectsParameter = new Parameter.Default("columnEffects");
        } else {
            this.columnEffectsParameter = columnParameter;
        }

        this.columnEffectsParameter.setDimension(columnLabels.size());
        addVariable(this.columnEffectsParameter);
        String[] labelArray = new String[columnLabels.size()];
        columnLabels.toArray(labelArray);
        this.columnEffectsParameter.setDimensionNames(labelArray);
        for (int i = 0; i < maxColumnTitre.length; i++) {
            this.columnEffectsParameter.setParameterValueQuietly(i, maxColumnTitre[i]);
        }

        if (rowParameter == null) {
            this.rowEffectsParameter = new Parameter.Default("rowEffects");
        } else {
            this.rowEffectsParameter = rowParameter;
        }

        this.rowEffectsParameter.setDimension(rowLabels.size());
        addVariable(this.rowEffectsParameter);
        labelArray = new String[rowLabels.size()];
        rowLabels.toArray(labelArray);
        this.rowEffectsParameter.setDimensionNames(labelArray);
        for (int i = 0; i < maxRowTitre.length; i++) {
            this.rowEffectsParameter.setParameterValueQuietly(i, maxRowTitre[i]);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\tAntigenicLikelihood:\n");
        sb.append("\t\t" + this.strains.getTaxonCount() + " strains\n");
        sb.append("\t\t" + columnLabels.size() + " unique columns\n");
        sb.append("\t\t" + rowLabels.size() + " unique rows\n");
        sb.append("\t\t" + measurements.size() + " assay measurements\n");
        Logger.getLogger("dr.evomodel").info(sb.toString());

        // initial locations

        double earliestDate = datesParameter.getParameterValue(0);
        for (int i=0; i<datesParameter.getDimension(); i++) {
            double date = datesParameter.getParameterValue(i);
            if (earliestDate > date) {
                earliestDate = date;
            }
        }

        for (int i = 0; i < locationsParameter.getParameterCount(); i++) {

            String name = strainNames.get(i);
            double date = (double) strainDateMap.get(strainNames.get(i));
            double diff = (date-earliestDate);
            locationsParameter.getParameter(i).setParameterValueQuietly(0, diff + MathUtils.nextGaussian());

            for (int j = 1; j < mdsDimension; j++) {
                double r = MathUtils.nextGaussian();
                locationsParameter.getParameter(i).setParameterValueQuietly(j, r);
            }

        }

        locationChanged = new boolean[this.locationsParameter.getRowDimension()];
        logLikelihoods = new double[measurements.size()];
        storedLogLikelihoods = new double[measurements.size()];

        makeDirty();
    }

    protected void setupLocationsParameter(MatrixParameter locationsParameter, List<String> strains) {
        locationsParameter.setColumnDimension(mdsDimension);
        locationsParameter.setRowDimension(strains.size());
        for (int i = 0; i < strains.size(); i++) {
            locationsParameter.getParameter(i).setId(strains.get(i));
        }
    }


    @Override
    protected void handleModelChangedEvent(Model model, Object object, int index) {
    }

    @Override
    protected void handleVariableChangedEvent(Variable variable, int index, Variable.ChangeType type) {
        if (variable == locationsParameter) {
            locationChanged[index / mdsDimension] = true;
        } else if (variable == mdsPrecisionParameter) {
            setLocationChangedFlags(true);
        } else if (variable == columnEffectsParameter) {
            setLocationChangedFlags(true);
        } else if (variable == rowEffectsParameter) {
            setLocationChangedFlags(true);
        } else {
            // could be a derived class's parameter
//            throw new IllegalArgumentException("Unknown parameter");
        }
        likelihoodKnown = false;
    }

    @Override
    protected void storeState() {
        System.arraycopy(logLikelihoods, 0, storedLogLikelihoods, 0, logLikelihoods.length);
    }

    @Override
    protected void restoreState() {
        double[] tmp = logLikelihoods;
        logLikelihoods = storedLogLikelihoods;
        storedLogLikelihoods = tmp;

        likelihoodKnown = false;
    }

    @Override
    protected void acceptState() {
    }

    @Override
    public Model getModel() {
        return this;
    }

    @Override
    public double getLogLikelihood() {
        if (!likelihoodKnown) {
            logLikelihood = computeLogLikelihood();
        }

        return logLikelihood;
    }

    // This function can be overwritten to implement other sampling densities, i.e. discrete ranks
    private double computeLogLikelihood() {

        double precision = mdsPrecisionParameter.getParameterValue(0);
        double sd = 1.0 / Math.sqrt(precision);

        logLikelihood = 0.0;
        int i = 0;
        for (Measurement measurement : measurements) {

            if (locationChanged[measurement.rowStrain] || locationChanged[measurement.columnStrain]) {
                double distance = computeDistance(measurement.rowStrain, measurement.columnStrain);

                double logNormalization = calculateTruncationNormalization(distance, sd);

                switch (measurement.type) {
                    case INTERVAL: {
                        double minTitre = transformTitre(measurement.minTitre, measurement.column, measurement.row, distance, sd);
                        double maxTitre = transformTitre(measurement.maxTitre, measurement.column, measurement.row, distance, sd);
                        logLikelihoods[i] = computeMeasurementIntervalLikelihood(minTitre, maxTitre) - logNormalization;
                    } break;
                    case POINT: {
                        double titre = transformTitre(measurement.minTitre, measurement.column, measurement.row, distance, sd);
                        logLikelihoods[i] = computeMeasurementLikelihood(titre) - logNormalization;
                    } break;
                    case LOWER_BOUND: {
                        double minTitre = transformTitre(measurement.minTitre, measurement.column, measurement.row, distance, sd);
                        logLikelihoods[i] = computeMeasurementLowerBoundLikelihood(minTitre) - logNormalization;
                    } break;
                    case UPPER_BOUND: {
                        double maxTitre = transformTitre(measurement.maxTitre, measurement.column, measurement.row, distance, sd);
                        logLikelihoods[i] = computeMeasurementUpperBoundLikelihood(maxTitre) - logNormalization;
                    } break;
                    case MISSING:
                        break;
                }
            }
            logLikelihood += logLikelihoods[i];
            i++;
        }

        likelihoodKnown = true;

        setLocationChangedFlags(false);

        return logLikelihood;
    }

    private void setLocationChangedFlags(boolean flag) {
        for (int i = 0; i < locationChanged.length; i++) {
            locationChanged[i] = flag;
        }
    }

    protected double computeDistance(int rowStrain, int columnStrain) {
        if (rowStrain == columnStrain) {
            return 0.0;
        }

        Parameter X = locationsParameter.getParameter(rowStrain);
        Parameter Y = locationsParameter.getParameter(columnStrain);
        double sum = 0.0;
        for (int i = 0; i < mdsDimension; i++) {
            double difference = X.getParameterValue(i) - Y.getParameterValue(i);
            sum += difference * difference;
        }
        return Math.sqrt(sum);
    }

    /**
     * Transforms a titre into log2 space and normalizes it with respect to a unit normal
     * @param titre
     * @param column
     * @param row
     * @param mean
     * @param sd
     * @return
     */
    private double transformTitre(double titre, int column, int row, double mean, double sd) {
        double rowEffect = rowEffectsParameter.getParameterValue(row);
        double columnEffect = columnEffectsParameter.getParameterValue(column);

        //double t = ((rowEffect + columnEffect) * 0.5) - titre;
        double t = columnEffect - titre;
        return (t - mean) / sd;
    }

    private static double computeMeasurementIntervalLikelihood_CDF(double minTitre, double maxTitre) {
        // once transformed, the minTitre will be the greater value
        double cdf1 = NormalDistribution.standardCDF(minTitre, false);
        double cdf2 = NormalDistribution.standardCDF(maxTitre, false);

        double lnL = Math.log(cdf1 - cdf2);
        if (cdf1 == cdf2) {
            lnL = Math.log(cdf1);
        }
        if (CHECK_INFINITE && Double.isNaN(lnL) || Double.isInfinite(lnL)) {
            throw new RuntimeException("infinite");
        }
        return lnL;
    }

    private static double computeMeasurementIntervalLikelihood(double minTitre, double maxTitre) {
        // once transformed, the minTitre will be the greater value
        double cdf1 = NormalDistribution.standardTail(minTitre, true);
        double cdf2 = NormalDistribution.standardTail(maxTitre, true);

        double lnL = Math.log(cdf2 - cdf1);
        if (cdf1 == cdf2) {
            lnL = Math.log(cdf1);
        }
        if (CHECK_INFINITE && Double.isNaN(lnL) || Double.isInfinite(lnL)) {
            throw new RuntimeException("infinite");
        }
        return lnL;
    }

    private static double computeMeasurementLikelihood(double titre) {
        double lnL = NormalDistribution.logPdf(titre, 0.0, 1.0);
        if (CHECK_INFINITE && Double.isNaN(lnL) || Double.isInfinite(lnL)) {
            throw new RuntimeException("infinite");
        }
        return lnL;
    }

    private static double computeMeasurementLowerBoundLikelihood(double transformedMinTitre) {
        // a lower bound in non-transformed titre so the bottom tail of the distribution
        double cdf = NormalDistribution.standardTail(transformedMinTitre, false);
        double lnL = Math.log(cdf);
        if (CHECK_INFINITE && Double.isNaN(lnL) || Double.isInfinite(lnL)) {
            throw new RuntimeException("infinite");
        }
        return lnL;
    }

    private static double computeMeasurementUpperBoundLikelihood(double transformedMaxTitre) {
        // a upper bound in non-transformed titre so the upper tail of the distribution

        // using special tail function of NormalDistribution (see main() in NormalDistribution for test)
        double tail = NormalDistribution.standardTail(transformedMaxTitre, true);
        double lnL = Math.log(tail);
        if (CHECK_INFINITE && Double.isNaN(lnL) || Double.isInfinite(lnL)) {
            throw new RuntimeException("infinite");
        }
        return lnL;
    }

    private static double calculateTruncationNormalization(double distance, double sd) {
        return NormalDistribution.cdf(distance, 0.0, sd, true);
    }

    @Override
    public void makeDirty() {
        likelihoodKnown = false;
        setLocationChangedFlags(true);
    }

    private class Measurement {
        private Measurement(final int column, final int columnStrain, final int row, final int rowStrain, final MeasurementType type, final double minTitre, final double maxTitre) {
            this.column = column;
            this.columnStrain = columnStrain;
            this.row = row;
            this.rowStrain = rowStrain;

            this.type = type;
            this.minTitre = Math.log(minTitre) / Math.log(2);
            this.maxTitre = Math.log(maxTitre) / Math.log(2);
        }

        final int column;
        final int row;
        final int columnStrain;
        final int rowStrain;

        final MeasurementType type;
        final double minTitre;
        final double maxTitre;

    };

    private final List<Measurement> measurements = new ArrayList<Measurement>();
    private final List<String> columnLabels = new ArrayList<String>();
    private final List<String> rowLabels = new ArrayList<String>();


    private final int mdsDimension;
    private final Parameter mdsPrecisionParameter;
    private final MatrixParameter locationsParameter;
    private final TaxonList strains;
    //    private final CompoundParameter tipTraitParameter;
    private final Parameter columnEffectsParameter;
    private final Parameter rowEffectsParameter;

    private double logLikelihood = 0.0;
    private boolean likelihoodKnown = false;

    private final boolean[] locationChanged;
    private double[] logLikelihoods;
    private double[] storedLogLikelihoods;

    // **************************************************************
    // XMLObjectParser
    // **************************************************************

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {
        public final static String FILE_NAME = "fileName";

        public final static String TIP_TRAIT = "tipTrait";
        public final static String LOCATIONS = "locations";
        public final static String DATES = "dates";
        public static final String MDS_DIMENSION = "mdsDimension";
        public static final String MDS_PRECISION = "mdsPrecision";
        public static final String COLUMN_EFFECTS = "columnEffects";
        public static final String ROW_EFFECTS = "rowEffects";

        public static final String STRAINS = "strains";

        public String getParserName() {
            return ANTIGENIC_LIKELIHOOD;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            String fileName = xo.getStringAttribute(FILE_NAME);
            DataTable<String[]> assayTable;
            try {
                assayTable = DataTable.Text.parse(new FileReader(fileName), true, false);
            } catch (IOException e) {
                throw new XMLParseException("Unable to read assay data from file: " + e.getMessage());
            }

            int mdsDimension = xo.getIntegerAttribute(MDS_DIMENSION);

//            CompoundParameter tipTraitParameter = null;
//            if (xo.hasChildNamed(TIP_TRAIT)) {
//                tipTraitParameter = (CompoundParameter) xo.getElementFirstChild(TIP_TRAIT);
//            }

            TaxonList strains = null;
            if (xo.hasChildNamed(STRAINS)) {
                strains = (TaxonList) xo.getElementFirstChild(STRAINS);
            }

            MatrixParameter locationsParameter = (MatrixParameter) xo.getElementFirstChild(LOCATIONS);

            Parameter datesParameter = null;
            if (xo.hasChildNamed(DATES)) {
                datesParameter = (Parameter) xo.getElementFirstChild(DATES);
            }

            Parameter mdsPrecision = (Parameter) xo.getElementFirstChild(MDS_PRECISION);

            Parameter columnEffectsParameter = (Parameter) xo.getElementFirstChild(COLUMN_EFFECTS);

            Parameter rowEffectsParameter = (Parameter) xo.getElementFirstChild(ROW_EFFECTS);

            AntigenicLikelihood AGL = new AntigenicLikelihood(
                    mdsDimension,
                    mdsPrecision,
                    strains,
                    locationsParameter,
                    datesParameter,
                    columnEffectsParameter,
                    rowEffectsParameter,
                    assayTable,
                    null);

            Logger.getLogger("dr.evomodel").info("Using EvolutionaryCartography model. Please cite:\n" + Utils.getCitationString(AGL));

            return AGL;
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "Provides the likelihood of immunological assay data such as Hemagglutinin inhibition (HI) given vectors of coordinates" +
                    "for viruses and sera/antisera in some multidimensional 'antigenic' space.";
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newStringRule(FILE_NAME, false, "The name of the file containing the assay table"),
                AttributeRule.newIntegerRule(MDS_DIMENSION, false, "The dimension of the space for MDS"),
                new ElementRule(STRAINS, TaxonList.class, "A taxon list of strains", true),
//                new ElementRule(TIP_TRAIT, CompoundParameter.class, "The parameter of tip locations from the tree", true),
                new ElementRule(LOCATIONS, MatrixParameter.class),
                new ElementRule(DATES, Parameter.class, "An optional parameter for strain dates to be stored", true),
                new ElementRule(COLUMN_EFFECTS, Parameter.class),
                new ElementRule(ROW_EFFECTS, Parameter.class),
                new ElementRule(MDS_PRECISION, Parameter.class)
        };

        public Class getReturnType() {
            return ContinuousAntigenicTraitLikelihood.class;
        }
    };

    public List<Citation> getCitations() {
        List<Citation> citations = new ArrayList<Citation>();
        citations.add(new Citation(
                new Author[]{
                        new Author("T", "Bedford"),
                        new Author("MA", "Suchard"),
                        new Author("P", "Lemey"),
                        new Author("G", "Dudas"),
                        new Author("C", "Russell"),
                        new Author("D", "Smith"),
                        new Author("A", "Rambaut")
                },
                Citation.Status.IN_PREPARATION
        ));
        return citations;
    }

    public static void main(String[] args) {
        double[] titres = {0.0, 2.0, 4.0, 6.0, 8.0, 10.0, 12.0, 14.0};

        System.out.println("titre\tpoint\tinterval(tail)\tinterval(cdf)\tlower\tupper");
        for (double titre : titres) {
            double point = AntigenicLikelihood.computeMeasurementLikelihood(titre);
            double interval = AntigenicLikelihood.computeMeasurementIntervalLikelihood(titre + 1.0, titre);
            double interval2 = AntigenicLikelihood.computeMeasurementIntervalLikelihood_CDF(titre + 1.0, titre);
            double lower = AntigenicLikelihood.computeMeasurementLowerBoundLikelihood(titre);
            double upper = AntigenicLikelihood.computeMeasurementUpperBoundLikelihood(titre);

            System.out.println(titre + "\t" + point + "\t" + interval + "\t" + interval2 + "\t" + lower + "\t" + upper);
      }
    }
}

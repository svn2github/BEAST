package dr.evomodel.continuous;

import dr.inference.model.*;
import dr.math.distributions.NormalDistribution;
import dr.util.DataTable;
import dr.xml.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author Andrew Rambaut
 * @author Marc Suchard
 * @version $Id$
 */
public class MultidimensionalScalingLikelihood extends AbstractModelLikelihood {


    public enum ObservationType {
        POINT,
        UPPER_BOUND,
        LOWER_BOUND,
        MISSING
    }

    public final static String MULTIDIMENSIONAL_SCALING_LIKELIHOOD = "multidimensionalScalingLikelihood";

    public MultidimensionalScalingLikelihood(String name) {

        super(name);
    }

    /**
     * A simple constructor for a fully specified symmetrical data matrix
     * @param mdsDimension
     * @param mdsPrecision
     * @param locationsParameter
     * @param dataTable
     */
    public MultidimensionalScalingLikelihood(
            int mdsDimension,
            Parameter mdsPrecision,
            MatrixParameter locationsParameter,
            DataTable<double[]> dataTable) {

        super(MULTIDIMENSIONAL_SCALING_LIKELIHOOD);

        // construct a compact data table
        String[] rowLabels = dataTable.getRowLabels();
        String[] columnLabels = dataTable.getRowLabels();

        int rowCount = dataTable.getRowCount();
        int observationCount = ((rowCount - 1) * rowCount) / 2;
        double[] observations = new double[observationCount];
        ObservationType[] observationTypes = new ObservationType[observationCount];
        int[] distanceIndices = new int[observationCount];
        int[] rowLocationIndices = new int[observationCount];
        int[] columnLocationIndices = new int[observationCount];

        int u = 0;
        for (int i = 0; i < rowCount; i++) {

            double[] dataRow = dataTable.getRow(i);

            for (int j = i + 1; j < rowCount; j++) {
                observations[u] = dataRow[j];
                observationTypes[u] = ObservationType.POINT;
                distanceIndices[u] = u;
                rowLocationIndices[u] = i;
                columnLocationIndices[u] = j;
                u++;
            }

        }

        initialize(mdsDimension, mdsPrecision, locationsParameter, rowLabels, observations, observationTypes, distanceIndices, rowLocationIndices, columnLocationIndices);
    }

    protected void initialize(
            final int mdsDimension,
            final Parameter mdsPrecision,
            final MatrixParameter locationsParameter,
            final String[] locationLabels,
            final double[] observations,
            final ObservationType[] observationTypes,
            final int[] distanceIndices,
            final int[] rowLocationIndices,
            final int[] columnLocationIndices) {

        this.mdsDimension = mdsDimension;

        locationCount = locationLabels.length;

        this.locationLabels = locationLabels;

        this.observations = observations;
        this.observationTypes = observationTypes;
        this.distanceIndices = distanceIndices;
        this.rowLocationIndices = rowLocationIndices;
        this.columnLocationIndices = columnLocationIndices;

        this.distancesCount = rowLocationIndices.length;
        this.observationCount = observations.length;
        this.upperThresholdCount = 0;
        this.lowerThresholdCount = 0;

        for (ObservationType type : observationTypes) {
            upperThresholdCount += (type == ObservationType.UPPER_BOUND ? 1 : 0);
            lowerThresholdCount += (type == ObservationType.LOWER_BOUND ? 1 : 0);
        }

        thresholdCount = upperThresholdCount + lowerThresholdCount;
        pointObservationCount = observationCount - thresholdCount;

        upperThresholdIndices = new int[upperThresholdCount];
        lowerThresholdIndices = new int[lowerThresholdCount];
        pointObservationIndices = new int[pointObservationCount];

        int ut = 0;
        int lt = 0;
        int po = 0;
        for (int i = 0; i < observationCount; i++) {
            switch (observationTypes[i]) {
                case POINT:
                    pointObservationIndices[po] = i;
                    po++;
                    break;
                case UPPER_BOUND:
                    upperThresholdIndices[ut] = i;
                    ut++;
                    break;
                case LOWER_BOUND:
                    lowerThresholdIndices[lt] = i;
                    lt++;
                    break;
            }
        }

        this.locationsParameter = locationsParameter;
        setupLocationsParameter(this.locationsParameter);
        addVariable(locationsParameter);
        locationUpdated = new boolean[locationsParameter.getParameterCount()];

        // a cache of row to column distances (column indices given by array above).
        distances = new double[distancesCount];
        storedDistances = new double[distancesCount];
        distanceUpdate = new boolean[distancesCount];

        // a cache of individual truncations
        truncations = new double[distancesCount];
        storedTruncations = new double[distancesCount];

        // a cache of threshold calcs
        thresholds = new double[thresholdCount];
        storedThresholds = new double[thresholdCount];

        this.mdsPrecisionParameter = mdsPrecision;
        addVariable(mdsPrecision);

        this.isLeftTruncated = true; // Re-normalize likelihood for strictly positive distances

        // make sure everything is calculated on first evaluation
        makeDirty();
    }

    protected void setupLocationsParameter(MatrixParameter locationsParameter) {
        locationsParameter.setColumnDimension(mdsDimension);
        locationsParameter.setRowDimension(locationCount);
        for (int i = 0; i < locationLabels.length; i++) {
            locationsParameter.getParameter(i).setId(locationLabels[i]);
        }
    }

    @Override
    protected void handleModelChangedEvent(Model model, Object object, int index) {
    }

    @Override
    protected void handleVariableChangedEvent(Variable variable, int index, Variable.ChangeType type) {
        // TODO Flag which cachedDistances or mdsPrecision need to be updated

        if (variable == locationsParameter) {
            int location = index / mdsDimension;
            int dim = index % mdsDimension;

            locationUpdated[location] = true;
            distancesKnown = false;
            thresholdsKnown = false;
            truncationKnown = false;
        } else if (variable == mdsPrecisionParameter) {
            for (int i = 0; i < distanceUpdate.length; i++) {
                distanceUpdate[i] = true;
            }
            thresholdsKnown = false;
            truncationKnown = false;
        } else {
            // could be a derived class's parameter
//            throw new IllegalArgumentException("Unknown parameter");
        }

        likelihoodKnown = false;
    }

    @Override
    protected void storeState() {
        System.arraycopy(distances, 0, storedDistances, 0, distances.length);
        System.arraycopy(truncations, 0, storedTruncations, 0, truncations.length);
        System.arraycopy(thresholds, 0, storedThresholds, 0, thresholds.length);

        storedLogLikelihood = logLikelihood;
        storedTruncationSum = truncationSum;
        storedThresholdSum = thresholdSum;
        storedSumOfSquaredResiduals = sumOfSquaredResiduals;
    }

    @Override
    protected void restoreState() {
        double[] tmp = storedDistances;
        storedDistances = distances;
        distances = tmp;
        distancesKnown = true;

        tmp = storedTruncations;
        storedTruncations = truncations;
        truncations = tmp;

        tmp = storedThresholds;
        storedThresholds = thresholds;
        thresholds = tmp;

        logLikelihood = storedLogLikelihood;
        likelihoodKnown = true;

        truncationSum = storedTruncationSum;
        truncationKnown = true;

        thresholdSum = storedThresholdSum;
        thresholdsKnown = true;

        sumOfSquaredResiduals = storedSumOfSquaredResiduals;
    }

    @Override
    protected void acceptState() {
        // do nothing
    }

    public void makeDirty() {
        distancesKnown = false;
        likelihoodKnown = false;
        truncationKnown = false;
        thresholdsKnown = false;

        for (int i = 0; i < locationUpdated.length; i++) {
            locationUpdated[i] = true;
        }
        for (int i = 0; i < distanceUpdate.length; i++) {
            distanceUpdate[i] = true;
        }
    }

    public Model getModel() {
        return this;
    }

    public double getLogLikelihood() {
        if (!likelihoodKnown) {
            if (!distancesKnown) {
                calculateDistances();
                sumOfSquaredResiduals = calculateSumOfSquaredResiduals();
                distancesKnown = true;
            }

            logLikelihood = computeLogLikelihood();
            likelihoodKnown = true;
        }

        for (int i = 0; i < locationUpdated.length; i++) {
            locationUpdated[i] = false;
        }
        for (int i = 0; i < distanceUpdate.length; i++) {
            distanceUpdate[i] = false;
        }

        // To override all the caching:
//        calculateDistances();
//        sumOfSquaredResiduals = calculateSumOfSquaredResiduals();
//        logLikelihood = computeLogLikelihood();

        return logLikelihood;
    }

    // This function can be overwritten to implement other sampling densities, i.e. discrete ranks
    protected double computeLogLikelihood() {

        double precision = mdsPrecisionParameter.getParameterValue(0);

        // totalNonMissingCount should be totalObservedCount (not > or < threshold)
        double logLikelihood = (pointObservationCount / 2) * Math.log(precision) - 0.5 * precision * sumOfSquaredResiduals;

        if (thresholdCount > 0) {
            if (!thresholdsKnown) {
                thresholdSum = calculateThresholdObservations(precision);
                thresholdsKnown = true;
            }
            logLikelihood += thresholdSum;
        }

        if (isLeftTruncated) {
            if (!truncationKnown) {
                truncationSum = calculateTruncation(precision);
                truncationKnown = true;
            }
            logLikelihood -= truncationSum;
        }

        return logLikelihood;
    }

    protected double calculateThresholdObservations(double precision) {
        double sum = 0.0;
        double sd = 1.0 / Math.sqrt(precision);
        int j = 0;
        for (int i = 0; i < upperThresholdCount; i++) {
            int observationIndex = upperThresholdIndices[i];
            int dist = distanceIndices[observationIndex];
            if (distanceUpdate[dist]) {
//                double cdf = NormalDistribution.cdf(observations[observationIndex], distances[dist], sd, false);
//                double tail = 1.0 - cdf;
                // using special tail function of NormalDistribution (see main() in NormalDistribution for test)
                double tail = NormalDistribution.tailCDF(observations[observationIndex], distances[dist], sd);
                thresholds[j] = Math.log(tail);
            }
            if (Double.isInfinite(thresholds[j])) {
                System.out.println("Error calculation threshold probability");
                System.out.println("observationIndex: " + observationIndex);
                System.out.println("observation: " + observations[observationIndex]);
                System.out.println("distanceIndex: " + dist);
                System.out.println("distance: " + distances[dist]);
                System.out.println("row: " + rowLocationIndices[dist] + " (" + locationLabels[getLocationIndex(rowLocationIndices[dist])] + ")");
                System.out.println("col: " + columnLocationIndices[dist] + " (" + locationLabels[getLocationIndex(columnLocationIndices[dist])] + ")");
                System.out.println();
            }
            sum += thresholds[j];
            j++;
        }
        for (int i = 0; i < lowerThresholdCount; i++) {
            int observationIndex = upperThresholdIndices[i];
            int dist = distanceIndices[observationIndex];
            if (distanceUpdate[dist]) {
                thresholds[j] = NormalDistribution.cdf(observations[observationIndex], distances[dist], sd, true);
            }
            sum += thresholds[j];
            j++;
        }

        return sum;
    }

    protected double calculateTruncation(double precision) {
        double sum = 0.0;
        double sd = 1.0 / Math.sqrt(precision);
        for (int i = 0; i < observationCount; i++) {
            int dist = distanceIndices[i];
            if (distanceUpdate[dist]) {
                truncations[dist] = NormalDistribution.cdf(distances[dist], 0.0, sd, true);
            }
            sum += truncations[dist];
        }
        return sum;
    }

    protected double calculateSumOfSquaredResiduals() {
        double sum = 0.0;
        for (int i = 0; i < observationCount; i++) {
            if (observationTypes[i] == ObservationType.POINT) {
                // Only increment sum if dataTable[i][j] is observed (not > or < threshold)
                double residual = distances[distanceIndices[i]] - observations[i];
                sum += residual * residual;
            }
        }
        return sum;
    }

    protected void calculateDistances() {
        for (int i = 0; i < distancesCount; i++) {
            int x = getLocationIndex(rowLocationIndices[i]);
            int y = getLocationIndex(columnLocationIndices[i]);
            if (locationUpdated[x] || locationUpdated[y]) {
                distances[i] = calculateDistance(
                        locationsParameter.getParameter(x),
                        locationsParameter.getParameter(y));
                distanceUpdate[i] = true;
            }
        }
    }

    /**
     *     Returns the index of the  given by index
     *     Overriding this allows the clustering of locations
     * @param index
     * @return
     */
    protected int getLocationIndex(int index) {
        return index;
    }

    public String[] getLocationLabels() {
        return locationLabels;
    }

    protected double calculateDistance(Parameter X, Parameter Y) {
        double sum = 0.0;
        for (int i = 0; i < mdsDimension; i++) {
            double difference = X.getParameterValue(i) - Y.getParameterValue(i);
            sum += difference * difference;
        }
        return Math.sqrt(sum);
    }

    public int getMDSDimension() {
        return mdsDimension;
    }

    public int getLocationCount() {
        return locationCount;
    }

    public MatrixParameter getLocationsParameter() {
        return locationsParameter;
    }

    // **************************************************************
    // XMLObjectParser
    // **************************************************************

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {
        public final static String FILE_NAME = "fileName";

        public final static String TIP_TRAIT = "tipTrait";
        public final static String LOCATIONS = "locations";
        public static final String MDS_DIMENSION = "mdsDimension";
        public static final String MDS_PRECISION = "mdsPrecision";

        public String getParserName() {
            return MULTIDIMENSIONAL_SCALING_LIKELIHOOD;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            String fileName = xo.getStringAttribute(FILE_NAME);
            DataTable<double[]> distanceTable;
            try {
                distanceTable = DataTable.Double.parse(new FileReader(fileName));
            } catch (IOException e) {
                throw new XMLParseException("Unable to read assay data from file, " + fileName);
            }

            if (distanceTable.getRowCount() != distanceTable.getColumnCount()) {
                throw new XMLParseException("Data table is not symmetrical.");
            }

            int mdsDimension = xo.getIntegerAttribute(MDS_DIMENSION);

            MatrixParameter locationsParameter = (MatrixParameter) xo.getElementFirstChild(LOCATIONS);

            Parameter mdsPrecision = (Parameter) xo.getElementFirstChild(MDS_PRECISION);

            return new MultidimensionalScalingLikelihood(mdsDimension, mdsPrecision, locationsParameter, distanceTable);
        }

        //************************************************************************
        // AbstractXMLObjectParser implementation
        //************************************************************************

        public String getParserDescription() {
            return "Provides the likelihood of pairwise distance given vectors of coordinates" +
                    "for points according to the multidimensional scaling scheme of XXX & Rafferty (xxxx).";
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newStringRule(FILE_NAME, false, "The name of the file containing the assay table"),
                AttributeRule.newIntegerRule(MDS_DIMENSION, false, "The dimension of the space for MDS"),
                new ElementRule(LOCATIONS, MatrixParameter.class),
                new ElementRule(MDS_PRECISION, Parameter.class)
        };

        public Class getReturnType() {
            return MultidimensionalScalingLikelihood.class;
        }
    };

    private int distancesCount;
    private int observationCount;
    private int upperThresholdCount;
    private int lowerThresholdCount;
    private int pointObservationCount;
    private int thresholdCount;


    private String[] locationLabels;

    private int locationCount;

    private double[] observations;
    private ObservationType[] observationTypes;
    protected int[] distanceIndices;
    private int[] rowLocationIndices;
    private int[] columnLocationIndices;
    private int[] upperThresholdIndices;
    private int[] lowerThresholdIndices;
    private int[] pointObservationIndices;

    private MatrixParameter locationsParameter;
    private Parameter mdsPrecisionParameter;

    private boolean likelihoodKnown = false;
    private double logLikelihood;
    private double storedLogLikelihood;

    protected boolean distancesKnown = false;
    private double sumOfSquaredResiduals;
    private double storedSumOfSquaredResiduals;
    private double[] distances;
    private double[] storedDistances;

    protected boolean[] locationUpdated;
    protected boolean[] distanceUpdate;

    protected boolean truncationKnown = false;
    private double truncationSum;
    private double storedTruncationSum;
    private double[] truncations;
    private double[] storedTruncations;

    protected boolean thresholdsKnown = false;
    private double thresholdSum;
    private double storedThresholdSum;
    private double[] thresholds;
    private double[] storedThresholds;

    private boolean isLeftTruncated;
    private int mdsDimension;
}

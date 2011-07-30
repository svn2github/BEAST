package dr.evomodel.continuous;

import dr.inference.model.*;
import dr.math.MathUtils;
import dr.math.distributions.NormalDistribution;
import dr.util.Author;
import dr.util.Citable;
import dr.util.Citation;
import dr.util.DataTable;
import dr.xml.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Andrew Rambaut
 * @author Marc Suchard
 * @version $Id$
 */
public class AntigenicTraitLikelihood extends MultidimensionalScalingLikelihood implements Citable {

    public final static String ANTIGENIC_TRAIT_LIKELIHOOD = "antigenicTraitLikelihood";

    /**
     * Constructor
     * @param mdsDimension dimension of the mds space
     * @param mdsPrecision parameter which gives the precision of the bmds
     * @param tipTraitParameter a parameter of locations for the tips of the tree (mapped to virus locations)
     * @param locationsParameter a parameter of locations of viruses/sera
     * @param dataTable the assay table (virus in rows, serum assays in columns)
     * @param virusAntiserumMap a map of viruses to corresponding sera
     * @param assayAntiserumMap a map of repeated assays for a given sera
     * @param log2Transform transform the data into log 2 space
     */
    public AntigenicTraitLikelihood(
            int mdsDimension,
            Parameter mdsPrecision,
            CompoundParameter tipTraitParameter,
            MatrixParameter locationsParameter,
            DataTable<String[]> dataTable,
            Map<String, String> virusAntiserumMap,
            Map<String, String> assayAntiserumMap,
            final boolean log2Transform) {

        super(ANTIGENIC_TRAIT_LIKELIHOOD);

        Logger.getLogger("dr.evomodel").info("Using EvolutionaryCartography model. Please cite:\n" + Citable.Utils.getCitationString(this));

        String[] virusNames = dataTable.getRowLabels();
        String[] assayNames = dataTable.getColumnLabels();

        // the total number of viruses is the number of rows in the table
        int virusCount = dataTable.getRowCount();
        int assayCount = dataTable.getColumnCount();

        int[] assayToSerumIndices = new int[assayNames.length];

        double[][] observationValueTable = new double[virusCount][assayCount];
        ObservationType[][] observationTypeTable = new ObservationType[virusCount][assayCount];

        initalizeTable(dataTable, observationValueTable, observationTypeTable, log2Transform);

        // locations are either viruses or sera (or both)
        List<String> locationNamesList = new ArrayList<String>();
        int[] virusToLocationIndices = new int[virusCount];
        int count = 0;
        for (String virusName : virusNames) {
            String name = null;

            if (virusAntiserumMap != null) {
                name = virusAntiserumMap.get(virusName);
            }

            if (name == null) {
                name = virusName;
            }

            virusToLocationIndices[count] = locationNamesList.size();
            locationNamesList.add(name);
            count++;
        }

        List<String> serumNamesList = new ArrayList<String>();
        count = 0;
        for (String assayName : assayNames) {
            String name = null;

            if (assayAntiserumMap != null) {
                name = assayAntiserumMap.get(assayName);
            }

            if (name == null) {
                name = assayName;
            }

            int index = serumNamesList.indexOf(name);
            if (index == -1) {
                index = serumNamesList.size();
                serumNamesList.add(name);
            }
            assayToSerumIndices[count] = index;
            count++;
        }
        String[] serumNames = new String[serumNamesList.size()];
        serumNamesList.toArray(serumNames);

        int serumCount = serumNames.length;
        int[] serumToLocationIndices = new int[serumCount];
        count = 0;
        for (String serumName : serumNames) {
            int index = locationNamesList.indexOf(serumName);
            if (index == -1) {
                index = locationNamesList.size();
                locationNamesList.add(serumName);
            }
            serumToLocationIndices[count] = index;
            count++;
        }

        String[] locationNames = new String[locationNamesList.size()];
        locationNamesList.toArray(locationNames);


        List<Double> observationList = new ArrayList<Double>();
        List<Integer> distanceIndexList = new ArrayList<Integer>();
        List<ObservationType> observationTypeList = new ArrayList<ObservationType>();

        int[] virusObservationCounts = new int[virusCount];
        int[] serumObservationCounts = new int[serumCount];

        List<Pair> locationPairs = new ArrayList<Pair>();
        // Build a sparse matrix of non-missing assay values
        for (int i = 0; i < virusCount; i++) {

            for (int j = 0; j < assayCount; j++) {
                int k = assayToSerumIndices[j];

                Double value = observationValueTable[i][j];
                ObservationType type = observationTypeTable[i][j];

                if (type != ObservationType.MISSING) {
                    observationList.add(value);
                    observationTypeList.add(type);

                    Pair pair = new Pair(virusToLocationIndices[i], serumToLocationIndices[k]);
                    int index = locationPairs.indexOf(pair);
                    if (index == -1) {
                        index = locationPairs.size();
                        locationPairs.add(pair);
                    }

                    distanceIndexList.add(index);

                    virusObservationCounts[i]++;
                    serumObservationCounts[k]++;
                }
            }
        }

        // check that all the viruses and sera have observations
        for (int i = 0; i < virusCount; i++) {
            if (virusObservationCounts[i] == 0) {
                System.err.println("WARNING: Virus " + virusNames[i] + " has 0 observations");
            }
        }
        for (int j = 0; j < serumCount; j++) {
            if (serumObservationCounts[j] == 0) {
                System.err.println("WARNING: Antisera " + serumNames[j] + " has 0 observations");
            }
        }

        // Convert into arrays
        double[] observations = new double[observationList.size()];
        for (int i = 0; i < observationList.size(); i++) {
            observations[i] = observationList.get(i);
        }

        int[] distanceIndices = new int[distanceIndexList.size()];
        for (int i = 0; i < distanceIndexList.size(); i++) {
            distanceIndices[i] = distanceIndexList.get(i);
        }

        int[] rowLocationIndices = new int[locationPairs.size()];
        for (int i = 0; i < rowLocationIndices.length; i++) {
            rowLocationIndices[i] = locationPairs.get(i).location1;
        }

        int[] columnLocationIndices = new int[locationPairs.size()];
        for (int i = 0; i < columnLocationIndices.length; i++) {
            columnLocationIndices[i] = locationPairs.get(i).location2;
        }

        ObservationType[] observationTypes = new ObservationType[observationTypeList.size()];
        observationTypeList.toArray(observationTypes);

        int thresholdCount = 0;
        for (int i = 0; i < observations.length; i++) {
            thresholdCount += (observationTypes[i] != ObservationType.POINT ? 1 : 0);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\tAntigenicTraitLikelihood:\n");
        sb.append("\t\t" + virusNames.length + " viruses\n");
        sb.append("\t\t" + assayNames.length + " assays\n");
        sb.append("\t\t" + serumNames.length + " antisera\n");
        sb.append("\t\t" + locationNames.length + " locations\n");
        sb.append("\t\t" + locationPairs.size() + " distances\n");
        sb.append("\t\t" + observations.length + " observations\n");
        sb.append("\t\t" + thresholdCount + " threshold observations\n");
        Logger.getLogger("dr.evomodel").info(sb.toString());

        initialize(
                mdsDimension,
                mdsPrecision,
                tipTraitParameter,
                locationsParameter,
                locationNames,
                observations,
                observationTypes,
                distanceIndices,
                rowLocationIndices,
                columnLocationIndices);

        // some random initial locations
        for (int i = 0; i < locationNames.length; i++) {
            locationsParameter.getParameter(i).setId(locationNames[i]);
            for (int j = 0; j < mdsDimension; j++) {
                double r = MathUtils.nextGaussian();
                locationsParameter.getParameter(i).setParameterValue(j, r);

//                locationsParameter.getParameter(i).setParameterValue(j, i * 1000);
            }
        }


    }

    private void initalizeTable(DataTable<String[]> dataTable, double[][] observationValueTable, ObservationType[][] observationTypeTable, boolean log2Transform) {
        // the largest measured value for any given column of data
        double[] maxColumnValue = new double[dataTable.getColumnCount()];
        // the largest measured value over all
        double maxAssayValue = 0.0;

        for (int i = 0; i < dataTable.getRowCount(); i++) {
            String[] dataRow = dataTable.getRow(i);

            for (int j = 0; j < dataTable.getColumnCount(); j++) {
                Double value = null;
                ObservationType type = null;

                if (dataRow[j].startsWith("<")) {
                    // is a lower bound
                    value = convertString(dataRow[j].substring(1));
                    if (Double.isNaN(value)) {
                        throw new RuntimeException("Illegal value in table as a threshold");
                    }
                    type = ObservationType.LOWER_BOUND;
                } else if (dataRow[j].startsWith(">")) {
                    // is a lower bound
                    value = convertString(dataRow[j].substring(1));
                    if (Double.isNaN(value)) {
                        throw new RuntimeException("Illegal value in table as a threshold");
                    }
                    type = ObservationType.UPPER_BOUND;
                } else {
                    value = convertString(dataRow[j]);
                    type = Double.isNaN(value) ? ObservationType.MISSING : ObservationType.POINT;
                }

                observationValueTable[i][j] = value;
                observationTypeTable[i][j] = type;

                if (!Double.isNaN(value)) {
                    if (value > maxColumnValue[j]) {
                        maxColumnValue[j] = value;
                    }
                    if (value > maxAssayValue) {
                        maxAssayValue = value;
                    }

                }

            }
        }

        if (log2Transform) {
            // transform and normalize the data...
            for (int i = 0; i < dataTable.getRowCount(); i++) {
                for (int j = 0; j < dataTable.getColumnCount(); j++) {
                    observationValueTable[i][j] = transform(observationValueTable[i][j], maxColumnValue[j], 2);
                    // the transformation reverses the bounds
                    if (observationTypeTable[i][j] == ObservationType.UPPER_BOUND) {
                        observationTypeTable[i][j] = ObservationType.LOWER_BOUND;
                    } else if (observationTypeTable[i][j] == ObservationType.LOWER_BOUND) {
                        observationTypeTable[i][j] = ObservationType.UPPER_BOUND;
                    }
                }
            }
        }
    }

    private double convertString(String value) {
        try {
            return java.lang.Double.valueOf(value);
        } catch (NumberFormatException nfe) {
            return java.lang.Double.NaN;
        }
    }

    private double transform(final double value, final double maxValue, final double base) {
        // log2(maxValue / value)
        return (Math.log(maxValue) - Math.log(value)) / Math.log(base);
    }

    class Pair {
        Pair(final int location1, final int location2) {
            this.location1 = location1;
            this.location2 = location2;
        }

        int location1;
        int location2;

        @Override
        public boolean equals(final Object o) {
            return ((Pair)o).location1 == location1 && ((Pair)o).location2 == location2;
        }
    };

    // **************************************************************
    // XMLObjectParser
    // **************************************************************

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {
        public final static String FILE_NAME = "fileName";
        public final static String VIRUS_MAP_FILE_NAME = "virusMapFile";
        public final static String ASSAY_MAP_FILE_NAME = "assayMapFile";

        public final static String TIP_TRAIT = "tipTrait";
        public final static String LOCATIONS = "locations";
        public static final String MDS_DIMENSION = "mdsDimension";
        public static final String MDS_PRECISION = "mdsPrecision";

        public static final String LOG_2_TRANSFORM = "log2Transform";
        public static final String TITRATION_THRESHOLD = "titrationThreshold";

        public String getParserName() {
            return ANTIGENIC_TRAIT_LIKELIHOOD;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            String fileName = xo.getStringAttribute(FILE_NAME);
            DataTable<String[]> assayTable;
            try {
                assayTable = DataTable.Text.parse(new FileReader(fileName));
            } catch (IOException e) {
                throw new XMLParseException("Unable to read assay data from file, " + fileName);
            }

            Map<String, String> virusAntiserumMap = null;
            if (xo.hasAttribute(VIRUS_MAP_FILE_NAME)) {
                try {
                    virusAntiserumMap = readMap(xo.getStringAttribute(VIRUS_MAP_FILE_NAME));
                } catch (IOException e) {
                    throw new XMLParseException("Virus map file not found: " + xo.getStringAttribute(VIRUS_MAP_FILE_NAME));
                }
            }

            Map<String, String> assayAntiserumMap = null;
            if (xo.hasAttribute(ASSAY_MAP_FILE_NAME)) {
                try {
                    assayAntiserumMap = readMap(xo.getStringAttribute(ASSAY_MAP_FILE_NAME));
                } catch (IOException e) {
                    throw new XMLParseException("Assay map file not found: " + xo.getStringAttribute(ASSAY_MAP_FILE_NAME));
                }
            }

            int mdsDimension = xo.getIntegerAttribute(MDS_DIMENSION);

            boolean log2Transform = false;
            if (xo.hasAttribute(LOG_2_TRANSFORM)) {
                log2Transform = xo.getBooleanAttribute(LOG_2_TRANSFORM);
            }

            // This parameter needs to be linked to the one in the IntegratedMultivariateTreeLikelihood (I suggest that the parameter is created
            // here and then a reference passed to IMTL - which optionally takes the parameter of tip trait values, in which case it listens and
            // updates accordingly.
            CompoundParameter tipTraitParameter = null;
            if (xo.hasChildNamed(TIP_TRAIT)) {
                tipTraitParameter = (CompoundParameter) xo.getElementFirstChild(TIP_TRAIT);
            }

            MatrixParameter locationsParameter = (MatrixParameter) xo.getElementFirstChild(LOCATIONS);

            Parameter mdsPrecision = (Parameter) xo.getElementFirstChild(MDS_PRECISION);

            return new AntigenicTraitLikelihood(mdsDimension, mdsPrecision, tipTraitParameter, locationsParameter, assayTable, virusAntiserumMap, assayAntiserumMap, log2Transform);
        }

        private  Map<String, String> readMap(String fileName) throws IOException {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));

            Map<String, String> map = new HashMap<String, String>();

            String line = reader.readLine();
            while (line != null) {
                if (line.trim().length() > 0) {
                    String[] parts = line.split("\t");
                    if (parts.length > 1) {
                        map.put(parts[0], parts[1]);
                    }
                }
                line = reader.readLine();
            }

            reader.close();

            return map;
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
                AttributeRule.newStringRule(VIRUS_MAP_FILE_NAME, true, "The name of the file containing the virus to serum map"),
                AttributeRule.newStringRule(ASSAY_MAP_FILE_NAME, true, "The name of the file containing the assay to serum map"),
                AttributeRule.newIntegerRule(MDS_DIMENSION, false, "The dimension of the space for MDS"),
                AttributeRule.newBooleanRule(LOG_2_TRANSFORM, true, "Whether to log2 transform the data"),
                new ElementRule(TIP_TRAIT, CompoundParameter.class, "The parameter of tip locations from the tree", true),
                new ElementRule(LOCATIONS, MatrixParameter.class),
                new ElementRule(MDS_PRECISION, Parameter.class)
        };

        public Class getReturnType() {
            return AntigenicTraitLikelihood.class;
        }
    };

    public List<Citation> getCitations() {
        List<Citation> citations = new ArrayList<Citation>();
        citations.add(new Citation(
                new Author[]{
                        new Author("A", "Rambaut"),
                        new Author("T", "Bedford"),
                        new Author("P", "Lemey"),
                        new Author("C", "Russell"),
                        new Author("D", "Smith"),
                        new Author("MA", "Suchard"),
                },
                Citation.Status.IN_PREPARATION
        ));
        return citations;
    }
}

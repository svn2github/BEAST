/*
 * TreeAnnotator.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
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

package dr.app.tools;

import dr.app.beast.BeastVersion;
import dr.app.util.Arguments;
import dr.geo.KMLCoordinates;
import dr.geo.KernelDensityEstimator2D;
import dr.geo.contouring.*;
import dr.inference.trace.*;
import dr.math.Procrustes;
import dr.util.Version;
import org.apache.commons.math.linear.*;
import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.correlation.Covariance;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import javax.transaction.xa.Xid;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.*;

/*
 * @author Marc Suchard and Andrew Rambaut
 */

public class AntigenicPlotter {

    private final static Version version = new BeastVersion();

    public final static ContourMode CONTOUR_MODE = ContourMode.SNYDER;
    public final static double HPD_VALUE = 0.95;
    public static final int GRIDSIZE = 200;

    public AntigenicPlotter(int burnin,
                            String inputFileName,
                            String outputFileName
    ) throws IOException {

        System.out.println("Reading log file...");

        FileReader fileReader = new FileReader(inputFileName);
        try {
            File file = new File(inputFileName);

            LogFileTraces traces = new LogFileTraces(inputFileName, file);
            traces.loadTraces();


            if (burnin == -1) {
                burnin = traces.getMaxState() / 10;
            }

            traces.setBurnIn(burnin);

            System.out.println();
            System.out.println("burnIn   <= " + burnin);
            System.out.println("maxState  = " + traces.getMaxState());
            System.out.println();

            int traceCount = traces.getTraceCount() / 2;
            int stateCount = traces.getStateCount();
            double[][][] data = new double[stateCount][traceCount][2];
            String[] labels = new String[traceCount];

            for (int i = 0; i < traceCount; i++) {
                String name = traces.getTraceName(i * 2);
                labels[i] = name.substring(0, name.length() - 1);
                for (int j = 0; j < stateCount; j++) {
                    data[j][i][0] = traces.getStateValue(i * 2, j);
                    data[j][i][1] = traces.getStateValue((i * 2) + 1, j);
                }
            }

            procrustinate(data);

            writeKML(outputFileName, labels, data);

        } catch (Exception e) {
            System.err.println("Error Parsing Input File: " + e.getMessage());

            e.printStackTrace(System.err);
            return;
        }
        fileReader.close();

    }

    private void procrustinate(final double[][][] data) {
        RealMatrix Xstar = new Array2DRowRealMatrix(data[data.length - 1]);
        for (int i = 0; i < data.length - 1; i++) {
            RealMatrix X = new Array2DRowRealMatrix(data[i]);
            RealMatrix Xnew  = Procrustes.procrustinate(X, Xstar, true, true);
            data[i] = Xnew.getData();
        }
    }

    private void writeKML(String fileName, String[] labels, double[][][] data) {
        int[] traceOrder = sortTraces(labels);

//        Element hpdSchema = new Element("Schema");
//        hpdSchema.setAttribute("id", "HPD_Schema");
//        hpdSchema.addContent(new Element("SimpleField")
//                .setAttribute("name", "Label")
//                .setAttribute("type", "string")
//                .addContent(new Element("displayName").addContent("Label")));
//        hpdSchema.addContent(new Element("SimpleField")
//                .setAttribute("name", "Point")
//                .setAttribute("type", "double")
//                .addContent(new Element("displayName").addContent("Point")));
//        hpdSchema.addContent(new Element("SimpleField")
//                .setAttribute("name", "Year")
//                .setAttribute("type", "double")
//                .addContent(new Element("displayName").addContent("Year")));
//        hpdSchema.addContent(new Element("SimpleField")
//                .setAttribute("name", "HPD")
//                .setAttribute("type", "double")
//                .addContent(new Element("displayName").addContent("HPD")));

        Element traceSchema = new Element("Schema");
        traceSchema.setAttribute("id", "Trace_Schema");
        traceSchema.addContent(new Element("SimpleField")
                .setAttribute("name", "Label")
                .setAttribute("type", "string")
                .addContent(new Element("displayName").addContent("Label")));
        traceSchema.addContent(new Element("SimpleField")
                .setAttribute("name", "Trace")
                .setAttribute("type", "double")
                .addContent(new Element("displayName").addContent("Trace")));
        traceSchema.addContent(new Element("SimpleField")
                .setAttribute("name", "State")
                .setAttribute("type", "double")
                .addContent(new Element("displayName").addContent("State")));

        Element centroidSchema = new Element("Schema");
        centroidSchema.setAttribute("id", "Centroid_Schema");
        centroidSchema.addContent(new Element("SimpleField")
                .setAttribute("name", "Label")
                .setAttribute("type", "string")
                .addContent(new Element("displayName").addContent("Label")));
        centroidSchema.addContent(new Element("SimpleField")
                .setAttribute("name", "Year")
                .setAttribute("type", "double")
                .addContent(new Element("displayName").addContent("Year")));
        centroidSchema.addContent(new Element("SimpleField")
                .setAttribute("name", "Trace")
                .setAttribute("type", "double")
                .addContent(new Element("displayName").addContent("Trace")));

//        final Element contourFolderElement = new Element("Folder");
//        Element contourFolderNameElement = new Element("name");
//        contourFolderNameElement.addContent("HPDs");
//        contourFolderElement.addContent(contourFolderNameElement);

        final Element traceFolderElement = new Element("Folder");
        Element traceFolderNameElement = new Element("name");
        traceFolderNameElement.addContent("traces");
        traceFolderElement.addContent(traceFolderNameElement);

        final Element centroidFolderElement = new Element("Folder");
        Element centroidFolderNameElement = new Element("name");
        centroidFolderNameElement.addContent("centroids");
        centroidFolderElement.addContent(centroidFolderNameElement);

        Element documentNameElement = new Element("name");
        String documentName = fileName;
        if (documentName.endsWith(".kml"))
            documentName = documentName.replace(".kml", "");
        documentNameElement.addContent(documentName);

        final Element documentElement = new Element("Document");
        documentElement.addContent(documentNameElement);
        documentElement.addContent(traceSchema);
        documentElement.addContent(centroidSchema);
//        documentElement.addContent(hpdSchema);
        documentElement.addContent(centroidFolderElement);
        documentElement.addContent(traceFolderElement);
//        documentElement.addContent(contourFolderElement);

        final Element rootElement = new Element("kml");
        rootElement.addContent(documentElement);

        Element traceElement = generateTraceElement(labels, data, traceOrder);
        traceFolderElement.addContent(traceElement);

        Element centroidElement = generateCentroidElement(labels, data, traceOrder);
        centroidFolderElement.addContent(centroidElement);

//        Element contourElement = generateKDEElement(0.95, labels, data, traceOrder);
//        contourFolderElement.addContent(contourElement);

        PrintStream resultsStream;

        try {
            resultsStream = new PrintStream(new File(fileName));
            XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat().setTextMode(Format.TextMode.PRESERVE));
            xmlOutputter.output(rootElement, resultsStream);

        } catch (IOException e) {
            System.err.println("Error opening file: " + fileName);
            System.exit(-1);
        }

    }

    private int[] sortTraces(final String[] labels) {
        Map<Label, Integer> orderMap = new HashMap<Label, Integer>();

        List<Label> labelList = new ArrayList<Label>();
        int i = 0;
        for (String label : labels) {
            Label l = new Label(label);
            labelList.add(l);
            orderMap.put(l, i);
            i++;
        }
        Collections.sort(labelList);

        int[] order = new int[labels.length];
        i = 0;
        for (Label label : labelList) {
            int index = orderMap.get(label);
            order[i] = index;
            i++;
        }

        return order;
    }

    class Label implements Comparable<Label> {
        Label(final String label) {
            this.label = label;
            String[] parts = label.split("/");
            year = Integer.parseInt(parts[parts.length - 1]);
            if (year < 12) {
                year += 2000;
            } else {
                year += 1900;
            }
        }

        public int compareTo(final Label label) {
            return this.year - label.year;
        }

        String label;
        int year;


    }
    private Element generateKDEElement(double hpdValue, String[] labels, double[][][] points, int[] order) {
        Element traceElement = new Element("Folder");
        Element nameElement = new Element("name");
        String name = "intervals";

        nameElement.addContent(name);
        traceElement.addContent(nameElement);

        for (int j = 0; j < points[0].length; j++)  {
            double x[] = new double[points.length];
            double y[] = new double[points.length];
            for (int i = 0; i < points.length; i++)  {
                x[i] = points[i][order[j]][0];
                y[i] = points[i][order[j]][1];
            }

            ContourMaker contourMaker;
            if (CONTOUR_MODE == ContourMode.JAVA)
                contourMaker = new KernelDensityEstimator2D(x, y, GRIDSIZE);
            else if (CONTOUR_MODE == ContourMode.R)
                contourMaker = new ContourWithR(x, y, GRIDSIZE);
            else if (CONTOUR_MODE == ContourMode.SNYDER)
                contourMaker = new ContourWithSynder(x, y, GRIDSIZE);
            else
                throw new RuntimeException("Unimplemented ContourModel!");

            ContourPath[] paths = contourMaker.getContourPaths(HPD_VALUE);
            int pathCounter = 1;
            for (ContourPath path : paths) {

                KMLCoordinates coords = new KMLCoordinates(path.getAllX(), path.getAllY());

                //because KML polygons require long,lat,alt we need to switch lat and long first
                coords.switchXY();
                Element placemarkElement = generatePlacemarkElementWithPolygon(hpdValue, coords, -1, pathCounter);
                //testing how many points are within the polygon
                traceElement.addContent(placemarkElement);

                pathCounter ++;
            }

        }
        return traceElement;
    }

    private Element generateTraceElement(String[] labels, double[][][] points, int[] order) {
        Element traceElement = new Element("Folder");
        Element nameElement = new Element("name");
        String name = "points";

        nameElement.addContent(name);
        traceElement.addContent(nameElement);

        for (int i = 0; i < points.length; i++)  {
            for (int j = 0; j < points[i].length; j++)  {
                Element placemarkElement = new Element("Placemark");

                placemarkElement.addContent(generateTraceData(labels[order[j]], j, i));


                Element pointElement = new Element("Point");
                Element coordinates = new Element("coordinates");
                coordinates.addContent(points[i][order[j]][1]+","+points[i][order[j]][0]+",0");
                pointElement.addContent(coordinates);
                placemarkElement.addContent(pointElement);

                traceElement.addContent(placemarkElement);

            }

        }
        return traceElement;
    }

    private Element generateTraceData(String label, int trace, int state) {
        Element data = new Element("ExtendedData");
        Element schemaData = new Element("SchemaData");
        schemaData.setAttribute("schemaUrl", "Trace_Schema");
        schemaData.addContent(new Element("SimpleData").setAttribute("name", "Label").addContent(label));
        schemaData.addContent(new Element("SimpleData").setAttribute("name", "Trace").addContent(Integer.toString(trace)));
        schemaData.addContent(new Element("SimpleData").setAttribute("name", "State").addContent(Integer.toString(state)));
        data.addContent(schemaData);
        return data;
    }

    private Element generateCentroidElement(String[] labels, double[][][] points, int[] order) {
        Element centroidElement = new Element("Folder");
        Element nameElement = new Element("name");
        String name = "centroids";

        nameElement.addContent(name);
        centroidElement.addContent(nameElement);

        double[][] centroids = new double[points[0].length][points[0][0].length];
        for (int i = 0; i < points.length; i++)  {
            for (int j = 0; j < points[i].length; j++)  {
                for (int k = 0; k < points[i][j].length; k++)  {
                    centroids[j][k] += points[i][j][k];
                }
            }
        }
        for (int j = 0; j < points[0].length; j++)  {
            for (int k = 0; k < points[0][j].length; k++)  {
                centroids[j][k] /= points.length;
            }
        }

        for (int j = 0; j < points[0].length; j++)  {
            Element placemarkElement = new Element("Placemark");

            placemarkElement.addContent(generateCentroidData(labels[order[j]], j));


            Element pointElement = new Element("Point");
            Element coordinates = new Element("coordinates");
            coordinates.addContent(centroids[order[j]][1]+","+centroids[order[j]][0]+",0");
            pointElement.addContent(coordinates);
            placemarkElement.addContent(pointElement);

            centroidElement.addContent(placemarkElement);
        }
        return centroidElement;
    }

    private Element generateCentroidData(String label, int trace) {
        Element data = new Element("ExtendedData");
        Element schemaData = new Element("SchemaData");
        schemaData.setAttribute("schemaUrl", "Centroid_Schema");
        schemaData.addContent(new Element("SimpleData").setAttribute("name", "Label").addContent(label));
        Label l = new Label(label);
        schemaData.addContent(new Element("SimpleData").setAttribute("name", "Year").addContent(Integer.toString(l.year)));
        schemaData.addContent(new Element("SimpleData").setAttribute("name", "Trace").addContent(Integer.toString(trace)));
        data.addContent(schemaData);
        return data;
    }

    private Element generatePlacemarkElementWithPolygon(double hpdValue, KMLCoordinates coords, int pointNumber, int pathCounter) {
        Element placemarkElement = new Element("Placemark");

        String name;
        Element placemarkNameElement = new Element("name");
        name = "kde_" + pathCounter;

        placemarkNameElement.addContent(name);
        placemarkElement.addContent(placemarkNameElement);

        placemarkElement.addContent(generateContourData(name, pointNumber, hpdValue));

        Element polygonElement = new Element("Polygon");
        Element altitudeMode = new Element("altitudeMode");
        altitudeMode.addContent("clampToGround");
        polygonElement.addContent(altitudeMode);
        Element tessellate = new Element("tessellate");
        tessellate.addContent("1");
        polygonElement.addContent(tessellate);
        Element outerBoundaryIs = new Element("outerBoundaryIs");
        Element LinearRing = new Element("LinearRing");
        LinearRing.addContent(coords.toXML());
        outerBoundaryIs.addContent(LinearRing);
        polygonElement.addContent(outerBoundaryIs);
        placemarkElement.addContent(polygonElement);

        return placemarkElement;
    }

    private Element generateContourData(String label, int point, double hpd) {
        Element data = new Element("ExtendedData");
        Element schemaData = new Element("SchemaData");
        schemaData.setAttribute("schemaUrl", "HPD_Schema");
        schemaData.addContent(new Element("SimpleData").setAttribute("name", "Label").addContent(label));
//        Label l = new Label(label);
//        schemaData.addContent(new Element("SimpleData").setAttribute("name", "Year").addContent(Integer.toString(l.year)));
        schemaData.addContent(new Element("SimpleData").setAttribute("name", "Point").addContent(Integer.toString(point)));
        if (hpd > 0) {
            schemaData.addContent(new Element("SimpleData").setAttribute("name", "HPD").addContent(Double.toString(hpd)));
        }
        data.addContent(schemaData);
        return data;
    }

    public static void printTitle() {
        System.out.println();
        centreLine("AntigenicPlotter " + version.getVersionString() + ", " + version.getDateString(), 60);
        centreLine("BEAST time vs. parameter density analysis", 60);
        centreLine("by", 60);
        centreLine("Andrew Rambaut and Marc A. Suchard", 60);
        System.out.println();
        System.out.println();
    }

    public static void centreLine(String line, int pageWidth) {
        int n = pageWidth - line.length();
        int n1 = n / 2;
        for (int i = 0; i < n1; i++) {
            System.out.print(" ");
        }
        System.out.println(line);
    }


    public static void printUsage(Arguments arguments) {

        arguments.printUsage("antigenicplotter", "<input-file-name> [<output-file-name>]");
        System.out.println();
        System.out.println("  Example: antigenicplotter -burnin 100 locations.log locations.kml");
        System.out.println();
    }

    //Main method
    public static void main(String[] args) throws IOException {

        String inputFileName = null;
        String outputFileName = null;

        printTitle();

        Arguments arguments = new Arguments(
                new Arguments.Option[]{
                        new Arguments.IntegerOption("burnin", "the number of states to be considered as 'burn-in' [default = 0]"),
                        new Arguments.Option("help", "option to print this message")
                });

        try {
            arguments.parseArguments(args);
        } catch (Arguments.ArgumentException ae) {
            System.out.println(ae);
            printUsage(arguments);
            System.exit(1);
        }

        if (arguments.hasOption("help")) {
            printUsage(arguments);
            System.exit(0);
        }

        int burnin = -1;
        if (arguments.hasOption("burnin")) {
            burnin = arguments.getIntegerOption("burnin");
        }

        String[] args2 = arguments.getLeftoverArguments();

        if (args2.length > 2) {
            System.err.println("Unknown option: " + args2[2]);
            System.err.println();
            printUsage(arguments);
            System.exit(1);
        }

        if (args2.length == 2) {
            inputFileName = args2[0];
            outputFileName = args2[1];
        } else {
            System.err.println("Missing input or output file name");
            printUsage(arguments);
            System.exit(1);
        }

        new AntigenicPlotter(burnin,
                inputFileName,
                outputFileName
        );

        System.exit(0);
    }

}
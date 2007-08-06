/*
 * TraceAnalysis.java
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

package dr.inference.trace;

import dr.util.NumberFormatter;

import java.io.File;

/**
 * @author Alexei Drummond
 * @version $Id: TraceAnalysis.java,v 1.23 2005/05/24 20:26:00 rambaut Exp $
 */
public class TraceAnalysis {

    /**
     * @param fileName the name of the log file to analyze
     * @param burnin   the state to discard up to
     * @return an array og analyses of the statistics in a log file.
     * @throws java.io.IOException if general error reading file
     * @throws TraceException      if trace file in wrong format or corrupted
     */
    public static TraceList analyzeLogFile(String fileName, int burnin) throws java.io.IOException, TraceException {

        File file = new File(fileName);
        LogFileTraces traces = new LogFileTraces(fileName, file);
        traces.loadTraces();
        traces.setBurnIn(burnin);

        for (int i = 0; i < traces.getTraceCount(); i++) {
            traces.analyseTrace(i);
        }
        return traces;
    }

    public static TraceList report(String fileName) throws java.io.IOException, TraceException {

        return report(fileName, -1);
    }

    public static TraceList report(String fileName, int burnin) throws java.io.IOException, TraceException {

        int fieldWidth = 14;
        int firstField = 25;
        NumberFormatter formatter = new NumberFormatter(6);
        formatter.setPadding(true);
        formatter.setFieldWidth(fieldWidth);

        File file = new File(fileName);

        LogFileTraces traces = new LogFileTraces(fileName, file);
        traces.loadTraces();
        traces.setBurnIn(burnin);

        System.out.println();
        System.out.println("burnIn   = " + burnin);
        System.out.println("maxState = " + traces.getMaxState());
        System.out.println();

        System.out.print(formatter.formatToFieldWidth("statistic", firstField));
        String[] names = new String[]{"mean", "hpdLower", "hpdUpper", "ESS"};

        for (String name : names) {
            System.out.print(formatter.formatToFieldWidth(name, fieldWidth));
        }
        System.out.println();

        int warning = 0;
        for (int i = 0; i < traces.getTraceCount(); i++) {

            traces.analyseTrace(i);
            TraceDistribution distribution = traces.getDistributionStatistics(i);

            double ess = distribution.getESS();
            System.out.print(formatter.formatToFieldWidth(traces.getTraceName(i), firstField));
            System.out.print(formatter.format(distribution.getMean()));
            System.out.print(formatter.format(distribution.getLowerHPD()));
            System.out.print(formatter.format(distribution.getUpperHPD()));
            System.out.print(formatter.format(ess));
            if (ess < 100) {
                warning += 1;
                System.out.println("*");
            } else {
                System.out.println();
            }
        }
        System.out.println();

        if (warning > 0) {
            System.out.println(" * WARNING: The results of this MCMC analysis may be invalid as ");
            System.out.println("            one or more statistics had very low effective sample sizes (ESS)");
        }

        System.out.flush();
        return traces;
    }

    /**
     * @param burnin     the number of states of burnin or if -1 then use 10%
     * @param filename   the file name of the log file to report on
     * @param drawHeader if true then draw header
     * @param hpds       if true then report 95% hpd upper and lower
     * @return the traces loaded from given file to create this short report
     * @throws java.io.IOException if general error reading file
     * @throws TraceException      if trace file in wrong format or corrupted
     */
    public static TraceList shortReport(String filename,
                                        final int burnin, boolean drawHeader, boolean hpds) throws java.io.IOException, TraceException {

        TraceList traces = analyzeLogFile(filename, burnin);

        int maxState = traces.getMaxState();

        double minESS = Double.MAX_VALUE;

        if (drawHeader) {
            System.out.print("file\t");
            for (int i = 0; i < traces.getTraceCount(); i++) {
                String traceName = traces.getTraceName(i);
                System.out.print(traceName + "\t");
                if (hpds) {
                    System.out.print(traceName + " hpdLower\t");
                    System.out.print(traceName + " hpdUpper\t");
                }
            }
            System.out.println("minESS\tchainLength");
        }

        System.out.print(filename + "\t");
        for (int i = 0; i < traces.getTraceCount(); i++) {
            TraceDistribution distribution = traces.getDistributionStatistics(i);
            System.out.print(distribution.getMean() + "\t");
            if (hpds) {
                System.out.print(distribution.getLowerHPD() + "\t");
                System.out.print(distribution.getUpperHPD() + "\t");
            }
            double ess = distribution.getESS();
            if (ess < minESS) {
                minESS = ess;
            }
        }
        System.out.println(minESS + "\t" + maxState);
		return traces;
	}



}

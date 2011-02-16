/*
 * Trace.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple class that stores a trace for a single statistic
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @version $Id: Trace.java,v 1.11 2005/07/11 14:07:26 rambaut Exp $
 */
public class Trace<T> {

//    public static final int INITIAL_SIZE = 1000;
//    public static final int INCREMENT_SIZE = 1000;

    //    private TraceType traceType = TraceType.CONTINUOUS;
    protected List<T> values = new ArrayList<T>();
    //    protected int valueCount = 0;
    protected String name;

    public Trace(String name) {
        this.name = name;
    }

//    public Trace(String name, int initialSize, T initValue) {
//        this.name = name;
//        if (initialSize > 0) {
//            this.values = (T[]) new Object[initialSize];
//        }
//        values[0] = initValue; // make getTraceType() working
//    }

    public Trace(String name, T[] valuesArray) {
        this(name);
//        List<T> newVL = Arrays.asList(valuesArray);
        Collections.addAll(this.values, valuesArray);
    }

    /**
     * @param value the valued to be added
     */
    public void add(T value) {
        values.add(value);
    }

    /**
     * @param valuesArray the values to be added
     */
    public void add(T[] valuesArray) {
        Collections.addAll(this.values, valuesArray);
    }

    public int getValuesSize() {
        return values.size();
    }

    public T getValue(int index) {
        return values.get(index); // filter?
    }

//    public T[] getValues() {
//        return values;
//    }

    // used by others (e.g. CombinedTraces) without filter applied

    public void getValues(int start, T[] destination, int offset) {
        System.arraycopy(values, start, destination, offset, getValuesSize() - start);
    }

    // used by others (e.g. CombinedTraces) without filter applied
    public void getValues(int start, int count, T[] destination, int offset) {
        System.arraycopy(values, start, destination, offset, count);
    }

    /**
     *
     * @param fromIndex low endpoint (inclusive) of the subList.
     * @param toIndex high endpoint (exclusive) of the subList.
     * @param selected  getValuesSize() should = selected.length
     * @return subList
     */
    public List<T> getValues(int fromIndex, int toIndex, boolean[] selected) {
        if (toIndex > getValuesSize() || fromIndex > toIndex)
            throw new RuntimeException("Invalid index : fromIndex = " + fromIndex + "; toIndex = " + toIndex
                    + "; List size = " + getValuesSize() + "; in Trace " + name);

        if (selected != null) {
            if (getValuesSize() != selected.length)
                throw new RuntimeException("size of values is different with selected[] in Trace " + name);

            List<T> valuesList = new ArrayList<T>();
            for (int i = fromIndex; i < toIndex; i++) {
                if (selected[i])
                    valuesList.add(values.get(i));
            }
            return valuesList;
        } else {
            return values.subList(fromIndex, toIndex);
        }
    }

    public String getName() {
        return name;
    }

    // *************** Large Memory Comsumption ******************
    public static <T> double[] arrayConvertToDouble(T[] src) {//todo
        double[] dest = null;
        if (src != null) {
            dest = new double[src.length];
            for (int i = 0; i < dest.length; i++) {
                dest[i] = ((Number) src[i]).doubleValue();
            }
        }
        return dest;
    }

    public static <T> int[] arrayConvertToInt(T[] src) {//todo
        int[] dest = null;
        if (src != null) {
            dest = new int[src.length];
            for (int i = 0; i < dest.length; i++) {
                dest[i] = ((Number) src[i]).intValue();
            }
        }
        return dest;
    }

    public static double[][] multiDArrayConvert(Double[][] src) {//todo
        double[][] dest = null;
        if (src != null) {
            dest = new double[src.length][src[0].length];
            for (int i = 0; i < dest.length; i++) {
                for (int j = 0; j < dest[i].length; j++) {
                    dest[i][j] = src[i][j].doubleValue();
                }
            }
        }
        return dest;
    }
    //************************************************************

    public Class getTraceType() {
        if (values.get(0) == null) {
            return null;
        }
        return values.get(0).getClass();
    }
//
//    public void setTraceType(TraceFactory.TraceType traceType) {
//        this.traceType = traceType;
//    }

    //******************** TraceCorrelation ****************************
    protected TraceCorrelation<T> traceStatistics;

    public TraceCorrelation getTraceStatistics() {
        return traceStatistics;
    }

    public void setTraceStatistics(TraceCorrelation traceStatistics) {
        this.traceStatistics = traceStatistics;
    }

    //******************** Filter ****************************
    protected Filter<T> filter;

    public void setFilter(Filter filter) {
        this.filter = filter;
//        if (traceStatistics == null)
//            throw new RuntimeException("Cannot set filter because traceStatistics = null in Trace " + name);
//        traceStatistics =
    }

    public Filter getFilter() {
        return filter;
    }

    public boolean isIn(int i) {
        return filter.isIn(values.get(i));
    }

    //******************** Trace Double ****************************
/*    public class D extends Trace<Double> {

        public D(String name, int initialSize) {
            super.name = name;
            super.values = new Double[initialSize];
//            values[0] = initValue; // make getTraceType() working
        }

        public D(String name, Double[] values) {
            this(name, values.length);
            valueCount = values.length;
            System.arraycopy(values, 0, this.values, 0, values.length);
        }

        public Double[] getValues(int length, int start, int offset, boolean[] selected) {
            return this.getValues(length, start, offset, valueCount - start, selected);
        }

        public Double[] getValues(int length, int start, int offset, int count, boolean[] selected) {
            Double[] destination = new Double[length];
            System.arraycopy(values, start, destination, offset, count);

            if (selected != null) {
                boolean[] destinationSelected = new boolean[length];
                System.arraycopy(selected, start, destinationSelected, offset, count);
                return getSeletedValues(destination, destinationSelected);
            } else {
                return destination;
            }
        }
    }

    //******************** Trace Integer ****************************
    public class I extends Trace<Integer> {

        public I(String name, int initialSize) {
            super.name = name;
            super.values = new Integer[initialSize];
//            values[0] = initValue; // make getTraceType() working
        }

        public I(String name, Integer[] values) {
            this(name, values.length);
            valueCount = values.length;
            System.arraycopy(values, 0, this.values, 0, values.length);
        }

        public Integer[] getValues(int length, int start, int offset, boolean[] selected) {
            return this.getValues(length, start, offset, valueCount - start, selected);
        }

        public Integer[] getValues(int length, int start, int offset, int count, boolean[] selected) {
            Integer[] destination = new Integer[length];
            System.arraycopy(values, start, destination, offset, count);

            if (selected != null) {
                boolean[] destinationSelected = new boolean[length];
                System.arraycopy(selected, start, destinationSelected, offset, count);
                return getSeletedValues(destination, destinationSelected);
            } else {
                return destination;
            }
        }
    }

    //******************** Trace String ****************************
    public class S extends Trace<String> {

        public S(String name, int initialSize, String initValue) {
            super.name = name;
            if (initialSize > 0) {
                super.values = new String[initialSize];
            }
            values[0] = initValue; // make getTraceType() working
        }

        public S(String name, String[] values) {
            super.name = name;
            super.values = new String[values.length];
            valueCount = values.length;
            System.arraycopy(values, 0, this.values, 0, values.length);
        }

        public String[] getValues(int length, int start, int offset, boolean[] selected) {
            return this.getValues(length, start, offset, valueCount - start, selected);
        }

        public String[] getValues(int length, int start, int offset, int count, boolean[] selected) {
            String[] destination = new String[length];
            System.arraycopy(values, start, destination, offset, count);

            if (selected != null) {
                boolean[] destinationSelected = new boolean[length];
                System.arraycopy(selected, start, destinationSelected, offset, count);
                return getSeletedValues(destination, destinationSelected);
            } else {
                return destination;
            }
        }
    } */

}
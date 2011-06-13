/*
 * BirthDeathSerialSamplingModel.java
 *
 * Copyright (C) 2002-2009 Alexei Drummond and Andrew Rambaut
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
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.evomodel.speciation;

import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evolution.util.Taxon;
import dr.inference.model.Parameter;
import dr.inference.model.Variable;

import java.util.Set;

/**
 * Beginning of tree prior for birth-death + serial sampling + extant sample proportion. More Tanja magic...
 *
 * @author Alexei Drummond
 */
public class BirthDeathSerialSamplingModel extends MaskableSpeciationModel {

    // birth rate
    Variable<Double> lambda;

    // death rate
    Variable<Double> mu;

    // serial sampling rate
    Variable<Double> psi;

    // extant sampling proportion
    Variable<Double> p;

    //boolean death rate is relative?
    boolean relativeDeath = false;

    boolean logTransformed = false; // log lambda, psi

    // boolean stating whether sampled individuals remain infectious, or become non-infectious
//    boolean sampledIndividualsRemainInfectious = false; // replaced by r

//    the additional parameter 0 <= r <= 1 has to be estimated.
    //    for r=1, this is sampledRemainInfectiousProb=0
    //    for r=0, this is sampledRemainInfectiousProb=1
    Variable<Double> r;

    Variable<Double> finalTimeInterval;

    // the origin of the infection, x0 > tree.getRoot();
    Variable<Double> origin;

    public BirthDeathSerialSamplingModel(
            Variable<Double> lambda,
            Variable<Double> mu,
            Variable<Double> psi,
            Variable<Double> p,
            boolean relativeDeath,
            Variable<Double> r,
            Variable<Double> finalTimeInterval,
            Variable<Double> origin,
            Type units) {

        this("birthDeathSerialSamplingModel", lambda, mu, psi, p, relativeDeath, r, finalTimeInterval, origin, units, false);
    }

    public BirthDeathSerialSamplingModel(
            String modelName,
            Variable<Double> lambda,
            Variable<Double> mu,
            Variable<Double> psi,
            Variable<Double> p,
            boolean relativeDeath,
            Variable<Double> r,
            Variable<Double> finalTimeInterval,
            Variable<Double> origin,
            Type units,
            boolean logTransformed) {

        super(modelName, units);

        this.logTransformed = logTransformed;
        this.relativeDeath = relativeDeath;

        this.lambda = lambda;
        addVariable(lambda);
        lambda.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, logTransformed ? Double.NEGATIVE_INFINITY : 0.0, 1));

        this.mu = mu;
        addVariable(mu);
        mu.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));

        this.psi = psi;
        addVariable(psi);
        psi.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, logTransformed ? Double.NEGATIVE_INFINITY : 0.0, 1));

        this.p = p;
        addVariable(p);
        p.addBounds(new Parameter.DefaultBounds(1.0, 0.0, 1));

        this.finalTimeInterval = finalTimeInterval;
        addVariable(finalTimeInterval);
        finalTimeInterval.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 1.0, 1));

        this.r = r;
        addVariable(r);
        r.addBounds(new Parameter.DefaultBounds(1.0, 0.0, 1));

        this.origin = origin;
        if (origin != null) {
            addVariable(origin);
            origin.addBounds(new Parameter.DefaultBounds(Double.POSITIVE_INFINITY, 0.0, 1));
        }
    }

    public static double p0(double b, double d, double p, double psi, double t) {
        double c1 = c1(b, d, psi);
        double c2 = c2(b, d, p, psi);

        double expc1trc2 = Math.exp(-c1 * t) * (1.0 - c2);

        return (b + d + psi + c1 * ((expc1trc2 - (1.0 + c2)) / (expc1trc2 + (1.0 + c2)))) / (2.0 * b);
    }

    public static double q(double b, double d, double p, double psi, double t) {
        double c1 = c1(b, d, psi);
        double c2 = c2(b, d, p, psi);
        double res = 2 * (1 - c2 * c2) + Math.exp(-c1 * t) * (1 - c2) * (1 - c2) + Math.exp(c1 * t) * (1 + c2) * (1 + c2);
        return res;
    }

    private static double c1(double b, double d, double psi) {
        return Math.abs(Math.sqrt(Math.pow(b - d - psi, 2.0) + 4.0 * b * psi));
    }

    private static double c2(double b, double d, double p, double psi) {
        return -(b - d - 2.0 * b * p - psi) / c1(b, d, psi);
    }


    public double p0(double t) {
        return p0(birth(), death(), p(), psi(), t);
    }

    public double q(double t) {
        return q(birth(), death(), p(), psi(), t);
    }

    private double c1() {
        return c1(birth(), death(), psi());
    }

    private double c2() {
        return c2(birth(), death(), p(), psi());
    }

    public double birth() {
        if (mask != null) return mask.birth();

        if (logTransformed) return Math.exp(lambda.getValue(0));
        return lambda.getValue(0);
    }

    public double death() {
        if (mask != null) return mask.death();
        return relativeDeath ? mu.getValue(0) * birth() : mu.getValue(0);
    }

    public double psi() {
        if (mask != null) return mask.psi();

        if (logTransformed) return Math.exp(psi.getValue(0));
        return psi.getValue(0);
    }

    public double p() {

        if (finalTimeInterval() == 0.0) {
            if (mask != null) return mask.p.getValue(0);
            return p.getValue(0);
        }
        return 0;
    }

    public double r() {
        if (mask != null) return mask.r();

        if (logTransformed) return Math.exp(r.getValue(0));
        return r.getValue(0);
    }

    // The mask does not affect the following three methods

    public boolean isSamplingOrigin() {
        return origin != null;
    }

    public double x0() {
        return origin.getValue(0);
    }

    public double finalTimeInterval() {
        return finalTimeInterval.getValue(0);
    }

    /**
     * Generic likelihood calculation
     *
     * @param tree the tree to calculate likelihood of
     * @return log-likelihood of density
     */
    public final double calculateTreeLogLikelihood(Tree tree) {

        if (isSamplingOrigin() && x0() < tree.getNodeHeight(tree.getRoot())) {
            return Double.NEGATIVE_INFINITY;
//            throw new RuntimeException("Orign value (" + x0() + ") cannot < tree root height (" + tree.getNodeHeight(tree.getRoot()) + ")");
        }

        //System.out.println("calculating tree log likelihood");
        double time = finalTimeInterval();

        // extant leaves
        int n = 0;
        // extinct leaves
        int m = 0;

        for (int i = 0; i < tree.getExternalNodeCount(); i++) {
            NodeRef node = tree.getExternalNode(i);
            if (tree.getNodeHeight(node) + time == 0.0) {
                n += 1;
            } else {
                m += 1;
            }
        }

        double x1 = tree.getNodeHeight(tree.getRoot()) + time;
        double c1 = c1();
        double c2 = c2();
        double b = birth();
        double p = p();

        double logL;
        if (isSamplingOrigin()) {
            logL = Math.log(1 / q(x0()));
        } else {
            double bottom = c1 * (c2 + 1) * (1 - c2 + (1 + c2) * Math.exp(c1 * x1));
            logL = Math.log(1 / bottom);
        }
        if (n > 0) {
            logL += n * Math.log(4 * p);
        }
        for (int i = 0; i < tree.getInternalNodeCount(); i++) {
            double x = tree.getNodeHeight(tree.getInternalNode(i)) + time;
            logL += Math.log(b / q(x));
        }
        for (int i = 0; i < tree.getExternalNodeCount(); i++) {
            double y = tree.getNodeHeight(tree.getExternalNode(i)) + time;

            if (y > 0.0) {
                logL += Math.log(psi() * (r() + (1 - r()) * p0(y)) * q(y));
            }
        }

        return logL;
    }

    public double calculateTreeLogLikelihood(Tree tree, Set<Taxon> exclude) {
        if (exclude.size() == 0) return calculateTreeLogLikelihood(tree);
        throw new RuntimeException("Not implemented!");
    }

    public void mask(SpeciationModel mask) {
        if (mask instanceof BirthDeathSerialSamplingModel) {
            this.mask = (BirthDeathSerialSamplingModel) mask;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void unmask() {
        mask = null;
    }

    // if a mask exists then use the mask's parameters instead (except for origin and finalTimeInterval)
    BirthDeathSerialSamplingModel mask = null;
}
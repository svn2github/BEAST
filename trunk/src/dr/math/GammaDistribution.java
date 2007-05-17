/*
 * GammaDistribution.java
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

package dr.math;

import dr.math.MathUtils;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * gamma distribution.
 * 
 * (Parameters: shape, scale; mean: scale*shape; variance: scale^2*shape)
 * 
 * @version $Id: GammaDistribution.java,v 1.9 2006/03/30 11:12:47 rambaut Exp $
 * 
 * @author Korbinian Strimmer
 * @author Gerton Lunter
 */
public class GammaDistribution implements Distribution {
    //
    // Public stuff
    //

    /**
         * Constructor
         */
    public GammaDistribution(double shape, double scale) {
	this.shape = shape;
	this.scale = scale;
    }

    public double getShape() {
	return shape;
    }

    public void setShape(double value) {
	shape = value;
    }

    public double getScale() {
	return scale;
    }

    public void setScale(double value) {
	scale = value;
    }

    public double pdf(double x) {
	return pdf(x, shape, scale);
    }

    public double logPdf(double x) {
	return logPdf(x, shape, scale);
    }

    public double cdf(double x) {
	return cdf(x, shape, scale);
    }

    public double quantile(double y) {
	return quantile(y, shape, scale);
    }

    public double mean() {
	return mean(shape, scale);
    }

    public double variance() {
	return variance(shape, scale);
    }

    public double nextGamma() {
	return nextGamma(shape, scale);
    }

    public final UnivariateFunction getProbabilityDensityFunction() {
	return pdfFunction;
    }

    private UnivariateFunction pdfFunction = new UnivariateFunction() {
	public final double evaluate(double x) {
	    return pdf(x);
	}

	public final double getLowerBound() {
	    return 0.0;
	}

	public final double getUpperBound() {
	    return Double.POSITIVE_INFINITY;
	}
    };

    /**
         * probability density function of the Gamma distribution
         * 
         * @param x
         *                argument
         * @param shape
         *                shape parameter
         * @param scale
         *                scale parameter
         * 
         * @return pdf value
         */
    public static double pdf(double x, double shape, double scale) {
	// return Math.pow(scale,-shape)*Math.pow(x, shape-1.0)/
	// Math.exp(x/scale + GammaFunction.lnGamma(shape));
	if (x < 0)
	    throw new IllegalArgumentException();
	if (x == 0) {
	    if (shape == 1.0)
		return 1.0 / scale;
	    else
		return 0.0;
	}
	if (shape == 1.0) {
	    return Math.exp(-x / scale) / scale;
	}

	double a = Math.exp((shape - 1.0) * Math.log(x / scale) - x / scale
		- GammaFunction.lnGamma(shape));

	return a / scale;
    }

    /**
         * the natural log of the probability density function of the
         * distribution
         * 
         * @param x
         *                argument
         * @param shape
         *                shape parameter
         * @param scale
         *                scale parameter
         * 
         * @return log pdf value
         */
    public static double logPdf(double x, double shape, double scale) {
	// double a = Math.pow(scale,-shape) * Math.pow(x, shape-1.0);
	// double b = x/scale + GammaFunction.lnGamma(shape);
	// return Math.log(a) - b;

	// AR - changed this to return -ve inf instead of throwing an
	// exception... This makes things
	// much easier when using this to calculate log likelihoods.
	// if (x < 0) throw new IllegalArgumentException();
	if (x < 0)
	    return Double.NEGATIVE_INFINITY;

	if (x == 0) {
	    if (shape == 1.0)
		return Math.log(1.0 / scale);
	    else
		return Double.NEGATIVE_INFINITY;
	}
	if (shape == 1.0) {
	    return (-x / scale) - Math.log(scale);
	}

	return ((shape - 1.0) * Math.log(x / scale) - x / scale - GammaFunction
		.lnGamma(shape))
		- Math.log(scale);
    }

    /**
         * cumulative density function of the Gamma distribution
         * 
         * @param x
         *                argument
         * @param shape
         *                shape parameter
         * @param scale
         *                scale parameter
         * 
         * @return cdf value
         */
    public static double cdf(double x, double shape, double scale) {
	return GammaFunction.incompleteGammaP(shape, x / scale);
    }

    /**
         * quantile (inverse cumulative density function) of the Gamma
         * distribution
         * 
         * @param y
         *                argument
         * @param shape
         *                shape parameter
         * @param scale
         *                scale parameter
         * 
         * @return icdf value
         */
    public static double quantile(double y, double shape, double scale) {
	return 0.5 * scale * pointChi2(y, 2.0 * shape);
    }

    /**
         * mean of the Gamma distribution
         * 
         * @param shape
         *                shape parameter
         * @param scale
         *                scale parameter
         * 
         * @return mean
         */
    public static double mean(double shape, double scale) {
	return scale * shape;
    }

    /**
         * variance of the Gamma distribution.
         * 
         * @param shape
         *                shape parameter
         * @param scale
         *                scale parameter
         * 
         * @return variance
         * 
         */
    public static double variance(double shape, double scale) {
	return scale * scale * shape;
    }

    /**
         * sample from the Gamma distribution. This could be calculated using
         * quantile, but current algorithm is faster.
         * 
         * @param shape
         *                shape parameter
         * @param scale
         *                scale parameter
         * 
         * @return sample
         */
    public static double nextGamma(double shape, double scale) {
	return nextGamma(shape, scale, false);
    }

    public static double nextGamma(double shape, double scale, boolean slowCode) {

	double sample = 0.0;

	if (shape < 0.00001) {
	
	    /*
             * special case: shape==0.0 is un-normalizable distribution; but
             * it works if v. small values are ignored (v. large ones don't happen)
             * This is useful, since in this way the truncated Gamma(0,x)-distribution can easily be calculated.
             */
                
	    double minimum = 1.0e-10;
	    double maximum = 1.0e+10;
	    double normalizingConstant = Math.log(maximum) - Math.log(minimum);
	    // Draw from 1/x (with boundaries), and shape by exp(-x)
	    do {
		sample = Math.exp( Math.log(minimum) + normalizingConstant*MathUtils.nextDouble() );
	    } while ( Math.exp(-sample) < MathUtils.nextDouble() );
	    return sample * scale;
	}
	
	do {
	    try {
		sample = quantile(MathUtils.nextDouble(), shape, scale);		
	    } catch (IllegalArgumentException e) {
		sample = 0.0;
	    }	
	} while (sample == 0.0);
	return sample;
    };

    
    /* The fast version doesn't work -- neither for shape<1.0, nor for shape>15.0 */
/*    
	 fast version 
	if (shape < 15) {

	    if (shape < 0.0) {
		throw new IllegalArgumentException(
			"Gamma distribution undefined for shape parameter "
				+ shape);
	    }

	    if (shape < 0.00001) {
		
                 * special case: shape==0.0 is un-normalizable distribution; but
                 * it works if v. small values are ignored (v. large ones don't happen)
                 * This is useful, since in this way the truncated Gamma(0,x)-distribution can easily be calculated.
                 
		double minimum = 1.0e-10;
		double maximum = 1.0e+10;
		double normalizingConstant = Math.log(maximum) - Math.log(minimum);
		double sample;
		// Draw from 1/x (with boundaries), and shape by exp(-x)
		do {
		    sample = Math.exp( Math.log(minimum) + normalizingConstant*MathUtils.nextDouble() );
		} while ( Math.exp(-sample) < MathUtils.nextDouble() );
		return sample * scale;
	    }

	    int integerPart = (int) Math.floor(shape + 0.0000001);
	    double fractionalPart = Math.max(0.0, shape - integerPart);
	    	
                 * first draw a number of exponential(1)-distributed variables
                 * to obtain a Gamma(integerPart,1)
                 
	    double sample = 0.0;
	    for (int i = 0; i < integerPart; i++) {
		sample += -Math.log(MathUtils.nextDouble());
	    }
	    
	    	
                 * now sample a Gamma(fractionalPart,1)-distributed variable,
                 * using a Wikipedia algorithm:
                 * 
                 * 1. Let m be 1. 
                 * 2. Generate V2m − 1 and V2m — independent uniformly distributed on (0, 1] variables. 
                 * 3. If V_{2m - 1} \le v_0, where v_0 = \frac e {e + \delta}, then go to step 4, else go to step 5. 
                 * 4. Let \xi_m = V_{2m - 1}^{1 / \delta}, \eta_m = V_{2m} \xi _m^ {\delta - 1}. Go to step 6. 
                 * 5. Let \xi_m = 1 - \ln {V_{2m - 1}}, \ \eta_m = V_{2m} e^{-\xi_m}.
                 * 6. If \eta_m > \xi_m^{\delta - 1} e^{-\xi_m}, then increment m and go to step 2. 
                 * 7. Assume ξ = ξm to be the realization of Gamma(δ,1).
                 * 
                 
	    
	    double xi, eta;
	    if (fractionalPart > 0.00001) {
		do {
		    double v1 = -MathUtils.nextDouble();
		    double v2 = -MathUtils.nextDouble();
		    if (v1 <= Math.E / (Math.E + fractionalPart)) {
			xi = Math.pow(v1, 1.0/fractionalPart);
			eta = v2 * Math.pow(xi, fractionalPart - 1.0);
		    } else {
			xi = 1.0 - Math.log( v1 );
			eta = v2 * Math.exp( -xi );
		    }
		} while ( eta > Math.pow(xi, fractionalPart - 1.0) * Math.exp(-xi));
		sample += xi;
	    }

	    	 finally scale and return 
	    return sample * scale;
	
	} else {
	    
	     dominate by a Gaussian distribution 
	    double mean = mean(shape,scale);
	    double var = variance(shape,scale);
	    double sd = Math.sqrt( var );
	    double sample, t;
	    
	    do {
		do {
		    sample = NormalDistribution.quantile( MathUtils.nextDouble(), mean, 1.5*sd );
		} while (sample < 0.0);
		t = 0.6 * pdf( sample, shape, scale ) / NormalDistribution.pdf( sample, mean, 1.5*sd );
	    } while ( MathUtils.nextDouble() < t);
	    
	    return sample;
	    
	}

    }
*/
    // Private

    protected double shape, scale;

    public static void main(String[] args) {
	
	System.out.println("K-S critical values: 1.22(10%), 1.36(5%), 1.63(1%)\n");
	test(1.0,1.0);
	testAddition(0.5,1.0,2);
	testAddition(0.25,1.0,4);
	testAddition(0.1,1.0,10);
	testAddition(10,1.0,10);
	testAddition(20,1.0,10);
	test(0.001,1.0);
	test(1.0,2.0);
	test(10.0,1.0);
	test(16.0,1.0);
	test(16.0,0.1);
	test(100.0,1.0);
	test(0.5,1.0);
	test(0.5,0.1);
	test(0.1,1.0);
	test(0.9,1.0);
		
    }
    
    /* assumes the two arrays are the same size */
    private static double KolmogorovSmirnov( List<Double> l1, List<Double> l2 )
    {
	
	int idx2 = 0;
	int max = 0;
	for (int i=0; i<l1.size(); i++) {
	    while (idx2 < l2.size() && l2.get(idx2).doubleValue() < l1.get(i).doubleValue()) {
		idx2++;
	    }
	    max = Math.max( max, idx2-i );
	}
	return max / Math.sqrt( 2.0*l1.size() );    
    }
    
    private static void test( double shape, double scale ) {
	
	int iterations = 5000;
	List<Double> slow = new ArrayList<Double>(0);
	List<Double> fast = new ArrayList<Double>(0);
	
	for (int i=0; i<iterations; i++) {
	    slow.add( nextGamma( shape, scale, true ));
	    fast.add( nextGamma( shape, scale, false ));
	}
	
	Collections.sort( slow );
	Collections.sort( fast );
	
	System.out.println("KS test for shape="+shape+" : "+KolmogorovSmirnov(slow, fast)+" and "+KolmogorovSmirnov(fast, slow));
	
    }

    private static void testAddition( double shape, double scale, int N ) {
	
	int iterations = 5000;
	List<Double> slow = new ArrayList<Double>(0);
	List<Double> fast = new ArrayList<Double>(0);
	List<Double> test = new ArrayList<Double>(0);
	
	for (int i=0; i<iterations; i++) {
	    double s = 0.0;
	    for (int j=0; j<N; j++) {
		s += nextGamma( shape, scale, true );
	    }
	    slow.add( s );
	    s = 0.0;
	    for (int j=0; j<N; j++) {
		s += nextGamma( shape, scale, false );
	    }
	    fast.add( s );
	    test.add( nextGamma( shape*N, scale, true ));
	}
	
	Collections.sort( slow );
	Collections.sort( fast );
	Collections.sort( test );
	
	System.out.println("KS test for shape="+shape+" : slow="+KolmogorovSmirnov(slow, test)+" & "+KolmogorovSmirnov(test,slow)+
		"; fast="+KolmogorovSmirnov(fast, test)+" & "+KolmogorovSmirnov(test,fast));
	
    }

    
    private static double pointChi2(double prob, double v) {
	// Returns z so that Prob{x<z}=prob where x is Chi2 distributed with df
        // = v
	// RATNEST FORTRAN by
	// Best DJ & Roberts DE (1975) The percentage points of the
	// Chi2 distribution. Applied Statistics 24: 385-388. (AS91)

	double e = 0.5e-6, aa = 0.6931471805, p = prob, g;
	double xx, c, ch, a = 0, q = 0, p1 = 0, p2 = 0, t = 0, x = 0, b = 0, s1, s2, s3, s4, s5, s6;

	if (p < 0.000002 || p > 0.999998 || v <= 0) {
	    throw new IllegalArgumentException("Arguments out of range");
	}
	g = GammaFunction.lnGamma(v / 2);
	xx = v / 2;
	c = xx - 1;
	if (v < -1.24 * Math.log(p)) {
	    ch = Math.pow((p * xx * Math.exp(g + xx * aa)), 1 / xx);
	    if (ch - e < 0) {
		return ch;
	    }
	} else {
	    if (v > 0.32) {
		x = NormalDistribution.quantile(p, 0, 1);
		p1 = 0.222222 / v;
		ch = v * Math.pow((x * Math.sqrt(p1) + 1 - p1), 3.0);
		if (ch > 2.2 * v + 6) {
		    ch = -2 * (Math.log(1 - p) - c * Math.log(.5 * ch) + g);
		}
	    } else {
		ch = 0.4;
		a = Math.log(1 - p);

		do {
		    q = ch;
		    p1 = 1 + ch * (4.67 + ch);
		    p2 = ch * (6.73 + ch * (6.66 + ch));
		    t = -0.5 + (4.67 + 2 * ch) / p1
			    - (6.73 + ch * (13.32 + 3 * ch)) / p2;
		    ch -= (1 - Math.exp(a + g + .5 * ch + c * aa) * p2 / p1)
			    / t;
		} while (Math.abs(q / ch - 1) - .01 > 0);
	    }
	}
	do {
	    q = ch;
	    p1 = 0.5 * ch;
	    if ((t = GammaFunction.incompleteGammaP(xx, p1, g)) < 0) {
		throw new IllegalArgumentException(
			"Arguments out of range: t < 0");
	    }
	    p2 = p - t;
	    t = p2 * Math.exp(xx * aa + g + p1 - c * Math.log(ch));
	    b = t / ch;
	    a = 0.5 * t - b * c;

	    s1 = (210 + a * (140 + a * (105 + a * (84 + a * (70 + 60 * a))))) / 420;
	    s2 = (420 + a * (735 + a * (966 + a * (1141 + 1278 * a)))) / 2520;
	    s3 = (210 + a * (462 + a * (707 + 932 * a))) / 2520;
	    s4 = (252 + a * (672 + 1182 * a) + c * (294 + a * (889 + 1740 * a))) / 5040;
	    s5 = (84 + 264 * a + c * (175 + 606 * a)) / 2520;
	    s6 = (120 + c * (346 + 127 * c)) / 5040;
	    ch += t
		    * (1 + 0.5 * t * s1 - b
			    * c
			    * (s1 - b
				    * (s2 - b
					    * (s3 - b
						    * (s4 - b * (s5 - b * s6))))));
	} while (Math.abs(q / ch - 1) > e);

	return (ch);
    }
}

package dr.evomodel.coalescent;

import dr.evomodel.tree.TreeModel;
import dr.inference.model.Likelihood;
import dr.math.distributions.GammaDistribution;

/**
 * Calculates a product of gamma densities and gamma tail probabilities.
 *
 * @author Guy Baele
 */

public class GammaProductLikelihood extends Likelihood.Abstract {
	
	public final static boolean USE_EXPONENTIAL = false;
	public final static boolean REDUCE_TO_EXPONENTIAL = false;
	
	//let's forget about this for the moment
	public final static boolean FIXED_TREE = false;
	
	private TreeModel treeModel;
	private double popSize;
	private double[] means;
	private double[] variances;
	
	public GammaProductLikelihood(TreeModel treeModel, double popSize, double[] means, double[] variances) {
		super(treeModel);
		this.treeModel = treeModel;
		this.popSize = popSize;
		this.means = means;
		this.variances = variances;
	}
	
	//make sure to provide a log(popSize)
	/*public GammaProductLikelihood(TreeModel treeModel, double popSize) {
		super(treeModel);
		this.treeModel = treeModel;
		this.popSize = popSize;
	}*/
	
	public double calculateLogLikelihood() {
		
		double logPDF = 0.0;
		
		if (USE_EXPONENTIAL) {
			
			CoalescentTreeIntervalStatistic ctis = new CoalescentTreeIntervalStatistic(treeModel);
			
			for (int i = 0; i < ctis.getDimension(); i++) {
				
				int combinations = (int)ctis.getLineageCount(i)*((int)ctis.getLineageCount(i)-1)/2;
				double branchLength = ctis.getStatisticValue(i);
				
				if (ctis.getLineageCount(i) != 1) {
					
					//GammaDistribution is not parameterized in terms of alpha and beta, but in terms of shape and scale!
					if (i == ctis.getDimension()-1) {
						//coalescent event at root: exponential density
						logPDF += GammaDistribution.logPdf(branchLength, 1.0, 1.0/(combinations*(1.0/popSize))) - 1.0*Math.log(combinations);
					} else if (ctis.getLineageCount(i) > ctis.getLineageCount(i+1)) {
						//coalescent event: exponential density
						logPDF += GammaDistribution.logPdf(branchLength, 1.0, 1.0/(combinations*(1.0/popSize))) - 1.0*Math.log(combinations);
					} else {
						//sampling event: exponential tail probability
						logPDF += Math.log(1-GammaDistribution.cdf(branchLength, 1.0, 1.0/(combinations*(1.0/popSize))));
					}
					
				}
				
			}
			
		} else {
			
			/*System.err.println("\nProvided empirical means and variances: ");
			for (int i = 0; i < means.length; i++) {
				System.err.println(means[i] + "   " + variances[i]);
			}*/
			
			//calculate alpha and beta for the gamma distributions
			double[] alphas = new double[means.length];
			for (int i = 0; i < alphas.length; i++) {
				alphas[i] = means[i]*means[i]/variances[i];
			}
			double[] betas = new double[means.length];
			for (int i = 0; i < betas.length; i++) {
				betas[i] = means[i]/variances[i];
			}
			
			if (REDUCE_TO_EXPONENTIAL) {
				for (int i = 0; i < alphas.length; i++) {
					alphas[i] = 1.0;
				}
				for (int i = 0; i < betas.length; i++) {
					betas[i] = 1.0/popSize;
				}
			}
			
			/*System.err.println("\nResulting alphas and betas: ");
			for (int i = 0; i < alphas.length; i++) {
				System.err.println(alphas[i] + "   " + betas[i]);
			}*/
			
			CoalescentTreeIntervalStatistic ctis = new CoalescentTreeIntervalStatistic(treeModel);
			
			//System.err.println(treeModel);
			
			int coalescentIntervalCounter = 0;
			for (int i = 0; i < ctis.getDimension(); i++) {
				
				int combinations = (int)ctis.getLineageCount(i)*((int)ctis.getLineageCount(i)-1)/2;
				//System.err.println("combinations = " + combinations);
				double branchLength = ctis.getStatisticValue(i);
				//System.err.println("branchLength = " + branchLength);
				
				if (ctis.getLineageCount(i) != 1) {
					
					//GammaDistribution is not parameterized in terms of alpha and beta, but in terms of shape and scale!
					if (i == ctis.getDimension()-1) {
						//coalescent event at root: gamma density
						//System.err.print("coalescent event at root: ");						
						logPDF += GammaDistribution.logPdf(branchLength, alphas[coalescentIntervalCounter], 1.0/(combinations*betas[coalescentIntervalCounter])) - alphas[coalescentIntervalCounter]*Math.log(combinations);
						//System.err.println(GammaDistribution.logPdf(branchLength, alphas[coalescentIntervalCounter], 1.0/(combinations*betas[coalescentIntervalCounter])) - alphas[coalescentIntervalCounter]*Math.log(combinations));
						//coalesent event: move towards next empirical mean/variance but nowhere else to go
						//coalescentIntervalCounter++;
					} else if (ctis.getLineageCount(i) > ctis.getLineageCount(i+1)) {
						//coalescent event: gamma density
						//System.err.print("coalescent event (not at root): ");						
						logPDF += GammaDistribution.logPdf(branchLength, alphas[coalescentIntervalCounter], 1.0/(combinations*betas[coalescentIntervalCounter])) - alphas[coalescentIntervalCounter]*Math.log(combinations);
						//System.err.println(GammaDistribution.logPdf(branchLength, alphas[coalescentIntervalCounter], 1.0/(combinations*betas[coalescentIntervalCounter])) - alphas[coalescentIntervalCounter]*Math.log(combinations));
						//coalesent event: move towards next empirical mean/variance
						coalescentIntervalCounter++;
					} else {
						//sampling event: gamma tail probability
						//System.err.print("sampling event: ");
						logPDF += Math.log(1-GammaDistribution.cdf(branchLength, alphas[coalescentIntervalCounter], 1.0/(combinations*betas[coalescentIntervalCounter])));
						//System.err.println(Math.log(1-GammaDistribution.cdf(branchLength, alphas[coalescentIntervalCounter], 1.0/(combinations*betas[coalescentIntervalCounter]))));
					}
					
				}
				
			}
			
		}
		
		//System.err.println("logPDF = " + logPDF);
		//System.exit(0);
		
		return logPDF;
		
	}
	
	
	
	public double calculateOldLogLikelihood() {
		
		double logPDF = 0.0;
		
		//Should not be used
		double logPopSize = 0.0;
		
		//means and variances are probably in the reverse order
		//System.err.println("\nProvided empirical means and variances: ");
		//for (int i = 0; i < means.length; i++) {
			//System.err.println(means[i] + "   " + variances[i]);
		//}
		
		CoalescentTreeIntervalStatistic ctis = new CoalescentTreeIntervalStatistic(treeModel);
		
		//System.err.println("\nDimension = " + ctis.getDimension() + "\nLineage info: ");
		//for (int i = ctis.getDimension()-1; i >= 0; i--) {
			//System.err.println(ctis.getLineageCount(i));
		//}
		//System.err.println("\nStatistic values: ");
		//for (int i = ctis.getDimension()-1; i >= 0; i--) {
			//System.err.println(ctis.getStatisticValue(i));
		//}
		
		//ignore possibility of more than 1 dimension for now
		//System.err.println("\nPopulation size = " + popSize);
		
		//System.err.println("\nTree: " + treeModel);
		
		//calculate alpha and beta for the gamma distributions
		double[] alphas = new double[means.length];
		for (int i = 0; i < alphas.length; i++) {
			alphas[i] = means[i]*means[i]/variances[i];
		}
		double[] betas = new double[means.length];
		for (int i = 0; i < betas.length; i++) {
			betas[i] = means[i]/variances[i];
		}
		
		if (REDUCE_TO_EXPONENTIAL) {
			for (int i = 0; i < alphas.length; i++) {
				alphas[i] = 1.0;
			}
			for (int i = 0; i < betas.length; i++) {
				betas[i] = 1.0/logPopSize;
			}
		}
		
		int indicator = 0;
		for (int i = ctis.getDimension()-1; i > 0; i--) {
			//System.err.println("\nInterval " + i);
			if (i == ctis.getDimension()-1) {
				//coalescent event: gamma density
				//System.err.println("Coalescent event at root");
				//System.err.println("Interval length = " + ctis.getStatisticValue(i));
				//System.err.println("Lineage count = " + ctis.getLineageCount(i));
				int combinations = (int)ctis.getLineageCount(i)*((int)ctis.getLineageCount(i)-1)/2;
				//System.err.println("Combinations = " + combinations);
				if (!FIXED_TREE) {
					logPDF += GammaDistribution.logPdf(ctis.getStatisticValue(i), alphas[indicator], 1.0/(combinations*betas[indicator]));
					//System.err.println(GammaDistribution.logPdf(ctis.getStatisticValue(i), alphas[indicator], 1.0/(combinations*betas[indicator])));
				} else {
					logPDF += GammaDistribution.logPdf(ctis.getStatisticValue(i), alphas[indicator], 1.0/(combinations*betas[indicator])) - alphas[indicator]*Math.log(combinations);
					//System.err.println(GammaDistribution.logPdf(ctis.getStatisticValue(i), alphas[indicator], 1.0/(combinations*betas[indicator])) - alphas[indicator]*Math.log(combinations));
				}
				if (indicator < (means.length-1)) {
					indicator++;
				}
			} else if (ctis.getLineageCount(i) - ctis.getLineageCount(i+1) > 0) {
				//coalescent event: gamma density
				//System.err.println("Coalescent event");
				//System.err.println("Interval length = " + ctis.getStatisticValue(i));
				//System.err.println("Lineage count = " + ctis.getLineageCount(i));
				int combinations = (int)ctis.getLineageCount(i)*((int)ctis.getLineageCount(i)-1)/2;
				//System.err.println("Combinations = " + combinations);
				if (!FIXED_TREE) {
					logPDF += GammaDistribution.logPdf(ctis.getStatisticValue(i), alphas[indicator], 1.0/(combinations*betas[indicator]));
					//System.err.println(GammaDistribution.logPdf(ctis.getStatisticValue(i), alphas[indicator], 1.0/(combinations*betas[indicator])));
				} else {
					logPDF += GammaDistribution.logPdf(ctis.getStatisticValue(i), alphas[indicator], 1.0/(combinations*betas[indicator])) - alphas[indicator]*Math.log(combinations);
					//System.err.println(GammaDistribution.logPdf(ctis.getStatisticValue(i), alphas[indicator], 1.0/(combinations*betas[indicator])) - alphas[indicator]*Math.log(combinations));
				}
				if (indicator < (means.length-1)) {
					indicator++;
				}
			} else {
				//new sample: gamma tail probability
				//System.err.println("New sample");
				//System.err.println("Interval length = " + ctis.getStatisticValue(i));
				//System.err.println("Lineage count = " + ctis.getLineageCount(i));
				int combinations = (int)ctis.getLineageCount(i)*((int)ctis.getLineageCount(i)-1)/2;
				//System.err.println("Combinations = " + combinations);
				logPDF += Math.log(1-GammaDistribution.cdf(ctis.getStatisticValue(i), alphas[indicator], 1.0/(combinations*betas[indicator])));
				//System.err.println(Math.log(1-GammaDistribution.cdf(ctis.getStatisticValue(i), alphas[indicator], 1.0/(combinations*betas[indicator]))));
			}
		}
		
		//System.err.println("\nlogPDF = " + logPDF);
		//System.exit(0);
		
		return logPDF;
	}
	
	/**
	 * Overridden to always return false.
	 */
	protected boolean getLikelihoodKnown() {
		return false;
	}

}

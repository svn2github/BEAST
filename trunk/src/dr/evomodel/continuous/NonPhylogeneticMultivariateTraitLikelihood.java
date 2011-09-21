package dr.evomodel.continuous;

import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.tree.TreeModel;
import dr.inference.model.CompoundParameter;
import dr.inference.model.Model;
import dr.inference.model.Parameter;
import dr.math.distributions.WishartSufficientStatistics;
import dr.math.matrixAlgebra.Matrix;
import dr.math.matrixAlgebra.Vector;
import dr.util.Author;
import dr.util.Citable;
import dr.util.Citation;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Integrated multivariate trait likelihood that assumes a fully-conjugate prior on the root and
 * no underlying tree structure.
 *
 * @author Gabriela Cybis
 * @author Marc A. Suchard
 * @author Bridgett vonHoldt
 */
public class NonPhylogeneticMultivariateTraitLikelihood extends FullyConjugateMultivariateTraitLikelihood {

    public NonPhylogeneticMultivariateTraitLikelihood(String traitName,
                                                     TreeModel treeModel,
                                                     MultivariateDiffusionModel diffusionModel,
                                                     CompoundParameter traitParameter,
                                                     Parameter deltaParameter,
                                                     List<Integer> missingIndices,
                                                     boolean cacheBranches,
                                                     boolean scaleByTime,
                                                     boolean useTreeLength,
                                                     BranchRateModel rateModel,
                                                     Model samplingDensity,
                                                     boolean reportAsMultivariate,
                                                     double[] rootPriorMean,
                                                     double rootPriorSampleSize,
                                                     boolean reciprocalRates,
                                                     boolean exchangeableTips) {
        super(traitName, treeModel, diffusionModel, traitParameter, deltaParameter, missingIndices, cacheBranches,
                scaleByTime, useTreeLength, rateModel, samplingDensity, reportAsMultivariate, rootPriorMean,
                rootPriorSampleSize, reciprocalRates);
        this.exchangeableTips = exchangeableTips;
        this.zeroHeightTip = findZeroHeightTip(treeModel);
    }

    private int findZeroHeightTip(Tree tree) {
        for (int i = 0; i < tree.getExternalNodeCount(); ++i) {
            NodeRef tip = tree.getExternalNode(i);
            if (tree.getNodeHeight(tip) == 0.0) {
                return i;
            }
        }
        return -1;
    }

    protected void printInformtion() {
        StringBuffer sb = new StringBuffer("Creating non-phylogenetic multivariate diffusion model:\n");
        sb.append("\tTrait: ").append(traitName).append("\n");
        sb.append("\tDiffusion process: ").append(diffusionModel.getId()).append("\n");
        sb.append("\tExchangeable tips:").append((exchangeableTips ? "yes" : "no")).append("\n");
        sb.append(extraInfo());
        sb.append("\tPlease cite:\n");
        sb.append(Citable.Utils.getCitationString(this));


        sb.append("\n\tDiffusion dimension   : ").append(dimTrait).append("\n");
        sb.append(  "\tNumber of observations: ").append(numData).append("\n");
        Logger.getLogger("dr.evomodel").info(sb.toString());
    }
        
    public List<Citation> getCitations() {
        List<Citation> citations = new ArrayList<Citation>();
        citations.add(
                new Citation(
                        new Author[]{                                                           
                                new Author("MA", "Suchard"),
                                new Author("P", "Lemey"),
                                new Author("A", "Rambaut"),
                        },
                        Citation.Status.IN_PREPARATION
                )
        );
        return citations;
    }

    private class SufficientStatistics {
        double sumWeight;
        double productWeight;
        double outerProduct;
        int nonMissingTips;

        SufficientStatistics(double sumWeight, double productWeight, double outerProduct, int nonMissingTips) {
            this.sumWeight = sumWeight;
            this.productWeight = productWeight;
            this.outerProduct = outerProduct;
            this.nonMissingTips = nonMissingTips;
        }
    }

    protected double getLengthToRoot(NodeRef nodeRef) {
        final double height;
        if (exchangeableTips) {
            height = getRescaledLengthToRoot(treeModel.getExternalNode(zeroHeightTip));
        } else {
            height = getRescaledLengthToRoot(nodeRef);
        }
        return height;
    }

    private SufficientStatistics computeOuterProductsForTips(double[][] traitPrecision, double[] tmpVector,
                                                             WishartSufficientStatistics wishartStatistics) {

        // Compute the contribution of each datum at the root
        final int rootIndex = treeModel.getRoot().getNumber();
        final int meanOffset = dim * rootIndex;

        // Zero-out root mean
        for (int d = 0; d < dim; ++d) {
            meanCache[meanOffset + d] = 0;
        }

        double outerProducts = 0.0;

        // Compute the contribution of each datum at the root
        double productWeight = 1.0;
        double sumWeight = 0.0;
        int nonMissingTips = 0;
        for (int i = 0; i < treeModel.getExternalNodeCount(); ++i) {
            NodeRef tipNode = treeModel.getExternalNode(i);
            final int tipNumber = tipNode.getNumber();

            double tipWeight = 0.0;
            if (!missing[tipNumber]) {
                
                tipWeight = 1.0 / getLengthToRoot(tipNode);

                int tipOffset = dim * tipNumber;
                int rootOffset = dim * rootIndex;

                for (int datum = 0; datum < numData; ++datum) {
                    // TODO Make faster when dimTrait == 1

                    // Add weighted tip value
                    for (int d = 0; d < dimTrait; ++d) {
                        meanCache[rootOffset + d] += tipWeight * meanCache[tipOffset + d];
                        tmpVector[d] = meanCache[tipOffset + d];
                    }

                    // Compute outer product
                    double yAy = computeWeightedAverageAndSumOfSquares(tmpVector, Ay, traitPrecision, dimTrait,
                                    tipWeight);
                    outerProducts += yAy;

                    if (DEBUG_NO_TREE) {
                        System.err.println("OP for " + tipNumber + " = " + yAy);
                        System.err.println("Value  = " + new Vector(tmpVector));
                        System.err.print  ("Prec   =\n" + new Matrix(traitPrecision));
                        System.err.println("weight = " + tipWeight + "\n");
                    }

                    tipOffset += dimTrait;
                    rootOffset += dimTrait;
                }
            }

            if (tipWeight > 0.0) {
                sumWeight += tipWeight;
                productWeight *= tipWeight;
                ++nonMissingTips;
            }
        }

        lowerPrecisionCache[rootIndex] = sumWeight;

        for (int d = 0; d < dim; ++d) {
            meanCache[meanOffset + d] /= sumWeight;
        }

        return new SufficientStatistics(sumWeight, productWeight, outerProducts,
                nonMissingTips);
    }

    protected boolean peel() {
        return false;
    }

    public double calculateLogLikelihood() {
        
        double[][] traitPrecision = diffusionModel.getPrecisionmatrix();
        double logDetTraitPrecision = Math.log(diffusionModel.getDeterminantPrecisionMatrix());
        double[] marginalRoot = tmp2;

        final boolean computeWishartStatistics = getComputeWishartSufficientStatistics();

        if (computeWishartStatistics) {
                wishartStatistics = new WishartSufficientStatistics(dimTrait);
        }

        // Compute the contribution of each datum at the root
        SufficientStatistics stats = computeOuterProductsForTips(traitPrecision, tmp2, wishartStatistics);
        double conditionalSumWeight = stats.sumWeight;
        double conditionalProductWeight = stats.productWeight;
        double outerProducts = stats.outerProduct;
        int nonMissingTips = stats.nonMissingTips;

        // Add in prior and integrate
        double sumWeight = conditionalSumWeight + rootPriorSampleSize;
        double productWeight = conditionalProductWeight * rootPriorSampleSize;
        double rootPrecision = productWeight / sumWeight;

        final int rootIndex = treeModel.getRoot().getNumber();
        int rootOffset = dim * rootIndex;

        for (int datum = 0; datum < numData; ++datum) {

            // Determine marginal root (scaled) mean
            for (int d = 0; d < dimTrait; ++d) {
                marginalRoot[d] = conditionalSumWeight * meanCache[rootOffset + d] + rootPriorSampleSize * rootPriorMean[d];
            }

            // Compute outer product contribution from prior
            double yAy1 = computeWeightedAverageAndSumOfSquares(rootPriorMean, Ay, traitPrecision, dimTrait,
                    rootPriorSampleSize);
            outerProducts += yAy1;  // TODO Only need to compute once

            if (DEBUG_NO_TREE) {
                System.err.println("OP for root");
                System.err.println("Value  = " + new Vector(rootPriorMean));
                System.err.print  ("Prec   = \n" + new Matrix(traitPrecision));
                System.err.println("Weight = " + rootPriorSampleSize + "\n");
            }

            // Compute outer product differences to complete square
            double yAy2 = computeWeightedAverageAndSumOfSquares(marginalRoot, Ay, traitPrecision, dimTrait,
                    1.0 / sumWeight);
            outerProducts -= yAy2;

            rootOffset += dimTrait;
        }

        if (DEBUG_NO_TREE) {
            System.err.println("SumWeight    : " + sumWeight);
            System.err.println("ProductWeight: " + productWeight);
            System.err.println("Total OP     : " + outerProducts);
        }

        // Compute log likelihood
        double logLikelihood =
                -LOG_SQRT_2_PI * dimTrait * nonMissingTips * numData
                + 0.5 * logDetTraitPrecision * nonMissingTips * numData
                + 0.5 * Math.log(rootPrecision) * dimTrait * numData        
                - 0.5 * outerProducts;

        if (DEBUG_NO_TREE) {
            System.err.println("logLikelihood (final) = " + logLikelihood);
            System.err.println("numData = " + numData);
        }

        areStatesRedrawn = false;  // Should redraw internal node states when needed
        return logLikelihood;
    }

    private final boolean exchangeableTips;
    private final int zeroHeightTip;

    private static final boolean DEBUG_NO_TREE = false;
}
package dr.evomodelxml.speciation;

import dr.evolution.tree.Tree;
import dr.evolution.util.Taxa;
import dr.evolution.util.Taxon;
import dr.evolution.util.TaxonList;
import dr.evomodel.speciation.SpeciationLikelihood;
import dr.evomodel.speciation.SpeciationModel;
import dr.inference.distribution.DistributionLikelihood;
import dr.inference.model.Statistic;
import dr.math.distributions.Distribution;
import dr.xml.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 */
public class SpeciationLikelihoodParser extends AbstractXMLObjectParser {

    public static final String SPECIATION_LIKELIHOOD = "speciationLikelihood";
    public static final String MODEL = "model";
    public static final String TREE = "speciesTree";
    public static final String INCLUDE = "include";
    public static final String EXCLUDE = "exclude";
   // public static final String COEFFS = "coefficients";

    public static final String CALIBRATION = "calibration";
    public static final String PARENT = dr.evomodelxml.tree.TMRCAStatisticParser.PARENT;

    public String getParserName() {
        return SPECIATION_LIKELIHOOD;
    }

    static private <T> void swap(List<T> l)  {
        assert l.size() == 2;

        final T o = l.get(0);
        l.set(0, l.get(1));
        l.set(1, o);
    }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        XMLObject cxo = xo.getChild(MODEL);
        SpeciationModel specModel = (SpeciationModel) cxo.getChild(SpeciationModel.class);

        cxo = xo.getChild(TREE);
        Tree tree = (Tree) cxo.getChild(Tree.class);

        Set<Taxon> excludeTaxa = null;

        if (xo.hasChildNamed(INCLUDE)) {
            excludeTaxa = new HashSet<Taxon>();
            for (int i = 0; i < tree.getTaxonCount(); i++) {
                excludeTaxa.add(tree.getTaxon(i));
            }

            cxo = xo.getChild(INCLUDE);
            for (int i = 0; i < cxo.getChildCount(); i++) {
                TaxonList taxonList = (TaxonList) cxo.getChild(i);
                for (int j = 0; j < taxonList.getTaxonCount(); j++) {
                    excludeTaxa.remove(taxonList.getTaxon(j));
                }
            }
        }

        if (xo.hasChildNamed(EXCLUDE)) {
            excludeTaxa = new HashSet<Taxon>();
            cxo = xo.getChild(EXCLUDE);
            for (int i = 0; i < cxo.getChildCount(); i++) {
                TaxonList taxonList = (TaxonList) cxo.getChild(i);
                for (int j = 0; j < taxonList.getTaxonCount(); j++) {
                    excludeTaxa.add(taxonList.getTaxon(j));
                }
            }
        }
        if (excludeTaxa != null) {
            Logger.getLogger("dr.evomodel").info("Speciation model excluding " + excludeTaxa.size() + " taxa from prior - " +
                    (tree.getTaxonCount() - excludeTaxa.size()) + " taxa remaining.");
        }

        final XMLObject cal = xo.getChild(CALIBRATION);
        if( cal != null ) {
            if( excludeTaxa != null ) {
                throw new XMLParseException("Sorry, not implemented: internal calibration prior + excluded taxa");
            }

            if( ! specModel.supportsInternalCalibration() ) {
              throw new XMLParseException("Sorry, not implemented: internal calibration prior for this model.");
            }

            List<Distribution> dists = new ArrayList<Distribution>();
            List<Taxa> taxa = new ArrayList<Taxa>();
            List<Boolean> forParent = new ArrayList<Boolean>();

            for(int k = 0; k < cal.getChildCount(); ++k) {
                final Object ck = cal.getChild(k);
                if ( DistributionLikelihood.class.isInstance(ck) ) {
                    dists.add( ((DistributionLikelihood) ck).getDistribution() );
                } else if ( Distribution.class.isInstance(ck) ) {
                    dists.add((Distribution) ck);
                } else if ( Taxa.class.isInstance(ck) ) {
                    final Taxa tx = (Taxa) ck;
                    taxa.add(tx);
                    forParent.add( tx.getTaxonCount() == 1 );
                } else {
                    XMLObject cko = (XMLObject) ck;
                    assert cko.getChildCount() == 2;

                    for(int i = 0; i < 2; ++i) {
                        final Object chi = cko.getChild(i);
                        if ( DistributionLikelihood.class.isInstance(chi) ) {
                            dists.add( ((DistributionLikelihood) chi).getDistribution() );
                        } else if ( Distribution.class.isInstance(chi) ) {
                            dists.add((Distribution) chi);
                        } else if ( Taxa.class.isInstance(chi) ) {
                            taxa.add((Taxa) chi);
                            boolean fp =  ((Taxa) chi).getTaxonCount() == 1;
                            if( cko.hasAttribute(PARENT) ) {
                                boolean ufp = cko.getBooleanAttribute(PARENT);
                                if( fp && ! ufp ) {
                                   throw new XMLParseException("forParent==false for a single taxon?? (must be true)");
                                }
                                fp = ufp;
                            }
                            forParent.add(fp);
                        } else {
                            assert false;
                        }
                    }
                }
            }

            if( dists.size() != taxa.size() ) {
                throw new XMLParseException("Mismatch in number of distributions and taxa specs");
            }

            final Statistic userPDF = (Statistic) cal.getChild(Statistic.class);
            if( userPDF == null ) {
                if( dists.size() > 2 ) {
                    throw new XMLParseException("Sorry, not implemented: multiple internal calibrations - please provide the " +
                            "log marginal explicitly.");
                }
                if( taxa.get(0).getTaxonCount() > taxa.get(1).getTaxonCount() ) {
                    swap(taxa);
                    swap(dists);
                    swap(forParent);
                }
                
                if( ! taxa.get(1).containsAll(taxa.get(0)) ) {
                    throw new XMLParseException("Sorry, not implemented: two non-nested clades");
                }
            }

            return new SpeciationLikelihood(tree, specModel, null, dists, taxa, forParent, userPDF);
        }

        return new SpeciationLikelihood(tree, specModel, excludeTaxa, null);
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "This element represents the likelihood of the tree given the speciation.";
    }

    public Class getReturnType() {
        return SpeciationLikelihood.class;
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }

    private final XMLSyntaxRule[] calibrationPoint = {
            AttributeRule.newBooleanRule(PARENT, true),
            new XORRule(
                    new ElementRule(Distribution.class),
                    new ElementRule(DistributionLikelihood.class)),
            new ElementRule(Taxa.class)
    };

    private final XMLSyntaxRule[] calibration = {
//            AttributeRule.newDoubleArrayRule(COEFFS,true, "use log(lam) -lam * c[0] + sum_k=1..n (c[k+1] * e**(-k*lam*x)) " +
//                    "as a calibration correction instead of default - used when additional constarints are put on the topology."),
            new ElementRule(Statistic.class, true),
            new XORRule(
                    new ElementRule(Distribution.class, 1, 100),
                    new ElementRule(DistributionLikelihood.class, 1, 100)),
            new ElementRule(Taxa.class, 1, 100),
            new ElementRule("point", calibrationPoint, 0, 100)
    };

    private final XMLSyntaxRule[] rules = {
            new ElementRule(MODEL, new XMLSyntaxRule[]{
                    new ElementRule(SpeciationModel.class)
            }),
            new ElementRule(TREE, new XMLSyntaxRule[]{
                    new ElementRule(Tree.class)
            }),

            new ElementRule(INCLUDE, new XMLSyntaxRule[]{
                    new ElementRule(Taxa.class, 1, Integer.MAX_VALUE)
            }, "One or more subsets of taxa which should be included from calculate the likelihood (the remaining taxa are excluded)", true),

            new ElementRule(EXCLUDE, new XMLSyntaxRule[]{
                    new ElementRule(Taxa.class, 1, Integer.MAX_VALUE)
            }, "One or more subsets of taxa which should be excluded from calculate the likelihood (which is calculated on the remaining subtree)", true),

            new ElementRule(CALIBRATION, calibration, true),
    };

}

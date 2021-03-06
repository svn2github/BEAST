package dr.app.beagle.evomodel.parsers;

//import dr.app.beagle.evomodel.treelikelihood.RestrictedPartialsSequenceLikelihood;
import dr.app.beagle.evomodel.substmodel.FrequencyModel;
import dr.evolution.alignment.PatternList;
import dr.evolution.alignment.Patterns;
import dr.evolution.alignment.SitePatterns;
import dr.app.beagle.evomodel.sitemodel.BranchSiteModel;
import dr.app.beagle.evomodel.sitemodel.GammaSiteRateModel;
import dr.app.beagle.evomodel.sitemodel.HomogenousBranchSiteModel;
import dr.app.beagle.evomodel.treelikelihood.BeagleTreeLikelihood;
import dr.app.beagle.evomodel.treelikelihood.PartialsRescalingScheme;
import dr.evolution.tree.Tree;
import dr.evolution.util.TaxonList;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.newtreelikelihood.TreeLikelihood;
import dr.evomodel.tree.TreeModel;
import dr.evomodel.treelikelihood.TipPartialsModel;
import dr.inference.model.Likelihood;
import dr.inference.model.CompoundLikelihood;
import dr.inference.model.Parameter;
import dr.xml.*;

import java.util.*;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author Marc Suchard
 * @version $Id$
 */
public class TreeLikelihoodParser extends AbstractXMLObjectParser {

    public static final String BEAGLE_INSTANCE_COUNT = "beagle.instance.count";

    public static final String TREE_LIKELIHOOD = TreeLikelihood.TREE_LIKELIHOOD;
    public static final String USE_AMBIGUITIES = "useAmbiguities";
    public static final String INSTANCE_COUNT = "instanceCount";
    //    public static final String DEVICE_NUMBER = "deviceNumber";
//    public static final String PREFER_SINGLE_PRECISION = "preferSinglePrecision";
    public static final String SCALING_SCHEME = "scalingScheme";
    public static final String PARTIALS_RESTRICTION = "partialsRestriction";

    public String getParserName() {
        return TREE_LIKELIHOOD;
    }

    protected BeagleTreeLikelihood createTreeLikelihood(PatternList patternList, TreeModel treeModel,
                                                        BranchSiteModel branchSiteModel, GammaSiteRateModel siteRateModel,
                                                        BranchRateModel branchRateModel,
                                                        boolean useAmbiguities, PartialsRescalingScheme scalingScheme,
                                                        Map<Set<String>, Parameter> partialsRestrictions,
                                                        XMLObject xo) throws XMLParseException {
        return new BeagleTreeLikelihood(
                patternList,
                treeModel,
                branchSiteModel,
                siteRateModel,
                branchRateModel,
                useAmbiguities,
                scalingScheme,
                partialsRestrictions
        );
    }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        boolean useAmbiguities = xo.getAttribute(USE_AMBIGUITIES, false);
        int instanceCount = xo.getAttribute(INSTANCE_COUNT, 1);
        if (instanceCount < 1) {
            instanceCount = 1;
        }

        String ic = System.getProperty(BEAGLE_INSTANCE_COUNT);
        if (ic != null && ic.length() > 0) {
            instanceCount = Integer.parseInt(ic);
        }

        PatternList patternList = (PatternList) xo.getChild(PatternList.class);
        TreeModel treeModel = (TreeModel) xo.getChild(TreeModel.class);
        GammaSiteRateModel siteRateModel = (GammaSiteRateModel) xo.getChild(GammaSiteRateModel.class);

        FrequencyModel rootFreqModel = (FrequencyModel) xo.getChild(FrequencyModel.class);

        BranchSiteModel branchSiteModel = new HomogenousBranchSiteModel(
                siteRateModel.getSubstitutionModel(),
                (rootFreqModel != null ? rootFreqModel :
                        siteRateModel.getSubstitutionModel().getFrequencyModel())
        );

        BranchRateModel branchRateModel = (BranchRateModel) xo.getChild(BranchRateModel.class);

        PartialsRescalingScheme scalingScheme = PartialsRescalingScheme.DEFAULT;
        if (xo.hasAttribute(SCALING_SCHEME)) {
            scalingScheme = PartialsRescalingScheme.parseFromString(xo.getStringAttribute(SCALING_SCHEME));
            if (scalingScheme == null)
                throw new XMLParseException("Unknown scaling scheme '"+xo.getStringAttribute(SCALING_SCHEME)+"' in "+
                        "BeagleTreeLikelihood object '"+xo.getId());

        }

        Map<Set<String>, Parameter> partialsRestrictions = null;

        if (xo.hasChildNamed(PARTIALS_RESTRICTION)) {
            XMLObject cxo = (XMLObject) xo.getChild(PARTIALS_RESTRICTION);
            TaxonList taxonList = (TaxonList) cxo.getChild(TaxonList.class);
            Parameter parameter = (Parameter) cxo.getChild(Parameter.class);
            try {
                Tree.Utils.getLeavesForTaxa(treeModel, taxonList);
            } catch (Tree.MissingTaxonException e) {
                throw new XMLParseException("Unable to parse taxon list: " + e.getMessage());
            }
            throw new XMLParseException("Restricting internal nodes is not yet implemented.  Contact Marc");

        }

        if (xo.getChild(TipPartialsModel.class) != null) {
            throw new XMLParseException("Sequence Error Models are not supported under BEAGLE yet. Please use Native BEAST Likelihood.");
        }

        if (instanceCount == 1) {
            return createTreeLikelihood(
                    patternList,
                    treeModel,
                    branchSiteModel,
                    siteRateModel,
                    branchRateModel,
                    useAmbiguities,
                    scalingScheme,
                    partialsRestrictions,
                    xo
            );
        }

        List<Likelihood> likelihoods = new ArrayList<Likelihood>();
        for (int i = 0; i < instanceCount; i++) {
            if (!(patternList instanceof SitePatterns)) {
                throw new XMLParseException("BEAGLE_INSTANCES option cannot be used with BEAUti-selected codon partitioning.");
            }
            Patterns subPatterns = new Patterns((SitePatterns)patternList, 0, 0, 1, i, instanceCount);
            BeagleTreeLikelihood treeLikelihood = createTreeLikelihood(
                    subPatterns,
                    treeModel,
                    branchSiteModel,
                    siteRateModel,
                    branchRateModel,
                    useAmbiguities,
                    scalingScheme,
                    partialsRestrictions,
                    xo);
            treeLikelihood.setId(xo.getId() + "_" + instanceCount);
            likelihoods.add(treeLikelihood);
        }

        return new CompoundLikelihood(instanceCount, likelihoods);
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "This element represents the likelihood of a patternlist on a tree given the site model.";
    }

    public Class getReturnType() {
        return Likelihood.class;
    }

    public XMLSyntaxRule[] getSyntaxRules() {
        return new XMLSyntaxRule[] {
                AttributeRule.newBooleanRule(USE_AMBIGUITIES, true),
                new ElementRule(PatternList.class),
                new ElementRule(TreeModel.class),
                new ElementRule(GammaSiteRateModel.class),
                new ElementRule(BranchRateModel.class, true),
                AttributeRule.newStringRule(SCALING_SCHEME,true),
                new ElementRule(PARTIALS_RESTRICTION, new XMLSyntaxRule[] {
                        new ElementRule(TaxonList.class),
                        new ElementRule(Parameter.class),
                }, true),
                new ElementRule(FrequencyModel.class, true),
                new ElementRule(TipPartialsModel.class, true)
        };
    }
}

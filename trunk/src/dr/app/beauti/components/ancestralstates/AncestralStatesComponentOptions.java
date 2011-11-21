package dr.app.beauti.components.ancestralstates;

import dr.app.beauti.options.*;
import dr.app.beauti.types.*;
import dr.evolution.util.Taxa;

import java.util.*;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public class AncestralStatesComponentOptions implements ComponentOptions {

    public AncestralStatesComponentOptions() {
    }

    public void createParameters(final ModelOptions modelOptions) {
    }

    public void selectParameters(final ModelOptions modelOptions, final List<Parameter> params) {
    }

    public void selectStatistics(final ModelOptions modelOptions, final List<Parameter> stats) {
        // no statistics required
    }

    public void selectOperators(final ModelOptions modelOptions, final List<Operator> ops) {
    }

    private AncestralStateOptions getOptions(final AbstractPartitionData partition) {
        AncestralStateOptions options = ancestralStateOptionsMap.get(partition);
        if (options == null) {
            options = new AncestralStateOptions();
            ancestralStateOptionsMap.put(partition, options);
        }
        return options;
    }

    public boolean usingAncestralStates(final AbstractPartitionData partition) {
            return reconstructAtNodes(partition) || reconstructAtMRCA(partition) || robustCounting(partition);
        }

    public boolean reconstructAtNodes(final AbstractPartitionData partition) {
        return getOptions(partition).reconstructAtNodes;
    }

    public void setReconstructAtNodes(final AbstractPartitionData partition, boolean reconstructAtNodes) {
        getOptions(partition).reconstructAtNodes = reconstructAtNodes;
    }

    public boolean reconstructAtMRCA(final AbstractPartitionData partition) {
        return getOptions(partition).reconstructAtMRCA;
    }

    public void setReconstructAtMRCA(final AbstractPartitionData partition, boolean reconstructAtMRCA) {
        getOptions(partition).reconstructAtMRCA = reconstructAtMRCA;
    }

    public String getMRCATaxonSet(final AbstractPartitionData partition) {
        AncestralStateOptions options = ancestralStateOptionsMap.get(partition);
        return options.mrcaTaxonSetName;
    }

    public void setMRCATaxonSet(final AbstractPartitionData partition, String taxonSetName) {
        getOptions(partition).mrcaTaxonSetName = taxonSetName;
    }

    public boolean robustCounting(final AbstractPartitionData partition) {
        return getOptions(partition).robustCounting;
    }

    public void setRobustCounting(final AbstractPartitionData partition, boolean robustCounting) {
        getOptions(partition).robustCounting = robustCounting;
    }

    public boolean dNdSRobustCounting(final AbstractPartitionData partition) {
        return getOptions(partition).dNdSRobustCounting;
    }

    public void setDNdSRobustCounting(final AbstractPartitionData partition, boolean dNdSRobustCounting) {
        if (dNdSRobustCounting && partition.getPartitionSubstitutionModel().getCodonPartitionCount() != 3) {
            throw new IllegalArgumentException("dNdS Robust Counting can only be used with 3 codon partition models.");
        }
        getOptions(partition).dNdSRobustCounting = dNdSRobustCounting;
    }

    public boolean dNdSRobustCountingAvailable(final AbstractPartitionData partition) {
        // todo  - are there other constraints of the use of this model?
        return partition.getPartitionSubstitutionModel().getCodonPartitionCount() == 3;
    }

    class AncestralStateOptions {
        boolean reconstructAtNodes = false;
        boolean reconstructAtMRCA = false;
        String mrcaTaxonSetName = null;
        boolean robustCounting = false;
        boolean dNdSRobustCounting = false;
    };

    private final Map<AbstractPartitionData, AncestralStateOptions> ancestralStateOptionsMap = new HashMap<AbstractPartitionData, AncestralStateOptions>();

}
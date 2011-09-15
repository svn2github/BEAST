package dr.app.beauti.components.continuous;

import dr.app.beauti.options.*;
import dr.app.beauti.types.OperatorType;
import dr.app.beauti.types.PriorScaleType;
import dr.evolution.continuous.Continuous;
import dr.evolution.datatype.ContinuousDataType;
import dr.util.Attribute;
import dr.util.DataTable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */

public class ContinuousComponentOptions implements ComponentOptions {

    public final static String PRECISION_GIBBS_OPERATOR = "precisionGibbsOperator";
    public final static String HALF_DF = "halfDF";

    final private BeautiOptions options;

	public ContinuousComponentOptions(final BeautiOptions options) {
		this.options = options;
	}

	public void createParameters(ModelOptions modelOptions) {
        for (AbstractPartitionData partitionData : options.getAllPartitionData(ContinuousDataType.INSTANCE)) {
            String prefix = partitionData.getPrefix(ContinuousDataType.INSTANCE);
            if (!modelOptions.parameterExists(prefix + HALF_DF)) {
                modelOptions.createParameterGammaPrior(prefix + HALF_DF, "half DF of 1 parameter gamma distributed RRW",
                        PriorScaleType.NONE, 0.5, 0.0, Double.POSITIVE_INFINITY, false);
                modelOptions.createScaleOperator(prefix + HALF_DF, modelOptions.demoTuning, 1.0);
            }
        }
	}

	public void selectOperators(ModelOptions modelOptions, List<Operator> ops) {
        for (AbstractPartitionData partitionData : options.getAllPartitionData(ContinuousDataType.INSTANCE)) {
            if (partitionData.getPartitionSubstitutionModel().getContinuousSubstModelType() == ContinuousSubstModelType.GAMMA_RRW) {
                ops.add(modelOptions.getOperator(partitionData.getPrefix(ContinuousDataType.INSTANCE) + HALF_DF));
            }
        }
	}

	public void selectParameters(ModelOptions modelOptions, List<Parameter> params) {
        for (AbstractPartitionData partitionData : options.getAllPartitionData(ContinuousDataType.INSTANCE)) {
            if (partitionData.getPartitionSubstitutionModel().getContinuousSubstModelType() == ContinuousSubstModelType.GAMMA_RRW) {
                params.add(modelOptions.getParameter(partitionData.getPrefix(ContinuousDataType.INSTANCE) + HALF_DF));
            }
        }
	}

	public void selectStatistics(ModelOptions modelOptions,
			List<Parameter> stats) {
		// Do nothing
	}

	public BeautiOptions getOptions() {
		return options;
	}
}
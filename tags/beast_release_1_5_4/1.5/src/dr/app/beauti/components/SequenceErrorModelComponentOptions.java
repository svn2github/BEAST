package dr.app.beauti.components;

import dr.app.beauti.enumTypes.OperatorType;
import dr.app.beauti.enumTypes.PriorScaleType;
import dr.app.beauti.enumTypes.SequenceErrorType;
import dr.app.beauti.options.*;

import java.util.List;

/**
 * @author Andrew Rambaut
 * @version $Id$
 */
public class SequenceErrorModelComponentOptions implements ComponentOptions {
    static public final String ERROR_MODEL = "errorModel";
    static public final String AGE_RATE = "ageRelatedErrorRate";
    static public final String BASE_RATE = "baseErrorRate";

    static public final String AGE_RATE_PARAMETER = ERROR_MODEL + "." + AGE_RATE;
    static public final String BASE_RATE_PARAMETER = ERROR_MODEL + "." + BASE_RATE;

    public SequenceErrorModelComponentOptions() {
    }

    public void createParameters(final ModelOptions modelOptions) {
        modelOptions.createParameterUniformPrior(AGE_RATE_PARAMETER,"age dependent sequence error rate",
                PriorScaleType.SUBSTITUTION_RATE_SCALE, 1.0E-8, 0.0, Double.POSITIVE_INFINITY);
        modelOptions.createParameterUniformPrior(BASE_RATE_PARAMETER,"base sequence error rate",
                PriorScaleType.UNITY_SCALE, 1.0E-8, 0.0, 1.0);

        modelOptions.createScaleOperator(AGE_RATE_PARAMETER, modelOptions.demoTuning, 3.0);
        modelOptions.createOperator(BASE_RATE_PARAMETER, OperatorType.RANDOM_WALK_REFLECTING, 0.05, 3.0);
    }

    public void selectParameters(final ModelOptions modelOptions, final List<Parameter> params) {
        if (hasAgeDependentRate()) {
            params.add(modelOptions.getParameter(AGE_RATE_PARAMETER));
        }
        if (hasBaseRate()) {
            params.add(modelOptions.getParameter(BASE_RATE_PARAMETER));
        }
    }

    public void selectStatistics(final ModelOptions modelOptions, final List<Parameter> stats) {
        // no statistics required
    }

    public void selectOperators(final ModelOptions modelOptions, final List<Operator> ops) {
        if (hasAgeDependentRate()) {
            ops.add(modelOptions.getOperator(AGE_RATE_PARAMETER));
        }
        if (hasBaseRate()) {
                ops.add(modelOptions.getOperator(BASE_RATE_PARAMETER));
        }
    }

    public boolean hasAgeDependentRate() {
        return (errorModelType == SequenceErrorType.AGE_ALL) || (errorModelType == SequenceErrorType.AGE_TRANSITIONS);
    }

    public boolean hasBaseRate() {
        return (errorModelType == SequenceErrorType.BASE_ALL) || (errorModelType == SequenceErrorType.BASE_TRANSITIONS);
    }

    public SequenceErrorType errorModelType = SequenceErrorType.NO_ERROR;
}
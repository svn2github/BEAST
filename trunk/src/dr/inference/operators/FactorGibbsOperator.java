package dr.inference.operators;

import dr.evomodel.continuous.LatentFactorModel;
import dr.math.matrixAlgebra.IllegalDimension;
import dr.math.matrixAlgebra.Matrix;
import dr.math.distributions.MultivariateNormalDistribution;
import dr.inference.model.Parameter;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 5/22/14
 * Time: 12:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class FactorGibbsOperator extends SimpleMCMCOperator implements GibbsOperator{
    private static final String FACTOR_GIBBS_OPERATOR="factorGibbsOperator";
    private LatentFactorModel LFM;
    private int numFactors=0;
    private Matrix idMat;
    public FactorGibbsOperator(LatentFactorModel LFM, double weight){
        this.LFM=LFM;
        setWeight(weight);
    }

    private Matrix getPrecision(){
        if(numFactors!=LFM.getFactorDimension()){
            numFactors=LFM.getFactorDimension();
            idMat=Matrix.buildIdentityTimesElementMatrix(numFactors, 1);
        }
        Matrix LoadMat=new Matrix(LFM.getLoadings().getParameterAsMatrix());
        Matrix colPrec=new Matrix(LFM.getColumnPrecision().getParameterAsMatrix());
        Matrix answer= null;
        try {
            answer = LoadMat.product(colPrec).product(LoadMat.transpose()).add(idMat);
        } catch (IllegalDimension illegalDimension) {
            illegalDimension.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return answer;
    }

    private Matrix getMean(){
        Matrix answer=null;
        Matrix data=new Matrix(LFM.getScaledData().getParameterAsMatrix());
        try {
            answer=getPrecision().inverse().product(new Matrix(LFM.getLoadings().getParameterAsMatrix())).product(new Matrix(LFM.getColumnPrecision().getParameterAsMatrix())).product(data);
        } catch (IllegalDimension illegalDimension) {
            illegalDimension.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return answer;
    }

    private void copy(double[] put, int i){
        Parameter working=LFM.getFactors().getParameter(i);
        for (int j = 0; j < working.getSize(); j++) {
           working.setParameterValueQuietly(j, put[j]);
        }
        working.fireParameterChangedEvent();
    }

    public int getStepCount() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getPerformanceSuggestion() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getOperatorName() {
        return FACTOR_GIBBS_OPERATOR;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public double doOperation() throws OperatorFailedException {
        Matrix mean=getMean();
        double[][] meanFull=mean.transpose().toComponents();
        double[][] precFull=getPrecision().toComponents();
        double[] nextList=null;
        double[] nextValue=null;
        for (int i = 0; i <mean.columns() ; i++) {
            nextList=meanFull[i];
            nextValue=MultivariateNormalDistribution.nextMultivariateNormalPrecision(nextList, precFull);
            copy(nextValue, i);
        }
        LFM.getFactors().fireParameterChangedEvent();

        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }


}

package dr.evomodel.epidemiology.casetocase;

import dr.inference.model.AbstractModel;
import dr.inference.model.Variable;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Matthew Hall
 * Date: 22/05/2012
 * Time: 14:40
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractOutbreak extends AbstractModel {

    public AbstractOutbreak(String name){
        super(name);
    }

    protected ArrayList<AbstractCase> cases;

    /*Likelihood for the root if it is painted with 'farm'  */

    public abstract double noTransmissionBranchLikelihood(AbstractCase farm, Integer farmInfectiousBy);

    public abstract double noTransmissionBranchLogLikelihood(AbstractCase farm, Integer farmInfectiousBy);


    /*Likelihood for a branch leading from a node painted 'parent' to a node painted 'child'. This deals with the
    probability that farm2 is infected at the first time and infectious at the second. The calculations for farm1
    will be done when that node is considered as a child, or as the root*/

    public abstract double transmissionBranchLikelihood(AbstractCase parent, AbstractCase child, Integer childInfected,
                                                        Integer childInfectiousBy);

    public abstract double transmissionBranchLogLikelihood(AbstractCase parent, AbstractCase child, Integer childInfected,
                                                           Integer childInfectiousBy);

    public ArrayList<AbstractCase> getCases(){
        return new ArrayList<AbstractCase>(cases);
    }

    public int size(){
        return cases.size();
    }

    public AbstractCase getCase(int i){
        return cases.get(i);
    }

    public AbstractCase getCase(String name){
        for(AbstractCase thisCase: cases){
            if(thisCase.getName().equals(name)){
                return thisCase;
            }
        }
        return null;
    }

}

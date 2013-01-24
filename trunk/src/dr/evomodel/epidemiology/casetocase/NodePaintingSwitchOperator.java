package dr.evomodel.epidemiology.casetocase;

import dr.evolution.tree.NodeRef;
import dr.evomodel.tree.TreeModel;
import dr.inference.operators.SimpleMCMCOperator;
import dr.math.MathUtils;
import dr.xml.*;

/**
 * Created with IntelliJ IDEA.
 * User: Matthew Hall
 * Date: 13/07/2012
 * Time: 14:46
 * To change this template use File | Settings | File Templates.
 */
public class NodePaintingSwitchOperator extends SimpleMCMCOperator{

    public static final String NODE_PAINTING_SWITCH_OPERATOR = "nodePaintingSwitchOperator";
    private CaseToCaseTransmissionLikelihood c2cLikelihood;


    public NodePaintingSwitchOperator(CaseToCaseTransmissionLikelihood c2cLikelihood, double weight){
        this.c2cLikelihood = c2cLikelihood;
        setWeight(weight);
    }

    public String getOperatorName(){
        return NODE_PAINTING_SWITCH_OPERATOR;
    }

    /*  Switch the painting of a randomly selected internal node from the painting of one of its children to the
    * painting of the other, and adjust the rest of the tree to ensure the result is still a painting.*/

    public double doOperation(){
        TreeModel tree = c2cLikelihood.getTree();
        int internalNodeCount = tree.getInternalNodeCount();
        int nodeToSwitch = MathUtils.nextInt(internalNodeCount);
        while(c2cLikelihood.isExtended() && c2cLikelihood.getSwitchLocks()[nodeToSwitch]){
            nodeToSwitch = MathUtils.nextInt(internalNodeCount);
        }
        NodeRef node = tree.getInternalNode(nodeToSwitch);
        AbstractCase currentCase = c2cLikelihood.getBranchMap()[node.getNumber()];
        for(int i=0; i<tree.getChildCount(node); i++){
            if(c2cLikelihood.getBranchMap()[tree.getChild(node,i).getNumber()]!=currentCase){
                adjustTree(tree, node, c2cLikelihood.getBranchMap(), c2cLikelihood.isExtended());
                flagForRecalculation(tree, node, c2cLikelihood.getRecalculationArray());
            }
        }
        c2cLikelihood.makeDirty();
        return 1;
    }

    /*Look at the children of the current node, pick the one that is not labelled the same, change the node's
    * label to that one, then go up the tree and replace any others higher up with the same label*/

    private static void adjustTree(TreeModel tree, NodeRef node, AbstractCase[] map, boolean extended){
        AbstractCase originalCase = map[node.getNumber()];
        if(tree.isExternal(node)){
            throw new RuntimeException("Node is external");
        }
        AbstractCase[] childCases = new AbstractCase[2];
        for(int i=0; i<tree.getChildCount(node); i++){
            childCases[i] = map[tree.getChild(node, i).getNumber()];
        }
        if(childCases[0]==childCases[1] && !extended){
            throw new RuntimeException("Node children are the same");
        }
        if(originalCase!=childCases[0] && originalCase!=childCases[1] && !extended){
            throw new RuntimeException("You've ended up with a node neither of whose children are have the same " +
                    "label as itself. Help!");
        }
        AbstractCase newCase;
        for(int i=0; i<2; i++){
            if(childCases[i]!=originalCase){
                newCase=childCases[i];
                map[node.getNumber()]=newCase;
                if(!tree.isRoot(node)){
                    changeParentNodes(tree, node, originalCase, newCase, map);
                }
            }
        }
    }

    private static void changeParentNodes(TreeModel tree, NodeRef node, AbstractCase originalCase, AbstractCase newCase,
                                          AbstractCase[] map){
        NodeRef parent = tree.getParent(node);
        if(map[parent.getNumber()]==originalCase){
            map[parent.getNumber()]=newCase;
            if(!tree.isRoot(parent)){
                changeParentNodes(tree, parent, originalCase, newCase, map);
            }
        }
    }

    private static void flagForRecalculation(TreeModel tree, NodeRef node, boolean[] flags){
        for(int i=0; i<tree.getChildCount(node); i++){
            flags[tree.getChild(node,i).getNumber()]=true;
        }
        NodeRef currentNode=node;
        while(!tree.isRoot(currentNode)){
            flags[currentNode.getNumber()]=true;
            currentNode = tree.getParent(currentNode);
        }
        flags[currentNode.getNumber()]=true;
    }

    public String getPerformanceSuggestion(){
        return "Not implemented";
    }

    /* Parser */

    public static XMLObjectParser PARSER = new AbstractXMLObjectParser(){

        public String getParserName(){
            return NODE_PAINTING_SWITCH_OPERATOR;
        }

        public Object parseXMLObject(XMLObject xo) throws XMLParseException {

            CaseToCaseTransmissionLikelihood ftLikelihood =
                    (CaseToCaseTransmissionLikelihood) xo.getChild(CaseToCaseTransmissionLikelihood.class);
            final double weight = xo.getDoubleAttribute("weight");
            return new NodePaintingSwitchOperator(ftLikelihood, weight);
        }

        public String getParserDescription(){
            return "This operator switches the painting of a random eligible internal node from the painting of one of " +
                    "its children to the painting of the other";
        }

        public Class getReturnType() {
            return NodePaintingSwitchOperator.class;
        }

        public XMLSyntaxRule[] getSyntaxRules() {
            return rules;
        }

        private final XMLSyntaxRule[] rules = {
                AttributeRule.newDoubleRule("weight"),
                new ElementRule(CaseToCaseTransmissionLikelihood.class),
        };
    };
}
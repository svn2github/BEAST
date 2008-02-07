/*
 * AddRemoveSubtreeOperator.java
 *
 * (c) 2002-2005 BEAST Development Core Team
 *
 * This package may be distributed under the
 * Lesser Gnu Public Licence (LGPL)
 */

package dr.evomodel.operators;

import dr.evolution.tree.MutableTree;
import dr.evolution.tree.NodeRef;
import dr.evolution.tree.Tree;
import dr.evomodel.tree.ARGModel;
import dr.evomodel.tree.ARGModel.Node;
import dr.inference.model.Parameter;
import dr.inference.model.VariableSizeCompoundParameter;
import dr.inference.model.VariableSizeParameter;
import dr.inference.operators.*;
import dr.math.MathUtils;
import dr.xml.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Adds and removes re-assortment events.
 * @author Erik Bloomquist 
 */

public class NewerARGEventOperator extends SimpleMCMCOperator implements CoercableMCMCOperator {

	public static final String ADD_PROBABILITY = "addProbability";
	public static final String ARG_EVENT_OPERATOR = "ARGEventOperator";
	public static final String INTERNAL_NODES = "internalNodes";
	public static final String INTERNAL_AND_ROOT = "internalNodesPlusRoot";
	public static final String NODE_RATES = "nodeRates";
	public static final String ROOT_MOVES_OK = "rootMovesOk";

	private ARGModel arg = null;
	private double size = 1.0;
	private double singlePartitionProbability = 0.0;
	private boolean isRecombination = false;
	private int mode = CoercableMCMCOperator.DEFAULT;
	private VariableSizeCompoundParameter internalNodeParameters;
	private VariableSizeCompoundParameter internalAndRootNodeParameters;
	private VariableSizeCompoundParameter nodeRates;
	private boolean rootMovesOK = false;
	
	private double aboveRootBifurcationProbability = 0.0;
	private double aboveRootReassortmentProbability = 0.0;
	private double extraAmountBeyondOldRoot = 10.0;
	
	public NewerARGEventOperator(ARGModel arg, int weight, double size, int mode,
	                           VariableSizeCompoundParameter param1,
	                           VariableSizeCompoundParameter param2,
	                           VariableSizeCompoundParameter param3,
	                           double singlePartitionProbability, 
	                           boolean isRecombination,
	                           boolean rootMovesOK) {
		this.arg = arg;
		this.size = size;
		this.internalNodeParameters = param1;
		this.internalAndRootNodeParameters = param2;
		this.nodeRates = param3;
		this.singlePartitionProbability = singlePartitionProbability;
		this.isRecombination = isRecombination;
		this.mode = CoercableMCMCOperator.COERCION_ON;
		this.rootMovesOK = rootMovesOK;
		
		if(rootMovesOK){
			aboveRootBifurcationProbability = 0.08;
			aboveRootReassortmentProbability = 0.02;
		}
		
		setWeight(weight);
	}


	/**
	 * Do a add/remove re-assortment node operation
	 * @return the log-transformed hastings ratio
	 */
	public double doOperation() throws OperatorFailedException {
		double logq;
		try {
			if (MathUtils.nextDouble() < 1.0/(1 + Math.exp(-size)))
				logq = AddOperation() - size;
			else
				logq = RemoveOperation() + size;
		} catch (NoReassortmentEventException ofe) {
			return Double.NEGATIVE_INFINITY; 
		}
		
		return logq;
	}

	
	
	
	
	
	private double AddOperation() throws OperatorFailedException {
		
		double newReassortmentHeight, newBifurcationHeight, logHastings = 0;
		double treeHeight = arg.getNodeHeight(arg.getRoot());
				
		//1.  Get the new bifurcation and re-assortment heights.
		if(MathUtils.nextDouble() < aboveRootBifurcationProbability){
			double theta = extraAmountBeyondOldRoot/treeHeight;
			newBifurcationHeight = treeHeight + 3.0; //MathUtils.nextExponential(theta);
			logHastings -= Math.log(aboveRootBifurcationProbability * theta) + theta;
		}else{
			newBifurcationHeight = treeHeight * MathUtils.nextDouble();
			logHastings -= Math.log(treeHeight*(1 - aboveRootBifurcationProbability));
		}
		if(MathUtils.nextDouble() < aboveRootReassortmentProbability){
			double theta = extraAmountBeyondOldRoot/treeHeight;
			newReassortmentHeight = treeHeight + 1.5;//MathUtils.nextExponential(theta);
			logHastings -= Math.log(aboveRootReassortmentProbability * theta) + theta;
		}else{
			newReassortmentHeight = treeHeight * MathUtils.nextDouble();
			logHastings -= Math.log(treeHeight*(1 - aboveRootReassortmentProbability));
		}
		
		if(newReassortmentHeight > newBifurcationHeight){
			double temp = newReassortmentHeight;
			newReassortmentHeight = newBifurcationHeight;
			newBifurcationHeight = temp;
		}

		//2. Find the possible re-assortment and bifurcation points.
		ArrayList<NodeRef> potentialBifurcationChildren = new ArrayList<NodeRef>();
		ArrayList<NodeRef> potentialReassortmentChildren = new ArrayList<NodeRef>();

		int totalPotentialBifurcationChildren = findPotentialAttachmentPoints(
				newBifurcationHeight, potentialBifurcationChildren);
		int totalPotentialReassortmentChildren = findPotentialAttachmentPoints(
				newReassortmentHeight, potentialReassortmentChildren);

		assert totalPotentialBifurcationChildren > 0;
		assert totalPotentialReassortmentChildren > 0;
		
		logHastings += Math.log((double)totalPotentialBifurcationChildren *
								  totalPotentialReassortmentChildren);

		//3.  Choose your re-assortment location.
		Node recNode = (Node) potentialReassortmentChildren.get(MathUtils
				.nextInt(totalPotentialReassortmentChildren));
		

		Node recParentL = recNode.leftParent;
		Node recParentR = recNode.rightParent;
		Node recParent = recParentL;
		if (recParentL != recParentR) {
			if (arg.getNodeHeight(recParentL) < newReassortmentHeight)
				recParent = recParentR;
			else if (arg.getNodeHeight(recParentR) > newReassortmentHeight){
				if(MathUtils.nextBoolean()){
					recParent = recParentR;
				}
				logHastings += Math.log(2.0);
			}else{
				throw new OperatorFailedException("There is something wrong");
			}
		}

		//4. Choose your bifurcation location.
		Node sisNode = (Node) potentialBifurcationChildren.get(MathUtils
				.nextInt(totalPotentialBifurcationChildren));


		Node sisParentL = sisNode.leftParent;
		Node sisParentR = sisNode.rightParent;
		Node sisParent = sisParentL;
		if (sisParentL != sisParentR) {
			if (arg.getNodeHeight(sisParentL) < newBifurcationHeight)
				sisParent = sisParentR;
			else if (arg.getNodeHeight(sisParentR) > newBifurcationHeight){
				if(MathUtils.nextBoolean()){
					sisParent = sisParentR;
				}
				logHastings += Math.log(2.0);
			}else{
				throw new OperatorFailedException("There is something wrong");
			}
		}

		//5. Make the new nodes.
		//Note: The height stuff is taken care of below.
		
		Node newReassortment = arg.new Node();
		newReassortment.bifurcation = false;
		newReassortment.rateParameter = new Parameter.Default(1.0);
		newReassortment.number = arg.getNodeCount() + 1;
		
		Node newBifurcation = arg.new Node();
		newBifurcation.rateParameter = new Parameter.Default(1.0);
		newBifurcation.number = arg.getNodeCount(); 
		
		//6. Begin editing the tree.
		arg.beginTreeEdit();
		
		//6a. This is when we do not create a new root.
		if(newBifurcationHeight < treeHeight){
			newBifurcation.heightParameter = new Parameter.Default(newBifurcationHeight);
			newReassortment.heightParameter = new Parameter.Default(newReassortmentHeight);
			newBifurcation.setupHeightBounds();
			newReassortment.setupHeightBounds();
			
			if (sisParent.bifurcation)
				arg.singleRemoveChild(sisParent, sisNode);
			else
				arg.doubleRemoveChild(sisParent, sisNode);
			if (sisNode != recNode) {
				if (recParent.bifurcation)
					arg.singleRemoveChild(recParent, recNode);
				else
					arg.doubleRemoveChild(recParent, recNode);
			}
			if (sisParent.bifurcation)
				arg.singleAddChild(sisParent, newBifurcation);
			else
				arg.doubleAddChild(sisParent, newBifurcation);
			if (sisNode != recNode)
				arg.singleAddChild(newBifurcation, sisNode);
			arg.doubleAddChild(newReassortment, recNode);

			VariableSizeParameter partitioning = new VariableSizeParameter(arg.getNumberOfPartitions());
			drawRandomPartitioning(partitioning);

			if (sisNode != recNode) {
				arg.addChildAsRecombinant(newBifurcation, recParent,
						newReassortment, partitioning);
			} else {
				arg.addChildAsRecombinant(newBifurcation, newBifurcation,
						newReassortment, partitioning);
			}
			arg.expandARGWithRecombinant(newBifurcation, newReassortment,
					internalNodeParameters,
					internalAndRootNodeParameters,
					nodeRates);
			assert nodeCheck();
			
		//6b. But here we do.
		} else if(newReassortmentHeight < treeHeight){
			
			newReassortment.heightParameter = new Parameter.Default(newReassortmentHeight);
			newReassortment.setupHeightBounds();
			
			sisNode = newBifurcation;
			if(arg.isRoot(recParent))
				recParent = newBifurcation;
			
			
			Node root = (Node) arg.getRoot();
			Node rootLeftChild = root.leftChild;
			Node rootRightChild = root.rightChild;
			
			arg.singleRemoveChild(root, rootLeftChild);
			arg.singleRemoveChild(root, rootRightChild);
			arg.singleAddChild(newBifurcation, rootLeftChild);
			arg.singleAddChild(newBifurcation, rootRightChild);
			
			if(recParent.isBifurcation())
				arg.singleRemoveChild(recParent, recNode);
			else
				arg.doubleRemoveChild(recParent, recNode);
			
			arg.doubleAddChild(newReassortment, recNode);
			arg.singleAddChild(root, newBifurcation);
			
			VariableSizeParameter partitioning = new VariableSizeParameter(arg.getNumberOfPartitions());
			drawRandomPartitioning(partitioning);
			
			
			arg.addChildAsRecombinant(root,recParent,newReassortment, partitioning);
			
			newBifurcation.heightParameter = new Parameter.Default(root.getHeight());
			
			newBifurcation.setupHeightBounds();
			root.heightParameter.setParameterValue(0, newBifurcationHeight);
			
			
			arg.expandARGWithRecombinant(newBifurcation,newReassortment, 
					internalNodeParameters,	internalAndRootNodeParameters,
					nodeRates);
			
			assert nodeCheck();
			
		}else{
			
			Node root = (Node) arg.getRoot();
			Node rootLeftChild = root.leftChild;
			Node rootRightChild = root.rightChild;
			
			arg.singleRemoveChild(root, rootLeftChild);
			arg.singleRemoveChild(root, rootRightChild);
			arg.singleAddChild(newBifurcation, rootLeftChild);
			arg.singleAddChild(newBifurcation, rootRightChild);
						
			arg.doubleAddChild(newReassortment, newBifurcation);
			arg.doubleAddChild(root, newReassortment);
						
			VariableSizeParameter partitioning = new VariableSizeParameter(arg.getNumberOfPartitions());
			drawRandomPartitioning(partitioning);
			
			newReassortment.partitioning = partitioning;
			
			newBifurcation.heightParameter = new Parameter.Default(arg.getNodeHeight(root));
			newReassortment.heightParameter = new Parameter.Default(newReassortmentHeight);
			root.heightParameter.setParameterValueQuietly(0, newBifurcationHeight);

			newBifurcation.setupHeightBounds();
			newReassortment.setupHeightBounds();
			
			arg.expandARGWithRecombinant(newBifurcation,newReassortment, 
					internalNodeParameters,	internalAndRootNodeParameters,
					nodeRates);
			
			assert nodeCheck();
			
		}
		
		arg.pushTreeSizeChangedEvent();

		try {
			arg.endTreeEdit();
		} catch (MutableTree.InvalidTreeException ite) {
			throw new RuntimeException(ite.toString() + "\n" + arg.toString()
					+ "\n" + Tree.Utils.uniqueNewick(arg, arg.getRoot()));
		}
				
		logHastings -= Math.log((double)findPotentialNodesToRemove(null,rootMovesOK));
		
		assert nodeCheck();
		
		return 0;
	}
	
	
	private int findPotentialAttachmentPoints(double time, List<NodeRef> list) {
		int count = 0;
		for(int i = 0, n = arg.getNodeCount(); i < n; i++){
			NodeRef nr = arg.getNode(i);
			if (!arg.isRoot(nr) && arg.getNodeHeight(nr) < time){
				if (arg.getNodeHeight(arg.getParent(nr, 0)) > time) {
					if (list != null)
						list.add(nr);
					count++;
				}
				if (arg.isReassortment(nr) && arg.getNodeHeight(arg.getParent(nr, 1)) > time) {
					if (list != null)
						list.add(nr);
					count++;
				}
			}else{
				if(arg.getNodeHeight(nr) < time){
					if(list != null)
						list.add(nr);
					count++;
				}
			}
		}
		return count;
	}
	
	
	private int findPotentialNodesToRemove(ArrayList<NodeRef> list, boolean rootDeleteOK) {
		int count = 0;
		int n = arg.getNodeCount();
		Node root = (Node) arg.getRoot();
		
		if(rootDeleteOK){
			for (int i = 0; i < n; i++) {
				Node node = (Node) arg.getNode(i);
				
				if (node.isReassortment() && (node.leftParent.isBifurcation() 
						|| node.rightParent.isBifurcation())) {
					if (list != null)
						list.add(node);
					count++;
				}
			}
		}else{
			for (int i = 0; i < n; i++) {
				Node node = (Node) arg.getNode(i);
				if (node.isReassortment()
				&& ((node.leftParent != root && node.leftParent.isBifurcation()) ||
				   (node.rightParent != root  && node.rightParent.isBifurcation()))) {
					if (list != null)
						list.add(node);
					count++;
				}
			}
		}

		return count;
	}
	
	

	private double RemoveOperation() throws OperatorFailedException {
		double logq = 0;

		// 1. Draw reassortment node uniform randomly

		ArrayList<NodeRef> potentialNodes = new ArrayList<NodeRef>();

		boolean rootDeleteOK = true;
		
		int totalPotentials = findPotentialNodesToRemove(potentialNodes,rootDeleteOK);
		
		if (totalPotentials == 0)
			throw new OperatorFailedException("No reassortment nodes to remove.");
		
		Node recNode = (Node) potentialNodes.get(MathUtils.nextInt(totalPotentials));

		double reverseReassortmentHeight = 0;
		double reverseBifurcationHeight = 0;
		double treeHeight = arg.getNodeHeight(arg.getRoot());

		reverseReassortmentHeight = arg.getNodeHeight(recNode);
				
		
		arg.beginTreeEdit();
		
		boolean doneSomething = false;
		Node recParent = recNode.leftParent;
		Node recChild = recNode.leftChild;

		
		if (recNode.leftParent == recNode.rightParent) {
			if(!arg.isRoot(recNode.leftParent)){
				Node recGrandParent = recParent.leftParent;

				arg.doubleRemoveChild(recGrandParent, recParent);
				arg.doubleRemoveChild(recNode, recChild);
				if (recGrandParent.bifurcation)
					arg.singleAddChild(recGrandParent, recChild);
				else
					arg.doubleAddChild(recGrandParent, recChild);
				doneSomething = true;
			}else{
				assert recChild.bifurcation;
				
				Node recChildLeft = recChild.leftChild;
				Node recChildRight = recChild.rightChild;
				
				arg.doubleRemoveChild(recParent, recNode);
				arg.doubleRemoveChild(recNode  , recChild);
				
				arg.singleRemoveChild(recChild, recChildLeft);
				arg.singleRemoveChild(recChild, recChildRight);
				
				arg.singleAddChild(recParent, recChildLeft);
				arg.singleAddChild(recParent, recChildRight);
				
				recParent.setHeight(recChild.getHeight());
				
				recParent = recChild;
				doneSomething = true;
			}
			
		} else { 
			
			Node recParent1 = recNode.leftParent;
			Node recParent2 = recNode.rightParent;
			
			//This is the easiest way of integrating 
			//root delete moves :)
			if(rootDeleteOK){
				if (!recParent1.bifurcation){
					recParent1 = recNode.rightParent;
					recParent2 = recNode.leftParent;
				}else if(recParent2.bifurcation){
					if (MathUtils.nextDouble() < 0.5) { 
						recParent1 = recNode.rightParent;
						recParent2 = recNode.leftParent;
					}
					logq += Math.log(2);
				}
			}else{
				if (!recParent1.bifurcation || recParent1.isRoot()) { 
					recParent1 = recNode.rightParent;
					recParent2 = recNode.leftParent;
				} else if (recParent2.bifurcation && !recParent2.isRoot()) { 
					if (MathUtils.nextDouble() < 0.5) { 
						recParent1 = recNode.rightParent;
						recParent2 = recNode.leftParent;
					}
					logq += Math.log(2);
				}
				
			}
			
			if(arg.isRoot(recParent1)){
				Node oldRoot = (Node) arg.getOtherChild(recParent1, recNode);
				Node oldRootLeft = oldRoot.leftChild;
				Node oldRootRight = oldRoot.rightChild;
				
				if(oldRoot == recParent2){
					arg.singleRemoveChild(recParent1, recNode);
					arg.singleRemoveChild(recParent1, oldRoot);
					arg.singleRemoveChild(oldRoot, oldRootLeft);
					arg.singleRemoveChild(oldRoot, oldRootRight);
					
					arg.singleAddChild(recParent1, oldRootLeft);
					arg.singleAddChild(recParent1, oldRootRight);
					
					arg.singleRemoveChild(recParent1, recNode);
					arg.doubleRemoveChild(recNode, recChild);
					
					arg.singleAddChild(recParent1, recChild);
					
					recParent1.setHeight(oldRoot.getHeight());
										
					recParent1 = oldRoot;
				}else{
					arg.singleRemoveChild(recParent1, recNode);
					arg.singleRemoveChild(recParent1, oldRoot);
					arg.singleRemoveChild(oldRoot, oldRootLeft);
					arg.singleRemoveChild(oldRoot, oldRootRight);
					
					arg.singleAddChild(recParent1, oldRootLeft);
					arg.singleAddChild(recParent1, oldRootRight);
					
					if(recParent2.bifurcation)
						arg.singleRemoveChild(recParent2, recNode);
					else
						arg.doubleRemoveChild(recParent2, recNode);
					
					arg.doubleRemoveChild(recNode,recChild);
					
					if(recParent2.bifurcation)
						arg.singleAddChild(recParent2, recChild);
					else
						arg.doubleAddChild(recParent2, recChild);
					
					recParent1.setHeight(oldRoot.getHeight());
					recParent1 = oldRoot;
						
				}
			}else{
				Node recGrandParent = recParent1.leftParent; 

				Node otherChild = recParent1.leftChild;
				if (otherChild == recNode)
					otherChild = recParent1.rightChild;

				if (recGrandParent.bifurcation)
					arg.singleRemoveChild(recGrandParent, recParent1);
				else
					arg.doubleRemoveChild(recGrandParent, recParent1);

				arg.singleRemoveChild(recParent1, otherChild);
				if (recParent2.bifurcation)
					arg.singleRemoveChild(recParent2, recNode);
				else
					arg.doubleRemoveChild(recParent2, recNode);
				arg.doubleRemoveChild(recNode, recChild);
				if (otherChild != recChild) {
					if (recGrandParent.bifurcation)
						arg.singleAddChild(recGrandParent, otherChild);
					else
						arg.doubleAddChild(recGrandParent, otherChild);
					if (recParent2.bifurcation)
						arg.singleAddChild(recParent2, recChild);
					else
						arg.doubleAddChild(recParent2, recChild);
				} else {
					if (recGrandParent.bifurcation)
						arg.singleAddChildWithOneParent(recGrandParent, otherChild);
					else
						arg.doubleAddChildWithOneParent(recGrandParent, otherChild);
					if (recParent2.bifurcation)
						arg.singleAddChildWithOneParent(recParent2, recChild);
					else
						arg.doubleAddChildWithOneParent(recParent2, recChild);
				}
			}
						
			doneSomething = true;
			recParent = recParent1;
		}
		
		
		reverseBifurcationHeight = arg.getNodeHeight(recParent);

		
		if (doneSomething) {
			try {
				arg.contractARGWithRecombinant(recParent, recNode,
						internalNodeParameters, internalAndRootNodeParameters, nodeRates);
			} catch (Exception e) {
				System.err.println(e.getMessage());
				System.err.println(e);
				System.exit(-1);
			}
		}
		
		
		int max = Math.max(recParent.getNumber(), recNode.getNumber());
		int min = Math.min(recParent.getNumber(), recNode.getNumber());
		
		for(int i = 0, n = arg.getNodeCount(); i < n; i++){
			Node x = (Node) arg.getNode(i);
			if(x.getNumber() > max){
				x.number--;
			}
			if(x.getNumber() > min){
				x.number--;
			}
		}
		
		
		arg.pushTreeSizeChangedEvent();
		try {
			arg.endTreeEdit();
		} catch (MutableTree.InvalidTreeException ite) {
			throw new RuntimeException(ite.toString() + "\n" + arg.toString()
					+ "\n" + Tree.Utils.uniqueNewick(arg, arg.getRoot()));
		} 
				
		assert nodeCheck() : arg.toARGSummary();
		
		logq = 0;
		return logq; // 1 / total potentials * 1 / 2 (if valid) * length1 * length2 * attachmentSisters
	}

	
	
	
	
	
	private double drawRandomPartitioning(Parameter partitioning) {
		double logq = 0;
		int len = arg.getNumberOfPartitions();
		if (len == 2) {
//            boolean first = MathUtils.nextBoolean();
			if (partitioning != null) {
				if (MathUtils.nextBoolean())
					partitioning.setParameterValueQuietly(0, 1.0);
				else
					partitioning.setParameterValueQuietly(1, 1.0);
			}
			return Math.log(2);
		}
		if (isRecombination) {
			logq += drawRandomRecombination(partitioning);
		} else {
			logq += drawRandomReassortment(partitioning);
		}
		return logq;
	}


	/* Draws a new partitioning.
	 * With probability singlePartitionProbability, one bit is set;
	 * otherwise, all bits are selected via a random permutation
	 *
	 */
	private double drawRandomReassortment(Parameter partitioning) {
		int len = arg.getNumberOfPartitions();
		double logq = 0;
		if (MathUtils.nextDouble() < singlePartitionProbability) {
			if (partitioning != null)
				partitioning.setParameterValueQuietly(MathUtils.nextInt(len), 1.0);
			return Math.log(len);
		}
		int[] permutation = MathUtils.permuted(len);
		int cut = MathUtils.nextInt(len - 1);
		for (int i = 0; i < len; i++) {
			logq += Math.log(i + 1);
			if (i > cut && partitioning != null)
				partitioning.setParameterValueQuietly(permutation[i], 1.0);
		}
		logq += Math.log(len - 1);
		return logq;
	}

	/* Draws a new partitioning.
	 * A break-pt is drawn uniformly
	 *
	 */
	private double drawRandomRecombination(Parameter partitioning) {
		int len = arg.getNumberOfPartitions();
		double logq = 0;
		double leftValue = MathUtils.nextInt(2);
		double rightValue = 1.0 - leftValue;
		logq += Math.log(2);
		if (partitioning != null) {
			int cut = MathUtils.nextInt(len - 1);
			for (int i = 0; i <= cut; i++)
				partitioning.setParameterValueQuietly(i, leftValue);
			for (int i = cut + 1; i < len; i++)
				partitioning.setParameterValueQuietly(i, rightValue);
		}
		logq += Math.log(len - 1);
		return logq;
	}


	public boolean nodeCheck(){
		for(int i = 0, n = arg.getNodeCount(); i < n; i++){
			Node x = (Node) arg.getNode(i);
			
			if(x.leftParent != x.rightParent && 
					x.leftChild != x.rightChild){
						return false;
			}
			if(x.leftParent != null){
				if(x.leftParent.leftChild.getNumber() != i &&
				   x.leftParent.rightChild.getNumber() != i)
						return false;
			}
			if(x.rightParent != null){
				if(x.rightParent.leftChild.getNumber() != i &&
				   x.rightParent.rightChild.getNumber() != i)
						return false;
			}
			if(x.leftChild != null){
				if(x.leftChild.leftParent.getNumber() != i &&
				   x.leftChild.rightParent.getNumber() != i)
						return false;
			}
			if(x.rightChild != null){
				if(x.rightChild.leftParent.getNumber() != i &&
				   x.rightChild.rightParent.getNumber() != i)
						return false;
			}		
			}

		return true;
	}
		
	


	public void sanityCheck() {
		int len = arg.getNodeCount();
		for (int i = 0; i < len; i++) {
			Node node = (Node) arg.getNode(i);
			if (node.bifurcation) {
				boolean equalChild = (node.leftChild == node.rightChild);
				if ((equalChild && node.leftChild != null)) {
					if (!node.leftChild.bifurcation && ((node.leftChild).leftParent == node))
						;
					else {
						System.err.println("Node " + (i + 1) + " is insane.");
						System.err.println(arg.toGraphString());
						System.exit(-1);
					}
				}
			} else {
				if ((node.leftChild != node.rightChild)) {
					System.err.println("Node " + (i + 1) + " is insane.");
					System.err.println(arg.toGraphString());
					System.exit(-1);
				}
			}
			if (!node.isRoot()) {
				double d;
				d = node.getHeight();
			}
		}
	}


	////
	////Coercible MCMC Operator stuff
	////
	
	
	public double getSize() {
		return size;
	}

	public void setSize(double size) {
		this.size = size;
	}

	public double getCoercableParameter() {
		return size;
	}

	public void setCoercableParameter(double value) {
		setSize(value);
	}

	public double getRawParameter() {
		return size;
	}

	public int getMode() {
		return mode;
	}

	public double getTargetAcceptanceProbability() {
		return 0.5;
	}


	public String getPerformanceSuggestion() {
		double prob = MCMCOperator.Utils.getAcceptanceProbability(this);
//		double targetProb = getTargetAcceptanceProbability();

//		double ws = OperatorUtils.optimizeWindowSize(getSize(), Double.MAX_VALUE, prob, targetProb);

		
		if (prob < getMinimumGoodAcceptanceLevel()) {
			return "Try setting addProbability closer to 0.5";
		} else if (prob > getMaximumGoodAcceptanceLevel()) {
			return "Try setting addProbability value closer to 0.5";
		} else return "";
	}

	public String getOperatorName() {
		return ARG_EVENT_OPERATOR;
	}

	public static dr.xml.XMLObjectParser PARSER = new dr.xml.AbstractXMLObjectParser() {

		public String getParserName() {
			return ARG_EVENT_OPERATOR;
		}

		public Object parseXMLObject(XMLObject xo) throws XMLParseException {

			double singlePartitionProbability = 0.0;
			boolean isRecombination = false;
			
			int mode = CoercableMCMCOperator.DEFAULT;
			if (xo.hasAttribute(AUTO_OPTIMIZE)) {
				if (xo.getBooleanAttribute(AUTO_OPTIMIZE)) {
					mode = CoercableMCMCOperator.COERCION_ON;
				} else {
					mode = CoercableMCMCOperator.COERCION_OFF;
				}
			}
			
			boolean rootMovesOK = false;
			if (xo.hasAttribute(ROOT_MOVES_OK) 
					&& xo.getBooleanAttribute(ROOT_MOVES_OK)) {
					rootMovesOK = true;
			}

			ARGModel treeModel = (ARGModel) xo.getChild(ARGModel.class);
			VariableSizeCompoundParameter parameter1 = 
				(VariableSizeCompoundParameter) xo.getSocketChild(INTERNAL_NODES);
			VariableSizeCompoundParameter parameter2 = 
				(VariableSizeCompoundParameter) xo.getSocketChild(INTERNAL_AND_ROOT);
			VariableSizeCompoundParameter parameter3 = 
				(VariableSizeCompoundParameter) xo.getSocketChild(NODE_RATES);
			
			int weight = xo.getIntegerAttribute("weight");
			double size = xo.getDoubleAttribute(ADD_PROBABILITY);
			if(size > 0 && size < 1)
				size = Math.log(size / (1.0- size));
			else
				throw new XMLParseException(ADD_PROBABILITY + " must be between 0 and 1");
						
			return new NewerARGEventOperator(treeModel, weight, size, 
					mode, parameter1, parameter2, parameter3, 
					singlePartitionProbability, isRecombination, rootMovesOK);
		}

		public String getParserDescription() {
			return "An operator that slides a subarg.";
		}

		public Class getReturnType() {
			return AddRemoveARGEventOperator.class;
		}

		public XMLSyntaxRule[] getSyntaxRules() {
			return rules;
		}

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
				AttributeRule.newDoubleRule(ADD_PROBABILITY, false,
						"The probability that the operator adds a new"
						+ " reassortment event"),
				new ElementRule(ARGModel.class),
				new ElementRule(INTERNAL_NODES, 
						new XMLSyntaxRule[]{
						new ElementRule(VariableSizeCompoundParameter.class)}),
				new ElementRule(INTERNAL_AND_ROOT, 
						new XMLSyntaxRule[]{
						new ElementRule(VariableSizeCompoundParameter.class)}),
				new ElementRule(NODE_RATES, 
						new XMLSyntaxRule[]{
						new ElementRule(VariableSizeCompoundParameter.class)}),
		};
	};
	
	private class NoReassortmentEventException extends OperatorFailedException{
		public NoReassortmentEventException(String message) {
			super(message);
		}
		
		public NoReassortmentEventException(){
			super("");
		}

		private static final long serialVersionUID = 1L;

	}

}


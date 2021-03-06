/*
 * ExchangeOperator.java
 *
 * Copyright (C) 2002-2006 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.evomodel.operators;

import dr.evolution.tree.MutableTree;
import dr.evolution.tree.NodeRef;
import dr.evomodel.tree.TreeModel;
import dr.inference.operators.*;
import dr.math.MathUtils;
import dr.xml.*;

/**
 * Implements branch exchange operations.
 * There is a NARROW and WIDE variety.
 * The narrow exchange is very similar to a rooted-tree
 * nearest-neighbour interchange but with the restriction
 * that node height must remain consistent.
 *
 * KNOWN BUGS: WIDE operator cannot be used on trees with 4 or less tips!
 */
public class ExchangeOperator extends SimpleMCMCOperator {

	public static final String NARROW_EXCHANGE = "narrowExchange";
	public static final String WIDE_EXCHANGE = "wideExchange";

	public static final int NARROW = 0;
	public static final int WIDE = 1;

	private static final int MAX_TRIES = 10000;

	private int mode = NARROW;
	private TreeModel tree;

	public ExchangeOperator(int mode, TreeModel tree, int weight) {
		this.mode = mode;
		this.tree = tree;
		setWeight(weight);
	}

	public double doOperation() throws OperatorFailedException {

		int tipCount = tree.getExternalNodeCount();

		switch (mode) {
			case NARROW: narrow(); break;
			case WIDE: wide(); break;
		}

		if (tree.getExternalNodeCount() != tipCount) {
			throw new RuntimeException("Lost some tips in " + ((mode == NARROW) ? "NARROW mode." : "WIDE mode."));
		}

		return 0.0;
	}

	/**
	 * WARNING: Assumes strictly bifurcating tree.
	 */
	public void narrow() throws OperatorFailedException {

		NodeRef i = null, iP = null, j = null, jP = null;
		int tries = 0;

		//Echoose

		while (tries < MAX_TRIES) {
			i = tree.getNode(MathUtils.nextInt(tree.getNodeCount()));
			while (tree.getRoot() == i || tree.getParent(i) == tree.getRoot()) {
				i = tree.getNode(MathUtils.nextInt(tree.getNodeCount()));
			}

			iP = tree.getParent(i);
			jP = tree.getParent(iP);
			j = tree.getChild(jP, 0);
			if (j == iP) {
				j = tree.getChild(jP, 1);
			}

			if ((tree.getNodeHeight(j) < tree.getNodeHeight(iP)) && (tree.getNodeHeight(i) < tree.getNodeHeight(jP))) {
				break;
			}
			tries += 1;
		}
		//System.out.println("tries = " + tries);

		//Eupdate
		if (tries < MAX_TRIES) {
			eupdate(i, j, iP, jP);

			tree.pushTreeChangedEvent(iP);
			tree.pushTreeChangedEvent(jP);
		} else throw new OperatorFailedException("Couldn't find valid narrow move on this tree!!");
	}

	/**
	 * WARNING: Assumes strictly bifurcating tree.
	 */
	public void wide() throws OperatorFailedException {

		NodeRef i = null, iP = null, j = null, jP = null;
		int tries = 0;

		//Echoose

		while (tries < MAX_TRIES) {
			i = tree.getNode(MathUtils.nextInt(tree.getNodeCount()));
			while (tree.getRoot() == i) {
				i = tree.getNode(MathUtils.nextInt(tree.getNodeCount()));
			}

			j = tree.getNode(MathUtils.nextInt(tree.getNodeCount()));
			while (j == i || j == tree.getRoot()) {
				j = tree.getNode(MathUtils.nextInt(tree.getNodeCount()));
			}

			iP = tree.getParent(i);
			jP = tree.getParent(j);

			if ((iP != jP) && (i != jP) && (j != iP) &&
				(tree.getNodeHeight(j) < tree.getNodeHeight(iP)) &&
				(tree.getNodeHeight(i) < tree.getNodeHeight(jP))) {
				break;
			}
			tries += 1;
		}
		//System.out.println("tries = " + tries);

		//Eupdate
		if (tries < MAX_TRIES) {
			eupdate(i, j, iP, jP);
		} else throw new OperatorFailedException("Couldn't find valid wide move on this tree!");
	}

	public int getMode() {
		return mode;
	}

	public String getOperatorName() {
		return ((mode == NARROW) ? "Narrow" : "Wide") + " Exchange";
	}

	private void eupdate(NodeRef i, NodeRef j, NodeRef iP, NodeRef jP) throws OperatorFailedException {

		tree.beginTreeEdit();
		tree.removeChild(iP, i);
		tree.removeChild(jP, j);
		tree.addChild(jP, i);
		tree.addChild(iP, j);

		try {
			tree.endTreeEdit();
		} catch(MutableTree.InvalidTreeException ite) {
			throw new OperatorFailedException(ite.toString());
		}
	}

	public double getMinimumAcceptanceLevel() { if (mode == NARROW) return 0.05; else return 0.01; }
	public double getMinimumGoodAcceptanceLevel() { if (mode == NARROW) return 0.05; else return 0.01; }

	public String getPerformanceSuggestion() {
		if (MCMCOperator.Utils.getAcceptanceProbability(this) < getMinimumAcceptanceLevel()) {
			return "";
		} else if (MCMCOperator.Utils.getAcceptanceProbability(this) > getMaximumAcceptanceLevel()){
			return "";
		} else {
			return "";
		}
	}

	public static XMLObjectParser NARROW_EXCHANGE_PARSER = new AbstractXMLObjectParser() {

		public String getParserName() { return NARROW_EXCHANGE; }

		public Object parseXMLObject(XMLObject xo) throws XMLParseException {

			TreeModel treeModel = (TreeModel)xo.getChild(TreeModel.class);
			int weight = xo.getIntegerAttribute("weight");

			return new ExchangeOperator(NARROW, treeModel, weight);
		}

		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************

		public String getParserDescription() {
			return "This element represents a narrow exchange operator. " +
				"This operator swaps a random subtree with its uncle.";
		}

		public Class getReturnType() { return ExchangeOperator.class; }

		public XMLSyntaxRule[] getSyntaxRules() { return rules; }

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
			AttributeRule.newIntegerRule("weight"),
			new ElementRule(TreeModel.class)
		};

	};

	public static XMLObjectParser WIDE_EXCHANGE_PARSER = new AbstractXMLObjectParser() {

		public String getParserName() { return WIDE_EXCHANGE; }

		public Object parseXMLObject(XMLObject xo) throws XMLParseException {

			TreeModel treeModel = (TreeModel)xo.getChild(TreeModel.class);
			int weight = xo.getIntegerAttribute("weight");

			return new ExchangeOperator(WIDE, treeModel, weight);
		}

		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************

		public String getParserDescription() {
			return "This element represents a wide exchange operator. " +
				"This operator swaps two random subtrees.";
		}

		public Class getReturnType() { return ExchangeOperator.class; }

		public XMLSyntaxRule[] getSyntaxRules() { return rules; }

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[] {
			AttributeRule.newIntegerRule("weight"),
			new ElementRule(TreeModel.class)
		};

	};
}

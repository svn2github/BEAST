/*
 * OrRule.java
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

package dr.xml;

import java.util.Set;
import java.util.TreeSet;

public class OrRule implements XMLSyntaxRule {

	public OrRule(XMLSyntaxRule a, XMLSyntaxRule b) {
		rules = new XMLSyntaxRule[] {a,b};
	}

	public OrRule(XMLSyntaxRule[] rules) {
		this.rules = rules;
	}

	public XMLSyntaxRule[] getRules() {
		return rules;
	}

	/**
	 * Returns true if the rule is satisfied for the given XML object.
	 */
	public boolean isSatisfied(XMLObject xo) {
        for (XMLSyntaxRule rule : rules) {
            if (rule.isSatisfied(xo)) return true;
        }
        return false;
	}

    public boolean containsAttribute(String name) {
        for( XMLSyntaxRule rule : rules ) {
            if( rule.containsAttribute((name)) ) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Describes the rule.
	 */
	public String ruleString() {
		String ruleString = "(" + rules[0].ruleString();
		for (int i = 1; i < rules.length; i++) {
			ruleString += "| " + rules[i].ruleString();
		}
		return ruleString + ")";
	}

	/**
	 * Describes the rule.
	 */
	public String htmlRuleString(XMLDocumentationHandler handler) {
		String html = "<div class=\"requiredcompoundrule\">At least one of:";
        for (XMLSyntaxRule rule : rules) {
            html += rule.htmlRuleString(handler);
        }
        html += "</div>";
		return html;
	}

	/**
	 * Describes the rule.
	 */
	public String wikiRuleString(XMLDocumentationHandler handler) {
		String html = "At least one of:";
        for (XMLSyntaxRule rule : rules) {
            html += rule.htmlRuleString(handler);
        }
        html += "\n";
		return html;
	}

	/**
	 * Describes the rule.
	 */
	public String ruleString(XMLObject xo) {

        for (XMLSyntaxRule rule : rules) {
            if (!rule.isSatisfied(xo)) return rule.ruleString(xo);
        }
        return ruleString();
	}

	/**
	 * @return a set containing the required types of this rule.
	 */
	public Set<Class> getRequiredTypes() {
		Set<Class> set = new TreeSet<Class>(ClassComparator.INSTANCE);
        for (XMLSyntaxRule rule : rules) {
            set.addAll(rule.getRequiredTypes());
        }
        return set;
	}

	XMLSyntaxRule[] rules;
}

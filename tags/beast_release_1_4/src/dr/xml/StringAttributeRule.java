/*
 * StringAttributeRule.java
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

import dr.math.MathUtils;

import java.util.List;
import java.util.ArrayList;


public class StringAttributeRule extends AttributeRule {
	
	/**
	 * Creates a required String attribute rule.
	 */
	public StringAttributeRule(String name, String description) {
		this(name, description, (String)null, false);
	}
	
	/**
	 * Creates a required String attribute rule.
	 */
	public StringAttributeRule(String name, String description, String example) {
		this(name, description, example, false);
	}

	/**
	 * Creates a String attribute rule.
	 */
	public StringAttributeRule(String name, String description, boolean optional) {
		this(name, description, null, optional, 0, Integer.MAX_VALUE);
	}

	/**
	 * Creates a String attribute rule.
	 */
	public StringAttributeRule(String name, String description, String example, boolean optional) {
		this(name, description, example, optional, 0, Integer.MAX_VALUE);
	}
	
	/**
	 * Creates a String attribute rule.
     * @param valid a list of valid tokens for this attribute
	 */
	public StringAttributeRule(String name, String description, String[] valid, boolean optional) {
		this(name, description, null, optional, 0, Integer.MAX_VALUE);
		validValues = new ArrayList();
        for (int i = 0; i < valid.length; i++) {
            validValues.add(valid[i]);
        }
		this.example = null;
	}

    /**
     * Creates a String attribute rule.
     * @param valid a list of valid tokens for this attribute
     */
    public StringAttributeRule(String name, String description, String[][] valid, boolean optional) {
        this(name, description, null, optional, 0, Integer.MAX_VALUE);
        validValues = new ArrayList();
        for (int i = 0; i < valid.length; i++) {
            for (int j = 0; j < valid[i].length; j++) {
                validValues.add(valid[i][j]);
            }
        }
        this.example = null;
    }


	/**
	 * Creates a String attribute rule.
	 */
	private StringAttributeRule(String name, String description, String example, boolean optional, int minLength, int maxLength) {
		setName(name);
		setAttributeClass(String.class);
		setOptional(optional);
		setDescription(description);
		this.example = example;
		this.minLength = minLength;
		this.maxLength = maxLength;
	}
	
	/**
	 * @return true if the required attribute of the correct type is present.
	 */
	public boolean isSatisfied(XMLObject xo) {
		if (super.isSatisfied(xo)) {
			if (!getOptional()) {
				try {
					String str = (String)getAttribute(xo);
					if (validValues != null) {
						for (int i =0; i < validValues.size(); i++) {
							if (str.equals((String)validValues.get(i))) return true;
						}
						return false;
					} else {
						return (str.length() >= minLength || str.length() <= maxLength);
					}
				} catch (XMLParseException xpe) {}
			}
			return true;
		}
		return false;
	}

    /**
     * @return a string describing the rule.
     */
    public String ruleString() {
        StringBuffer rule = new StringBuffer(super.ruleString());

        if (validValues != null && validValues.size() > 0) {
            rule.append(" from {");
            rule.append(validValues.get(0));
            for (int i = 1; i < validValues.size(); i++) {
                rule.append(", ");
                rule.append(validValues.get(i));
            }
        }
        return rule.toString();
    }


    /**
	 * @return a string describing the rule.
	 */
	public String htmlRuleString(XMLDocumentationHandler handler) {
		String rule = 
			"<div class=\"" + (getOptional() ? "optional" : "required") + "rule\"> Attribute " + 
		 	" <span class=\"attrname\">" + getName() + 
			"</span>";
			
		if (validValues != null) {
			rule += " &isin; {<tt>" + validValues.get(0) + "</tt>";
			for (int i =1; i < validValues.size(); i++) {
				rule += ", <tt>" + validValues.get(i) + "</tt>";
			}
			rule += "}";
		} else {
			rule += " is string";
		}	
		
		rule += " <div class=\"description\">" + getDescription() + "</div>" ;
			
		rule += "</div>" ;
		
		return rule;
	}
	
	public String getExample() {
		if (validValues != null) {
			return (String)validValues.get(MathUtils.nextInt(validValues.size()));
		} else return example;
	}
	
	public boolean hasExample() {
		return (validValues != null || example != null);
	}
		
	private int minLength = 0, maxLength = Integer.MAX_VALUE;
	private List validValues = null;
	private String example = null;
}

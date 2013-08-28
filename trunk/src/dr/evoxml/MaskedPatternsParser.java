/*
 * MergePatternsParser.java
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

package dr.evoxml;

import dr.evolution.alignment.PatternList;
import dr.evolution.alignment.Patterns;
import dr.evolution.alignment.SiteList;
import dr.xml.*;

import java.util.logging.Logger;

/**
 * @author Andrew Rambaut
 *
 * @version $Id$
 */
public class MaskedPatternsParser extends AbstractXMLObjectParser {

    public static final String MASKED_PATTERNS = "maskedPatterns";
    public static final String MASK = "mask";
    public static final String NEGATIVE = "negative";

    public String getParserName() { return MASKED_PATTERNS; }

    /**
     * Parses a patterns element and returns a patterns object.
     */
    public Object parseXMLObject(XMLObject xo) throws XMLParseException {

        Patterns patterns = null;

        SiteList siteList = (SiteList)xo.getChild(SiteList.class);

        boolean negativeMask = xo.getBooleanAttribute(NEGATIVE);
        String mask = (String)xo.getElementFirstChild(MASK);

        String[] maskArray = mask.split("\\s+");
        if (maskArray.length != siteList.getSiteCount()) {
            throw new XMLParseException("The mask needs to be the same length as the alignment");
        }

        for (int i = 0; i < maskArray.length; i++) {
            if (Boolean.parseBoolean(maskArray[i]) != negativeMask) {
                if (patterns == null) {
                    patterns = new Patterns(siteList, i, i, 1);
                } else {
                    patterns.addPatterns(siteList, i, i, 1);
                }
            }
        }

        if (patterns == null) {
            throw new XMLParseException("The mask needs include at least one pattern");
        }

        if (xo.hasAttribute(XMLParser.ID)) {
            final Logger logger = Logger.getLogger("dr.evoxml");
            logger.info("Site patterns '" + xo.getId() + "' created by masking alignment with id '" + siteList.getId() + "'");
            logger.info("  pattern count = " + patterns.getPatternCount());
        }

        return patterns;
    }

    public XMLSyntaxRule[] getSyntaxRules() { return rules; }

    private final XMLSyntaxRule[] rules = {
        AttributeRule.newBooleanRule(NEGATIVE, true),
        new ElementRule(SiteList.class),
        new ElementRule(MASK, String.class, "A parameter of 1s and 0s that represent included and excluded sites")
    };

    public String getParserDescription() {
        return "A weighted list of the unique site patterns (unique columns) in an alignment.";
    }

    public Class getReturnType() { return PatternList.class; }

}

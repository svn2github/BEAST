/*
 * Copyright (C) 2002-2009 Alexei Drummond and Andrew Rambaut
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
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.app.beauti.options;

import dr.evolution.alignment.Alignment;
import dr.evolution.alignment.Patterns;
import dr.evolution.datatype.DataType;
import dr.evolution.util.TaxonList;

/**
 * @author Andrew Rambaut
 * @author Alexei Drummond
 * @author Walter Xie
 */
public class PartitionData extends AbstractPartitionData {

    private final Alignment alignment;

    private int fromSite;
    private int toSite;
    private int every = 1;


    public PartitionData(BeautiOptions options, String name, String fileName, Alignment alignment) {
        this(options, name, fileName, alignment, -1, -1, 1);
    }

    public PartitionData(BeautiOptions options, String name, String fileName, Alignment alignment, int fromSite, int toSite, int every) {
        this.options = options;
        this.name = name;
        this.fileName = fileName;
        this.alignment = alignment;

        this.fromSite = fromSite;
        this.toSite = toSite;
        this.every = every;

        this.trait = null;

        Patterns patterns = null;
        if (alignment != null) {
            patterns = new Patterns(alignment);
        }
        calculateMeanDistance(patterns);
    }

    public PartitionData(BeautiOptions options, String name, TraitData trait) {
        this.options = options;
        this.name = name;
        this.fileName = null;
        this.alignment = null;

        this.fromSite = -1;
        this.toSite = -1;
        this.every = 1;

        this.trait = trait;

        calculateMeanDistance(null);
    }

    public Alignment getAlignment() {
        return alignment;
    }

    public int getFromSite() {
        return fromSite;
    }

    public int getToSite() {
        return toSite;
    }

    public int getEvery() {
        return every;
    }

    public TaxonList getTaxonList() {
        return getAlignment();  
    }

    public int getSiteCount() {
        if (alignment != null) {
            int from = getFromSite();
            if (from < 1) {
                from = 1;
            }
            int to = getToSite();
            if (to < 1) {
                to = alignment.getSiteCount();
            }
            return (to - from + 1) / every;
        } else {
            // must be a trait
            return -1;
        }
    }

    public DataType getDataType() {
        if (alignment != null) {
            return alignment.getDataType();
        } else {
            return trait.getDataType();
        }
    }

    public String getDataDescription() {
        if (alignment != null) {
            return alignment.getDataType().getDescription();
        } else {
            return trait.getTraitType().toString();
        }
    }

}

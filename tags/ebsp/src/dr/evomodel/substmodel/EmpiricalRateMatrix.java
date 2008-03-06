/*
 * EmpiricalRateMatrix.java
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

package dr.evomodel.substmodel;

import dr.evolution.datatype.AminoAcids;
import dr.evolution.datatype.DataType;

/**
 * An interface for empirical rate matrices.
 *
 * @version $Id: EmpiricalRateMatrix.java,v 1.3 2005/05/24 20:25:58 rambaut Exp $
 *
 * @author Andrew Rambaut
 * @author Alexei Drummond
 */
public interface EmpiricalRateMatrix {

	String getName();
	DataType getDataType();
	
	double[] getEmpiricalRates();
	double[] getEmpiricalFrequencies();
			
	public abstract class Abstract implements EmpiricalRateMatrix {
	
		public Abstract(String name, DataType dataType) {
			this.name = name;
			this.dataType = dataType;
		}
		
		public final String getName() { return name; }
		
		public final DataType getDataType() { return dataType; }
		
		public final double[] getEmpiricalRates() { return rates; }
		public final double[] getEmpiricalFrequencies() { return frequencies; }
			
		protected double[] rates = null;
		protected double[] frequencies = null;
		
		private String name;
		protected DataType dataType;
	}

    public abstract class AbstractAminoAcid extends Abstract {
	
		public AbstractAminoAcid(String name) {
			super(name, AminoAcids.INSTANCE);
			
			int n = dataType.getStateCount();
			rates = new double[(n * (n - 1)) / 2];
			frequencies = new double[n];
		}
		
		public final void setEmpiricalRates(double[][]matrix, String aminoAcidOrder) {
			int k = 0;
			
			for (int i = 0; i < dataType.getStateCount(); i++) {
			
				int u = aminoAcidOrder.indexOf(dataType.getChar(i));
				
				for (int j = i + 1; j < dataType.getStateCount(); j++) {
				
					int v = aminoAcidOrder.indexOf(dataType.getChar(j));
					
					if (u < v) {
						rates[k] = matrix[u][v];
					} else {
						rates[k] = matrix[v][u];
					}
					
					k++;
				}
			}
		}
		
		public final void setEmpiricalFrequencies(double[]freqs, String aminoAcidOrder) {
			
			for (int i = 0; i < dataType.getStateCount(); i++) {
			
				int u = aminoAcidOrder.indexOf(dataType.getChar(i));
				frequencies[i] = freqs[u];
			}
		}
	};
}
/*
 * Model.java
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

package dr.inference.model;

import dr.util.Identifiable;

/**
 * An interface that describes a model of some data.
 *
 * @version $Id: Model.java,v 1.6 2005/05/24 20:26:00 rambaut Exp $
 *
 * @author Alexei Drummond
 * @author Andrew Rambaut
 */

public interface Model extends Identifiable {

	/**
	 * Adds a listener that is notified when the this model changes.
	 */
	void addModelListener(ModelListener listener);

	void removeModelListener(ModelListener listener);

	/**
	 * This function should be called to store the state of the
	 * entire model. This makes the model state invalid until either 
	 * an acceptModelState or restoreModelState is called.
	 */
	void storeModelState();

	/**
	 * This function should be called to restore the state of the entire model. 
	 */
	void restoreModelState();

	/**
	 * This function should be called to accept the state of the entire model
	 */
	void acceptModelState();

	/**
	 * @return whether this model is in a valid state
	 */
	boolean isValidState();

	/**
	 * @return the total number of sub-models
	 */
	int getModelCount();
	
	/**
	 * @return the ith sub-model
	 */
	Model getModel(int i);

	/**
	 * @return the total number of parameters in this model
	 */
	int getParameterCount();
	
	/**
	 * @return the ith parameter
	 */
	Parameter getParameter(int i);
	
	/**
	 * @return the parameter of the component that is called name
	 */
	Parameter getParameter(String name);
	
	/**
	 * @return the name of this model
	 */
	String getModelName();

	/**
	 * A helper class for storing listeners and firing events.
	 */
	public class ListenerHelper {

		public void fireModelChanged(Model model) {
			fireModelChanged(model, model, -1);
		}
		
		public void fireModelChanged(Model model, Object object) {
			fireModelChanged(model, object, -1);
		}
		
		public void fireModelChanged(Model model, Object object, int index) {
			if (listeners != null) {
				for (int i = 0; i < listeners.size(); i++) {
					ModelListener listener = (ModelListener)listeners.get(i);
					listener.modelChangedEvent(model, object, index);
				}
			}
		}
		
		public void addModelListener(ModelListener listener) {
			if (listeners == null) {
				listeners = new java.util.ArrayList();
			}	
			listeners.add(listener);
		}
		
		public void removeModelListener(ModelListener listener) {
			if (listeners != null) {
				listeners.remove(listener);
			}
		}

		private java.util.ArrayList listeners = null;
	}

}
		

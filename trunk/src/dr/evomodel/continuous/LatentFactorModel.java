/*
 * LatentFactorModel.java
 *
 * Copyright (c) 2002-2013 Alexei Drummond, Andrew Rambaut and Marc Suchard
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

package dr.evomodel.continuous;

import dr.inference.model.MatrixParameter;
import dr.math.matrixAlgebra.IllegalDimension;
import dr.math.matrixAlgebra.Matrix;


/**
 * @author Max Tolkoff
 * @author Marc Suchard
 */

public class LatentFactorModel {
    private Matrix data;
    private Matrix factors;
    private Matrix loadings;
    private Matrix residual;



    public LatentFactorModel(MatrixParameter dataIn, MatrixParameter factorsIn, MatrixParameter loadingsIn) {
        data = new Matrix(dataIn.getParameterAsMatrix());
        factors = new Matrix(factorsIn.getParameterAsMatrix());
        loadings = new Matrix(loadingsIn.getParameterAsMatrix());
        computeResiduals();
        System.out.print(residual.toComponents()[0][0]);
    }

    public Matrix getData(){
        Matrix ans=data;
        return ans;
    }

    public Matrix getFactors(){
        Matrix ans=factors;
        return ans;
    }

    public Matrix getLoadings(){
        Matrix ans=loadings;
        return ans;
    }

    public Matrix getResidual(){
        Matrix ans=residual;
        return ans;
    }

    public void computeResiduals(){
        try {
            residual = data.subtract(loadings.product(factors));
        } catch (IllegalDimension illegalDimension) {
            illegalDimension.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}

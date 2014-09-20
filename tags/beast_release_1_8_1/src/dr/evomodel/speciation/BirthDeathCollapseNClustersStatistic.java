/*
        This file is part of BEAST.

        BEAST is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        BEAST is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with DISSECT.  If not, see <http://www.gnu.org/licenses/>.
*/

package dr.evomodel.speciation;


import dr.evomodelxml.speciation.BirthDeathCollapseNClustersStatisticParser;
import dr.inference.model.Statistic;

/**
 * @author Graham Jones
 *         Date: 01/10/2013
 */
public class BirthDeathCollapseNClustersStatistic extends Statistic.Abstract {
    private SpeciesTreeModel spptree;
    private BirthDeathCollapseModel bdcm;


    public BirthDeathCollapseNClustersStatistic(SpeciesTreeModel spptree, BirthDeathCollapseModel bdcm) {
        super(BirthDeathCollapseNClustersStatisticParser.BDC_NCLUSTERS_STATISTIC);
        this.spptree = spptree;
        this.bdcm = bdcm;
    }

    @Override
    public int getDimension() {
        return 1;
    }

    @Override
    public double getStatisticValue(int dim) {
        int ninodes = spptree.getInternalNodeCount();
        int n =  0;
        for (int i = 0; i < ninodes; i++) {
            double h = spptree.getNodeHeight(spptree.getInternalNode(i));
            if (!BirthDeathCollapseModel.belowCollapseHeight(h, bdcm.getCollapseHeight())) {
                n++;
            }
        }
        return n+1;
    }
}

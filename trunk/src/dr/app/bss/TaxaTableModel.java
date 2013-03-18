package dr.app.bss;

import javax.swing.table.AbstractTableModel;

@SuppressWarnings("serial")
public class TaxaTableModel extends AbstractTableModel {

	private PartitionDataList dataList;

	private String[] columnNames = { "Name", "Height" };
	private double[] heights = null;

	public TaxaTableModel(PartitionDataList dataList) {
		this.dataList = dataList;
	}// END: Constructor

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return this.dataList.taxonList.getTaxonCount();
	}

	public Class<? extends Object> getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}

	public boolean isCellEditable(int row, int col) {
		return false;
	}

	public String getColumnName(int column) {
		return columnNames[column];
	}

	public Object getValueAt(int row, int col) {
		switch (col) {

		case 0:
			return this.dataList.taxonList.getTaxonId(row);

		case 1:

			if (heights != null) {
				return heights[row];
			} else {
				return 0.0;
			}

		default:
			return null;

		}// END: switch
	}// END: getValueAt

	public void setValueAt(Object value, int row, int col) {

		switch (col) {

		case 0:
			this.dataList.taxonList.getTaxon(row).setId(value.toString());
			break;

		case 1:
			// dataList.get(0).taxonList.getTaxon(row).getHeight();
			break;

		default:
			break;

		}// END: switch
	}// END: setValueAt

	private void getHeights() {

		heights = new double[dataList.taxonList.getTaxonCount()];
		for (int i = 0; i < dataList.taxonList.getTaxonCount(); i++) {

			heights[i] = (Double) dataList.taxonList.getTaxon(i).getAttribute(
					Utils.ABSOLUTE_HEIGHT);

		}// END: taxon loop

	}// END: getHeights

	public String toString() {
		StringBuffer buffer = new StringBuffer();

		buffer.append(getColumnName(0));
		for (int j = 1; j < getColumnCount(); j++) {
			buffer.append("\t");
			buffer.append(getColumnName(j));
		}
		buffer.append("\n");

		for (int i = 0; i < getRowCount(); i++) {
			buffer.append(getValueAt(i, 0));
			for (int j = 1; j < getColumnCount(); j++) {
				buffer.append("\t");
				buffer.append(getValueAt(i, j));
			}
			buffer.append("\n");
		}

		return buffer.toString();
	}

	public void fireTaxaChanged() {
		getHeights();
		fireTableDataChanged();
	}

	public void setDataList(PartitionDataList dataList) {
		this.dataList = dataList;
	}

}// END: TaxaTableModel class

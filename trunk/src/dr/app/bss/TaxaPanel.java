package dr.app.bss;

import dr.evolution.util.*;
import dr.app.gui.table.DateCellEditor;
import dr.app.gui.table.TableSorter;
import dr.app.beauti.options.DateGuesser;
import dr.app.beauti.tipdatepanel.GuessDatesDialog;
import dr.app.beauti.util.PanelUtils;
import jam.framework.Exportable;
import jam.table.HeaderRenderer;
import jam.table.TableRenderer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.BorderUIResource;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import dr.app.gui.table.TableEditorStopper;

@SuppressWarnings("serial")
public class TaxaPanel extends JPanel implements Exportable {

	private BeagleSequenceSimulatorFrame frame = null;
	private ArrayList<PartitionData> dataList = null;

	private JScrollPane scrollPane = new JScrollPane();
	private JTable dataTable = null;
	private DataTableModel dataTableModel = null;

	private ClearDatesAction clearDatesAction = new ClearDatesAction();
	private GuessDatesAction guessDatesAction = new GuessDatesAction();

	private JComboBox unitsCombo = new JComboBox(new String[] { "Years",
			"Months", "Days" });
	private JComboBox directionCombo = new JComboBox(new String[] {
			"Since some time in the past", "Before the present" });

	private DateGuesser guesser = new DateGuesser();
	private double[] heights = null;
	private GuessDatesDialog guessDatesDialog = null;

    public TaxaPanel(BeagleSequenceSimulatorFrame frame, ArrayList<PartitionData> dataList) {

        this.frame = frame;
        this.dataList = dataList;

        dataTableModel = new DataTableModel();
        TableSorter sorter = new TableSorter(dataTableModel);
        dataTable = new JTable(sorter);

        sorter.setTableHeader(dataTable.getTableHeader());

        dataTable.getTableHeader().setReorderingAllowed(false);
        dataTable.getTableHeader().setDefaultRenderer(
                new HeaderRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));

        dataTable.getColumnModel().getColumn(0).setCellRenderer(
                new TableRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));
        dataTable.getColumnModel().getColumn(0).setPreferredWidth(80);

        dataTable.getColumnModel().getColumn(1).setCellRenderer(
                new TableRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));
        dataTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        dataTable.getColumnModel().getColumn(1).setCellEditor(
                new DateCellEditor());

        dataTable.getColumnModel().getColumn(2).setCellRenderer(
                new TableRenderer(SwingConstants.LEFT, new Insets(0, 4, 0, 4)));
        dataTable.getColumnModel().getColumn(2).setPreferredWidth(80);

        TableEditorStopper.ensureEditingStopWhenTableLosesFocus(dataTable);

        dataTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent evt) {
                selectionChanged();
            }
        });

        scrollPane = new JScrollPane(dataTable,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setOpaque(false);

        PanelUtils.setupComponent(unitsCombo);
        PanelUtils.setupComponent(directionCombo);

        JToolBar toolBar1 = new JToolBar();
        toolBar1.setFloatable(false);
        toolBar1.setOpaque(false);

        toolBar1.setLayout(new FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));
        JButton button = new JButton(clearDatesAction);
        PanelUtils.setupComponent(button);
        toolBar1.add(button);
        button = new JButton(guessDatesAction);
        PanelUtils.setupComponent(button);
        toolBar1.add(button);
        toolBar1.add(new JToolBar.Separator(new Dimension(12, 12)));
        final JLabel unitsLabel = new JLabel("Dates specified as ");
        toolBar1.add(unitsLabel);
        toolBar1.add(unitsCombo);
        toolBar1.add(directionCombo);

        setOpaque(false);
        setBorder(new BorderUIResource.EmptyBorderUIResource(new java.awt.Insets(12, 12, 12, 12)));
        setLayout(new BorderLayout(0, 0));

        add(toolBar1, "North");
        add(scrollPane, "Center");

        ItemListener listener = new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
                timeScaleChanged();
            }
        };
        unitsCombo.addItemListener(listener);
        directionCombo.addItemListener(listener);

    }

    public final void dataChanged() {

        if (dataList.get(0).taxonList.getTaxonCount() > 0) {
            clearDatesAction.setEnabled(true);
            guessDatesAction.setEnabled(true);
            unitsCombo.setEnabled(true);
            directionCombo.setEnabled(true);

            //originField.setEnabled(true);

        }

        timeScaleChanged();
    }

    public final void timeScaleChanged() {
        Units.Type units = Units.Type.YEARS;
        switch (unitsCombo.getSelectedIndex()) {
            case 0:
                units = Units.Type.YEARS;
                break;
            case 1:
                units = Units.Type.MONTHS;
                break;
            case 2:
                units = Units.Type.DAYS;
                break;
        }

        boolean backwards = directionCombo.getSelectedIndex() == 1;

        for (int i = 0; i < dataList.get(0).taxonList.getTaxonCount(); i++) {
            Date date = dataList.get(0).taxonList.getTaxon(i).getDate();
            double d = date.getTimeValue();

            Date newDate = createDate(d, units, backwards, 0.0);

            dataList.get(0).taxonList.getTaxon(i).setDate(newDate);
        }

        calculateHeights();

        dataTableModel.fireTableDataChanged();
        frame.fireTaxaChanged();
    }

    private Date createDate(double timeValue, Units.Type units, boolean backwards, double origin) {
        if (backwards) {
            return Date.createTimeAgoFromOrigin(timeValue, units, origin);
        } else {
            return Date.createTimeSinceOrigin(timeValue, units, origin);
        }
    }

    public JComponent getExportableComponent() {
        return dataTable;
    }

    public void selectionChanged() {

        int[] selRows = dataTable.getSelectedRows();
        if (selRows == null || selRows.length == 0) {
            frame.dataSelectionChanged(false);
        } else {
            frame.dataSelectionChanged(true);
        }
    }


    public void clearDates() {
        for (int i = 0; i < dataList.get(0).taxonList.getTaxonCount(); i++) {
            java.util.Date origin = new java.util.Date(0);

            double d = 0.0;

            Date date = Date.createTimeSinceOrigin(d, Units.Type.YEARS, origin);
            dataList.get(0).taxonList.getTaxon(i).setAttribute("date", date);
        }

        // adjust the dates to the current timescale...
        timeScaleChanged();

        dataTableModel.fireTableDataChanged();
        frame.fireTaxaChanged();
    }

    public void guessDates() {

        if (guessDatesDialog == null) {
            guessDatesDialog = new GuessDatesDialog(frame);
        }

        int result = guessDatesDialog.showDialog();

        if (result == -1 || result == JOptionPane.CANCEL_OPTION) {
            return;
        }

        guesser.guessDates = true;
        guessDatesDialog.setupGuesser(guesser);

//        String warningMessage = null;

        guesser.guessDates(dataList.get(0).taxonList);

//        if (warningMessage != null) {
//            JOptionPane.showMessageDialog(this, "Warning: some dates may not be set correctly - \n" + warningMessage,
//                    "Error guessing dates",
//                    JOptionPane.WARNING_MESSAGE);
//        }

        // adjust the dates to the current timescale...
        timeScaleChanged();

        dataTableModel.fireTableDataChanged();
        frame.fireTaxaChanged();
    }

    public class ClearDatesAction extends AbstractAction {
        /**
         *
         */
        private static final long serialVersionUID = -7281309694753868635L;

        public ClearDatesAction() {
            super("Clear Dates");
            setToolTipText("Use this tool to remove sampling dates from each taxon");
        }

        public void actionPerformed(ActionEvent ae) {
            clearDates();
        }
    }

    public class GuessDatesAction extends AbstractAction {

        public GuessDatesAction() {
            super("Guess Dates");
            setToolTipText("Use this tool to guess the sampling dates from the taxon labels");
        }

        public void actionPerformed(ActionEvent ae) {
            guessDates();
        }
    }

    private void calculateHeights() {

        double maximumTipHeight = 0.0;
        if (dataList.get(0).taxonList == null || dataList.get(0).taxonList.getTaxonCount() == 0) return;

        heights = null;

        dr.evolution.util.Date mostRecent = null;
        for (int i = 0; i < dataList.get(0).taxonList.getTaxonCount(); i++) {
            Date date = dataList.get(0).taxonList.getTaxon(i).getDate();
            if ((date != null) && (mostRecent == null || date.after(mostRecent))) {
                mostRecent = date;
            }
        }

        if (mostRecent != null) {
            heights = new double[dataList.get(0).taxonList.getTaxonCount()];

            TimeScale timeScale = new TimeScale(mostRecent.getUnits(), true, mostRecent.getAbsoluteTimeValue());
            double time0 = timeScale.convertTime(mostRecent.getTimeValue(), mostRecent);

            for (int i = 0; i < dataList.get(0).taxonList.getTaxonCount(); i++) {
                Date date = dataList.get(0).taxonList.getTaxon(i).getDate();
                if (date != null) {
                    heights[i] = timeScale.convertTime(date.getTimeValue(), date) - time0;
                    if (heights[i] > maximumTipHeight) maximumTipHeight = heights[i];
                }
            }
        }
    }

    class DataTableModel extends AbstractTableModel {

        String[] columnNames = {"Name", "Date", "Height"};

        public DataTableModel() {
        }

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return dataList.get(0).taxonList.getTaxonCount();
        }

        public Object getValueAt(int row, int col) {
            switch (col) {
                case 0:
                    return dataList.get(0).taxonList.getTaxonId(row);
                case 1:
                    Date date = dataList.get(0).taxonList.getTaxon(row).getDate();
                    if (date != null) {
                        return date.getTimeValue();
                    } else {
                        return "-";
                    }
                case 2:
                    if (heights != null) {
                        return heights[row];
                    } else {
                        return "0.0";
                    }
            }
            return null;
        }

        public void setValueAt(Object aValue, int row, int col) {
            if (col == 0) {
            	dataList.get(0).taxonList.getTaxon(row).setId(aValue.toString());
            } else if (col == 1) {
                Date date = dataList.get(0).taxonList.getTaxon(row).getDate();
                if (date != null) {
                    double d = (Double) aValue;
                    Date newDate = createDate(d, date.getUnits(), date.isBackwards(), date.getOrigin());
                    dataList.get(0).taxonList.getTaxon(row).setDate(newDate);
                }
            }

            timeScaleChanged();
        }

        public boolean isCellEditable(int row, int col) {
            if (col == 0) return true;
            if (col == 1) {
                Date date = dataList.get(0).taxonList.getTaxon(row).getDate();
                return (date != null);
            }
            return false;
        }

        public String getColumnName(int column) {
            return columnNames[column];
        }

        public Class<? extends Object> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

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
    }

}//END: class

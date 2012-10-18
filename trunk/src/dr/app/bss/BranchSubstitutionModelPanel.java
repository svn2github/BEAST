package dr.app.bss;

import jam.framework.Exportable;
import jam.panels.OptionsPanel;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import dr.app.gui.components.RealNumberField;

@SuppressWarnings("serial")
public class BranchSubstitutionModelPanel extends JPanel implements Exportable {

	private BeagleSequenceSimulatorFrame frame = null;
	private ArrayList<BeagleSequenceSimulatorData> dataList = null;
	private OptionsPanel optionPanel;

	private JComboBox substitutionCombo;
	private RealNumberField[] substitutionParameterFields = new RealNumberField[BeagleSequenceSimulatorData.substitutionParameterNames.length];

	public BranchSubstitutionModelPanel(final BeagleSequenceSimulatorFrame frame,
			final ArrayList<BeagleSequenceSimulatorData> dataList) {

		super();

		this.frame = frame;
		this.dataList = dataList;

		setOpaque(false);
		setLayout(new BorderLayout());

		optionPanel = new OptionsPanel(12, 12, SwingConstants.CENTER);
		add(optionPanel, BorderLayout.NORTH);
		
		substitutionCombo = new JComboBox();
		substitutionCombo.setOpaque(false);

		for (String substitutionModel : BeagleSequenceSimulatorData.substitutionModels) {
			substitutionCombo.addItem(substitutionModel);
		}// END: fill loop

		substitutionCombo.addItemListener(new ListenSubstitutionCombo());

		for (int i = 0; i < BeagleSequenceSimulatorData.substitutionParameterNames.length; i++) {
			substitutionParameterFields[i] = new RealNumberField();
			substitutionParameterFields[i].setColumns(8);
			substitutionParameterFields[i].setValue(dataList.get(0).substitutionParameterValues[i]);
		}// END: fill loop

		setSubstitutionArguments();
		
	}// END: Constructor

	private void setSubstitutionArguments() {

		optionPanel.removeAll();
		optionPanel.addComponents(new JLabel("Branch substitution model:"), substitutionCombo);
		optionPanel.addSeparator();
		optionPanel.addLabel("Set parameter values:");

		int index = substitutionCombo.getSelectedIndex();

		for (int i = 0; i < dataList.get(0).substitutionParameterIndices[index].length; i++) {

			int k = dataList.get(0).substitutionParameterIndices[index][i];

			JPanel panel = new JPanel(new BorderLayout(6, 6));
			panel.add(substitutionParameterFields[k], BorderLayout.WEST);
			panel.setOpaque(false);
			optionPanel.addComponentWithLabel(BeagleSequenceSimulatorData.substitutionParameterNames[k] + ":", panel);

		}// END: indices loop

//		validate();
//		repaint();
	}// END: setSubstitutionArguments

	private class ListenSubstitutionCombo implements ItemListener {
		public void itemStateChanged(ItemEvent ie) {

			setSubstitutionArguments();
			frame.fireModelChanged();

		}// END: actionPerformed
	}// END: ListenSubstitutionCombo

	public void collectSettings() {

		dataList.get(0).substitutionModel = substitutionCombo.getSelectedIndex();
		for (int i = 0; i < BeagleSequenceSimulatorData.substitutionParameterNames.length; i++) {

			dataList.get(0).substitutionParameterValues[i] = substitutionParameterFields[i].getValue();

		}// END: fill loop
	}// END: collectSettings
	
	public JComponent getExportableComponent() {
		return this;
	}//END: getExportableComponent

}// END: class

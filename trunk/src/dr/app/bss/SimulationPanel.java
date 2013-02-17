package dr.app.bss;

import jam.framework.Exportable;
import jam.panels.OptionsPanel;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import org.virion.jam.components.WholeNumberField;

@SuppressWarnings("serial")
public class SimulationPanel extends JPanel implements Exportable {

	private BeagleSequenceSimulatorFrame frame;
	private PartitionDataList dataList;
	private OptionsPanel optionPanel;

	private WholeNumberField sequenceLengthField;

	private WholeNumberField simulationsNumberField;

	// Buttons
	private JButton simulate;
	private JButton generateXML;

	// Radio buttons
	private JRadioButton numberOfSimulationsRadioButton;
	private JRadioButton simulateForEachTreeRadioButton;

	// Strings for radio buttons
	private String firstSimulationType;
	private String secondSimulationType;

	public SimulationPanel(final BeagleSequenceSimulatorFrame frame,
			final PartitionDataList dataList) {

		this.frame = frame;
		this.dataList = dataList;

		optionPanel = new OptionsPanel(12, 12, SwingConstants.CENTER);

		// number of sites
		sequenceLengthField = new WholeNumberField(1, Integer.MAX_VALUE);
		sequenceLengthField.setColumns(8);
		sequenceLengthField.setValue(dataList.siteCount);
		optionPanel.addComponentWithLabel("Number of sites:",
				sequenceLengthField);

		// Simulation Type
		JPanel simulationTypeHolder = new JPanel();
		simulationTypeHolder.setOpaque(false);
		simulationTypeHolder.setLayout(new GridLayout(2, 1));

		// Button group
		ButtonGroup buttonGroup = new ButtonGroup();

		// Add first radio button
		JPanel simulationsNumberHolder = new JPanel();
		simulationsNumberHolder.setOpaque(false);
		firstSimulationType = new String("Number of simulations");
		numberOfSimulationsRadioButton = new JRadioButton();
		numberOfSimulationsRadioButton.setActionCommand(firstSimulationType);
		numberOfSimulationsRadioButton
				.setToolTipText("Generate specified number of datasets.");
		numberOfSimulationsRadioButton.setSelected(true);
		numberOfSimulationsRadioButton
				.addActionListener(new ChooseAnalysisTypeListener());
		simulationsNumberHolder.add(numberOfSimulationsRadioButton);

		// Add label
		JLabel simulationsNumberLabel = new JLabel(firstSimulationType);
		simulationsNumberHolder.add(simulationsNumberLabel);

		// Add number field
		simulationsNumberField = new WholeNumberField(1, Integer.MAX_VALUE);
		simulationsNumberField.setColumns(8);
		simulationsNumberField.setValue(dataList.simulationsCount);
		simulationsNumberHolder.add(simulationsNumberField);
		simulationTypeHolder.add(simulationsNumberHolder);

		// Add second radio button
		secondSimulationType = new String("Simulate for each tree");
		simulateForEachTreeRadioButton = new JRadioButton(secondSimulationType);
		simulateForEachTreeRadioButton.setActionCommand(secondSimulationType);
		simulateForEachTreeRadioButton
				.setToolTipText("Generate one dataset for each tree topology in .trees file.");
		simulateForEachTreeRadioButton
				.addActionListener(new ChooseAnalysisTypeListener());
		simulationTypeHolder.add(simulateForEachTreeRadioButton);

		// Group radio buttons
		buttonGroup.add(numberOfSimulationsRadioButton);
		buttonGroup.add(simulateForEachTreeRadioButton);

		optionPanel.addComponentWithLabel("Simulation type:",
				simulationTypeHolder);

		// Buttons holder
		JPanel buttonsHolder = new JPanel();
		buttonsHolder.setOpaque(false);

		// simulate button
		simulate = new JButton("Simulate",
				BeagleSequenceSimulatorApp.biohazardIcon);
		simulate.addActionListener(new ListenSimulate());
		buttonsHolder.add(simulate);

		generateXML = new JButton("Generate XML",
				BeagleSequenceSimulatorApp.hammerIcon);
		generateXML.addActionListener(new ListenGenerateXML());
		buttonsHolder.add(generateXML);

		setOpaque(false);
		setLayout(new BorderLayout());
		add(optionPanel, BorderLayout.NORTH);
		add(buttonsHolder, BorderLayout.SOUTH);

		setFirstSimulationType();
		
	}// END: SimulationPanel

	public final void collectSettings() {

		dataList.siteCount = sequenceLengthField.getValue();
		dataList.simulationsCount = simulationsNumberField.getValue();

		// frame.fireModelChanged();
	}// END: collectSettings

	private class ListenSimulate implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			frame.doExport();

		}// END: actionPerformed
	}// END: ListenSaveLocationCoordinates

	private class ListenGenerateXML implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			frame.doGenerateXML();

		}// END: actionPerformed
	}// END: ListenSaveLocationCoordinates

	private void setFirstSimulationType() {
		simulationsNumberField.setEnabled(true);
		frame.disableTreesFileButton();
		frame.enableTreeFileButton();
		frame.showTreeColumn();
	}// END: setFirstSimulationType

	private void setSecondSimulationType() {
		simulationsNumberField.setEnabled(false);
		frame.enableTreesFileButton();
		frame.disableTreeFileButton();
		frame.hideTreeColumn();
	}// END: setSecondSimulationType

	class ChooseAnalysisTypeListener implements ActionListener {
		public void actionPerformed(ActionEvent ev) {

			if (ev.getActionCommand() == firstSimulationType) {

				setFirstSimulationType();
				frame.setStatus(firstSimulationType + " selected");

			} else if (ev.getActionCommand() == secondSimulationType) {

				setSecondSimulationType();
				frame.setStatus(secondSimulationType + " selected");

			} else {
				throw new RuntimeException(
						"Unimplemented analysis type selected");
			}

		}// END: actionPerformed
	}// END: ChooseAnalysisTypeListener

	@Override
	public JComponent getExportableComponent() {
		return this;
	}// END: getExportableComponent

}// END: class

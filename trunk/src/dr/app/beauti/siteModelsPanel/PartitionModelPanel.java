/*
 * PartitionModelPanel.java
 *
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

package dr.app.beauti.siteModelsPanel;

import jam.panels.OptionsPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import dr.app.beauti.components.dnds.DnDsComponentOptions;
import dr.app.beauti.options.PartitionSubstitutionModel;
import dr.app.beauti.types.BinaryModelType;
import dr.app.beauti.types.DiscreteSubstModelType;
import dr.app.beauti.types.FrequencyPolicyType;
import dr.app.beauti.types.MicroSatModelType;
import dr.app.beauti.util.PanelUtils;
import dr.app.gui.components.WholeNumberField;
import dr.app.util.OSType;
import dr.evolution.datatype.DataType;
import dr.evolution.datatype.Microsatellite;
import dr.evomodel.substmodel.AminoAcidModelType;
import dr.evomodel.substmodel.NucModelType;

/**
 * @author Alexei Drummond
 * @author Walter Xie
 */
public class PartitionModelPanel extends OptionsPanel {

	// Components
	private static final long serialVersionUID = -1645661616353099424L;

	private JComboBox nucSubstCombo = new JComboBox(EnumSet.range(
			NucModelType.HKY, NucModelType.TN93).toArray());
	private JComboBox aaSubstCombo = new JComboBox(AminoAcidModelType.values());
	private JComboBox binarySubstCombo = new JComboBox(BinaryModelType.values());
	private JCheckBox useAmbiguitiesTreeLikelihoodCheck = new JCheckBox(
			"Use ambiguities in the tree likelihood associated with this model");

	private JComboBox frequencyCombo = new JComboBox(FrequencyPolicyType
			.values());

	private JComboBox heteroCombo = new JComboBox(new String[] { "None",
			"Gamma", "Invariant Sites", "Gamma + Invariant Sites" });

	private JComboBox gammaCatCombo = new JComboBox(new String[] { "4", "5",
			"6", "7", "8", "9", "10" });
	private JLabel gammaCatLabel;

	private JComboBox codingCombo = new JComboBox(new String[] { "Off",
			"2 partitions: positions (1 + 2), 3",
			"3 partitions: positions 1, 2, 3" });

	private JCheckBox substUnlinkCheck = new JCheckBox(
			"Unlink substitution rate parameters across codon positions");
	private JCheckBox heteroUnlinkCheck = new JCheckBox(
			"Unlink rate heterogeneity model across codon positions");
	private JCheckBox freqsUnlinkCheck = new JCheckBox(
			"Unlink base frequencies across codon positions");

	private JCheckBox robustCountingCheck = new JCheckBox("Use robust counting");
	private JButton setSRD06Button;

	private JCheckBox dolloCheck = new JCheckBox("Use Stochastic Dollo Model");
	// private JComboBox dolloCombo = new JComboBox(new String[]{"Analytical",
	// "Sample"});

	private JComboBox discreteTraitSiteModelCombo = new JComboBox(
			DiscreteSubstModelType.values());
	private JCheckBox activateBSSVS = new JCheckBox(
	// "Activate BSSVS"
			"Infer social network with BSSVS");

	// =========== micro sat ===========
	private JTextField microsatName = new JTextField();
	private WholeNumberField microsatMax = new WholeNumberField(2,
			Integer.MAX_VALUE);
	private WholeNumberField microsatMin = new WholeNumberField(1,
			Integer.MAX_VALUE);
	private JComboBox rateProportionCombo = new JComboBox(
			MicroSatModelType.RateProportionality.values());
	private JComboBox mutationBiasCombo = new JComboBox(
			MicroSatModelType.MutationalBias.values());
	private JComboBox phaseCombo = new JComboBox(MicroSatModelType.Phase
			.values());
	JCheckBox shareMicroSatCheck = new JCheckBox(
			"Share one microsatellite among all substitution model(s)");

	protected final PartitionSubstitutionModel model;

	public PartitionModelPanel(final PartitionSubstitutionModel partitionModel) {

		super(12, (OSType.isMac() ? 6 : 24));

		this.model = partitionModel;

		initCodonPartitionComponents();

		PanelUtils.setupComponent(nucSubstCombo);
		nucSubstCombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				model.setNucSubstitutionModel((NucModelType) nucSubstCombo
						.getSelectedItem());
			}
		});
		nucSubstCombo
				.setToolTipText("<html>Select the type of nucleotide substitution model.</html>");

		PanelUtils.setupComponent(aaSubstCombo);
		aaSubstCombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				model.setAaSubstitutionModel((AminoAcidModelType) aaSubstCombo
						.getSelectedItem());
			}
		});
		aaSubstCombo
				.setToolTipText("<html>Select the type of amino acid substitution model.</html>");

		PanelUtils.setupComponent(binarySubstCombo);
		binarySubstCombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				model
						.setBinarySubstitutionModel((BinaryModelType) binarySubstCombo
								.getSelectedItem());
				useAmbiguitiesTreeLikelihoodCheck.setSelected(binarySubstCombo
						.getSelectedItem() == BinaryModelType.BIN_COVARION);
				useAmbiguitiesTreeLikelihoodCheck.setEnabled(binarySubstCombo
						.getSelectedItem() != BinaryModelType.BIN_COVARION);
			}
		});
		binarySubstCombo
				.setToolTipText("<html>Select the type of binary substitution model.</html>");

		PanelUtils.setupComponent(useAmbiguitiesTreeLikelihoodCheck);
		useAmbiguitiesTreeLikelihoodCheck.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				model
						.setUseAmbiguitiesTreeLikelihood(useAmbiguitiesTreeLikelihoodCheck
								.isSelected());
			}
		});
		useAmbiguitiesTreeLikelihoodCheck
				.setToolTipText("<html>Detemine useAmbiguities in &lt treeLikelihood &gt .</html>");

		PanelUtils.setupComponent(frequencyCombo);
		frequencyCombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				model.setFrequencyPolicy((FrequencyPolicyType) frequencyCombo
						.getSelectedItem());
			}
		});
		frequencyCombo
				.setToolTipText("<html>Select the policy for determining the base frequencies.</html>");

		PanelUtils.setupComponent(heteroCombo);
		heteroCombo
				.setToolTipText("<html>Select the type of site-specific rate<br>heterogeneity model.</html>");
		heteroCombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {

				boolean gammaHetero = heteroCombo.getSelectedIndex() == 1
						|| heteroCombo.getSelectedIndex() == 3;

				model.setGammaHetero(gammaHetero);
				model.setInvarHetero(heteroCombo.getSelectedIndex() == 2
						|| heteroCombo.getSelectedIndex() == 3);

				if (gammaHetero) {
					gammaCatLabel.setEnabled(true);
					gammaCatCombo.setEnabled(true);
				} else {
					gammaCatLabel.setEnabled(false);
					gammaCatCombo.setEnabled(false);
				}

				if (codingCombo.getSelectedIndex() != 0) {
					heteroUnlinkCheck
							.setEnabled(heteroCombo.getSelectedIndex() != 0);
					heteroUnlinkCheck.setSelected(heteroCombo
							.getSelectedIndex() != 0);
				}
			}
		});

		PanelUtils.setupComponent(gammaCatCombo);
		gammaCatCombo
				.setToolTipText("<html>Select the number of categories to use for<br>the discrete gamma rate heterogeneity model.</html>");
		gammaCatCombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {

				model.setGammaCategories(gammaCatCombo.getSelectedIndex() + 4);
			}
		});

		class ListenSetSRD06Button implements ActionListener {
			public void actionPerformed(ActionEvent ev) {
				setSRD06Model();
			}
		}

		setSRD06Button = new JButton("Use SRD06 model");
		setSRD06Button.addActionListener(new ListenSetSRD06Button());
		PanelUtils.setupComponent(setSRD06Button);
		setSRD06Button
				.setToolTipText("<html>Sets the SRD06 model as described in<br>"
						+ "Shapiro, Rambaut & Drummond (2006) <i>MBE</i> <b>23</b>: 7-9.</html>");

		// ////////////////////////////
		// ---dNdS robust counting---//
		// ////////////////////////////
		// TODO

		robustCountingCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				if (robustCountingCheck.isSelected()) {

					if (checkRobustCounting()) {
						setRobustCountingModel();
					}// END: checkRobustCounting

				} else {
					removeRobustCountingModel();
				}

			}// END: actionPerformed

			private boolean checkRobustCounting() {
				// TODO Auto-generated method stub
				return false;
			}

			private void setRobustCountingModel() {
				DnDsComponentOptions comp = (DnDsComponentOptions) model
						.getOptions().getComponentOptions(
								DnDsComponentOptions.class);

				comp.addPartition(model);

			}

			private void removeRobustCountingModel() {
				DnDsComponentOptions comp = (DnDsComponentOptions) model
						.getOptions().getComponentOptions(
								DnDsComponentOptions.class);

				// Remove model from ComponentOptions
				comp.removePartition(model);

			}

		});

		PanelUtils.setupComponent(robustCountingCheck);
		robustCountingCheck
				.setToolTipText("<html>"
						+ "Enable counting of synonymous and non-synonymous mutations as described in<br> Lemey, Minin, Bielejec, Kosakovsky-Pond & Suchard (in preparation)"
						+ "</html>");

		// ////////////////////////
		// ---END: dNdS button---//
		// ////////////////////////

		PanelUtils.setupComponent(dolloCheck);
		dolloCheck.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				model.setDolloModel(true);
			}
		});
		dolloCheck.setEnabled(true);
		dolloCheck
				.setToolTipText("<html>Activates a Stochastic Dollo model as described in<br>"
						+ "Alekseyenko, Lee & Suchard (2008) <i>Syst Biol</i> <b>57</b>: 772-784.</html>");

		PanelUtils.setupComponent(discreteTraitSiteModelCombo);
		discreteTraitSiteModelCombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				model
						.setDiscreteSubstType((DiscreteSubstModelType) discreteTraitSiteModelCombo
								.getSelectedItem());
			}
		});

		PanelUtils.setupComponent(activateBSSVS);
		activateBSSVS.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				model.setActivateBSSVS(activateBSSVS.isSelected());
			}
		});
		activateBSSVS
				.setToolTipText("<html>Actives Bayesian stochastic search variable selection on the rates as decribed in<br>"
						+ "Lemey, Rambaut, Drummond & Suchard (2009) <i>PLoS Computational Biology</i> <b>5</b>: e1000520</html>");

		// ============ micro-sat ================
		microsatName.setColumns(30);
		microsatName.addKeyListener(new java.awt.event.KeyListener() {
			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				model.getMicrosatellite().setName(microsatName.getText());
			}
		});
		microsatMax.setColumns(10);
		microsatMax.addKeyListener(new java.awt.event.KeyListener() {
			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				model.getMicrosatellite().setMax(
						Integer.parseInt(microsatMax.getText()));
			}
		});
		microsatMin.setColumns(10);
		microsatMin.addKeyListener(new java.awt.event.KeyListener() {
			public void keyTyped(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
				model.getMicrosatellite().setMin(
						Integer.parseInt(microsatMin.getText()));
			}
		});

		PanelUtils.setupComponent(shareMicroSatCheck);
		shareMicroSatCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				model.getOptions().shareMicroSat = shareMicroSatCheck
						.isSelected();
				if (shareMicroSatCheck.isSelected()) {
					model.getOptions().shareMicroSat();
				} else {
					model.getOptions().unshareMicroSat();
				}
				setOptions();
			}
		});

		PanelUtils.setupComponent(rateProportionCombo);
		rateProportionCombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				model
						.setRatePorportion((MicroSatModelType.RateProportionality) rateProportionCombo
								.getSelectedItem());
			}
		});
		// rateProportionCombo.setToolTipText("<html>Select the type of microsatellite substitution model.</html>");
		PanelUtils.setupComponent(mutationBiasCombo);
		mutationBiasCombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				model
						.setMutationBias((MicroSatModelType.MutationalBias) mutationBiasCombo
								.getSelectedItem());
			}
		});
		PanelUtils.setupComponent(phaseCombo);
		phaseCombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				model.setPhase((MicroSatModelType.Phase) phaseCombo
						.getSelectedItem());
			}
		});

		setupPanel();
		setOpaque(false);
	}

	/**
	 * Sets the components up according to the partition model - but does not
	 * layout the top level options panel.
	 */
	public void setOptions() {

		if (SiteModelsPanel.DEBUG) {
			String modelName = (model == null) ? "null" : model.getName();
			Logger.getLogger("dr.app.beauti").info(
					"ModelsPanel.setModelOptions(" + modelName + ")");
		}

		if (model == null) {
			return;
		}

		int dataType = model.getDataType().getType();
		switch (dataType) {
		case DataType.NUCLEOTIDES:
			nucSubstCombo.setSelectedItem(model.getNucSubstitutionModel());
			frequencyCombo.setSelectedItem(model.getFrequencyPolicy());

			break;

		case DataType.AMINO_ACIDS:
			aaSubstCombo.setSelectedItem(model.getAaSubstitutionModel());

			break;

		case DataType.TWO_STATES:
		case DataType.COVARION:
			binarySubstCombo
					.setSelectedItem(model.getBinarySubstitutionModel());
			useAmbiguitiesTreeLikelihoodCheck.setSelected(model
					.isUseAmbiguitiesTreeLikelihood());

			break;

		case DataType.GENERAL:
			discreteTraitSiteModelCombo.setSelectedItem(model
					.getDiscreteSubstType());
			activateBSSVS.setSelected(model.isActivateBSSVS());
			break;

		case DataType.MICRO_SAT:
			microsatName.setText(model.getMicrosatellite().getName());
			microsatMax.setText(Integer.toString(model.getMicrosatellite()
					.getMax()));
			microsatMin.setText(Integer.toString(model.getMicrosatellite()
					.getMin()));
			shareMicroSatCheck.setSelected(model.getOptions().shareMicroSat);
			rateProportionCombo.setSelectedItem(model.getRatePorportion());
			mutationBiasCombo.setSelectedItem(model.getMutationBias());
			phaseCombo.setSelectedItem(model.getPhase());
			shareMicroSatCheck.setEnabled(model.getOptions()
					.getPartitionSubstitutionModels(Microsatellite.INSTANCE)
					.size() > 1);
			break;

		default:
			throw new IllegalArgumentException("Unknown data type");
		}

		if (model.isGammaHetero() && !model.isInvarHetero()) {
			heteroCombo.setSelectedIndex(1);
		} else if (!model.isGammaHetero() && model.isInvarHetero()) {
			heteroCombo.setSelectedIndex(2);
		} else if (model.isGammaHetero() && model.isInvarHetero()) {
			heteroCombo.setSelectedIndex(3);
		} else {
			heteroCombo.setSelectedIndex(0);
		}

		gammaCatCombo.setSelectedIndex(model.getGammaCategories() - 4);

		if (model.getCodonHeteroPattern() == null) {
			codingCombo.setSelectedIndex(0);
		} else if (model.getCodonHeteroPattern().equals("112")) {
			codingCombo.setSelectedIndex(1);
		} else {
			codingCombo.setSelectedIndex(2);
		}

		substUnlinkCheck.setSelected(model.isUnlinkedSubstitutionModel());
		heteroUnlinkCheck.setSelected(model.isUnlinkedHeterogeneityModel());
		freqsUnlinkCheck.setSelected(model.isUnlinkedFrequencyModel());

		dolloCheck.setSelected(model.isDolloModel());
	}

	/**
	 * Configure this panel for the Shapiro, Rambaut and Drummond 2006 codon
	 * position model
	 */
	private void setSRD06Model() {
		nucSubstCombo.setSelectedIndex(0);
		heteroCombo.setSelectedIndex(1);
		codingCombo.setSelectedIndex(1);
		substUnlinkCheck.setSelected(true);
		heteroUnlinkCheck.setSelected(true);
	}

	/**
	 * Lays out the appropriate components in the panel for this partition
	 * model.
	 */
	private void setupPanel() {

		switch (model.getDataType().getType()) {
		case DataType.NUCLEOTIDES:
			addComponentWithLabel("Substitution Model:", nucSubstCombo);
			addComponentWithLabel("Base frequencies:", frequencyCombo);
			addComponentWithLabel("Site Heterogeneity Model:", heteroCombo);
			heteroCombo.setSelectedIndex(0);
			gammaCatLabel = addComponentWithLabel(
					"Number of Gamma Categories:", gammaCatCombo);
			gammaCatCombo.setEnabled(false);

			addSeparator();

			addComponentWithLabel("Partition into codon positions:",
					codingCombo);

			JPanel panel2 = new JPanel();
			panel2.setOpaque(false);
			panel2.setLayout(new BoxLayout(panel2, BoxLayout.PAGE_AXIS));
			panel2.setBorder(BorderFactory
					.createTitledBorder("Link/Unlink parameters:"));
			panel2.add(substUnlinkCheck);
			panel2.add(heteroUnlinkCheck);
			panel2.add(freqsUnlinkCheck);

			addComponent(panel2);

			addComponent(setSRD06Button);

			// ///////////////////
			// ---dNdS button---//
			// ///////////////////
			addComponent(robustCountingCheck);
			// ////////////////////////
			// ---END: dNdS button---//
			// ////////////////////////

			break;

		case DataType.AMINO_ACIDS:
			addComponentWithLabel("Substitution Model:", aaSubstCombo);
			addComponentWithLabel("Site Heterogeneity Model:", heteroCombo);
			heteroCombo.setSelectedIndex(0);
			gammaCatLabel = addComponentWithLabel(
					"Number of Gamma Categories:", gammaCatCombo);
			gammaCatCombo.setEnabled(false);

			break;

		case DataType.TWO_STATES:
		case DataType.COVARION:
			addComponentWithLabel("Substitution Model:", binarySubstCombo);
			addComponentWithLabel("Base frequencies:", frequencyCombo);
			addComponentWithLabel("Site Heterogeneity Model:", heteroCombo);
			heteroCombo.setSelectedIndex(0);
			gammaCatLabel = addComponentWithLabel(
					"Number of Gamma Categories:", gammaCatCombo);
			gammaCatCombo.setEnabled(false);

			addSeparator();

			addComponentWithLabel("", useAmbiguitiesTreeLikelihoodCheck);

			break;

		case DataType.GENERAL:
			addComponentWithLabel("Discrete Trait Substitution Model:",
					discreteTraitSiteModelCombo);
			addComponent(activateBSSVS);
			break;

		case DataType.MICRO_SAT:
			addComponentWithLabel("Microsatellite Name:", microsatName);
			addComponentWithLabel("Max of Length:", microsatMax);
			addComponentWithLabel("Min of Length:", microsatMin);
			addComponent(shareMicroSatCheck);

			addSeparator();

			addComponentWithLabel("Rate Proportionality:", rateProportionCombo);
			addComponentWithLabel("Mutational Bias:", mutationBiasCombo);
			addComponentWithLabel("Phase:", phaseCombo);
			break;

		default:
			throw new IllegalArgumentException("Unknown data type");

		}

		// if (BeautiApp.advanced) {
		addSeparator();
		addComponent(dolloCheck);
		// }

		setOptions();
	}

	/**
	 * Initializes and binds the components related to modeling codon positions.
	 */
	private void initCodonPartitionComponents() {

		PanelUtils.setupComponent(substUnlinkCheck);

		substUnlinkCheck.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				model.setUnlinkedSubstitutionModel(substUnlinkCheck
						.isSelected());
			}
		});
		substUnlinkCheck.setEnabled(false);
		substUnlinkCheck.setToolTipText(""
				+ "<html>Gives each codon position partition different<br>"
				+ "substitution model parameters.</html>");

		PanelUtils.setupComponent(heteroUnlinkCheck);
		heteroUnlinkCheck.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				model.setUnlinkedHeterogeneityModel(heteroUnlinkCheck
						.isSelected());
			}
		});
		heteroUnlinkCheck.setEnabled(heteroCombo.getSelectedIndex() != 0);
		heteroUnlinkCheck
				.setToolTipText("<html>Gives each codon position partition different<br>rate heterogeneity model parameters.</html>");

		PanelUtils.setupComponent(freqsUnlinkCheck);
		freqsUnlinkCheck.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {
				model.setUnlinkedFrequencyModel(freqsUnlinkCheck.isSelected());
			}
		});
		freqsUnlinkCheck.setEnabled(false);
		freqsUnlinkCheck
				.setToolTipText("<html>Gives each codon position partition different<br>nucleotide frequency parameters.</html>");

		PanelUtils.setupComponent(codingCombo);
		codingCombo
				.setToolTipText("<html>Select how to partition the codon positions.</html>");
		codingCombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ev) {

				switch (codingCombo.getSelectedIndex()) {
				case 0:
					model.setCodonHeteroPattern(null);
					break;
				case 1:
					model.setCodonHeteroPattern("112");
					break;
				default:
					model.setCodonHeteroPattern("123");
					break;

				}

				if (codingCombo.getSelectedIndex() != 0) {
					// codon position partitioning
					substUnlinkCheck.setEnabled(true);
					heteroUnlinkCheck
							.setEnabled(heteroCombo.getSelectedIndex() != 3);
					freqsUnlinkCheck.setEnabled(true);
					substUnlinkCheck.setSelected(true);
					heteroUnlinkCheck.setSelected(heteroCombo
							.getSelectedIndex() != 0);
					freqsUnlinkCheck.setSelected(true);

				} else {
					substUnlinkCheck.setEnabled(false);
					substUnlinkCheck.setSelected(false);
					heteroUnlinkCheck.setEnabled(false);
					heteroUnlinkCheck.setSelected(false);
					freqsUnlinkCheck.setEnabled(false);
					freqsUnlinkCheck.setSelected(false);
				}
			}
		});
	}

}

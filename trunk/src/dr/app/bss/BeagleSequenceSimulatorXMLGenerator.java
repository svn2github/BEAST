package dr.app.bss;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import dr.app.beauti.util.XMLWriter;
import dr.evolution.datatype.DataType;
import dr.evolution.tree.Tree;
import dr.evolution.util.Taxon;
import dr.evolution.util.TaxonList;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.sitemodel.GammaSiteModel;
import dr.evomodel.sitemodel.SiteModel;
import dr.evomodel.substmodel.NucModelType;
import dr.evomodel.tree.TreeModel;
import dr.evomodelxml.branchratemodel.StrictClockBranchRatesParser;
import dr.evomodelxml.coalescent.CoalescentSimulatorParser;
import dr.evomodelxml.sitemodel.GammaSiteModelParser;
import dr.evomodelxml.substmodel.FrequencyModelParser;
import dr.evomodelxml.substmodel.GTRParser;
import dr.evomodelxml.substmodel.HKYParser;
import dr.evomodelxml.substmodel.TN93Parser;
import dr.evomodelxml.substmodel.YangCodonModelParser;
import dr.evomodelxml.tree.TreeModelParser;
import dr.evoxml.NewickParser;
import dr.evoxml.TaxaParser;
import dr.evoxml.TaxonParser;
import dr.inference.model.ParameterParser;
import dr.util.Attribute;
import dr.xml.XMLParser;

public class BeagleSequenceSimulatorXMLGenerator {

	private PartitionDataList dataList;

	public BeagleSequenceSimulatorXMLGenerator(PartitionDataList dataList) {

		this.dataList = dataList;

	}// END: Constructor

	public void generateXML(File file) throws IOException {

		XMLWriter writer = new XMLWriter(new BufferedWriter(
				new FileWriter(file)));

		// //////////////
		// ---header---//
		// //////////////

		writer.writeText("<?xml version=\"1.0\" standalone=\"yes\"?>");
		writer.writeComment("Generated by "
				+ BeagleSequenceSimulatorApp.BEAGLE_SEQUENCE_SIMULATOR + " "
				+ BeagleSequenceSimulatorApp.VERSION);

		writer.writeOpenTag("beast");
		writer.writeBlankLine();

		// ////////////////////
		// ---taxa element---//
		// ////////////////////

		try {

			writeTaxa(dataList.taxonList, writer);
			writer.writeBlankLine();

		} catch (Exception e) {

			System.err.println(e);
			throw new RuntimeException("Taxon list generation has failed:\n"
					+ e.getMessage());

		}// END: try-catch block

		// /////////////////////////////
		// ---starting tree element---//
		// /////////////////////////////

		try {

			for (PartitionData data : dataList) {

				TreeModel tree = data.treeModel;
				writeStartingTree(tree, writer);
				writer.writeBlankLine();

			}// END: partitions loop

		} catch (Exception e) {

			System.err.println(e);
			throw new RuntimeException("Starting tree generation has failed:\n"
					+ e.getMessage());

		}// END: try-catch block

		// //////////////////////////
		// ---tree model element---//
		// //////////////////////////

		try {

			for (PartitionData data : dataList) {

				TreeModel tree = data.treeModel;
				writeTreeModel(tree, writer);
				writer.writeBlankLine();

			}// END: partitions loop

		} catch (Exception e) {

			System.err.println(e);
			throw new RuntimeException("Tree model generation has failed:\n"
					+ e.getMessage());

		}// END: try-catch block

		// //////////////////////////////////
		// ---branch rates model element---//
		// //////////////////////////////////

		try {

			for (PartitionData data : dataList) {

				writeBranchRatesModel(data, writer);
				writer.writeBlankLine();

			}// END: partitions loop

		} catch (Exception e) {

			System.err.println(e);
			throw new RuntimeException("Clock model generation has failed:\n"
					+ e.getMessage());

		}// END: try-catch block

		// ///////////////////////////////
		// ---frequency model element---//
		// ///////////////////////////////

		try {

			for (PartitionData data : dataList) {

				writeFrequencyModel(data, writer);
				writer.writeBlankLine();

			}// END: partitions loop

		} catch (Exception e) {

			System.err.println(e);
			throw new RuntimeException(
					"Frequency model generation has failed:\n" + e.getMessage());

		}// END: try-catch block

		// ////////////////////////////
		// ---branch model element---//
		// ////////////////////////////

		try {

			for (PartitionData data : dataList) {

				writeBranchModel(data, writer);
				writer.writeBlankLine();

			}// END: partitions loop

		} catch (Exception e) {

			System.err.println(e);
			throw new RuntimeException("Branch model generation has failed:\n"
					+ e.getMessage());

		}// END: try-catch block

		// //////////////////////////
		// ---site model element---//
		// //////////////////////////

		try {

			for (PartitionData data : dataList) {

				writeSiteModel(data, writer);
				writer.writeBlankLine();

			}// END: partitions loop

		} catch (Exception e) {

			System.err.println(e);
			throw new RuntimeException("Site model generation has failed:\n"
					+ e.getMessage());

		}// END: try-catch block

		// /////////////////////////////////////////
		// ---beagle sequence simulator element---//
		// /////////////////////////////////////////

		try {

//			for (PartitionData data : dataList) {

				writeBeagleSequenceSimulator(writer);
				writer.writeBlankLine();

//			}// END: partitions loop

		} catch (Exception e) {

			System.err.println(e);
			throw new RuntimeException("Beagle Sequence Simulator element generation has failed:\n"
					+ e.getMessage());

		}// END: try-catch block
		
		writer.writeCloseTag("beast");
		writer.flush();
		writer.close();
	}// END: generateXML

	private void writeBeagleSequenceSimulator(XMLWriter writer) {
		//TODO
		
		
	}//END: writeBeagleSequenceSimulator
	
	private void writeSiteModel(PartitionData data, XMLWriter writer) {

		writer.writeOpenTag(GammaSiteModel.SITE_MODEL,
				new Attribute[] { new Attribute.Default<String>(XMLParser.ID,
						SiteModel.SITE_MODEL) });

		writer.writeOpenTag(GammaSiteModelParser.SUBSTITUTION_MODEL);

		int substitutionModelIndex = data.substitutionModelIndex;
		switch (substitutionModelIndex) {

		case 0: // HKY

			writer.writeIDref(NucModelType.HKY.getXMLName(), PartitionData.substitutionModels[0].toLowerCase());
			break;

		case 1: // GTR

			writer.writeIDref(GTRParser.GTR_MODEL, PartitionData.substitutionModels[1].toLowerCase());
			break;

		case 2: // TN93

			writer.writeIDref(NucModelType.TN93.getXMLName(), PartitionData.substitutionModels[2].toLowerCase());
			break;

		case 3: // Yang Codon Model

			writer.writeIDref(YangCodonModelParser.YANG_CODON_MODEL, PartitionData.substitutionModels[3].replaceAll(" +", ".").toLowerCase());
			break;

		}// END: switch
		
		writer.writeCloseTag(GammaSiteModelParser.SUBSTITUTION_MODEL);
		
		int siteRateModelIndex = data.siteRateModelIndex;
		switch (siteRateModelIndex) {

		case 0: // no model

			// do nothing

			break;

		case 1: // GammaSiteRateModel
            writer.writeOpenTag(GammaSiteModelParser.GAMMA_SHAPE,
                    new Attribute.Default<String>(
                            GammaSiteModelParser.GAMMA_CATEGORIES, String.valueOf(data.siteRateModelParameterValues[0])));
            
			writeParameter(null, "alpha", 1,
					String.valueOf(data.siteRateModelParameterValues[1]), null,
					null, writer);
            
            writer.writeCloseTag(GammaSiteModelParser.GAMMA_SHAPE);
			break;
		}// END: switch

		writer.writeCloseTag(GammaSiteModel.SITE_MODEL);

	}// END: writeSiteModel

	private void writeBranchModel(PartitionData data, XMLWriter writer) {

		int substitutionModelIndex = data.substitutionModelIndex;

		switch (substitutionModelIndex) {

		case 0: // HKY

			writer.writeOpenTag(NucModelType.HKY.getXMLName(),
					new Attribute[] { new Attribute.Default<String>(
							XMLParser.ID, PartitionData.substitutionModels[0].toLowerCase()) });

			writer.writeOpenTag(FrequencyModelParser.FREQUENCIES);

			writer.writeIDref(FrequencyModelParser.FREQUENCY_MODEL, "freqModel");

			writer.writeCloseTag(FrequencyModelParser.FREQUENCIES);

			writeParameter(HKYParser.KAPPA, HKYParser.KAPPA, 1,
					String.valueOf(data.substitutionParameterValues[0]), null,
					null, writer);

			writer.writeCloseTag(NucModelType.HKY.getXMLName());

			break;

		case 1: // GTR

			writer.writeOpenTag(GTRParser.GTR_MODEL,
					new Attribute[] { new Attribute.Default<String>(
							XMLParser.ID, PartitionData.substitutionModels[1].toLowerCase()) });

			writer.writeOpenTag(FrequencyModelParser.FREQUENCIES);

			writer.writeIDref(FrequencyModelParser.FREQUENCY_MODEL, "freqModel");

			writer.writeCloseTag(FrequencyModelParser.FREQUENCIES);

			writeParameter(GTRParser.A_TO_C,
					String.valueOf(data.substitutionParameterValues[1]), 1,
					null, null, null, writer);
			writeParameter(GTRParser.A_TO_G,
					String.valueOf(data.substitutionParameterValues[2]), 1,
					null, null, null, writer);
			writeParameter(GTRParser.A_TO_T,
					String.valueOf(data.substitutionParameterValues[3]), 1,
					null, null, null, writer);
			writeParameter(GTRParser.C_TO_G,
					String.valueOf(data.substitutionParameterValues[4]), 1,
					null, null, null, writer);
			writeParameter(GTRParser.C_TO_T,
					String.valueOf(data.substitutionParameterValues[5]), 1,
					null, null, null, writer);
			writeParameter(GTRParser.G_TO_T,
					String.valueOf(data.substitutionParameterValues[6]), 1,
					null, null, null, writer);

			writer.writeCloseTag(GTRParser.GTR_MODEL);

			break;

		case 2: // TN93

			writer.writeOpenTag(NucModelType.TN93.getXMLName(),
					new Attribute[] { new Attribute.Default<String>(
							XMLParser.ID, PartitionData.substitutionModels[2].toLowerCase()) });

			writer.writeOpenTag(FrequencyModelParser.FREQUENCIES);

			writer.writeIDref(FrequencyModelParser.FREQUENCY_MODEL, "freqModel");

			writer.writeCloseTag(FrequencyModelParser.FREQUENCIES);

			writeParameter(TN93Parser.KAPPA1, "kappa1", 1,
					String.valueOf(data.substitutionParameterValues[7]), null,
					null, writer);

			writeParameter(TN93Parser.KAPPA2, "kappa2", 1,
					String.valueOf(data.substitutionParameterValues[8]), null,
					null, writer);

			writer.writeCloseTag(NucModelType.TN93.getXMLName());

			break;

		case 3: // Yang Codon Model

			writer.writeOpenTag(YangCodonModelParser.YANG_CODON_MODEL,
					new Attribute[] { new Attribute.Default<String>(
							XMLParser.ID, PartitionData.substitutionModels[3].replaceAll(" +", ".").toLowerCase()) });

			writer.writeOpenTag(FrequencyModelParser.FREQUENCIES);

			writer.writeIDref(FrequencyModelParser.FREQUENCY_MODEL, "freqModel");

			writer.writeCloseTag(FrequencyModelParser.FREQUENCIES);

			writeParameter(YangCodonModelParser.OMEGA, "omega", 1,
					String.valueOf(data.substitutionParameterValues[9]), null,
					null, writer);

			writeParameter(YangCodonModelParser.KAPPA, "kappa", 1,
					String.valueOf(data.substitutionParameterValues[10]), null,
					null, writer);

			writer.writeCloseTag(YangCodonModelParser.YANG_CODON_MODEL);

			break;

		}// END: switch

	}// END: writeBranchModel

	// private String multiDimensionalValue(int dimension, double firstValue,
	// double repValue) {
	//
	// String value = firstValue + "";
	//
	// for (int i = 0; i < dimension - 1; i++) {
	//
	// value += " " + repValue;
	//
	// }
	//
	// return value;
	// }// END: multiDimensionalValue

	private void writeFrequencyModel(PartitionData data, XMLWriter writer) {

		DataType dataType = null;
		String frequencies = null;
		int dataTypeIndex = data.dataTypeIndex;

		switch (dataTypeIndex) {

		case 0: // Nucleotide

			dataType = data.createDataType();

			frequencies = data.frequencyParameterValues[0] + "";
			for (int i = 1; i < 4; i++) {
				frequencies += " " + data.frequencyParameterValues[i];
			}

			// frequencies = String
			// .valueOf(data.frequencyParameterValues[0])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[1])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[2])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[3]);

			writer.writeOpenTag(FrequencyModelParser.FREQUENCY_MODEL, // tagname
					new Attribute[] { // attributes[]
							new Attribute.Default<String>(XMLParser.ID,
									"freqModel"), // id
							new Attribute.Default<String>("dataType", dataType
									.getDescription()) // dataType
					});

			writeParameter(FrequencyModelParser.FREQUENCIES, null,
					dataType.getStateCount(), frequencies, 0.0, 1.0, writer);

			writer.writeCloseTag(FrequencyModelParser.FREQUENCY_MODEL);

			break;

		case 1: // Codon

			dataType = data.createDataType();

			frequencies = data.frequencyParameterValues[4] + "";
			for (int i = 5; i < 64; i++) {
				frequencies += " " + data.frequencyParameterValues[i];
			}

			// frequencies = String
			// .valueOf(data.frequencyParameterValues[4])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[5])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[6])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[7])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[8])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[9])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[10])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[11])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[12])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[13])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[14])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[15])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[16])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[17])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[18])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[19])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[20])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[21])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[22])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[23])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[24])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[25])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[26])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[27])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[28])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[29])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[30])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[31])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[32])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[33])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[34])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[35])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[36])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[37])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[38])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[39])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[40])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[41])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[42])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[43])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[44])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[45])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[46])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[47])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[48])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[49])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[50])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[51])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[52])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[53])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[54])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[55])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[56])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[57])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[58])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[59])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[60])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[61])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[62])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[63])
			// + " "
			// + String.valueOf(data.frequencyParameterValues[64]);

			writer.writeOpenTag(FrequencyModelParser.FREQUENCY_MODEL, // tagname
					new Attribute[] { // attributes[]
							new Attribute.Default<String>(XMLParser.ID,
									"freqModel"), // id
							new Attribute.Default<String>("dataType", dataType
									.getDescription()) // dataType
					});

			writeParameter(FrequencyModelParser.FREQUENCIES, null,
					dataType.getStateCount(), frequencies, 0.0, 1.0, writer);

			writer.writeCloseTag(FrequencyModelParser.FREQUENCY_MODEL);

		}// END: switch

	}// END: writeFrequencyModel

	private void writeBranchRatesModel(PartitionData data, XMLWriter writer) {

		int clockModel = data.clockModelIndex;
		switch (clockModel) {

		case 0: // StrictClock

				writer.writeOpenTag(
						StrictClockBranchRatesParser.STRICT_CLOCK_BRANCH_RATES,
						new Attribute[] { new Attribute.Default<String>(
								XMLParser.ID, BranchRateModel.BRANCH_RATES) });

				writeParameter("rate", "clock.rate", 1,
						String.valueOf(data.clockParameterValues[0]), null,
						null, writer);

				writer.writeCloseTag(StrictClockBranchRatesParser.STRICT_CLOCK_BRANCH_RATES);

			break;

		}// END: switch

	}// END: writeBranchRatesModel

	private void writeTreeModel(TreeModel tree, XMLWriter writer) {

		final String treeModelName = TreeModel.TREE_MODEL;

		writer.writeTag(TreeModel.TREE_MODEL, new Attribute.Default<String>(
				XMLParser.ID, treeModelName), false);

		writer.writeIDref("tree", BeagleSequenceSimulatorApp.STARTING_TREE);

		// writer.writeOpenTag(TreeModelParser.ROOT_HEIGHT);
		// writer.writeTag(ParameterParser.PARAMETER,
		// new Attribute.Default<String>(XMLParser.ID, treeModelName + "."
		// + CoalescentSimulatorParser.ROOT_HEIGHT), true);
		// writer.writeCloseTag(TreeModelParser.ROOT_HEIGHT);

		writeParameter(TreeModelParser.ROOT_HEIGHT, treeModelName + "."
				+ CoalescentSimulatorParser.ROOT_HEIGHT, 1, null, null, null,
				writer);

		writer.writeOpenTag(TreeModelParser.NODE_HEIGHTS,
				new Attribute.Default<String>(TreeModelParser.INTERNAL_NODES,
						"true"));
		// writer.writeTag(ParameterParser.PARAMETER, new
		// Attribute.Default<String>(XMLParser.ID, treeModelName + "." +
		// "internalNodeHeights"), true);
		writeParameter(null, treeModelName + "."
				+ CoalescentSimulatorParser.ROOT_HEIGHT, 1, null, null, null,
				writer);
		writer.writeCloseTag(TreeModelParser.NODE_HEIGHTS);

		writer.writeOpenTag(TreeModelParser.NODE_HEIGHTS,
				new Attribute[] {
						new Attribute.Default<String>(
								TreeModelParser.INTERNAL_NODES, "true"),
						new Attribute.Default<String>(
								TreeModelParser.ROOT_NODE, "true") });
		// writer.writeTag(ParameterParser.PARAMETER, new
		// Attribute.Default<String>(XMLParser.ID, treeModelName + "." +
		// "allInternalNodeHeights"), true);
		writeParameter(null, treeModelName + "." + "allInternalNodeHeights", 1,
				null, null, null, writer);
		writer.writeCloseTag(TreeModelParser.NODE_HEIGHTS);

		writer.writeCloseTag(TreeModel.TREE_MODEL);

	}// END: writeTreeModel

	private void writeTaxa(TaxonList taxonList, XMLWriter writer) {

		writer.writeOpenTag(TaxaParser.TAXA, // tagname
				new Attribute[] { // attributes[]
				new Attribute.Default<String>(XMLParser.ID, TaxaParser.TAXA) });

		for (int i = 0; i < taxonList.getTaxonCount(); i++) {

			Taxon taxon = taxonList.getTaxon(i);

			writer.writeTag(
					TaxonParser.TAXON, // tagname
					new Attribute[] { // attributes[]
					new Attribute.Default<String>(XMLParser.ID, taxon.getId()) },
					true // close
			);

		}// END: i loop

		writer.writeCloseTag(TaxaParser.TAXA);
	}// END: writeTaxa

	private void writeStartingTree(TreeModel tree, XMLWriter writer) {

		writer.writeOpenTag(NewickParser.NEWICK,
				new Attribute[] { new Attribute.Default<String>(XMLParser.ID,
						BeagleSequenceSimulatorApp.STARTING_TREE),
				// new Attribute.Default<String>(DateParser.UNITS,
				// options.datesUnits.getAttribute()),
				// new Attribute.Default<Boolean>(SimpleTreeParser.USING_DATES,
				// options.clockModelOptions.isTipCalibrated())
				});
		writer.writeText(Tree.Utils.newick(tree));
		writer.writeCloseTag(NewickParser.NEWICK);

	}// END: writeStartingTree
	
	@SuppressWarnings("rawtypes")
	private void writeParameter(String wrapper, String id, int dimension,
			String value, Double lower, Double upper, XMLWriter writer) {

		if (wrapper != null) {
			writer.writeOpenTag(wrapper);
		}

		ArrayList<Attribute.Default> attributes = new ArrayList<Attribute.Default>();

		if (id != null) {
			attributes.add(new Attribute.Default<String>(XMLParser.ID, id));
		}

		if (dimension > 1) {
			attributes.add(new Attribute.Default<String>(
					ParameterParser.DIMENSION, String.valueOf(dimension)));
		}

		if (value != null) {
			attributes.add(new Attribute.Default<String>(ParameterParser.VALUE,
					value));
		}

		if (lower != null) {
			attributes.add(new Attribute.Default<String>(ParameterParser.LOWER,
					String.valueOf(lower)));
		}

		if (upper != null) {
			attributes.add(new Attribute.Default<String>(ParameterParser.UPPER,
					String.valueOf(upper)));
		}

		Attribute[] attrArray = new Attribute[attributes.size()];
		for (int i = 0; i < attrArray.length; i++) {
			attrArray[i] = attributes.get(i);
		}

		writer.writeTag(ParameterParser.PARAMETER, attrArray, true);

		if (wrapper != null) {
			writer.writeCloseTag(wrapper);
		}

	}// END: writeParameter

}// END: class

package dr.app.beauti.components.dollo;

import dr.app.beauti.generator.BaseComponentGenerator;
import dr.app.beauti.options.BeautiOptions;
import dr.app.beauti.options.PartitionSubstitutionModel;
import dr.app.beauti.util.XMLWriter;
import dr.util.Attribute;

/**
 * @author Marc Suchard
 * @version $Id$
 */

public class DolloComponentGenerator extends BaseComponentGenerator {

	private String logSuffix = ".dNdS.log";

	protected DolloComponentGenerator(BeautiOptions options) {
		super(options);
	}

	public boolean usesInsertionPoint(InsertionPoint point) {

		DolloComponentOptions component = (DolloComponentOptions) options
				.getComponentOptions(DolloComponentOptions.class);

		if (component.getPartitionList().size() == 0) {
			// Empty, so do nothing
			return false;
		}

		switch (point) {
		case IN_OPERATORS:
		case IN_FILE_LOG_PARAMETERS:
		case IN_TREES_LOG:
		case AFTER_TREES_LOG:
		case AFTER_MCMC:
			return true;
		default:
			return false;
		}

	}// END: usesInsertionPoint

	protected void generate(InsertionPoint point, Object item, XMLWriter writer) {

		DolloComponentOptions component = (DolloComponentOptions) options
				.getComponentOptions(DolloComponentOptions.class);

		switch (point) {
		case IN_OPERATORS:
			writeCodonPartitionedRobustCounting(writer, component);
			break;
		case IN_FILE_LOG_PARAMETERS:
			writeLogs(writer, component);
			break;
		case IN_TREES_LOG:
			writeTreeLogs(writer, component);
			break;
		case AFTER_TREES_LOG:
			writeDNdSLogger(writer, component);
			break;
		case AFTER_MCMC:
			writeDNdSPerSiteAnalysisReport(writer, component);
			break;
		default:
			throw new IllegalArgumentException(
					"This insertion point is not implemented for "
							+ this.getClass().getName());
		}

	}// END: generate

	protected String getCommentLabel() {
		return "Codon partitioned robust counting";
	}

	private void writeCodonPartitionedRobustCounting(XMLWriter writer,
			DolloComponentOptions component) {

		for (PartitionSubstitutionModel model : component.getPartitionList()) {
			writeCodonPartitionedRobustCounting(writer, model);
		}
	}

	// Called for each model that requires robust counting (can be more than
	// one)
	private void writeCodonPartitionedRobustCounting(XMLWriter writer,
			PartitionSubstitutionModel model) {

		System.err.println("DEBUG: Writing RB for " + model.getName());

		writer.writeComment("Robust counting for: " + model.getName());

		// S operator
		writer.writeOpenTag("codonPartitionedRobustCounting",
				new Attribute[] {
						new Attribute.Default<String>("id", "robustCounting1"),
						new Attribute.Default<String>("labeling", "S"),
						new Attribute.Default<String>("useUniformization",
								"true"),
						new Attribute.Default<String>("unconditionedPerBranch",
								"true") });

		writer.writeIDref("idref", "treeModel");
		writer.writeOpenTag("firstPosition");
		writer.writeIDref("ancestralTreeLikelihood", "CP1.treeLikelihood");
		writer.writeCloseTag("firstPosition");

		writer.writeOpenTag("secondPosition");
		writer.writeIDref("ancestralTreeLikelihood", "CP2.treeLikelihood");
		writer.writeCloseTag("secondPosition");

		writer.writeOpenTag("thirdPosition");
		writer.writeIDref("ancestralTreeLikelihood", "CP3.treeLikelihood");
		writer.writeCloseTag("thirdPosition");

		writer.writeCloseTag("codonPartitionedRobustCounting");

		writer.writeBlankLine();

		// N operator:
		writer.writeOpenTag("codonPartitionedRobustCounting",
				new Attribute[] {
						new Attribute.Default<String>("id", "robustCounting2"),
						new Attribute.Default<String>("labeling", "N"),
						new Attribute.Default<String>("useUniformization",
								"true"),
						new Attribute.Default<String>("unconditionedPerBranch",
								"true") });

		writer.writeIDref("idref", "treeModel");
		writer.writeOpenTag("firstPosition");
		writer.writeIDref("ancestralTreeLikelihood", "CP1.treeLikelihood");
		writer.writeCloseTag("firstPosition");

		writer.writeOpenTag("secondPosition");
		writer.writeIDref("ancestralTreeLikelihood", "CP2.treeLikelihood");
		writer.writeCloseTag("secondPosition");

		writer.writeOpenTag("thirdPosition");
		writer.writeIDref("ancestralTreeLikelihood", "CP3.treeLikelihood");
		writer.writeCloseTag("thirdPosition");

		writer.writeCloseTag("codonPartitionedRobustCounting");

	}// END: writeCodonPartitionedRobustCounting()

	private void writeLogs(XMLWriter writer, DolloComponentOptions component) {

		for (PartitionSubstitutionModel model : component.getPartitionList()) {
			writeLogs(writer, model);
		}
	}

	private void writeLogs(XMLWriter writer, PartitionSubstitutionModel model) {

		writer.writeComment("Robust counting for: " + model.getName());

		writer.writeIDref("codonPartitionedRobustCounting", "robustCounting1");
		writer.writeIDref("codonPartitionedRobustCounting", "robustCounting2");

	}// END: writeLogs

	private void writeTreeLogs(XMLWriter writer, DolloComponentOptions component) {

		for (PartitionSubstitutionModel model : component.getPartitionList()) {
			// We re-use the same method
			writeLogs(writer, model);
		}
	}

	private void writeDNdSLogger(XMLWriter writer,
			DolloComponentOptions component) {

		for (PartitionSubstitutionModel model : component.getPartitionList()) {
			writeDNdSLogger(writer, model);
		}
	}

	private void writeDNdSLogger(XMLWriter writer,
			PartitionSubstitutionModel model) {

		writer.writeComment("Robust counting for: " + model.getName());

		writer.writeOpenTag("log", new Attribute[] {
				new Attribute.Default<String>("id", "fileLog_dNdS"),
				new Attribute.Default<String>("logEvery", "10000"),
				new Attribute.Default<String>("fileName", model.getName()
						+ logSuffix) });

		writer
				.writeOpenTag("dNdSLogger",
						new Attribute[] { new Attribute.Default<String>("id",
								"dNdS") });
		writer.writeIDref("idref", "treeModel");
		writer.writeIDref("codonPartitionedRobustCounting", "robustCounting1");
		writer.writeIDref("codonPartitionedRobustCounting", "robustCounting2");
		writer.writeCloseTag("dNdSLogger");

		writer.writeCloseTag("log");

	}// END: writeLogs

	private void writeDNdSPerSiteAnalysisReport(XMLWriter writer,
			DolloComponentOptions component) {

		for (PartitionSubstitutionModel model : component.getPartitionList()) {
			writeDNdSPerSiteAnalysisReport(writer, model);
		}
	}

	private void writeDNdSPerSiteAnalysisReport(XMLWriter writer,
			PartitionSubstitutionModel model) {

		writer.writeComment("Robust counting for: " + model.getName());

		writer.writeOpenTag("report");

		writer.write("<dNdSPerSiteAnalysis fileName=" + '\"' + model.getName()
				+ logSuffix + '\"' + "/> \n");

		writer.writeCloseTag("report");

	}// END: writeDNdSReport

}// END: class
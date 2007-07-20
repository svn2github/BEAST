package dr.evomodel.tree;

import dr.evoxml.GraphMLUtils;
import dr.inference.loggers.LogFormatter;
import dr.inference.loggers.MCLogger;
import dr.inference.loggers.MLLogger;
import dr.inference.loggers.TabDelimitedFormatter;
import dr.xml.*;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

/**
 * Created by IntelliJ IDEA.
 * User: msuchard
 * Date: Jan 13, 2007
 * Time: 2:16:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class ArgLogger extends MCLogger {

	public static final String LOG_ARG = "logArg";
	public static final String DOT_FORMAT = "dotFormat";
	public static final String COMPRESSED_STRING = "compressedString";

	private ARGModel argModel;
	private boolean dotFormat;
	private boolean newickFormat;

	public ArgLogger(ARGModel argModel, LogFormatter formatter, int logEvery, boolean dotFormat, boolean newickFormat) {
		super(formatter, logEvery);
		this.argModel = argModel;
		this.dotFormat = dotFormat;
		this.newickFormat = newickFormat;
	}


	public static final String GRAPHML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">";
	public static final String GRAPHML_FOOTER = "</graphml>";

	public void startLogging() {
		if (!dotFormat && !newickFormat)
			logLine(GRAPHML_HEADER);
		if (newickFormat)
			logLine("state ARG.string");
	}

	public void stopLogging() {
		if (!dotFormat && !newickFormat)
			logLine(GRAPHML_FOOTER);
	}

	static XMLOutputter outputter = new XMLOutputter(org.jdom.output.Format.getPrettyFormat());

	public void log(int state) {
		if (logEvery <= 0 || ((state % logEvery) == 0)) {
			Element graphElement = argModel.toXML();
			graphElement.setAttribute(ARGModel.ID_ATTRIBUTE, "STATE_" + state);
			if (dotFormat)
				logLine(GraphMLUtils.dotFormat(graphElement));
			else if (newickFormat)
				logLine("ARG STATE_" + state + " = " + argModel.toGraphStringCompressed(false));
			else
				logLine(outputter.outputString(graphElement));
		}
	}


	public static XMLObjectParser PARSER = new AbstractXMLObjectParser() {

		public String getParserName() {
			return LOG_ARG;
		}

		/**
		 * @return an object based on the XML element it was passed.
		 */
		public Object parseXMLObject(XMLObject xo) throws XMLParseException {

			ARGModel argModel = (ARGModel) xo.getChild(ARGModel.class);

			String fileName = null;
			String title = null;
			boolean dotFormat = false;
			boolean newickFormat = false;
//			boolean nexusFormat = false;

//			String colouringLabel = "demes";
//			String rateLabel = "rate";
//			String likelihoodLabel = "lnP";

			if (xo.hasAttribute(TITLE)) {
				title = xo.getStringAttribute(TITLE);
			}

			if (xo.hasAttribute(FILE_NAME)) {
				fileName = xo.getStringAttribute(FILE_NAME);
			}

			if (xo.hasAttribute(DOT_FORMAT)) {
				dotFormat = xo.getBooleanAttribute(DOT_FORMAT);
			}

			if (xo.hasAttribute(COMPRESSED_STRING)) {
				newickFormat = xo.getBooleanAttribute(COMPRESSED_STRING);
			}

			if (dotFormat && newickFormat) {
				throw new XMLParseException("An ARG logger may only return one graphic representation.");
			}

//			boolean substitutions = false;
//			if (xo.hasAttribute(BRANCH_LENGTHS)) {
//				substitutions = xo.getStringAttribute(BRANCH_LENGTHS).equals(SUBSTITUTIONS);
//			}

//			BranchRateModel branchRateModel = (BranchRateModel) xo.getChild(BranchRateModel.class);

//			ColourSamplerModel colourSamplerModel = (ColourSamplerModel) xo.getChild(ColourSamplerModel.class);

//			Likelihood likelihood = (Likelihood) xo.getChild(Likelihood.class);

			// logEvery of zero only displays at the end
			int logEvery = 1;

			if (xo.hasAttribute(LOG_EVERY)) {
				logEvery = xo.getIntegerAttribute(LOG_EVERY);
			}

			PrintWriter pw;

			if (fileName != null) {

				try {
					File file = new File(fileName);
					String name = file.getName();
					String parent = file.getParent();

					if (!file.isAbsolute()) {
						parent = System.getProperty("user.dir");
					}

//					System.out.println("Writing log file to "+parent+System.getProperty("path.separator")+name);
					pw = new PrintWriter(new FileOutputStream(new File(parent, name)));
				} catch (FileNotFoundException fnfe) {
					throw new XMLParseException("File '" + fileName + "' can not be opened for " + getParserName() + " element.");
				}
			} else {
				pw = new PrintWriter(System.out);
			}

			LogFormatter formatter = new TabDelimitedFormatter(pw);

			ArgLogger logger = new ArgLogger(argModel, formatter, logEvery, dotFormat, newickFormat);

//			TreeLogger logger = new TreeLogger(tree, branchRateModel, rateLabel,
//					colourSamplerModel, colouringLabel, likelihood, likelihoodLabel,
//					formatter, logEvery, nexusFormat, substitutions);

			if (title != null) {
				logger.setTitle(title);
			}

			return logger;
		}

		//************************************************************************
		// AbstractXMLObjectParser implementation
		//************************************************************************
		public XMLSyntaxRule[] getSyntaxRules() {
			return rules;
		}

		private XMLSyntaxRule[] rules = new XMLSyntaxRule[]{
				AttributeRule.newIntegerRule(LOG_EVERY),
				new StringAttributeRule(FILE_NAME,
						"The name of the file to send log output to. " +
								"If no file name is specified then log is sent to standard output", true),
				new StringAttributeRule(TITLE, "The title of the log", true),
//				AttributeRule.newBooleanRule(NEXUS_FORMAT, true,
//						"Whether to use the NEXUS format for the tree log"),
//				new StringAttributeRule(BRANCH_LENGTHS, "What units should the branch lengths be in", new String[]{TIME, SUBSTITUTIONS}, true),
				new ElementRule(ARGModel.class, "The ARG which is to be logged"),
//				new ElementRule(BranchRateModel.class, true),
//				new ElementRule(ColourSamplerModel.class, true),
//				new ElementRule(Likelihood.class, true)
		};

		public String getParserDescription() {
			return "Logs an ARG to a file";
		}

		public String getExample() {
			return
					"<!-- The " + getParserName() + " element takes an argModel to be logged -->\n" +
							"<" + getParserName() + " " + LOG_EVERY + "=\"100\" " + FILE_NAME + "=\"log.args\">\n" +
							"	<argModel idref=\"treeModel1\"/>\n" +
							"</" + getParserName() + ">\n";
		}

		public Class getReturnType() {
			return MLLogger.class;
		}
	};


}

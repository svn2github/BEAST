package dr.app.beagle.evomodel.branchmodel.lineagespecific;

import dr.inference.model.CompoundParameter;
import dr.xml.AbstractXMLObjectParser;
import dr.xml.ElementRule;
import dr.xml.XMLObject;
import dr.xml.XMLParseException;
import dr.xml.XMLSyntaxRule;

public class BranchSpecificTraitParser extends AbstractXMLObjectParser {

	public static final String BRANCH_SPECIFIC_TRAIT = "branchSpecificTrait";

	@Override
	public String getParserName() {
		return BRANCH_SPECIFIC_TRAIT;
	}

	@Override
	public Object parseXMLObject(XMLObject xo) throws XMLParseException {

		BranchSpecific branchSpecific = (BranchSpecific) xo.getChild(BranchSpecific.class);
		CompoundParameter parameter = (CompoundParameter) xo.getChild(CompoundParameter.class);

		return new BranchSpecificTrait(branchSpecific, parameter);
	}

	@Override
	public XMLSyntaxRule[] getSyntaxRules() {
		return new XMLSyntaxRule[] {

		new ElementRule(BranchSpecific.class, false), //
				new ElementRule(CompoundParameter.class, false), //

		};
	}

	@Override
	public String getParserDescription() {
		return BRANCH_SPECIFIC_TRAIT;
	}

	@Override
	public Class getReturnType() {
		return BranchSpecificTrait.class;
	}

}// END: class

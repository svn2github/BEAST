package dr.app.beagle.evomodel.parsers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dr.app.beagle.evomodel.sitemodel.EpochBranchSubstitutionModel;
import dr.app.beagle.evomodel.substmodel.FrequencyModel;
import dr.app.beagle.evomodel.substmodel.SubstitutionModel;
import dr.evolution.datatype.DataType;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.substmodel.AbstractSubstitutionModel;
import dr.inference.model.Parameter;
import dr.xml.AbstractXMLObjectParser;
import dr.xml.ElementRule;
import dr.xml.XMLObject;
import dr.xml.XMLParseException;
import dr.xml.XMLSyntaxRule;

public class BeagleSubstitutionEpochModelParser extends AbstractXMLObjectParser {

	public static final String SUBSTITUTION_EPOCH_MODEL = "beagleSubstitutionEpochModel";
	public static final String MODELS = "models";
	public static final String TRANSITION_TIMES = "transitionTimes";

	@Override
	public Object parseXMLObject(XMLObject xo) throws XMLParseException {

		DataType dataType = null;
		List<FrequencyModel> frequencyModelList = new ArrayList<FrequencyModel>();
		List<SubstitutionModel> substModelList = new ArrayList<SubstitutionModel>();
		XMLObject cxo = xo.getChild(MODELS);

		for (int i = 0; i < cxo.getChildCount(); i++) {

			SubstitutionModel substModel = (SubstitutionModel) cxo.getChild(i);

			if (dataType == null) {

				dataType = substModel.getDataType();

			} else if (dataType != substModel.getDataType()) {

				throw new XMLParseException(
						"Substitution models across epoches must use the same data type.");

			}//END: dataType check

			if (frequencyModelList.size() == 0) {

				frequencyModelList.add(substModel.getFrequencyModel());

			} else if (frequencyModelList.get(0) != substModel.getFrequencyModel()) {

				throw new XMLParseException(
						"Substitution models across epoches must currently use the same frequency model.\n Harass Marc to fix this.");

			}//END: freqModels no check

			substModelList.add(substModel);
		}//END: i loop

		BranchRateModel branchRateModel = (BranchRateModel)xo.getChild(BranchRateModel.class);
		
		Parameter epochTransitionTimes = (Parameter) xo
				.getChild(Parameter.class);

		if (epochTransitionTimes.getDimension() != substModelList.size() - 1) {
			throw new XMLParseException(
					"# of transition times must equal # of substitution models - 1\n"
							+ epochTransitionTimes.getDimension() + "\n"
							+ substModelList.size());
		}

	    // quietly sort in increasing order
		double sortedEpochTransitionTimes[] = epochTransitionTimes.getAttributeValue();
		Arrays.sort(sortedEpochTransitionTimes);
		for(int i = 0; i < epochTransitionTimes.getDimension(); i ++) {
			epochTransitionTimes.setParameterValueQuietly(i, sortedEpochTransitionTimes[i]);
		}//END: i loop

		return new EpochBranchSubstitutionModel(substModelList, 
				frequencyModelList, 
				branchRateModel,
				epochTransitionTimes);
	}// END: parseXMLObject

	@Override
	public XMLSyntaxRule[] getSyntaxRules() {

//        return new XMLSyntaxRule[]{
//                new ElementRule(MODELS,
//                        new XMLSyntaxRule[]{
//                                new ElementRule(AbstractSubstitutionModel.class, 1, Integer.MAX_VALUE),
//                        }
//                ),
//                new ElementRule(Parameter.class),
//        };

		return null;
	}

	@Override
	public String getParserDescription() {
		return null;
	}

	@Override
	public Class<EpochBranchSubstitutionModel> getReturnType() {
		return EpochBranchSubstitutionModel.class;
	}

	public String getParserName() {
		return SUBSTITUTION_EPOCH_MODEL;
	}

}// END: class

/*
 * OptimizedBeagleTreeLikelihoodParser.java
 *
 * Copyright (c) 2002-2013 Alexei Drummond, Andrew Rambaut and Marc Suchard
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
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package dr.app.beagle.evomodel.parsers;

import dr.app.beagle.evomodel.branchmodel.BranchModel;
import dr.app.beagle.evomodel.branchmodel.HomogeneousBranchModel;
import dr.app.beagle.evomodel.sitemodel.GammaSiteRateModel;
import dr.app.beagle.evomodel.substmodel.FrequencyModel;
import dr.app.beagle.evomodel.substmodel.SubstitutionModel;
import dr.app.beagle.evomodel.treelikelihood.AbstractTreeLikelihood;
import dr.app.beagle.evomodel.treelikelihood.BeagleTreeLikelihood;
import dr.app.beagle.evomodel.treelikelihood.PartialsRescalingScheme;
import dr.evolution.alignment.PatternList;
import dr.evolution.alignment.Patterns;
import dr.evolution.alignment.SitePatterns;
import dr.evolution.tree.Tree;
import dr.evolution.util.TaxonList;
import dr.evomodel.branchratemodel.BranchRateModel;
import dr.evomodel.tree.TreeModel;
import dr.evomodel.treelikelihood.TipStatesModel;
import dr.inference.model.CompoundLikelihood;
import dr.inference.model.Likelihood;
import dr.inference.model.Parameter;
import dr.inference.model.ThreadedCompoundLikelihood;
import dr.xml.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Guy Baele
 */
public class OptimizedBeagleTreeLikelihoodParser extends AbstractXMLObjectParser {

    public static final String OPTIMIZED_BEAGLE_TREE_LIKELIHOOD = "optimizedBeagleTreeLikelihood";
    
    public final int TEST_RUNS = 100;
    //public final boolean SPLIT_BY_PATTERN_COUNT = false;

    public String getParserName() {
        return OPTIMIZED_BEAGLE_TREE_LIKELIHOOD;
    }

    protected BeagleTreeLikelihood createTreeLikelihood(PatternList patternList, TreeModel treeModel,
                                                        BranchModel branchModel,
                                                        GammaSiteRateModel siteRateModel,
                                                        BranchRateModel branchRateModel,
                                                        TipStatesModel tipStatesModel,
                                                        boolean useAmbiguities, PartialsRescalingScheme scalingScheme,
                                                        Map<Set<String>, Parameter> partialsRestrictions,
                                                        XMLObject xo) throws XMLParseException {
        return new BeagleTreeLikelihood(
                patternList,
                treeModel,
                branchModel,
                siteRateModel,
                branchRateModel,
                tipStatesModel,
                useAmbiguities,
                scalingScheme,
                partialsRestrictions
        );
    }

    public Object parseXMLObject(XMLObject xo) throws XMLParseException {
    	
    	int childCount = xo.getChildCount();
    	List<Likelihood> likelihoods = new ArrayList<Likelihood>();
    	
    	for (int i = 0; i < childCount; i++) {
    		likelihoods.add((Likelihood)xo.getChild(i));
    	}
    	
    	System.err.println("-----");
    	System.err.println(childCount + " BeagleTreeLikelihoods added.");
    	
    	int[] instanceCounts = new int[childCount];
    	for (int i = 0; i < childCount; i++) {
    		instanceCounts[i] = 1;
    	}
    	int[] currentLocation = new int[childCount];
    	for (int i = 0; i < childCount; i++) {
    		currentLocation[i] = i;
    	}
    	
    	int[] siteCounts = new int[childCount];
    	//store everything for later use
    	SitePatterns[] patterns = new SitePatterns[childCount];
    	TreeModel[] treeModels = new TreeModel[childCount];
    	BranchModel[] branchModels = new BranchModel[childCount];
    	GammaSiteRateModel[] siteRateModels = new GammaSiteRateModel[childCount];
    	BranchRateModel[] branchRateModels = new BranchRateModel[childCount];
    	boolean[] ambiguities = new boolean[childCount];
    	PartialsRescalingScheme[] rescalingSchemes = new PartialsRescalingScheme[childCount];
    	List<Map<Set<String>, Parameter>> partialsRestrictions = new ArrayList<Map<Set<String>, Parameter>>();
    	for (int i = 0; i < likelihoods.size(); i++) {
    		patterns[i] = (SitePatterns)((BeagleTreeLikelihood)likelihoods.get(i)).getPatternsList();
    		siteCounts[i] = patterns[i].getPatternCount();
    		treeModels[i] = ((BeagleTreeLikelihood)likelihoods.get(i)).getTreeModel();
    		branchModels[i] = ((BeagleTreeLikelihood)likelihoods.get(i)).getBranchModel();
    		siteRateModels[i] = (GammaSiteRateModel) ((BeagleTreeLikelihood)likelihoods.get(i)).getSiteRateModel();
    		branchRateModels[i] = ((BeagleTreeLikelihood)likelihoods.get(i)).getBranchRateModel();
    		ambiguities[i] = ((BeagleTreeLikelihood)likelihoods.get(i)).useAmbiguities();
    		rescalingSchemes[i] = ((BeagleTreeLikelihood)likelihoods.get(i)).getRescalingScheme();
    		partialsRestrictions.add(i, ((BeagleTreeLikelihood)likelihoods.get(i)).getPartialsRestrictions());
    	}
    	System.err.println("Pattern counts: ");
    	for (int i = 0;i < siteCounts.length; i++) {
    		System.err.print(siteCounts[i] + " ");
    	}
    	System.err.println();
    	System.err.println("Instance counts: ");
    	for (int i = 0;i < instanceCounts.length; i++) {
    		System.err.print(instanceCounts[i] + " ");
    	}
    	System.err.println();
    	System.err.println("Current locations: ");
		for (int i = 0;i < currentLocation.length; i++) {
			System.err.print(currentLocation[i] + " ");
		}
		System.err.println();
    	
		//ThreadedCompoundLikelihood compound = new ThreadedCompoundLikelihood(likelihoods);
		CompoundLikelihood compound = new CompoundLikelihood(likelihoods);
    	
    	double start = System.nanoTime();
    	for (int i = 0; i < TEST_RUNS; i++) {
        	compound.makeDirty();
        	compound.getLogLikelihood();
        }
    	double end = System.nanoTime();
        double baseResult = end - start;
        System.err.println("Starting evaluation took: " + baseResult);
        
        int longestIndex = 0;
        int longestSize = siteCounts[0];

        
        
        //START TEST CODE
        /*System.err.println("Detailed evaluation times: ");
        long[] evaluationTimes = compound.getEvaluationTimes();
        int[] evaluationCounts = compound.getEvaluationCounts();
        long longest = evaluationTimes[0];
        for (int i = 0; i < evaluationTimes.length; i++) {
        	System.err.println(i + ": time=" + evaluationTimes[i] + "   count=" + evaluationCounts[i]);
            if (evaluationTimes[i] > longest) {
            	longest = evaluationTimes[i];
        	}
        }*/
        //END TEST CODE
        
        
        
        /*if (SPLIT_BY_PATTERN_COUNT) {

        	boolean notFinished = true;

        	while (notFinished) {

        		for (int i = 0; i < siteCounts.length; i++) {
        			if (siteCounts[i] > longestSize) {
        				longestIndex = i;
        				longestSize = siteCounts[longestIndex];
        			}
        		}
        		System.err.println("Split likelihood " + longestIndex + " with pattern count " + longestSize);

        		//split it in 2
        		int instanceCount = ++instanceCounts[longestIndex];

        		List<Likelihood> newList = new ArrayList<Likelihood>();
        		for (int i = 0; i < instanceCount; i++) {
        			Patterns subPatterns = new Patterns(patterns[longestIndex], 0, 0, 1, i, instanceCount);

        			BeagleTreeLikelihood treeLikelihood = createTreeLikelihood(
        					subPatterns, treeModels[longestIndex], branchModels[longestIndex], siteRateModels[longestIndex], branchRateModels[longestIndex],
        					null, 
        					ambiguities[longestIndex], rescalingSchemes[longestIndex], partialsRestrictions.get(longestIndex),
        					xo);

        			treeLikelihood.setId(xo.getId() + "_" + instanceCount);
        			newList.add(treeLikelihood);
        		}
        		for (int i = 0; i < newList.size()-1; i++) {
        			likelihoods.remove(currentLocation[longestIndex]);
        		}
        		//likelihoods.remove(longestIndex);
        		//likelihoods.add(longestIndex, new CompoundLikelihood(newList));
        		for (int i = 0; i < newList.size(); i++) {
        			likelihoods.add(currentLocation[longestIndex], newList.get(i));
        		}
        		for (int i = longestIndex+1; i < currentLocation.length; i++) {
        			currentLocation[i]++;
        		}
        		//compound = new ThreadedCompoundLikelihood(likelihoods);
        		compound = new CompoundLikelihood(likelihoods);
        		siteCounts[longestIndex] = (instanceCount-1)*siteCounts[longestIndex]/instanceCount;
        		longestSize = (instanceCount-1)*longestSize/instanceCount;

        		//check number of likelihoods
        		System.err.println("Number of BeagleTreeLikelihoods: " + compound.getLikelihoodCount());
        		System.err.println("Pattern counts: ");
        		for (int i = 0;i < siteCounts.length; i++) {
        			System.err.print(siteCounts[i] + " ");
        		}
        		System.err.println();
        		System.err.println("Instance counts: ");
        		for (int i = 0;i < instanceCounts.length; i++) {
        			System.err.print(instanceCounts[i] + " ");
        		}
        		System.err.println();
        		System.err.println("Current locations: ");
        		for (int i = 0;i < currentLocation.length; i++) {
        			System.err.print(currentLocation[i] + " ");
        		}
        		System.err.println();

        		//evaluate speed
        		start = System.nanoTime();
        		for (int i = 0; i < TEST_RUNS; i++) {
        			compound.makeDirty();
        			compound.getLogLikelihood();
        		}
        		end = System.nanoTime();
        		double newResult = end - start;
        		System.err.println("New evaluation took: " + newResult + " vs. old evaluation: " + baseResult);

        		if (newResult < baseResult) {
            		baseResult = newResult;
            	} else {
            		notFinished = false;
            		
            		//remove 1 instanceCount
            		System.err.print("Removing 1 instance count: " + instanceCount);
            		instanceCount = --instanceCounts[longestIndex];
            		System.err.println(" -> " + instanceCount + " for likelihood " + longestIndex);
            		newList = new ArrayList<Likelihood>();
                	for (int i = 0; i < instanceCount; i++) {
                		Patterns subPatterns = new Patterns(patterns[longestIndex], 0, 0, 1, i, instanceCount);

                		BeagleTreeLikelihood treeLikelihood = createTreeLikelihood(
                                subPatterns, treeModels[longestIndex], branchModels[longestIndex], siteRateModels[longestIndex], branchRateModels[longestIndex],
                                null, 
                                ambiguities[longestIndex], rescalingSchemes[longestIndex], partialsRestrictions.get(longestIndex),
                                xo);
                		
                        treeLikelihood.setId(xo.getId() + "_" + instanceCount);
                        newList.add(treeLikelihood);
                	}
                	for (int i = 0; i < newList.size()+1; i++) {
            			likelihoods.remove(currentLocation[longestIndex]);
            		}
                	for (int i = 0; i < newList.size(); i++) {
                		likelihoods.add(currentLocation[longestIndex], newList.get(i));
                	}
                	for (int i = longestIndex+1; i < currentLocation.length; i++) {
            			currentLocation[i]--;
            		}
                	//likelihoods.remove(longestIndex);
                	//likelihoods.add(longestIndex, new CompoundLikelihood(newList));
                	
                	//compound = new ThreadedCompoundLikelihood(likelihoods);
                	compound = new CompoundLikelihood(likelihoods);
                	siteCounts[longestIndex] = (instanceCount+1)*siteCounts[longestIndex]/instanceCount;
                	longestSize = (instanceCount+1)*longestSize/instanceCount;
                	
                	System.err.println("Pattern counts: ");
                	for (int i = 0;i < siteCounts.length; i++) {
                		System.err.print(siteCounts[i] + " ");
                	}
                	System.err.println();
                	System.err.println("Instance counts: ");
                	for (int i = 0;i < instanceCounts.length; i++) {
                		System.err.print(instanceCounts[i] + " ");
                	}
                	System.err.println();
                	System.err.println("Current locations: ");
            		for (int i = 0;i < currentLocation.length; i++) {
            			System.err.print(currentLocation[i] + " ");
            		}
            		System.err.println();
                	
            	}
        		
        	}

        } else {*/
        	
        //TRY_ALL_LIKELIHOODS, starting with the highest pattern count
        //Try splitting the same likelihood until no further improvement, then move on towards the next one
        boolean notFinished = true;

        //construct list with likelihoods to split up
        List<Integer> splitList = new ArrayList<Integer>();
        for (int i = 0; i < siteCounts.length; i++) {
        	int top = 0;
        	for (int j = 0; j < siteCounts.length; j++) {
        		if (siteCounts[j] > siteCounts[top]) {
        			top = j;
        		}
        	}
        	siteCounts[top] = 0;
        	splitList.add(top);
        }
        for (int i = 0; i < likelihoods.size(); i++) {
        	siteCounts[i] = patterns[i].getPatternCount();
        }
        //print list
        System.err.print("Ordered list of likelihoods to be evaluated: ");
        for (int i = 0; i < splitList.size(); i++) {
        	System.err.print(splitList.get(i) + " ");
        }
        System.err.println();

        while (notFinished) {

        	//split it in 2
        	longestIndex = splitList.get(0);
        	int instanceCount = ++instanceCounts[longestIndex];

        	List<Likelihood> newList = new ArrayList<Likelihood>();
        	for (int i = 0; i < instanceCount; i++) {
        		Patterns subPatterns = new Patterns(patterns[longestIndex], 0, 0, 1, i, instanceCount);

        		BeagleTreeLikelihood treeLikelihood = createTreeLikelihood(
        				subPatterns, treeModels[longestIndex], branchModels[longestIndex], siteRateModels[longestIndex], branchRateModels[longestIndex],
        				null, 
        				ambiguities[longestIndex], rescalingSchemes[longestIndex], partialsRestrictions.get(longestIndex),
        				xo);

        		treeLikelihood.setId(xo.getId() + "_" + instanceCount);
        		newList.add(treeLikelihood);
        	}
        	for (int i = 0; i < newList.size()-1; i++) {
        		likelihoods.remove(currentLocation[longestIndex]);
        	}
        	//likelihoods.remove(longestIndex);
        	//likelihoods.add(longestIndex, new CompoundLikelihood(newList));
        	for (int i = 0; i < newList.size(); i++) {
        		likelihoods.add(currentLocation[longestIndex], newList.get(i));
        	}
        	for (int i = longestIndex+1; i < currentLocation.length; i++) {
        		currentLocation[i]++;
        	}
        	//compound = new ThreadedCompoundLikelihood(likelihoods);
        	compound = new CompoundLikelihood(likelihoods);
        	siteCounts[longestIndex] = (instanceCount-1)*siteCounts[longestIndex]/instanceCount;
        	longestSize = (instanceCount-1)*longestSize/instanceCount;

        	//check number of likelihoods
        	System.err.println("Number of BeagleTreeLikelihoods: " + compound.getLikelihoodCount());
        	System.err.println("Pattern counts: ");
        	for (int i = 0;i < siteCounts.length; i++) {
        		System.err.print(siteCounts[i] + " ");
        	}
        	System.err.println();
        	System.err.println("Instance counts: ");
        	for (int i = 0;i < instanceCounts.length; i++) {
        		System.err.print(instanceCounts[i] + " ");
        	}
        	System.err.println();
        	System.err.println("Current locations: ");
        	for (int i = 0;i < currentLocation.length; i++) {
        		System.err.print(currentLocation[i] + " ");
        	}
        	System.err.println();

        	//evaluate speed
        	start = System.nanoTime();
        	for (int i = 0; i < TEST_RUNS; i++) {
        		compound.makeDirty();
        		compound.getLogLikelihood();
        	}
        	end = System.nanoTime();
        	double newResult = end - start;
        	System.err.println("New evaluation took: " + newResult + " vs. old evaluation: " + baseResult);



        	//START TEST CODE
        	/*evaluationTimes = compound.getEvaluationTimes();
                evaluationCounts = compound.getEvaluationCounts();
                longest = evaluationTimes[0];
                for (int i = 0; i < evaluationTimes.length; i++) {
                	System.err.println(i + ": time=" + evaluationTimes[i] + "   count=" + evaluationCounts[i]);
                    if (evaluationTimes[i] > longest) {
                    	longest = evaluationTimes[i];
                	}
                }*/
        	//END TEST CODE


        	//System.exit(0);

        	if (newResult < baseResult) {
        		baseResult = newResult;
        	} else {
        		splitList.remove(0);
        		if (splitList.size() == 0) {
        			notFinished = false;
        		}

        		//remove 1 instanceCount
        		System.err.print("Removing 1 instance count: " + instanceCount);
        		instanceCount = --instanceCounts[longestIndex];
        		System.err.println(" -> " + instanceCount + " for likelihood " + longestIndex);
        		newList = new ArrayList<Likelihood>();
        		for (int i = 0; i < instanceCount; i++) {
        			Patterns subPatterns = new Patterns(patterns[longestIndex], 0, 0, 1, i, instanceCount);

        			BeagleTreeLikelihood treeLikelihood = createTreeLikelihood(
        					subPatterns, treeModels[longestIndex], branchModels[longestIndex], siteRateModels[longestIndex], branchRateModels[longestIndex],
        					null, 
        					ambiguities[longestIndex], rescalingSchemes[longestIndex], partialsRestrictions.get(longestIndex),
        					xo);

        			treeLikelihood.setId(xo.getId() + "_" + instanceCount);
        			newList.add(treeLikelihood);
        		}
        		for (int i = 0; i < newList.size()+1; i++) {
        			likelihoods.remove(currentLocation[longestIndex]);
        		}
        		for (int i = 0; i < newList.size(); i++) {
        			likelihoods.add(currentLocation[longestIndex], newList.get(i));
        		}
        		for (int i = longestIndex+1; i < currentLocation.length; i++) {
        			currentLocation[i]--;
        		}
        		//likelihoods.remove(longestIndex);
        		//likelihoods.add(longestIndex, new CompoundLikelihood(newList));

        		//compound = new ThreadedCompoundLikelihood(likelihoods);
        		compound = new CompoundLikelihood(likelihoods);
        		siteCounts[longestIndex] = (instanceCount+1)*siteCounts[longestIndex]/instanceCount;
        		longestSize = (instanceCount+1)*longestSize/instanceCount;

        		System.err.println("Pattern counts: ");
        		for (int i = 0;i < siteCounts.length; i++) {
        			System.err.print(siteCounts[i] + " ");
        		}
        		System.err.println();
        		System.err.println("Instance counts: ");
        		for (int i = 0;i < instanceCounts.length; i++) {
        			System.err.print(instanceCounts[i] + " ");
        		}
        		System.err.println();
        		System.err.println("Current locations: ");
        		for (int i = 0;i < currentLocation.length; i++) {
        			System.err.print(currentLocation[i] + " ");
        		}
        		System.err.println();

        	}

        }

        //}

        
        
        
    	
    	/*CompoundLikelihood compound = new CompoundLikelihood(likelihoods);
    	
    	double start = System.nanoTime();
    	for (int i = 0; i < TEST_RUNS; i++) {
        	compound.makeDirty();
        	compound.getLogLikelihood();
        }
    	double end = System.nanoTime();
        double baseResult = end - start;
        System.err.println("Starting evaluation took: " + baseResult);
        
        System.err.println("Detailed evaluation times: ");
        long[] evaluationTimes = compound.getEvaluationTimes();
        int[] evaluationCounts = compound.getEvaluationCounts();
        long longest = evaluationTimes[0];
        int longestIndex = 0;
        ArrayList<Integer> list = null;
        
        if (TRY_ALL_LIKELIHOODS) {
        	
        	for (int i = 0; i < evaluationTimes.length; i++) {
            	System.err.println(i + ": time=" + evaluationTimes[i] + "   count=" + evaluationCounts[i]);
            	if (evaluationTimes[i] > longest) {
            		longest = evaluationTimes[i];
            		longestIndex = i;
            	}
            }
            System.err.println("Longest likelihood calculation: " + longestIndex + " took " + longest);
			
        	long[] evalCopy = new long[evaluationTimes.length];
        	System.arraycopy(evaluationTimes, 0, evalCopy, 0, evalCopy.length);
        	list = new ArrayList<Integer>();
        	for (int i = 0; i < evalCopy.length; i++) {
        		int top = 0;
        		for (int j = 0; j < evalCopy.length; j++) {
        			if (evalCopy[j] > evalCopy[top]) {
        				top = j;
        			}
        		}
        		evalCopy[top] = 0;
        		list.add(top);
        	}
        	//print list
        	System.err.print("Ordered list of evaluation times: ");
        	for (int i = 0; i < list.size(); i++) {
        		System.err.print(list.get(i) + " ");
        	}
        	System.err.println();
        	
        }
        
        boolean notFinished = true;
    
        	while (notFinished) {
            	
        		if (TRY_ALL_LIKELIHOODS) {
        			
        			//pick next one from the list
        			longestIndex = list.get(0);
        			
        		} else {
        			
        			longest = evaluationTimes[0];
        			for (int i = 0; i < evaluationTimes.length; i++) {
                    	System.err.println(i + ": time=" + evaluationTimes[i] + "   count=" + evaluationCounts[i]);
                    	if (evaluationTimes[i] > longest) {
                    		longest = evaluationTimes[i];
                    		longestIndex = i;
                    	}
                    }
                    System.err.println("Longest likelihood calculation: " + longestIndex + " took " + longest);
        			
        		}
        		
            	System.err.println("Split up likelihood " + longestIndex);
            	
            	//split it in 2
            	int instanceCount = ++instanceCounts[longestIndex];
            	
            	List<Likelihood> newList = new ArrayList<Likelihood>();
            	for (int i = 0; i < instanceCount; i++) {
            		Patterns subPatterns = new Patterns(patterns[longestIndex], 0, 0, 1, i, instanceCount);

            		BeagleTreeLikelihood treeLikelihood = createTreeLikelihood(
                            subPatterns, treeModels[longestIndex], branchModels[longestIndex], siteRateModels[longestIndex], branchRateModels[longestIndex],
                            null, 
                            ambiguities[longestIndex], rescalingSchemes[longestIndex], partialsRestrictions.get(longestIndex),
                            xo);
            		
                    treeLikelihood.setId(xo.getId() + "_" + instanceCount);
                    newList.add(treeLikelihood);
            	}
            	likelihoods.remove(longestIndex);
            	likelihoods.add(longestIndex, new CompoundLikelihood(instanceCount, newList));
            	//likelihoods.add(longestIndex, new CompoundLikelihood(newList));
            	//compound = new CompoundLikelihood(totalInstances,likelihoods);
            	compound = new CompoundLikelihood(likelihoods);
            	siteCounts[longestIndex] /= 2;
            	
            	//check number of likelihoods
            	System.err.println("Number of BeagleTreeLikelihoods: " + likelihoods.size());
            	System.err.println("Pattern counts: ");
            	for (int i = 0;i < siteCounts.length; i++) {
            		System.err.print(siteCounts[i] + " ");
            	}
            	System.err.println();
            	System.err.println("Instance counts: ");
            	for (int i = 0;i < instanceCounts.length; i++) {
            		System.err.print(instanceCounts[i] + " ");
            	}
            	System.err.println();
            	
            	//evaluate speed
            	start = System.nanoTime();
            	for (int i = 0; i < TEST_RUNS; i++) {
                	compound.makeDirty();
                	//this does not help
                	//compound.resetEvaluationTimes();
                	compound.getLogLikelihood();
                }
            	end = System.nanoTime();
                double newResult = end - start;
                System.err.println("New evaluation took: " + newResult + " vs. old evaluation: " + baseResult);
                
                System.err.println("Detailed evaluation times: ");
                evaluationTimes = compound.getEvaluationTimes();
                evaluationCounts = compound.getEvaluationCounts();
                //when there are multiple instances, get the maximum of each individual thread?
                //work on this later, when a machine is available
                //longest = evaluationTimes[0];
                //longestIndex = 0;
                for (int i = 0; i < evaluationTimes.length; i++) {
                	System.err.println(i + ": time=" + evaluationTimes[i] + "   count=" + evaluationCounts[i]);
                	//if (evaluationTimes[i] > longest) {
                	//	longest = evaluationTimes[i];
                	//	longestIndex = i;
                	//}
                	if (instanceCounts[i] > 1) {
                		long[] tempTimes = ((CompoundLikelihood)likelihoods.get(i)).getEvaluationTimes();
                		int[] tempCounts = ((CompoundLikelihood)likelihoods.get(i)).getEvaluationCounts();
                		for (int j = 0; j < tempCounts.length; j++) {
                			System.err.println("  time=" + tempTimes[j] + "   count=" + tempCounts[j]);
                		}
                		System.err.println("  Resetting times and counts.");
                		((CompoundLikelihood)likelihoods.get(i)).resetEvaluationTimes();
                 	}
                }
                //System.err.println("Longest likelihood calculation: " + longestIndex + " took " + longest);
            	
                if (TRY_ALL_LIKELIHOODS) {
                	
                	if (newResult < baseResult) {
                		baseResult = newResult;
                	} else {
                		list.remove(0);
                		System.err.print("Ordered list of evaluation times: ");
                    	for (int i = 0; i < list.size(); i++) {
                    		System.err.print(list.get(i) + " ");
                    	}
                    	System.err.println();
                		//remove 1 instanceCount
                		System.err.print("Removing 1 instance count: " + instanceCount);
                		instanceCount = --instanceCounts[longestIndex];
                		System.err.println(" -> " + instanceCount + " for likelihood " + longestIndex);
                		newList = new ArrayList<Likelihood>();
                    	for (int i = 0; i < instanceCount; i++) {
                    		Patterns subPatterns = new Patterns(patterns[longestIndex], 0, 0, 1, i, instanceCount);

                    		BeagleTreeLikelihood treeLikelihood = createTreeLikelihood(
                                    subPatterns, treeModels[longestIndex], branchModels[longestIndex], siteRateModels[longestIndex], branchRateModels[longestIndex],
                                    null, 
                                    ambiguities[longestIndex], rescalingSchemes[longestIndex], partialsRestrictions.get(longestIndex),
                                    xo);
                    		
                            treeLikelihood.setId(xo.getId() + "_" + instanceCount);
                            newList.add(treeLikelihood);
                    	}
                    	likelihoods.remove(longestIndex);
                    	likelihoods.add(longestIndex, new CompoundLikelihood(instanceCount, newList));
                    	//likelihoods.add(longestIndex, new CompoundLikelihood(newList));
                    	//compound = new CompoundLikelihood(totalInstances, likelihoods);
                    	compound = new CompoundLikelihood(likelihoods);
                    	siteCounts[longestIndex] *= 2;
                	}
                	if (list.isEmpty()) {
                		notFinished = false;
                	}
                	
                } else {
                
                	if (newResult < baseResult) {
                		baseResult = newResult;
                	} else {
                		notFinished = false;
                	}
                }
                
            }*/
        
    	return compound;
    	
    }

    //************************************************************************
    // AbstractXMLObjectParser implementation
    //************************************************************************

    public String getParserDescription() {
        return "Parses a collection of BeagleTreeLikelihoods and determines the number of partitions.";
    }

    public Class getReturnType() {
        return Likelihood.class;
    }

    public static final XMLSyntaxRule[] rules = {
            new ElementRule(BeagleTreeLikelihood.class, 1, Integer.MAX_VALUE)
    };

    public XMLSyntaxRule[] getSyntaxRules() {
        return rules;
    }
}

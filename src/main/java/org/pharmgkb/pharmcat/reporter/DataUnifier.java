package org.pharmgkb.pharmcat.reporter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pharmgkb.pharmcat.haplotype.model.json.DiplotypeCall;
import org.pharmgkb.pharmcat.reporter.model.CPICException;
import org.pharmgkb.pharmcat.reporter.model.CPICinteraction;
import org.pharmgkb.pharmcat.reporter.model.Group;
import org.pharmgkb.pharmcat.reporter.resultsJSON.Gene;
import org.pharmgkb.pharmcat.reporter.resultsJSON.Interaction;
import org.pharmgkb.pharmcat.reporter.resultsJSON.MultiGeneInteraction;

/**
 * 
 * This is the primary class and method for matching data the the three different sources.
 * I hate just about everything about how this was done, but for the sake of a quick hack to get
 * reports up and running it will have to do.
 * 
 * @author greytwist
 *
 */
public class DataUnifier {
    List<DiplotypeCall> calls;
    Map<String, List<CPICException>> exceptions;
    List<CPICinteraction> drugGenes;
    
    Map<String, Set<String>> dipCheckSet; 
	
	


   public DataUnifier( List<DiplotypeCall> calls,
                       Map<String, List<CPICException>> matches,
                       List<CPICinteraction> drugGenes){
        this.calls = calls;
        this.exceptions = matches;
        this.drugGenes = drugGenes;
        
        
        this.dipCheckSet = new HashMap< String, Set<String>>();
        
    }


	
	/*
     *  Call to do the actual matching, this should all be broken out into
     *  independent methods so errors are clearly and atomically identified 
     *  and handled.
     *  
     *  This is going to need to be rethought through and reconstructed
     */
    public void findMatches(List<Gene> geneListToReturn, List<MultiGeneInteraction> multiInteractionsToReturn ){

    	// this map will be used to search through the drug gene requirements 
    	Map< String, Gene> geneReport = new HashMap<String, Gene>();
    	
        ExceptionMatcher exceptMatchTest = new ExceptionMatcher();
        DrugRecommendationMatcher recMatchTest = new DrugRecommendationMatcher();

        /*
         * This loop performs 4 functions at the same time
         * 1 - converts each diplotype call object into a reportable gene object, and add to hashmap for quick look up
         * 2 - searches through exception list and adds any exceptions to the called genes
         * 3 - constructs the big gene can called diplotye sets for checking through 
         *  drug gene interactions
         * 
         */
        
        for( DiplotypeCall call : calls ){

        	//1 - convert calls from haplotype call set to gene reportable format
            Gene gene = new Gene(call);

            //2 - get exceptions for each gene called if a specific exception exists
            if( exceptions.containsKey(call.getGene()) ){
            	// add any known gene exceptions
                for( CPICException exception : exceptions.get(call.getGene() ) ){
                   if( exceptMatchTest.test( gene, exception.getMatches()) ){
                       gene.addException(exception);
                   }
                }
            }
            //1 - add gene to report hash for quick searching later
            geneReport.put(gene.getGene(), gene);

            //3 - convert diplotype calls to single objects
            Set<String> tmp = new HashSet<String>(); 
            for ( Object single : gene.getDips().toArray() ){
            	tmp.add( single.toString() );
            	
            }
            dipCheckSet.put(gene.getGene(), tmp);
            
        }
        
        /*
         * This is the loop for looking through the cpic drug gene interactions and trying to figure out which apply to the situation
         */ 
        for( CPICinteraction interact : drugGenes ){

        	//if statment only that will handle all single gene drug interactions
        	if( interact.getRelatedChemicals().size() == 1 ){
        
        		if( geneReport.containsKey( interact.getRelatedGenes().get(0).getSymbol() )){
        			Interaction interactionToAdd = new Interaction(interact);
        			for( Group test : interact.getGroups() ){
        				Set<String> searchSet = dipCheckSet.get(interact.getRelatedGenes().get(0).getSymbol());
	            		Set<String> diplotypesForAnnot =  new HashSet<String>(test.getGenotypes()) ;
	            	
	            		boolean associationMatch = false;
	            		
	            		for( String dip : diplotypesForAnnot ){
	            			if( searchSet.contains(dip)){
	            				associationMatch = true;
	            				break;
	            			}
	            		}
	            		
	            		if( associationMatch ){
	            			interactionToAdd.addToGroup(test);	
	            		}	            	
        			}// group test loop end
        			
        			geneReport.get( interact.getRelatedGenes().get(0).getSymbol() ).addInteraction(interactionToAdd);
        			
        		}// end of containsKey check
        	} /*else if( interact.getRelatedChemicals().size() > 1 ){
        		
        		MultiGeneInteraction multiCheck = new MultiGeneInteraction( interact );
        		boolean geneMatch = true;
        
        		
        	}*/ else {
        		continue;
        		//Throw error about having no defined chemicals for a guideline and do not process
        	}

        	
        }
        //return thing here!
    }
}
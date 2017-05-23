package org.hobbit.benchmark.gerbil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.gerbil.io.nif.NIFParser;
import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFParser;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.hobbit.benchmark.gerbil.comparator.DocumentComparator;
import org.hobbit.core.components.AbstractDataGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.gerbil.commons.CONSTANTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BengalDataGenerator extends AbstractDataGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BengalDataGenerator.class);

    private static final int MIN_SENTENCE = 20;
    private static final int MAX_SENTENCE = 500;
    
//    private static final SelectorType SELECTOR_TYPE = SelectorType.SIM_STAR;
    private static final boolean USE_PARAPHRASING = true;
    private static final boolean USE_PRONOUNS = false;
    private static final boolean USE_SURFACEFORMS = true;
    private static final boolean USE_AVATAR = false;
    private static final boolean USE_ONLY_OBJECT_PROPERTIES = false;
    private static final long WAITING_TIME_BETWEEN_DOCUMENTS = 500;



    private NIFWriter writer = new TurtleNIFWriter();
    private NIFParser parser = new TurtleNIFParser();
    
    private int numberOfDocuments;

    private String endpoint="http://dbpedia.org/sparql";

    private List<Document>  documents;

    private static int phases;

//    private static int min_sentence;
//
//    private static int max_sentence;
//
//    private static SelectorType selectorType;
//
//    private static boolean useParaphrasing;
//
//    private static boolean usePronouns;
//
//    private static boolean useSurfaceforms;
//
//    private static boolean useAvatar;
//
//    private static boolean useOnlyObjectProperties;

    private static String path = "corpora"+File.separator;
    
    
    @Override
    public void init() throws Exception {
        super.init();


        Map<String, String> env = System.getenv();
        // Get the number of documents from the parameters
        if (!env.containsKey(CONSTANTS.GERBIL_DATA_GENERATOR_NUMBER_OF_DOCUMENTS_KEY)) {
            throw new IllegalArgumentException(
                    "Couldn't get \"" + CONSTANTS.GERBIL_DATA_GENERATOR_NUMBER_OF_DOCUMENTS_KEY + "\" from the environment. Aborting.");
        }
        try {
            numberOfDocuments = Integer.parseInt(env.get(CONSTANTS.GERBIL_DATA_GENERATOR_NUMBER_OF_DOCUMENTS_KEY));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Couldn't get \"" + CONSTANTS.GERBIL_DATA_GENERATOR_NUMBER_OF_DOCUMENTS_KEY + "\" from the environment. Aborting.", e);
        }

        if (!env.containsKey(CONSTANTS.GERBIL_DATA_GENERATOR_SEED_KEY)) {
            throw new IllegalArgumentException("Couldn't get \"" + CONSTANTS.GERBIL_DATA_GENERATOR_SEED_KEY + "\" from the environment. Aborting.");
        }
        long seed;
	try {
            seed = Long.parseLong(env.get(CONSTANTS.GERBIL_DATA_GENERATOR_SEED_KEY)) + getGeneratorId();
//            random = new Random(Long.parseLong(env.get(CONSTANTS.GERBIL_DATA_GENERATOR_SEED_KEY)) + getGeneratorId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Couldn't get \"" + CONSTANTS.GERBIL_DATA_GENERATOR_SEED_KEY + "\" from the environment. Aborting.", e);
        }

	if (!env.containsKey(CONSTANTS.BENGAL_TASK_KEY)) {
            throw new IllegalArgumentException("Couldn't get \"" + CONSTANTS.BENGAL_TASK_KEY + "\" from the environment. Aborting.");
        }
        int task=1;
        try{
            task = Integer.parseInt(env.get(CONSTANTS.BENGAL_TASK_KEY));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Couldn't get \"" + CONSTANTS.BENGAL_TASK_KEY + "\" from the environment. Aborting.", e);
        }
        
//        min_sentence = MIN_SENTENCE;
//        try{
//            min_sentence = Integer.parseInt(env.get(CONSTANTS.BENGAL_MIN_SENTENCE));
//        }catch(Exception e){}
//        max_sentence = MAX_SENTENCE;
//        try{
//            max_sentence = Integer.parseInt(env.get(CONSTANTS.BENGAL_MAX_SENTENCE));
//        }catch(Exception e){}
//        
//        selectorType = SELECTOR_TYPE;
//        try{
//            selectorType = SelectorType.valueOf(env.get(CONSTANTS.BENGAL_SELECTOR_TYPE));
//        }catch(Exception e){}
//        useParaphrasing = USE_PARAPHRASING;
//        try{
//            useParaphrasing = Boolean.valueOf(env.get(CONSTANTS.BENGAL_USE_PARAPHRASING));
//        }catch(Exception e){}
//        usePronouns = USE_PRONOUNS;
//        try{
//            usePronouns = Boolean.valueOf(env.get(CONSTANTS.BENGAL_USE_PRONOUNS));
//        }catch(Exception e){}
//        useSurfaceforms = USE_SURFACEFORMS;
//        try{
//            useSurfaceforms = Boolean.valueOf(env.get(CONSTANTS.BENGAL_USE_SURFACEFORMS));
//        }catch(Exception e){}
//        useAvatar = USE_AVATAR;
//        try{
//            useAvatar = Boolean.valueOf(env.get(CONSTANTS.BENGAL_USE_AVATAR));
//        }catch(Exception e){}
//        useOnlyObjectProperties = USE_ONLY_OBJECT_PROPERTIES;;
//        try{
//            useOnlyObjectProperties = Boolean.valueOf(env.get(CONSTANTS.BENGAL_USE_ONLY_OBJECT_PROPERTIES));
//        }catch(Exception e){}
        
        phases=3;
        try{
            phases  = Integer.parseInt(env.get(CONSTANTS.BENGAL_PHASES));
        }catch(Exception e){}
        documents = generateCorpus2(task, endpoint, seed, numberOfDocuments);
    }
    
    
    private List<Document> generateCorpus2(int task, String endpoint, long seed, int numberOfDocuments) throws FileNotFoundException{
	List<Document> ret=new ArrayList<Document> ();
	File f=null;
	switch(task){
	case 1:
	    f = new File(path+"task1.nif");
	    break;
	case 2:
	    f = new File(path+"task2.nif");
	    break;
	case 3:
	    f = new File(path+"task3.nif");
	    break;
	}
	List<Document> allDocuments = parser.parseNIF(new FileReader(f));
	Collections.sort(allDocuments, new DocumentComparator());

	int startDoc = Long.valueOf(seed%allDocuments.size()).intValue();
	for(int i=0;i<numberOfDocuments;i++){
	    if(startDoc>=allDocuments.size()){startDoc=0;}
	    ret.add(allDocuments.get(startDoc++));
	}
	
	return ret;
	
    }
    
//    private static List<Document> generateCorpus(int task, String endpoint, long seed, int numberOfDocuments){
//	
//
//	Set<String> classes = new HashSet<>();
//	classes = getTaskOntology(task);
//
//	// instantiate components;
//	TripleSelectorFactory factory = new TripleSelectorFactory();
//	TripleSelector tripleSelector = null;
//	BVerbalizer verbalizer = null;
//	AvatarVerbalizer alernativeVerbalizer = null;
//	if (useAvatar) {
//		alernativeVerbalizer = AvatarVerbalizer.create(classes,
//			useOnlyObjectProperties ? classes : new HashSet<>(), endpoint, null, seed, false);
//		if (alernativeVerbalizer == null) {
//			return null;
//		}
//	} else {
//		tripleSelector = factory.create(selectorType, classes,
//				useOnlyObjectProperties ? classes : new HashSet<>(), endpoint, null, min_sentence, max_sentence,
//				seed);
//		verbalizer = new SemWeb2NLVerbalizer(SparqlEndpoint.getEndpointDBpedia(), usePronouns, useSurfaceforms);
//	}
//	Paraphraser paraphraser = null;
//	if (useParaphrasing) {
//		ParaphraseService paraService = Paraphrasing.create();
//		if (paraService != null) {
//			paraphraser = new ParaphraserImpl(paraService);
//		} else {
//			LOGGER.error("Couldn't create paraphrasing service. Aborting.");
//			return null;
//		}
//	}
//	List<Statement> triples;
//	Document document = null;
//	List<Document> documents = new ArrayList<>();
//	int counter = 0;
//	while (documents.size() < numberOfDocuments) {
//		if (useAvatar) {
//			document = alernativeVerbalizer.nextDocument();
//		} else {
//			// select triples
//			triples = tripleSelector.getNextStatements();
//			if ((triples != null) && (triples.size() >= min_sentence)) {
//				// create document
//				document = verbalizer.generateDocument(triples);
//				if (document != null) {
//					List<NumberOfVerbalizedTriples> tripleCounts = document
//							.getMarkings(NumberOfVerbalizedTriples.class);
//					if ((tripleCounts.size() > 0) && (tripleCounts.get(0).getNumberOfTriples() < min_sentence)) {
//						LOGGER.error(
//								"The generated document does not have enough verbalized triples. It will be discarded.");
//						document = null;
//					}
//				}
//				if (document != null) {
//					// paraphrase document
//					if (paraphraser != null) {
//						try {
//							document = paraphraser.getParaphrase(document);
//						} catch (Exception e) {
//							LOGGER.error("Got exception from paraphraser. Using the original document.", e);
//						}
//					}
//				}
//			}
//		}
//		// If the generation and paraphrasing were successful
//		if (document != null) {
//			LOGGER.info("Created document #" + counter);
//			document.setDocumentURI("http://aksw.org/generated/" + counter);
//			counter++;
//			documents.add(document);
//			document = null;
//		}
//		try {
//			if (!useAvatar) {
//				Thread.sleep(WAITING_TIME_BETWEEN_DOCUMENTS);
//			}
//		} catch (InterruptedException e) {
//		}
//	}
//
//	return documents;
//	
//    }

    @Override
    protected void generateData() throws Exception {
	byte[] data;
	for(int i=phases; i>0;i--){
            
            for(Document document : documents){
        	data = RabbitMQUtils.writeString(writer.writeNIF(Arrays.asList(document)));
		sendDataToTaskGenerator(data);
        	wait(getWaitTime(i));
            }
        }
    }
    
    private long getWaitTime(int phase) {
	long initial = 125;
	return Math.round(initial*(Math.pow(2,phase-1)-1));	
    }
    

 
    private static Set<String> getTaskOntology(int task){
	Set<String> ret = new HashSet<String>();
	String fileName="";
	switch(task){
	case 1:
	    fileName="task1.txt";
	    break;
	case 2:
	    fileName="task2.txt";
	    break;
	case 3:
	    fileName="task3.txt";
	    break;	
	}
	try(BufferedReader reader = new BufferedReader(new FileReader(fileName))){
	    String line="";
	    while((line=reader.readLine())!=null){
		ret.add("<"+line+">");
	    }
	} catch (IOException e) {
	    LOGGER.error("File for task Ontologie could not be found", e);
	    return ret;
	}
		
	return ret;

    }
    
    @Override
    public void close() throws IOException {
        super.close();
    }

}
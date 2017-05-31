package org.hobbit.benchmark.gerbil.adapters;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.io.nif.NIFParser;
import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFParser;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.hobbit.benchmark.gerbil.systems.HobbitAnnotator;
import org.hobbit.benchmark.gerbil.systems.impl.AIDAWrapper;
import org.hobbit.benchmark.gerbil.systems.impl.AgdistisWrapper;
import org.hobbit.benchmark.gerbil.systems.impl.FOXWrapper;
import org.hobbit.benchmark.gerbil.systems.impl.FREDWrapper;
import org.hobbit.benchmark.gerbil.systems.impl.NERDWrapper;
import org.hobbit.benchmark.gerbil.systems.impl.SpotlightWrapper;
import org.hobbit.benchmark.gerbil.systems.impl.TagMeWrapper;
import org.hobbit.benchmark.gerbil.systems.impl.XLisaWrapper;
import org.hobbit.core.components.AbstractSystemAdapter;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GerbilSystemAdapter extends AbstractSystemAdapter {
	
	
	protected static Logger LOGGER = LoggerFactory.getLogger(GerbilSystemAdapter.class);

	protected HobbitAnnotator annotator;

	protected NIFParser reader = new TurtleNIFParser();
    	protected NIFWriter writer = new TurtleNIFWriter();

    	private Property name = ResourceFactory.createProperty("http://gerbil.org/systems/name");
    	
        @Override
        public void init() throws Exception {
    		super.init();
    		this.
    		LOGGER.info(this.systemParamModel.listResourcesWithProperty(name).toList()+"");
    		String systemName = this.systemParamModel.listObjectsOfProperty(name).next().asLiteral().getString();
    		LOGGER.info("SYSTEM NAME: "+systemName);
    		systemName="Spotlight-ACRT2KB-OKET1";
    		switch(systemName){
    		case "Aida-AC2KB-ERec":
    		    annotator = new AIDAWrapper("https://gate.d5.mpi-inf.mpg.de/aida/service/disambiguate", false);
    		    break;
    		case "Aida-D2KB":
    		    annotator = new AIDAWrapper("https://gate.d5.mpi-inf.mpg.de/aida/service/disambiguate", true);
    		    break;
    		case "Agdisits":
    		    annotator = new AgdistisWrapper("139.18.2.164", "8080");
    		    break;
    		case "FOX":
    		    annotator = new FOXWrapper("http://139.18.2.164:4444/call/ner/entities");
    		    break;
    		case "FRED":
    		    annotator = new FREDWrapper("http://wit.istc.cnr.it/stlab-tools/fred");
    		    break;
    		case "NERD":
    		    annotator = new NERDWrapper("http://nerd.eurecom.fr/api/");
    		    break;
    		case "Spotlight-ACRT2KB-OKET1":
    		    annotator = new SpotlightWrapper("http://model.dbpedia-spotlight.org:2222/rest/", 0);
    		    break;
    		case "Spotlight-D2KB-ETyping":
    		    annotator = new SpotlightWrapper("http://model.dbpedia-spotlight.org:2222/rest/", 1);
    		    break;
    		case "Spotlight-ERec":
    		    annotator = new SpotlightWrapper("http://spotlight.sztaki.hu:2222/rest/", 2);
    		    break;
    		case "TagMe-ACD2KB":
    		    annotator = new TagMeWrapper("https://tagme.d4science.org/tagme/tag", "https://tagme.d4science.org/tagme/spot", true);
    		    break;
    		case "TagMe-ERec":
    		    annotator = new TagMeWrapper("https://tagme.d4science.org/tagme/tag", "https://tagme.d4science.org/tagme/spot", false);
    		    break;
    		case "xLisaNER":
    		    annotator = new XLisaWrapper("en","en","dbedia","NER");
    		    break;
    		case "xLisaNGRAM":
    		    annotator = new XLisaWrapper("en","en","dbedia","NGRAM");
    		    break;
    		}
    	
        }
    	
    	
    /**
     * You MIGHT need this, depends on the benchmark.
     * @see <a href="https://project-hobbit.eu/challenges">Challenges and their Benchmarks</a>
     */
    public void receiveGeneratedData(byte[] data) {
        // handle the incoming data as described in the benchmark description
	}
    
    
    
    /**
     *  Create results for the incoming data.
     *  The structure of the incoming data should be defied by the <a href="https://project-hobbit.eu/challenges">Challenges and their Benchmarks</a>
     *
     *  e.g If you want to benchmark against the QALD-Challenge,
     *  you can expect the incoming data to be questions. Accordingly,
     *  your result[] output should be answers. 
     *  The data structure of the incoming and outgoing data should follow 
     *  the QALD-Json format. You can find <a href="https://github.com/AKSW/NLIWOD/tree/master/qa.commons/src/main/java/org/aksw/qa/commons/load">here</a>
     *   a loder and parser and a complete class structure for QALD-Json. 
     *   These are already included as dependency.
     *  
     *
     *  @see <a href="https://github.com/hobbit-project/platform/wiki/Develop-a-system-adapter#the-task-queue">The Task Queue and structure of data[]</a>
     *
     */
    public void receiveGeneratedTask(String taskId, byte[] data) {
		Document document=null;
		try{
		    List<Document> documents = reader.parseNIF(RabbitMQUtils.readString(data));
		    document = documents.get(0);
		}catch(Exception e){
		    LOGGER.error("DOCUMENT WAS NULL.", e);
		    document = new DocumentImpl();
		}
		 
      	    	try {
	    	    LOGGER.info("Get Answer for Document:"+document);
		    document = getAnswers(document);
		    LOGGER.info("Got answers:");
	    	    LOGGER.info(document+"");
		} catch (GerbilException e1) {
		    document = null;
		    LOGGER.error("Could not get answer due to", e1);
		}

		// Send the result to the evaluation storage
		try {
            		sendResultToEvalStorage(taskId, RabbitMQUtils.writeString(writer.writeNIF(Arrays.asList(document))));

		} catch (IOException e) {
			//Log the error.
		    	LOGGER.error("Problem sending results to eval storage. ", e);
		}
	}

	 /*
	  * Use This to switch through several possibiliets (A2KB, Etyping,..)
	  */
	protected Document getAnswers(Document document) throws GerbilException{
	    return annotator.annotate(document);
	}


    @Override
	public void close() throws IOException {
//	annotator.close();
	super.close();
    }


}


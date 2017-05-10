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
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.hobbit.benchmark.gerbil.systems.HobbitAnnotator;
import org.hobbit.benchmark.gerbil.systems.impl.AIDAWrapper;
import org.hobbit.benchmark.gerbil.systems.impl.FOXWrapper;
import org.hobbit.benchmark.gerbil.systems.impl.FREDWrapper;
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
    		String systemName = this.systemParamModel.listObjectsOfProperty(name).next().asLiteral().getString();
    		switch(systemName){
    		case "Aida":
    		    annotator = new AIDAWrapper("https://gate.d5.mpi-inf.mpg.de/aida/service/disambiguate");
    		    break;
    		case "FOX":
    		    annotator = new FOXWrapper();
    		    break;
    		case "FRED":
    		    annotator = new FREDWrapper("http://wit.istc.cnr.it/stlab-tools/fred");
    		    break;
    		case "TagMe":
    		    annotator = new TagMeWrapper("https://tagme.d4science.org/tagme/tag", "https://tagme.d4science.org/tagme/spot");
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
    	
		List<Document> documents = reader.parseNIF(RabbitMQUtils.readString(data));
       		Document document = documents.get(0);

	    	try {
		    getAnswers(document);
		} catch (GerbilException e1) {
		    document = null;
		}

		// Send the result to the evaluation storage
		try {
            		sendResultToEvalStorage(taskId, RabbitMQUtils.writeString(writer.writeNIF(Arrays.asList(document))));

		} catch (IOException e) {
			//Log the error
		}
	}

	 /*
	  * Use This to switch through several possibiliets (A2KB, Etyping,..)
	  */
	protected void getAnswers(Document document) throws GerbilException{
	    document = annotator.annotate(document);
	}


    @Override
	public void close() throws IOException {
//	annotator.close();
	super.close();
    }


}


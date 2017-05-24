package org.hobbit.benchmark.gerbil;

import org.aksw.gerbil.datatypes.ExperimentType;
import org.aksw.gerbil.semantic.vocabs.GERBIL;
import org.apache.jena.rdf.model.NodeIterator;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractBenchmarkController;
import org.hobbit.gerbil.commons.CONSTANTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GerbilBenchmark extends AbstractBenchmarkController {

    private static final Logger LOGGER = LoggerFactory
	    .getLogger(GerbilBenchmark.class);

    private static final String DATA_GENERATOR_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/gerbil/gerbildatagenerator";
    private static final String BENGAL_DATA_GENERATOR_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/conrads/bengaldatagenerator";
    private static final String TASK_GENERATOR_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/gerbil/gerbiltaskgenerator";
    private static final String BENGAL_TASK_GENERATOR_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/conrads/bengaltaskgenerator";
    private static final String EVALUATION_MODULE_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/gerbil/gerbilevaluationmodule";

    private static final String GERBIL2_PREFIX = "http://w3id.org/gerbil/hobbit/vocab#";
    // private static final String EVALUATION_STORE_CONTAINER_IMAGE =
    // "hobbit/evaluation_store";
    // private static final String EVALUATION_STORE_CONTAINER_IMAGE =
    // "in_memory_evaluation_storage";
   
    private String selectorType="sym_star", minSentences="20", maxSentences="500", usePronouns="false",
	    useParaphrasing="true",useAvatar="false",useOOP="false", phases="3", useSurfaceforms="true";


    private int task=1;
    private ExperimentType experimentType;

    @Override
    public void init() throws Exception {
        super.init();
        	
        // TODO define this somewhere else
        int numberOfGenerators = 1;
        long seed = 31;
        boolean isBengal = false;
        int numberOfDocuments = -1; 
        String datasetName = "";
        
        NodeIterator iterator = benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(GERBIL2_PREFIX+"isBengal"));
        if(iterator.hasNext() && Boolean.valueOf(iterator.next().asLiteral().getBoolean())){
            
            isBengal = true;
            iterator = benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel
        	  .getProperty(GERBIL2_PREFIX+"hasNumberOfDocuments"));
            numberOfDocuments = -1;
            if (iterator.hasNext()) {
        	try {
        	    numberOfDocuments = iterator.next().asLiteral().getInt();
        	} catch (Exception e) {
        	    LOGGER.error("Exception while parsing parameter.", e);
        	}
            }
            if (numberOfDocuments < 0 ) {
        	LOGGER.warn("Couldn't get the number of documents from the parameter model. Using the default value 100.");
        	numberOfDocuments = 100;
            }
            iterator=benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(GERBIL2_PREFIX+"hasSeed"));
            if(iterator.hasNext())
            	seed = iterator.next().asLiteral().getLong();
            iterator=benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(GERBIL2_PREFIX+"selectorType"));
            if(iterator.hasNext())
            	selectorType = iterator.next().asLiteral().getString();
            iterator=benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(GERBIL2_PREFIX+"minSentences"));
            if(iterator.hasNext())
        	minSentences = iterator.next().asLiteral().getString();
            iterator=benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(GERBIL2_PREFIX+"maxSentences"));
            if(iterator.hasNext())
            	maxSentences = iterator.next().asLiteral().getString();
            iterator=benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(GERBIL2_PREFIX+"usePronouns"));
            if(iterator.hasNext())
            	usePronouns = iterator.next().asLiteral().getString();
            iterator=benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(GERBIL2_PREFIX+"useParaphrasing"));
            if(iterator.hasNext())
            	useParaphrasing = iterator.next().asLiteral().getString();
            iterator=benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(GERBIL2_PREFIX+"useAvatar"));
            if(iterator.hasNext())
            	useAvatar = iterator.next().asLiteral().getString();
            iterator=benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(GERBIL2_PREFIX+"useOnlyObjectProperties"));
            if(iterator.hasNext())
            	useOOP = iterator.next().asLiteral().getString();
            iterator=benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(GERBIL2_PREFIX+"phases"));
            if(iterator.hasNext())
            	phases = iterator.next().asLiteral().getString();
            iterator=benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel.getProperty(GERBIL2_PREFIX+"useSurfaceforms"));
            if(iterator.hasNext())
            	useSurfaceforms = iterator.next().asLiteral().getString();
            
        }
        else{
            iterator = benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel
                .getProperty(GERBIL2_PREFIX+"hasDataset"));
            datasetName="";
            if(iterator.hasNext()){
        	try {
        	    datasetName = DatasetMapper.getName(iterator.next().asResource().getLocalName());
        	} catch (Exception e) {
        	    LOGGER.error("Exception while parsing parameter.", e);
        	}
            }


        
            iterator = benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel
        	    .getProperty(GERBIL2_PREFIX+"hasExperimentType"));
            experimentType = null;
            if (iterator.hasNext()) {
        	try {
        	    experimentType = GERBIL.getExperimentTypeFromResource(iterator.next().asResource());
        	} catch (Exception e) {
        	    LOGGER.error("Exception while parsing parameter.", e);
        	}
            }
            if (experimentType == null) {
        	LOGGER.error("Couldn't get the experiment type from the parameter model. Using the default value.");
        	experimentType = ExperimentType.A2KB;

            }
        }
        // FIXME find a way to define the number of generators

        // FIXME for the usage of Bengal, we need a DBpedia endpoint. Create
        // such a component here

        if(!isBengal){
            createDataGenerators(DATA_GENERATOR_CONTAINER_IMAGE, numberOfGenerators, new String[] {
        	    CONSTANTS.GERBIL_DATASET_TO_TEST_NAME + "=" + datasetName,
        	    CONSTANTS.GERBIL_EXPERIMENT_TYPE + "=" + experimentType.getName() });
            createTaskGenerators(TASK_GENERATOR_CONTAINER_IMAGE, numberOfGenerators,
                    new String[] { CONSTANTS.GERBIL_TASK_GENERATOR_EXPERIMENT_TYPE_PARAMETER_KEY + "=" + experimentType.name() });
        }else{

            createDataGenerators(BENGAL_DATA_GENERATOR_CONTAINER_IMAGE, numberOfGenerators, new String[] {
        	    CONSTANTS.GERBIL_DATA_GENERATOR_NUMBER_OF_DOCUMENTS_KEY + "=" + numberOfDocuments,
        	    CONSTANTS.GERBIL_DATA_GENERATOR_SEED_KEY + "=" + seed,
        	    CONSTANTS.BENGAL_TASK_KEY+"="+task,
        	    CONSTANTS.BENGAL_MIN_SENTENCE+"="+minSentences,
        	    CONSTANTS.BENGAL_MAX_SENTENCE+"="+maxSentences,
        	    CONSTANTS.BENGAL_PHASES+"="+phases,
        	    CONSTANTS.BENGAL_SELECTOR_TYPE+"="+selectorType,
        	    CONSTANTS.BENGAL_USE_AVATAR+"="+useAvatar,
        	    CONSTANTS.BENGAL_USE_ONLY_OBJECT_PROPERTIES+"="+useOOP,
        	    CONSTANTS.BENGAL_USE_PARAPHRASING+"="+useParaphrasing,
        	    CONSTANTS.BENGAL_USE_PRONOUNS+"="+usePronouns,
        	    CONSTANTS.BENGAL_USE_SURFACEFORMS+"="+useSurfaceforms
            });
            createTaskGenerators(BENGAL_TASK_GENERATOR_CONTAINER_IMAGE, numberOfGenerators,
                    new String[] { CONSTANTS.GERBIL_TASK_GENERATOR_EXPERIMENT_TYPE_PARAMETER_KEY + "=" + experimentType.name() });
        }
        


        createTaskGenerators(TASK_GENERATOR_CONTAINER_IMAGE, numberOfGenerators, new String[] {
                CONSTANTS.GERBIL_TASK_GENERATOR_EXPERIMENT_TYPE_PARAMETER_KEY + "=" + experimentType.name() });

        // createEvaluationStorage();
        createEvaluationStorage(DEFAULT_EVAL_STORAGE_IMAGE,
                new String[] { Constants.ACKNOWLEDGEMENT_FLAG_KEY + "=true" });
        // createEvaluationStorage(EVALUATION_STORE_CONTAINER_IMAGE, new
        // String[] { "HOBBIT_RABBIT_HOST="
        // + connection.getAddress().toString() });

        waitForComponentsToInitialize();
    }

    @Override
    protected void executeBenchmark() throws Exception {
	// give the start signals
	sendToCmdQueue(Commands.TASK_GENERATOR_START_SIGNAL);
	sendToCmdQueue(Commands.DATA_GENERATOR_START_SIGNAL);
	// wait for the data generators to finish their work
	waitForDataGenToFinish();
	// wait for the task generators to finish their work
	waitForTaskGenToFinish();
	// wait for the system to terminate
	waitForSystemToFinish();
	// start the evaluation module
	createEvaluationModule(
		EVALUATION_MODULE_CONTAINER_IMAGE,
		new String[] { CONSTANTS.GERBIL_EVALUATION_MODULE_EXPERIMENT_TYPE_KEY
			+ "=" + experimentType.name() });
	// wait for the evaluation to finish
	waitForEvalComponentsToFinish();
	// the evaluation module should have sent an RDF model containing the
	// results. We should add the configuration of the benchmark to this
	// model.
	// FIXME add parameters
	// this.resultModel.add(null);
	sendResultModel(this.resultModel);
    }
}

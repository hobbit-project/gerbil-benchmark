package org.hobbit.benchmark.gerbil;

import org.aksw.gerbil.datatypes.ExperimentType;
import org.aksw.gerbil.semantic.vocabs.GERBIL;
import org.apache.jena.rdf.model.NodeIterator;
import org.hobbit.core.Commands;
import org.hobbit.core.components.AbstractBenchmarkController;
import org.hobbit.gerbil.commons.CONSTANTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GerbilBenchmark extends AbstractBenchmarkController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GerbilBenchmark.class);

    private static final String DATA_GENERATOR_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/gerbilbenchmark/gerbildatagenerator";
    private static final String TASK_GENERATOR_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/gerbilbenchmark/gerbiltaskgenerator";
    private static final String EVALUATION_MODULE_CONTAINER_IMAGE = "git.project-hobbit.eu:4567/gerbilbenchmark/gerbil-evaluation-module";
//     private static final String EVALUATION_STORE_CONTAINER_IMAGE =
//     "hobbit/evaluation_store";
//    private static final String EVALUATION_STORE_CONTAINER_IMAGE = "in_memory_evaluation_storage";

    private ExperimentType experimentType;

    @Override
    public void init() throws Exception {
        super.init();

        // TODO define this somewhere else
        int numberOfGenerators = 1;
        long seed = 31;

        // ex:BenchmarkRun_123 rdf:type hobbit:BenchmarkRun;
        // ex:hasNumberOfDocuments "200"^^xsd:unsignedInt;
        // ex:hasExperimentType ex:A2KB;
        NodeIterator iterator = benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel
                .getProperty("http://example.org/hasNumberOfDocuments"));
        int numberOfDocuments = -1;
        if (iterator.hasNext()) {
            try {
                numberOfDocuments = iterator.next().asLiteral().getInt();
            } catch (Exception e) {
                LOGGER.error("Exception while parsing parameter.", e);
            }
        }
        if (numberOfDocuments < 0) {
            LOGGER.error("Couldn't get the number of documents from the parameter model. Using the default value.");
            numberOfDocuments = 100;
        }
        iterator = benchmarkParamModel.listObjectsOfProperty(benchmarkParamModel
                .getProperty("http://example.org/hasExperimentType"));
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

        // FIXME find a way to define the number of generators

        // FIXME for the usage of Bengal, we need a DBpedia endpoint. Create
        // such a component here

        createDataGenerators(DATA_GENERATOR_CONTAINER_IMAGE, numberOfGenerators, new String[] {
                CONSTANTS.GERBIL_DATA_GENERATOR_NUMBER_OF_DOCUMENTS_KEY + "=" + numberOfDocuments,
                CONSTANTS.GERBIL_DATA_GENERATOR_SEED_KEY + "=" + seed });

        createTaskGenerators(TASK_GENERATOR_CONTAINER_IMAGE, numberOfGenerators,
                new String[] { CONSTANTS.GERBIL_TASK_GENERATOR_EXPERIMENT_TYPE_PARAMETER_KEY + "=" + experimentType.name() });

        createEvaluationStorage();
//        createEvaluationStorage(EVALUATION_STORE_CONTAINER_IMAGE, new String[] { "HOBBIT_RABBIT_HOST="
//                + connection.getAddress().toString() });

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
        createEvaluationModule(EVALUATION_MODULE_CONTAINER_IMAGE,
                new String[] { CONSTANTS.GERBIL_EVALUATION_MODULE_EXPERIMENT_TYPE_KEY + "=" + experimentType.name() });
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
package org.hobbit.benchmark.gerbil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.aksw.gerbil.dataset.DatasetConfiguration;
import org.aksw.gerbil.dataset.check.EntityCheckerManager;
import org.aksw.gerbil.datatypes.ExperimentType;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.semantic.sameas.SameAsRetriever;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.web.config.DatasetsConfig;
import org.aksw.gerbil.web.config.RootConfig;
import org.hobbit.core.components.AbstractDataGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.gerbil.commons.CONSTANTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GerbilDataGenerator extends AbstractDataGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GerbilDataGenerator.class);

    private String datasetName;

    private NIFWriter writer = new TurtleNIFWriter();
    private ExperimentType experimentType;
    private List<DatasetConfiguration> configs;
    
    @Override
    public void init() throws Exception {
	LOGGER.info("Starting initialization of Gerbil Data Generator");
	super.init();
	Map<String, String> envVariables = System.getenv();
	
	createConfigurations(envVariables.get(CONSTANTS.GERBIL_DATASET_TO_TEST_NAME), ExperimentType.valueOf(envVariables.get(CONSTANTS.GERBIL_EXPERIMENT_TYPE)));
    }
    
    public void createConfigurations(String datasetName, ExperimentType experimentType){
	this.datasetName = datasetName;
	this.experimentType = experimentType;
	LOGGER.info("Using dataset {{}} and experimentType {{}}", datasetName, experimentType);

	SameAsRetriever retriever = null;
	EntityCheckerManager entityCheckerManager = null;
	configs = DatasetsConfig.datasets(entityCheckerManager, retriever).getAdaptersForName(datasetName);
	
	LOGGER.info("Initialization of Gerbil Data Generator done.");
    }

    @Override
    protected void generateData() throws Exception {

	LOGGER.info("Got the following configurations: {{}}", configs);
	for(DatasetConfiguration config : configs) {
	    LOGGER.info("Starting with config {{}}", config.getName());
	    byte[] data;
	    for (Document document : config.getDataset(experimentType).getInstances()) {
		data = RabbitMQUtils.writeString(writer.writeNIF(Arrays.asList(document)));
		sendDataToTaskGenerator(data);
	    }
	}
    }
    
    public void testData() throws GerbilException{
	LOGGER.info("Got the following configurations: {{}}", configs);
	for(DatasetConfiguration config : configs) {
	    LOGGER.info("Starting with config {{}}", config.getName());
	    byte[] data;
	    for (Document document : config.getDataset(experimentType).getInstances()) {
		LOGGER.info("FOUND: "+document.getText());
	    }
	}
    }

}

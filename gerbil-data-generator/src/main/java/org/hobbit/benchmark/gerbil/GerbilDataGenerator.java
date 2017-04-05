package org.hobbit.benchmark.gerbil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.aksw.gerbil.dataset.DatasetConfiguration;
import org.aksw.gerbil.datatypes.ExperimentType;
import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.web.config.DatasetsConfig;
import org.hobbit.core.components.AbstractDataGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;

public class GerbilDataGenerator extends AbstractDataGenerator {

    private static final String GERBIL_DATASET_TO_TEST_NAME = "gerbil-benchmark.datasetName";
    private static final Object GERBIL_EXPERIMENT_TYPE = "gerbil-benchmark.experimentType";
    private String datasetName;

    private NIFWriter writer = new TurtleNIFWriter();
    private ExperimentType experimentType;
    private List<DatasetConfiguration> configs;
    
    @Override
    public void init() throws Exception {
	super.init();
	Map<String, String> envVariables = System.getenv();
	// FIXME set correct envVariables
	datasetName = envVariables.get(GERBIL_DATASET_TO_TEST_NAME);
	experimentType = ExperimentType.valueOf(envVariables.get(GERBIL_EXPERIMENT_TYPE));
	//TODO add properties?, add SAS and ECM?
	configs = DatasetsConfig.datasets(null, null).getAdaptersForName(datasetName);
    }

    @Override
    protected void generateData() throws Exception {

	for(DatasetConfiguration config : configs) {
	    byte[] data;
	    for (Document document : config.getDataset(experimentType).getInstances()) {
		data = RabbitMQUtils.writeString(writer.writeNIF(Arrays.asList(document)));
		sendDataToTaskGenerator(data);
	    }
	}
    }

//    private DatasetConfiguration getDatasetConfig(EntityCheckerManager entityCheckerManager, SameAsRetriever globalRetriever){
//	return new SingletonDatasetConfigImpl(datasetName,
//		cacheable, constructor, constructorArgs, type,
//		entityCheckerManager, globalRetriever);
//    }
    
}

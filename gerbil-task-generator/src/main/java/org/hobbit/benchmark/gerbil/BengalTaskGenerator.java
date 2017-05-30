package org.hobbit.benchmark.gerbil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.aksw.gerbil.datatypes.ExperimentType;
import org.aksw.gerbil.execute.DocumentInformationReducer;
import org.aksw.gerbil.io.nif.NIFParser;
import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFParser;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.hobbit.core.components.AbstractSequencingTaskGenerator;
import org.hobbit.core.components.AbstractTaskGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BengalTaskGenerator extends AbstractTaskGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BengalTaskGenerator.class);

    public static final String EXPERIMENT_TYPE_PARAMETER_KEY = "gerbil.experiment_type";

    protected NIFParser reader = new TurtleNIFParser();
    protected NIFWriter writer = new TurtleNIFWriter();

    @Override
    public void init() throws Exception {
        super.init();
        Map<String, String> envVariables = System.getenv();

    }

    @Override
    protected void generateTask(byte[] data) throws Exception {
        // Create tasks(s) based on the incoming data inside this method.
        // You might want to use the id of this data generator and the
        // number of all data generators running in parallel.
        // int dataGeneratorId = getGeneratorId();
        // int numberOfGenerators = getNumberOfGenerators();

        // Create an ID for the task
        String taskId = getNextTaskId();

        List<Document> documents = reader.parseNIF(RabbitMQUtils.readString(data));
        Document document = documents.get(0);
        LOGGER.info("Received document " + document.toString());

        // Create the task and the expected answer
        byte[] taskData = RabbitMQUtils.writeString(writer.writeNIF(Arrays.asList(DocumentInformationReducer
                .reduceToPlainText(document))));
        byte[] expectedAnswerData = data;

        // Send the task to the system (and store the timestamp)
        long timestamp = System.currentTimeMillis();
        sendTaskToSystemAdapter(taskId, taskData);

        // Send the expected answer to the evaluation store
        sendTaskToEvalStorage(taskId, timestamp, expectedAnswerData);
    }
}

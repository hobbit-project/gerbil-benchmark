package org.hobbit.benchmark.gerbil.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.aksw.gerbil.dataset.impl.derczysnki.DerczynskiDataset;
import org.aksw.gerbil.dataset.impl.micro.Microposts2016Dataset;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.io.nif.NIFParser;
import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFParser;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.hobbit.core.components.AbstractSystemAdapter;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummySystemAdapter extends AbstractSystemAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DummySystemAdapter.class);

    protected NIFParser reader = new TurtleNIFParser();
    protected NIFWriter writer = new TurtleNIFWriter();
    private int i=0;

    @Override
    public void receiveGeneratedData(byte[] data) {
        LOGGER.warn("Got unexpected data from the data generators.");
    }

    @Override
    public void receiveGeneratedTask(String taskId, byte[] data) {
        List<Document> documents = reader.parseNIF(RabbitMQUtils.readString(data));
        Document document = documents.get(0);
        DerczynskiDataset dataset = new DerczynskiDataset(ClassLoader.getSystemResource("data.test").getFile());
        try {
	    dataset.init();
	} catch (GerbilException e1) {
	    LOGGER.error("Could not initialize Derzynski Dataset");
	}

        for(Document doc : dataset.getInstances()){
            if(doc.getText().equals(document.getText())){
        	document = doc;
        	break;
            }
        }
        LOGGER.info("Sending document " + document.toString());
        try {
            sendResultToEvalStorage(taskId, RabbitMQUtils.writeString(writer.writeNIF(Arrays.asList(document))));
        } catch (IOException e) {
            LOGGER.error("Got an exception while sending response.", e);
        }
    }
}

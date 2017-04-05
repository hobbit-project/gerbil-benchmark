package org.hobbit.benchmark.gerbil;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.aksw.gerbil.io.nif.NIFParser;
import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFParser;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hobbit.core.components.AbstractDataGenerator;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.gerbil.commons.CONSTANTS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BengalDataGenerator extends AbstractDataGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BengalDataGenerator.class);

    private static final String BENGAL_IMAGE_NAME = "bengal-service";
    private static final int MIN_SENTENCE = 10;
    private static final int MAX_SENTENCE = 500;


    private NIFParser parser = new TurtleNIFParser();
    private NIFWriter writer = new TurtleNIFWriter();
    private int numberOfDocuments;
    private String bengalService;
    private Random random;
    protected CloseableHttpClient client;

    @Override
    public void init() throws Exception {
        super.init();
// FIXME
//        bengalService = createContainer(BENGAL_IMAGE_NAME, new String[] {});
//        if (bengalService == null) {
//            LOGGER.error("Couldn't create bengal service container.");
//            throw new IllegalStateException("Couldn't create bengal service container.");
//        }

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
        try {
            random = new Random(Long.parseLong(env.get(CONSTANTS.GERBIL_DATA_GENERATOR_SEED_KEY)) + getGeneratorId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Couldn't get \"" + CONSTANTS.GERBIL_DATA_GENERATOR_SEED_KEY + "\" from the environment. Aborting.", e);
        }

        client = HttpClientBuilder.create().build();
    }

    @Override
    protected void generateData() throws Exception {
        int dataGeneratorId = getGeneratorId();
        Document document = null;
        byte[] data;
        int numberOfDocumentsByThisGenerator = determineNumberOfDocs();
        int counter = 0;
        while (counter < numberOfDocumentsByThisGenerator) {
            document = requestDocument(random.nextLong());
            if (document != null) {
                LOGGER.info("Created document #" + counter);
                document.setDocumentURI("http://aksw.org/generated/" + dataGeneratorId + "_" + counter);
                ++counter;
                data = RabbitMQUtils.writeString(writer.writeNIF(Arrays.asList(document)));
                sendDataToTaskGenerator(data);
                document = null;
            }
        }
    }

    private int determineNumberOfDocs() {
        int numberOfGenerators = getNumberOfGenerators();
        int docsToGenerate = numberOfDocuments / numberOfGenerators;
        if (getGeneratorId() < (numberOfDocuments % numberOfGenerators)) {
            ++docsToGenerate;
        }
        return docsToGenerate;
    }

    private Document requestDocument(long seed) throws IOException {
        return new DocumentImpl(Long.toString(seed));
//        FIXME
//        StringBuilder builder = new StringBuilder();
//        builder.append("http://");
//        builder.append(bengalService);
//        builder.append("?min=");
//        builder.append(MIN_SENTENCE);
//        builder.append("&max=");
//        builder.append(MAX_SENTENCE);
//        builder.append("&seed=");
//        builder.append(seed);
//        HttpGet request = new HttpGet(builder.toString());
//        CloseableHttpResponse response = null;
//        HttpEntity entity = null;
//        try {
//            response = client.execute(request);
//            if (response.getStatusLine().getStatusCode() >= 400) {
//                LOGGER.error("Response has wrong status: " + response.getStatusLine());
//            }
//            entity = response.getEntity();
//            List<Document> documents = parser.parseNIF(entity.getContent());
//            if (documents.size() == 0) {
//                LOGGER.error("Couldn't get a document from the bengal service. Returning null.");
//            } else {
//                if (documents.size() > 1) {
//                    LOGGER.info("Got more than one document from the bengal service. Only the first will be used.");
//                }
//                return documents.get(0);
//            }
//        } finally {
//            IOUtils.closeQuietly(response);
//            if (entity != null) {
//                EntityUtils.consumeQuietly(entity);
//            }
//        }
//        return null;
    }

    @Override
    public void close() throws IOException {
        if (bengalService != null) {
            stopContainer(bengalService);
        }
        super.close();
    }

}
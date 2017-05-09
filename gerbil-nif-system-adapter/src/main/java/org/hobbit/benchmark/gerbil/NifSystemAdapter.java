package org.hobbit.benchmark.gerbil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.gerbil.annotator.impl.nif.AdaptedNIFBasedAnnotatorWebservice;
import org.aksw.gerbil.io.nif.impl.TurtleNIFParser;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.semantic.vocabs.NIF_SYS;
import org.aksw.gerbil.transfer.nif.Document;
import org.apache.commons.io.IOUtils;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractSystemAdapter;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.utils.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NifSystemAdapter extends AbstractSystemAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NifSystemAdapter.class);

    private static final String NOT_MASTER_NODE_KEY = "NOT_MASTER_NODE";
    private static final String NIF_SYSTEM_ADAPTER_DOCKER_IMAGE = "git.project-hobbit.eu:4567/gerbil/gerbilnifsystemadapter";
    private static final String HOST_PLACE_HOLDER = "HOST";

    private boolean isMaster = true;
    private boolean isTerminating = false;
    private Set<String> slaveNodes = null;
    private String systemContainer = null;
    private TurtleNIFParser parser = new TurtleNIFParser();
    private TurtleNIFWriter writer = new TurtleNIFWriter();
    private AdaptedNIFBasedAnnotatorWebservice annotator;

    public NifSystemAdapter() {
        // We have to add the broadcast command header to receive messages about
        // terminated containers
        addCommandHeaderId(Constants.HOBBIT_SESSION_ID_FOR_BROADCASTS);
    }

    @Override
    public void init() throws Exception {
        super.init();

        Map<String, String> env = System.getenv();
        // Check whether this node is the master or not
        if (env.containsKey(NOT_MASTER_NODE_KEY)) {
            try {
                isMaster = Boolean.getBoolean(env.get(NOT_MASTER_NODE_KEY));
            } catch (Exception e) {
                LOGGER.warn(
                        "Couldn't read the value of " + NOT_MASTER_NODE_KEY + ". Assuming that this node is a slave.",
                        e);
                isMaster = false;
            }
        } else {
            isMaster = true;
        }
        // Get the system image name
        String systemImage = RdfHelper.getStringValue(systemParamModel, null, NIF_SYS.instanceImageName);
        if (systemImage == null) {
            throw new IllegalArgumentException("Couldn't load system image name. Aborting.");
        }
        String systemUrl = RdfHelper.getStringValue(systemParamModel, null, NIF_SYS.webserviceUrl);
        if (systemUrl == null) {
            throw new IllegalArgumentException("Couldn't load webservice url. Aborting.");
        }

        // If this is the master, create the additional adapters if they are
        // needed
        if (isMaster) {
            createSlaveNodes();
        }
        // Create the system container
        createSystem(systemImage);
        // Create the annotation system
        systemUrl = generateSystemUrl(systemUrl);
        annotator = new AdaptedNIFBasedAnnotatorWebservice(systemUrl, "NIF-based-system");
    }

    private void createSlaveNodes() throws Exception {
        if (systemParamModel.contains(null, NIF_SYS.numberOfInstances)) {
            try {
                int numberOfInstances = Integer
                        .getInteger(RdfHelper.getStringValue(systemParamModel, null, NIF_SYS.numberOfInstances));
                if (numberOfInstances > 1) {
                    for (int i = 1; i < numberOfInstances; ++i) {
                        createSlaveNode();
                    }
                }
            } catch (NumberFormatException e) {
                LOGGER.error("Couldn't load number of instances that should be created. Assuming 1.", e);
            }
        } else {
            LOGGER.error("Couldn't load number of instances that should be created. Assuming 1.");
        }
    }

    private void createSlaveNode() throws Exception {
        String containerName = createContainer(NIF_SYSTEM_ADAPTER_DOCKER_IMAGE, Constants.CONTAINER_TYPE_SYSTEM,
                new String[] { NOT_MASTER_NODE_KEY + "=true", Constants.SYSTEM_PARAMETERS_MODEL_KEY + "="
                        + System.getenv().get(Constants.SYSTEM_PARAMETERS_MODEL_KEY) });
        if (containerName != null) {
            slaveNodes.add(containerName);
        } else {
            throw new Exception("Couldn't create slave node. Aborting.");
        }
    }

    private void createSystem(String systemImage) throws Exception {
        String containerName = createContainer(NIF_SYSTEM_ADAPTER_DOCKER_IMAGE, Constants.CONTAINER_TYPE_SYSTEM,
                new String[] { NOT_MASTER_NODE_KEY + "=true", Constants.SYSTEM_PARAMETERS_MODEL_KEY + "="
                        + System.getenv().get(Constants.SYSTEM_PARAMETERS_MODEL_KEY) });
        if (containerName != null) {
            systemContainer = containerName;
        } else {
            throw new Exception("Couldn't create slave node. Aborting.");
        }
    }

    private String generateSystemUrl(String systemUrl) {
        int pos = systemUrl.indexOf(HOST_PLACE_HOLDER);
        if (pos >= 0) {
            return systemUrl.replaceFirst(HOST_PLACE_HOLDER, systemContainer);
        } else {
            return "http://" + systemContainer + systemUrl;
        }
    }

    @Override
    public void receiveGeneratedData(byte[] data) {
        LOGGER.error("Got data that can not be processed by this adapter. It will be ignored.");
    }

    @Override
    public void receiveGeneratedTask(String taskId, byte[] data) {
        List<Document> documents = parser.parseNIF(RabbitMQUtils.readString(data));
        try {
            Document document = documents.get(0);
            documents.clear();
            documents.add(annotator.request(document));
            sendResultToEvalStorage(taskId, RabbitMQUtils.writeString(writer.writeNIF(documents)));
        } catch (Exception e) {
            LOGGER.error("Got exception while processing task.", e);
        }
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        if (command == Commands.DOCKER_CONTAINER_TERMINATED) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            String containerName = RabbitMQUtils.readString(buffer);
            int exitCode = buffer.get();
            if (!isTerminating) {
                if ((slaveNodes != null) && (slaveNodes.contains(containerName))) {
                    LOGGER.error("One of the slaves terminated with exit code {}.", exitCode);
                } else if ((systemContainer != null) && (systemContainer.equals(containerName))) {
                    LOGGER.error("The benchmarked system terminated with exit code {}. Terminating.", exitCode);
                    terminate(new Exception("The benchmarked system terminated with exit code " + exitCode));
                }
            }
        }
        super.receiveCommand(command, data);
    }

    @Override
    protected synchronized void terminate(Exception cause) {
        isTerminating = true;
        super.terminate(cause);
    }

    @Override
    public void close() throws IOException {
        // close the annotator client
        IOUtils.closeQuietly(annotator);
        // stop the annotation service
        stopContainer(systemContainer);
        super.close();
    }
}

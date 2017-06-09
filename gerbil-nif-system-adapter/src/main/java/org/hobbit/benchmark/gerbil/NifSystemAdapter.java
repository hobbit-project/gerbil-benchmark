package org.hobbit.benchmark.gerbil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.aksw.gerbil.annotator.impl.nif.AdaptedNIFBasedAnnotatorWebservice;
import org.aksw.gerbil.io.nif.impl.TurtleNIFParser;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.semantic.vocabs.NIF_SYS;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.apache.commons.io.IOUtils;
import org.hobbit.core.Commands;
import org.hobbit.core.Constants;
import org.hobbit.core.components.AbstractSystemAdapter;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.core.run.ComponentStarter;
import org.hobbit.utils.rdf.RdfHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NifSystemAdapter extends AbstractSystemAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(NifSystemAdapter.class);

    private static final String NOT_MASTER_NODE_KEY = "NOT_MASTER_NODE";
    private static final String NIF_SYSTEM_ADAPTER_DOCKER_IMAGE = "git.project-hobbit.eu:4567/gerbil/gerbilnifsystemadapter";
    private static final String HOST_PLACE_HOLDER = "HOST";

    private boolean isTerminating = false;
    private Set<String> slaveNodes = null;
    private String systemContainer = null;
    private TurtleNIFParser parser = new TurtleNIFParser();
    private TurtleNIFWriter writer = new TurtleNIFWriter();
    private AdaptedNIFBasedAnnotatorWebservice annotator;
    private Semaphore slaveTerminationSemaphore = new Semaphore(0);
    private Semaphore annotatorTerminationSemaphore = new Semaphore(0);
    /**
     * Used for debugging.
     */
    private Semaphore receivedTasksCounter = new Semaphore(0);
    /**
     * Used for debugging.
     */
    private Semaphore solvedTasksCounter = new Semaphore(0);

    public static void main(String[] args) {
        ComponentStarter.main(new String[] { NifSystemAdapter.class.getCanonicalName() });
    }

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
            slaveNodes = null;
        } else {
            slaveNodes = new HashSet<>();
        }
        // Get the system image name
        String systemImage = RdfHelper.getStringValue(systemParamModel, null, NIF_SYS.instanceImageName);
        if (systemImage == null) {
            LOGGER.warn("Couldn't load system image name. It is assumed that no system container has to be started.");
            // throw new IllegalArgumentException("Couldn't load system image
            // name. Aborting.");
        }
        String systemUrl = RdfHelper.getStringValue(systemParamModel, null, NIF_SYS.webserviceUrl);
        if (systemUrl == null) {
            throw new IllegalArgumentException("Couldn't load webservice url. Aborting.");
        }

        // If this is the master, create the additional adapters if they are
        // needed
        if (slaveNodes != null) {
            createSlaveNodes();
        }
        // Create the system container
        if (systemImage != null) {
            createSystem(systemImage);
        }
        // Create the annotation system
        systemUrl = generateSystemUrl(systemUrl);
        annotator = new AdaptedNIFBasedAnnotatorWebservice(systemUrl, "NIF-based-system");
        // Wait for annotator to work
        Document document = new DocumentImpl("This is a text document.", "http://example.org/test-doc");
        boolean noResponse = true;
        LOGGER.info("Waiting for the service to be available.");
        while (noResponse) {
            try {
                annotator.request(document);
                noResponse = false;
            } catch (Exception e) {
                Thread.sleep(1000);
            }
        }
        LOGGER.info("Service seems to be available. Initialization done.");
    }

    private void createSlaveNodes() throws Exception {
        if (systemParamModel.contains(null, NIF_SYS.numberOfInstances)) {
            try {
                int numberOfInstances = Integer
                        .parseInt(RdfHelper.getStringValue(systemParamModel, null, NIF_SYS.numberOfInstances));
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
        String containerName = createContainer(systemImage, Constants.CONTAINER_TYPE_SYSTEM,
                new String[] { NOT_MASTER_NODE_KEY + "=true", Constants.SYSTEM_PARAMETERS_MODEL_KEY + "="
                        + System.getenv().get(Constants.SYSTEM_PARAMETERS_MODEL_KEY) });
        if (containerName != null) {
            systemContainer = containerName;
        } else {
            throw new Exception("Couldn't create slave node. Aborting.");
        }
    }

    private String generateSystemUrl(String systemUrl) {
        if (systemContainer != null) {
            int pos = systemUrl.indexOf(HOST_PLACE_HOLDER);
            if (pos >= 0) {
                return systemUrl.replaceFirst(HOST_PLACE_HOLDER, systemContainer);
            } else {
                return "http://" + systemContainer + systemUrl;
            }
        } else {
            return systemUrl;
        }
    }

    @Override
    public void receiveGeneratedData(byte[] data) {
        LOGGER.error("Got data that can not be processed by this adapter. It will be ignored.");
    }

    @Override
    public void receiveGeneratedTask(String taskId, byte[] data) {
        receivedTasksCounter.release();
        List<Document> documents = parser.parseNIF(RabbitMQUtils.readString(data));
        try {
            Document document = documents.get(0);
            documents.clear();
            documents.add(annotator.request(document));
            sendResultToEvalStorage(taskId, RabbitMQUtils.writeString(writer.writeNIF(documents)));
            solvedTasksCounter.release();
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
            if ((slaveNodes != null) && (slaveNodes.contains(containerName))) {
                slaveTerminationSemaphore.release();
                LOGGER.error("One of the slaves terminated with exit code {}.", exitCode);
            } else if ((systemContainer != null) && (systemContainer.equals(containerName))) {
                LOGGER.error("The benchmarked system terminated with exit code {}. Terminating.", exitCode);
                annotatorTerminationSemaphore.release();
                if (!isTerminating) {
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
        if (slaveNodes != null) {
            // wait for the slaves to terminate (we don't have to terminate them
            // since they should do this by themselves
            try {
                slaveTerminationSemaphore.acquire(slaveNodes.size());
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted while waiting for the slave instances to terminate.");
            }
        }
        // close the annotator client
        IOUtils.closeQuietly(annotator);
        // stop the annotation service
        if (systemContainer != null) {
            stopContainer(systemContainer);
        }
        try {
            annotatorTerminationSemaphore.tryAcquire(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            // nothing to do
        }
        LOGGER.info("NIF System adapter closing after {} tasks have been received and {} tasks have been answered.",
                receivedTasksCounter.availablePermits(), solvedTasksCounter.availablePermits());
        super.close();
    }
}

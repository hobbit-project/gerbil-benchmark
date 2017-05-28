package org.hobbit.benchmark.gerbil;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import org.aksw.gerbil.dataset.impl.nif.FileBasedNIFDataset;
import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;

/**
 * Simple class that separates the given corpus into several single, smaller
 * files.
 * 
 * @author Micha
 *
 */
public class SimpleCorpusSeparator {

    private static final String INPUT_FILE = "../../BENGAL/task2.ttl";
    private static final int TASK_ID = 2;
    private static final int GENERATORS = 4;
    private static final int DOCS_PER_GENERATOR = 140;

    public static void main(String[] args) throws Exception {
        FileBasedNIFDataset dataset = new FileBasedNIFDataset(INPUT_FILE);
        dataset.init();
        List<Document> documents = dataset.getInstances();
        NIFWriter writer = new TurtleNIFWriter();
        for (int i = 0; i < GENERATORS; ++i) {
            OutputStream os = null;
            try {
                os = new FileOutputStream(TASK_ID + "task" + i + ".nif");
                writer.writeNIF(documents.subList(i * DOCS_PER_GENERATOR, (i + 1) * DOCS_PER_GENERATOR), os);
            } finally {
                os.close();
            }
        }
        dataset.close();
    }
}

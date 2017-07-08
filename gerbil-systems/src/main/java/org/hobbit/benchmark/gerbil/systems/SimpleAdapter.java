package org.hobbit.benchmark.gerbil.systems;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import org.aksw.gerbil.annotator.A2KBAnnotator;
import org.aksw.gerbil.annotator.Annotator;
import org.aksw.gerbil.annotator.AnnotatorConfiguration;
import org.aksw.gerbil.annotator.C2KBAnnotator;
import org.aksw.gerbil.annotator.D2KBAnnotator;
import org.aksw.gerbil.annotator.EntityRecognizer;
import org.aksw.gerbil.annotator.EntityTyper;
import org.aksw.gerbil.annotator.OKETask1Annotator;
import org.aksw.gerbil.annotator.OKETask2Annotator;
import org.aksw.gerbil.annotator.RT2KBAnnotator;
import org.aksw.gerbil.datatypes.ExperimentType;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.semantic.vocabs.GERBIL;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.web.config.AdapterList;
import org.aksw.gerbil.web.config.AnnotatorsConfig;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.benchmark.gerbil.adapters.GerbilSystemAdapter;
import org.hobbit.utils.rdf.RdfHelper;
import org.hobbit.vocab.HOBBIT;

public class SimpleAdapter implements HobbitAnnotator, Closeable {

    private ExperimentType experimentType;
    private Annotator annotator;

    public static SimpleAdapter create(Model systemModel) throws GerbilException {
        List<Resource> systems = RdfHelper.getSubjectResources(systemModel, RDF.type, HOBBIT.SystemInstance);
        if (systems.isEmpty()) {
            throw new IllegalArgumentException("Couldn't get any system instance definition from the model.");
        }
        if (systems.size() > 1) {
            throw new IllegalArgumentException("Got multple system instance definitions from the model.");
        }
        Resource system = systems.get(0);

        String name = RdfHelper.getStringValue(systemModel, system, GerbilSystemAdapter.name);
        if (name == null) {
            throw new IllegalArgumentException("Couldn't get the name of the annotator.");
        }

        Resource experimentTypeResource = RdfHelper.getObjectResource(systemModel, system, GERBIL.experimentType);
        ExperimentType experimentType = getExperimentTypeFromResource(experimentTypeResource);
        if (experimentType == null) {
            throw new IllegalArgumentException("Couldn't get the experiment type of the \"" + name + "\" annotator.");
        }

        AdapterList<AnnotatorConfiguration> adapterConfigs = AnnotatorsConfig.annotators();
        List<AnnotatorConfiguration> configs = adapterConfigs.getAdaptersForExperiment(experimentType);
        for (AnnotatorConfiguration config : configs) {
            if (config.getName().equals(name)) {
                Annotator annotator = config.getAnnotator(experimentType);
                return new SimpleAdapter(experimentType, annotator);
            }
        }
        throw new IllegalArgumentException("Couldn't find an annotator config fitting the given name \"" + name
                + "\" and the given experiment type \"" + experimentType + "\".");
    }

    protected SimpleAdapter(ExperimentType experimentType, Annotator annotator) {
        super();
        this.experimentType = experimentType;
        this.annotator = annotator;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Document annotate(Document document) throws GerbilException {
        switch (experimentType) {
        case Sa2KB:
        case A2KB: {
            document.getMarkings().addAll(((A2KBAnnotator) annotator).performA2KBTask(document));
            return document;
        }
        case Rc2KB:
        case Sc2KB:
        case C2KB: {
            document.getMarkings().addAll(((C2KBAnnotator) annotator).performC2KB(document));
            return document;
        }
        case D2KB: {
            document.getMarkings().addAll(((D2KBAnnotator) annotator).performD2KBTask(document));
            return document;
        }
        case ERec: {
            document.getMarkings().addAll(((EntityRecognizer) annotator).performRecognition(document));
            return document;
        }
        case ETyping: {
            document.getMarkings().addAll(((EntityTyper) annotator).performTyping(document));
            return document;
        }
        case OKE_Task1: {
            document.getMarkings().addAll(((OKETask1Annotator) annotator).performTask1(document));
            return document;
        }
        case OKE_Task2: {
            document.getMarkings().addAll(((OKETask2Annotator) annotator).performTask2(document));
            return document;
        }
        case RT2KB: {
            document.getMarkings().addAll(((RT2KBAnnotator) annotator).performRT2KBTask(document));
            return document;
        }
        default: {
            throw new IllegalArgumentException("Unknown Experiment Type " + experimentType + ".");
        }
        }
    }

    @SuppressWarnings("deprecation")
    public static ExperimentType getExperimentTypeFromResource(Resource resource) {
        if (resource == null) {
            return null;
        }
        String uri = resource.getURI();

        if (GERBIL.A2KB.getURI().equals(uri)) {
            return ExperimentType.A2KB;
        } else if (GERBIL.C2KB.getURI().equals(uri)) {
            return ExperimentType.C2KB;
        } else if (GERBIL.D2KB.getURI().equals(uri)) {
            return ExperimentType.D2KB;
        } else if (GERBIL.OKE2015_Task1.getURI().equals(uri)) {
            return ExperimentType.OKE_Task1;
        } else if (GERBIL.OKE2015_Task2.getURI().equals(uri)) {
            return ExperimentType.OKE_Task2;
        } else if (GERBIL.ERec.getURI().equals(uri)) {
            return ExperimentType.ERec;
        } else if (GERBIL.ETyping.getURI().equals(uri)) {
            return ExperimentType.ETyping;
        } else if (GERBIL.RT2KB.getURI().equals(uri)) {
            return ExperimentType.RT2KB;
        } else if (GERBIL.Sa2KB.getURI().equals(uri)) {
            return ExperimentType.Sa2KB;
        } else if (GERBIL.Sc2KB.getURI().equals(uri)) {
            return ExperimentType.Sc2KB;
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        annotator.close();
    }

}

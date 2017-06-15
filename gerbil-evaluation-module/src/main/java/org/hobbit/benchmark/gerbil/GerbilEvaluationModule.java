package org.hobbit.benchmark.gerbil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.aksw.gerbil.annotator.AnnotatorConfigurationImpl;
import org.aksw.gerbil.annotator.decorator.ErrorCountingAnnotatorDecorator;
import org.aksw.gerbil.config.GerbilConfiguration;
import org.aksw.gerbil.database.ExperimentDAO;
import org.aksw.gerbil.database.ResultNameToIdMapping;
import org.aksw.gerbil.dataset.DatasetConfigurationImpl;
import org.aksw.gerbil.datatypes.ErrorTypes;
import org.aksw.gerbil.datatypes.ExperimentTaskConfiguration;
import org.aksw.gerbil.datatypes.ExperimentTaskResult;
import org.aksw.gerbil.datatypes.ExperimentType;
import org.aksw.gerbil.evaluate.DoubleEvaluationResult;
import org.aksw.gerbil.evaluate.EvaluationResult;
import org.aksw.gerbil.evaluate.EvaluationResultContainer;
import org.aksw.gerbil.evaluate.Evaluator;
import org.aksw.gerbil.evaluate.EvaluatorFactory;
import org.aksw.gerbil.evaluate.IntEvaluationResult;
import org.aksw.gerbil.evaluate.SubTaskResult;
import org.aksw.gerbil.evaluate.impl.FMeasureCalculator;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.io.nif.NIFParser;
import org.aksw.gerbil.io.nif.impl.TurtleNIFParser;
import org.aksw.gerbil.matching.Matching;
import org.aksw.gerbil.semantic.sameas.SameAsRetriever;
import org.aksw.gerbil.semantic.sameas.SameAsRetrieverUtils;
import org.aksw.gerbil.semantic.subclass.SimpleSubClassInferencer;
import org.aksw.gerbil.semantic.subclass.SubClassInferencer;
import org.aksw.gerbil.semantic.vocabs.GERBIL;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.Meaning;
import org.aksw.gerbil.transfer.nif.MeaningSpan;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.TypedSpan;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.TypedNamedEntity;
import org.aksw.gerbil.web.config.RootConfig;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.hobbit.core.components.AbstractEvaluationModule;
import org.hobbit.core.rabbit.RabbitMQUtils;
import org.hobbit.gerbil.commons.GERBIL2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.LongArrayList;

public class GerbilEvaluationModule extends AbstractEvaluationModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(GerbilEvaluationModule.class);

    public static final String EXPERIMENT_TYPE_KEY = "gerbil.experimentType";

    private static final String HTTP_SAME_AS_RETRIEVAL_DOMAIN_KEY = "org.aksw.gerbil.semantic.sameas.impl.http.HTTPBasedSameAsRetriever.domain";
    private static final String WIKIPEDIA_BASED_SAME_AS_RETRIEVAL_DOMAIN_KEY = "org.aksw.gerbil.semantic.sameas.impl.wiki.WikipediaApiBasedSingleUriSameAsRetriever.domain";
    private static final String SAME_AS_CACHE_FILE_KEY = "org.aksw.gerbil.semantic.sameas.CachingSameAsRetriever.cacheFile";

    protected NIFParser reader = new TurtleNIFParser();
    private List<Document> expectedDocuments = new ArrayList<Document>();
    private List<Document> receivedDocuments = new ArrayList<Document>();
    // private Evaluator<? extends Marking> evaluator;
    private List<Evaluator<?>> evaluators = new ArrayList<Evaluator<?>>();
    protected ExperimentType type;
    protected Matching matching;
    protected LongArrayList runtimes = new LongArrayList();
    protected int errorCount = 0;

    private SameAsRetriever globalRetriever;

    public static final String IS_BENGAL = "IS_BENGAL";
    public static final String PHASES = "PHASES";
    public static final String COUNT = "DOCS_PER_PHASE";
    protected boolean isBengal = false;

    private List<StressTestDocumentResult> stressTestData = new ArrayList<StressTestDocumentResult>();

    protected int phases = 0;
    protected int docsPerPhase = 0;
    protected StressTestPhaseResult phaseResults[];
    protected StressTestPhaseResult overallStressResult;

    @Override
    public void init() throws Exception {
        super.init();

        type = null;
        if (System.getenv().containsKey(EXPERIMENT_TYPE_KEY)) {
            try {
                type = ExperimentType.valueOf(System.getenv().get(EXPERIMENT_TYPE_KEY));
            } catch (Exception e) {
                String errorMsg = "Couldn't parse experiment type. Aborting.";
                LOGGER.error(errorMsg, e);
                throw new Exception(errorMsg, e);
            }
        }
        if (type == null) {
            String errorMsg = "Couldn't get the experiment type. Aborting.";
            LOGGER.error(errorMsg);
            throw new Exception(errorMsg);
        }
        if (System.getenv().containsKey(IS_BENGAL)) {
            isBengal = Boolean.valueOf(System.getenv(IS_BENGAL));
        }
        if (isBengal) {
            phases = Integer.parseInt(System.getenv(PHASES));
            docsPerPhase = Integer.parseInt(System.getenv(COUNT));
        }

        generateMatcher();

        generateEvaluators();

        generateRetriever();
    }

    protected void generateRetriever() {
        LOGGER.info("Reconfiguring sameAs retrieval...");
        // Remove all domains for HTTP based same as retrieval
        GerbilConfiguration.getInstance().setProperty(HTTP_SAME_AS_RETRIEVAL_DOMAIN_KEY, new String[0]);

        // Remove all domains for Wikipedia based same as retrieval
        GerbilConfiguration.getInstance().setProperty(WIKIPEDIA_BASED_SAME_AS_RETRIEVAL_DOMAIN_KEY, new String[0]);
        // Remove the usage of cache files
        GerbilConfiguration.getInstance().clearProperty(SAME_AS_CACHE_FILE_KEY);
        // FIXME write the createSameAsRetriever method in another Class and
        // just use it in the RootConfig
        globalRetriever = RootConfig.createSameAsRetriever();
    }

    @SuppressWarnings("deprecation")
    protected void generateMatcher() {
        switch (type) {
        case A2KB:
        case ERec:
        case ETyping:
        case OKE_Task1:
        case OKE_Task2:
        case Sa2KB:
            matching = Matching.WEAK_ANNOTATION_MATCH;
            break;
        case D2KB:
        case C2KB:
        case RT2KB:
        case Sc2KB:
        case Rc2KB:
            matching = Matching.STRONG_ENTITY_MATCH;
            break;
        }
    }

    protected void generateEvaluators() {
        Model model = ModelFactory.createDefaultModel();
        try {

            model.read(new FileInputStream("Musicbrainz.ttl"), "http://purl.org/ontology/", "TTL");
            model.read(new FileInputStream("DBpediaTypes.ttl"), "http://dbpedia.org/ontology/", "TTL");
        } catch (FileNotFoundException e) {
            LOGGER.error("Could not open Type hierarchies");
        }
        SubClassInferencer inferencer = new SimpleSubClassInferencer(model);
        EvaluatorFactory factory = new EvaluatorFactory(inferencer);
        // evaluator = factory.createEvaluator(type, new
        // ExperimentTaskConfiguration(null, null, type, matching), null);
        // evaluators.add(evaluator);
        factory.addEvaluators(evaluators, new ExperimentTaskConfiguration(null, null, type, matching), null);
    }

    @Override
    protected void evaluateResponse(byte[] expectedData, byte[] receivedData, long taskSentTimestamp,
            long responseReceivedTimestamp) throws Exception {
        Document receivedDocument = new DocumentImpl();
        Document expectedDocument = new DocumentImpl();
        if (receivedData == null || receivedData.length == 0) {
            this.errorCount++;
            receivedData = new byte[0];
        } else {
            runtimes.add(responseReceivedTimestamp - taskSentTimestamp);
            receivedDocument = parseDocument(receivedData);
        }
        if (expectedData == null || expectedData.length == 0) {
            expectedData = new byte[0];

        } else {
            expectedDocument = parseDocument(expectedData);

        }

        if (expectedData.length > 0) {
            SameAsRetrieverUtils.addSameURIsToMarkings(globalRetriever, expectedDocument.getMarkings());
        }
        if (receivedData.length > 0) {
            SameAsRetrieverUtils.addSameURIsToMarkings(globalRetriever, receivedDocument.getMarkings());
        }
        expectedDocuments.add(expectedDocument);
        receivedDocuments.add(receivedDocument);
        LOGGER.info("exp: " + expectedDocument);
        LOGGER.info("recv" + receivedDocument);
        if (isBengal) {
            if (receivedData.length == 0) {
                stressTestData
                        .add(new StressTestDocumentResult(taskSentTimestamp, responseReceivedTimestamp, true, 0.0));
            } else {
                EvaluationResult evalResult = evaluate(Arrays.asList(expectedDocument),
                        Arrays.asList(receivedDocument));
                ExperimentTaskResult expResult = new ExperimentTaskResult("", "", type, matching, new double[6],
                        ExperimentDAO.TASK_FINISHED, 0, System.currentTimeMillis());
                transformResults(evalResult, expResult);
                System.out.println(Arrays.toString(expResult.results));
                stressTestData.add(new StressTestDocumentResult(taskSentTimestamp, responseReceivedTimestamp, false,
                        expResult.getMicroF1Measure()));
            }
        }
    }

    @Override
    protected Model summarizeEvaluation() throws Exception {
        LOGGER.info("expected: " + this.expectedDocuments.toString());
        LOGGER.info("received: " + this.receivedDocuments.toString());

        EvaluationResult evalResult = evaluate(expectedDocuments, receivedDocuments);

        ExperimentTaskResult expResult = new ExperimentTaskResult("", "", type, matching, new double[6],
                ExperimentDAO.TASK_FINISHED, 0, System.currentTimeMillis());
        transformResults(evalResult, expResult);

        if (isBengal) {
            // sort the received data ascending
            Collections.sort(stressTestData);
            phaseResults = new StressTestPhaseResult[phases];
            int count = 0;
            int phase = 0;
            long duration = 0;
            long durationSum = 0;
            long overallDurationSum = 0;
            double f1ScoreSumPhase = 0;
            double overallF1ScoreSum = 0;
            int errorCount = 0;
            for (StressTestDocumentResult r : stressTestData) {
                ++count;
                if (r.error) {
                    ++errorCount;
                } else {
                    duration = r.endTime - r.startTime;
                    if(duration < 0) {
                        LOGGER.error("Got a negative duration. It will be set to 0.");
                    } else {
                        durationSum += duration;
                        overallDurationSum += duration;
                    }
                }
                f1ScoreSumPhase += r.f1;
                overallF1ScoreSum += r.f1;
                if (count == docsPerPhase) {
                    phaseResults[phase] = createPhaseResult(durationSum, f1ScoreSumPhase, errorCount);
                    count = 0;
                    ++phase;
                    f1ScoreSumPhase = 0;
                    durationSum = 0;
                    errorCount = 0;
                }
            }
            if (count > 0) {
                LOGGER.warn("Got an unfinished phase #{} (count == {} while {} docs per phase have been expected)",
                        phase, count, docsPerPhase);
                phaseResults[phase] = createPhaseResult(durationSum, f1ScoreSumPhase, errorCount);
            }
            overallStressResult = createPhaseResult(overallDurationSum, overallF1ScoreSum, 0);
        }

        // DataIDGenerator generator = new
        // DataIDGenerator("http://example.org/MyGerbilBenchmark/");
        // Model model = generator.generateDataIDModel();
        // generator.addToModel(model, Arrays.asList(expResult), experimentUri);
        // model.add(model.getResource(experimentUri), RDF.type,
        // HOBBIT.Experiment);
        return generateModel(expResult);
    }

    private StressTestPhaseResult createPhaseResult(long durationSum, double f1ScoreSumPhase, int errors) {
        double beta = 0;
        if (durationSum == 0) {
            if (f1ScoreSumPhase > 0) {
                LOGGER.error("Got a duration of 0. Setting it to 1ms.");
                durationSum = 1;
            }
        }
        if (durationSum > 0) {
            beta = (f1ScoreSumPhase * 1000d) / durationSum;
        }
        return new StressTestPhaseResult(durationSum, f1ScoreSumPhase, beta, errors);
    }

    @SuppressWarnings("deprecation")
    private EvaluationResult evaluate(List<Document> expectedDocuments, List<Document> receivedDocuments)
            throws GerbilException {
        EvaluationResult evalResult = null;
        switch (type) {
        case D2KB: {
            evalResult = evaluate(evaluators, getMarkings(receivedDocuments, MeaningSpan.class),
                    getMarkings(expectedDocuments, MeaningSpan.class));
            break;
        }
        case Sa2KB:
        case A2KB: {
            evalResult = evaluate(evaluators, getMarkings(receivedDocuments, MeaningSpan.class),
                    getMarkings(expectedDocuments, MeaningSpan.class));
            break;
        }
        case C2KB: {
            evalResult = evaluate(evaluators, getMarkings(receivedDocuments, Meaning.class),
                    getMarkings(expectedDocuments, Meaning.class));
            break;
        }
        case Sc2KB: // Falls through
        case Rc2KB: {
            throw new GerbilException(ErrorTypes.UNEXPECTED_EXCEPTION);
        }
        case ERec: {
            evalResult = evaluate(evaluators, getMarkings(receivedDocuments, Span.class),
                    getMarkings(expectedDocuments, Span.class));
            break;
        }
        case ETyping: {
            evalResult = evaluate(evaluators, getMarkings(receivedDocuments, TypedSpan.class),
                    getMarkings(expectedDocuments, TypedSpan.class));
            break;
        }
        case OKE_Task1: {
            evalResult = evaluate(evaluators, getMarkings(receivedDocuments, TypedNamedEntity.class),
                    getMarkings(expectedDocuments, TypedNamedEntity.class));
            break;
        }
        case OKE_Task2: {
            evalResult = evaluate(evaluators, getMarkings(receivedDocuments, TypedNamedEntity.class),
                    getMarkings(expectedDocuments, TypedNamedEntity.class));
            break;
        }
        case RT2KB: {
            evalResult = evaluate(evaluators, getMarkings(receivedDocuments, TypedSpan.class),
                    getMarkings(expectedDocuments, TypedSpan.class));
            break;
        }
        default:
            throw new GerbilException("This experiment type isn't implemented yet. Sorry for this.",
                    ErrorTypes.UNEXPECTED_EXCEPTION);
        }
        return evalResult;
    }

    protected <T extends Marking> List<List<T>> getMarkings(List<Document> documents, Class<T> clazz) {
        List<List<T>> markings = new ArrayList<List<T>>(documents.size());

        for (Document document : documents) {
            if (document != null) {
                markings.add(document.getMarkings(clazz));
            } else {
                markings.add(new ArrayList<T>());
            }
        }
        return markings;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Marking> EvaluationResult evaluate(List<Evaluator<? extends Marking>> evaluators,
            List<List<T>> annotatorResults, List<List<T>> goldStandard) {

        EvaluationResultContainer evalResults = new EvaluationResultContainer();
        for (Evaluator<? extends Marking> e : evaluators) {
            ((Evaluator<T>) e).evaluate(annotatorResults, goldStandard, evalResults);
        }
        return evalResults;
    }

    protected Document parseDocument(byte[] data) {
        // parse the document from the given byte array
        List<Document> documents = reader.parseNIF(RabbitMQUtils.readString(data));
        // If at least one document could be parsed
        if ((documents != null) && (documents.size() > 0)) {
            // If there is more than one document
            if (documents.size() != 1) {
                StringBuilder builder = new StringBuilder();
                builder.append("Got more than one document in a single response [");
                boolean first = true;
                for (int i = 0; i < documents.size(); ++i) {
                    if (first) {
                        first = false;
                    } else {
                        builder.append(", ");
                    }
                    builder.append(documents.get(i).getDocumentURI());
                }
                builder.append("]. Only the first document is used.");
                LOGGER.warn(builder.toString());
            }
            return documents.get(0);
        }
        return null;
    }

    protected void transformResults(EvaluationResult result, ExperimentTaskResult expResult) {
        if (result instanceof SubTaskResult) {
            double[] arr = new double[6];
            SubTaskResult subtask = (SubTaskResult) result;
            ExperimentTaskConfiguration config = subtask.getConfiguration();
            config.annotatorConfig = new AnnotatorConfigurationImpl("...", true, null, null, type);
            config.datasetConfig = new DatasetConfigurationImpl("...", true, null, null, type, null, globalRetriever);

            ExperimentTaskResult subTask = new ExperimentTaskResult(config, arr, ExperimentDAO.TASK_FINISHED, 0);
            List<EvaluationResult> tempResults = ((EvaluationResultContainer) result).getResults();
            for (EvaluationResult tempResult : tempResults) {
                transformResults(tempResult, subTask);
            }
            expResult.addSubTask(subTask);
        } else if (result instanceof EvaluationResultContainer) {
            List<EvaluationResult> tempResults = ((EvaluationResultContainer) result).getResults();
            for (EvaluationResult tempResult : tempResults) {
                transformResults(tempResult, expResult);
            }
        } else if (result instanceof DoubleEvaluationResult) {
            switch (result.getName()) {
            case FMeasureCalculator.MACRO_F1_SCORE_NAME: {
                expResult.results[ExperimentTaskResult.MACRO_F1_MEASURE_INDEX] = ((DoubleEvaluationResult) result)
                        .getValueAsDouble();
                return;
            }
            case FMeasureCalculator.MACRO_PRECISION_NAME: {
                expResult.results[ExperimentTaskResult.MACRO_PRECISION_INDEX] = ((DoubleEvaluationResult) result)
                        .getValueAsDouble();
                return;
            }
            case FMeasureCalculator.MACRO_RECALL_NAME: {
                expResult.results[ExperimentTaskResult.MACRO_RECALL_INDEX] = ((DoubleEvaluationResult) result)
                        .getValueAsDouble();
                return;
            }
            case FMeasureCalculator.MICRO_F1_SCORE_NAME: {
                expResult.results[ExperimentTaskResult.MICRO_F1_MEASURE_INDEX] = ((DoubleEvaluationResult) result)
                        .getValueAsDouble();
                return;
            }
            case FMeasureCalculator.MICRO_PRECISION_NAME: {
                expResult.results[ExperimentTaskResult.MICRO_PRECISION_INDEX] = ((DoubleEvaluationResult) result)
                        .getValueAsDouble();
                return;
            }
            case FMeasureCalculator.MICRO_RECALL_NAME: {
                expResult.results[ExperimentTaskResult.MICRO_RECALL_INDEX] = ((DoubleEvaluationResult) result)
                        .getValueAsDouble();
                return;
            }
            default: {
                int id = ResultNameToIdMapping.getInstance().getResultId(result.getName());
                if (id == ResultNameToIdMapping.UKNOWN_RESULT_TYPE) {
                    LOGGER.error("Got an unknown additional result \"" + result.getName() + "\". Discarding it.");
                } else {
                    expResult.addAdditionalResult(id, ((DoubleEvaluationResult) result).getValueAsDouble());
                }
            }
            }
            return;
        } else if (result instanceof IntEvaluationResult) {
            if (result.getName().equals(ErrorCountingAnnotatorDecorator.ERROR_COUNT_RESULT_NAME)) {
                expResult.errorCount = errorCount;// ((IntEvaluationResult)
                                                  // result).getValueAsInt();
                return;
            }
            int id = ResultNameToIdMapping.getInstance().getResultId(result.getName());
            if (id == ResultNameToIdMapping.UKNOWN_RESULT_TYPE) {
                LOGGER.error("Got an unknown additional result \"" + result.getName() + "\". Discarding it.");
            } else {
                expResult.addAdditionalResult(id, ((IntEvaluationResult) result).getValueAsInt());
            }
        }
    }

    protected Model generateModel(ExperimentTaskResult result) {
        Model model = createDefaultModel();
        Resource experiment = model.getResource(experimentUri);

        model.addLiteral(experiment, GERBIL.macroPrecision, result.getMacroPrecision());
        model.addLiteral(experiment, GERBIL.macroRecall, result.getMacroRecall());
        model.addLiteral(experiment, GERBIL.macroF1, result.getMacroF1Measure());
        model.addLiteral(experiment, GERBIL.microPrecision, result.getMicroPrecision());
        model.addLiteral(experiment, GERBIL.microRecall, result.getMicroRecall());
        model.addLiteral(experiment, GERBIL.microF1, result.getMicroF1Measure());
        model.addLiteral(experiment, GERBIL.errorCount, result.errorCount + this.errorCount);

        if (result.getNumberOfSubTasks() > 0) {
            for (ExperimentTaskResult subResult : result.getSubTasks()) {
                Resource subRes = model.getResource(experimentUri + "_" + subResult.type.getName());

                model.add(subRes, GERBIL.subExperimentOf, experiment);
                model.addLiteral(subRes, GERBIL.macroPrecision, subResult.getMacroPrecision());
                model.addLiteral(subRes, GERBIL.macroRecall, subResult.getMacroRecall());
                model.addLiteral(subRes, GERBIL.macroF1, subResult.getMacroF1Measure());
                model.addLiteral(subRes, GERBIL.microPrecision, subResult.getMicroPrecision());
                model.addLiteral(subRes, GERBIL.microRecall, subResult.getMicroRecall());
                model.addLiteral(subRes, GERBIL.microF1, subResult.getMicroF1Measure());
                model.addLiteral(subRes, GERBIL.errorCount, subResult.errorCount + this.errorCount);
                model.add(subRes, GERBIL.experimentType, GERBIL.getExperimentTypeResource(subResult.type));

            }
        }

        long durationSum = 0;
        double avgMillisPerDoc = 0;
        if (runtimes.buffer.length > 0) {
            for (int i = 0; i < runtimes.elementsCount; ++i) {
                durationSum += runtimes.buffer[i];
            }
            avgMillisPerDoc = durationSum / (double) runtimes.elementsCount;
        }
        model.addLiteral(experiment, model.getProperty(GERBIL.getURI() + "avgMillisPerDoc"), avgMillisPerDoc);

        if (isBengal) {
            for (int i = 0; i < phases; i++) {
                if (phaseResults[i] != null) {
                    Resource phase = GERBIL2.getPhaseResource(experimentUri, i);
                    model.add(experiment, GERBIL2.hasPhase, phase);
                    model.addLiteral(phase, GERBIL2.duration, phaseResults[i].durationSum);
                    model.addLiteral(phase, GERBIL2.f1ScorePoints, phaseResults[i].f1ScoreSum);
                    model.addLiteral(phase, GERBIL2.beta, phaseResults[i].beta);
                    model.addLiteral(phase, GERBIL.errorCount, phaseResults[i].errors);
                }
            }
            if (overallStressResult != null) {
                model.addLiteral(experiment, GERBIL2.duration, overallStressResult.durationSum);
                model.addLiteral(experiment, GERBIL2.f1ScorePoints, overallStressResult.f1ScoreSum);
                model.addLiteral(experiment, GERBIL2.beta, overallStressResult.beta);
                // we don't have to add error here
            }
        }

        // TODO add handling of additional results
        // TODO add handling of sub experiments

        return model;
    }
}

package org.hobbit.benchmark.gerbil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.aksw.gerbil.annotator.decorator.ErrorCountingAnnotatorDecorator;
import org.aksw.gerbil.database.ExperimentDAO;
import org.aksw.gerbil.database.ResultNameToIdMapping;
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
import org.aksw.gerbil.semantic.vocabs.GERBIL;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.Meaning;
import org.aksw.gerbil.transfer.nif.MeaningSpan;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.TypedSpan;
import org.aksw.gerbil.transfer.nif.data.TypedNamedEntity;
import org.aksw.gerbil.web.config.RootConfig;
import org.apache.jena.rdf.model.Model;
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

    private static final String GERBIL2_PREFIX = "http://w3id.org/gerbil/hobbit/vocab#";

    protected NIFParser reader = new TurtleNIFParser();
    private List<Document> expectedDocuments = new ArrayList<Document>();
    private List<Document> receivedDocuments = new ArrayList<Document>();
    private Evaluator<? extends Marking> evaluator;
    private List<Evaluator<? extends Marking>> evaluators;
    protected ExperimentType type;
    protected Matching matching;
    protected LongArrayList runtimes = new LongArrayList();
    protected int errorCount = 0;

    private SameAsRetriever globalRetriever;

    private static final String IS_BENGAL = "IS_BENGAL";
    private static final String PHASES = "PHASES";
    private static final String COUNT = "DOCS_PER_PHASE";
    protected boolean isBengal = false;

    private SortedSet<StressTestDocumentResult> stressTestData = new TreeSet<StressTestDocumentResult>();

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
            matching = Matching.WEAK_ANNOTATION_MATCH;
            break;
        case D2KB:
        case C2KB:
        case Rc2KB:
        case Sa2KB:
        case Sc2KB:
            matching = Matching.STRONG_ENTITY_MATCH;
            break;

        }
    }

    @SuppressWarnings("unchecked")
    protected void generateEvaluators() {
        EvaluatorFactory factory = new EvaluatorFactory();
        evaluator = factory.createEvaluator(type, new ExperimentTaskConfiguration(null, null, type, matching), null);
    }

    @Override
    protected void evaluateResponse(byte[] expectedData, byte[] receivedData, long taskSentTimestamp,
            long responseReceivedTimestamp) throws Exception {
        if (receivedData.length == 0) {
            this.errorCount++;
        } else {
            runtimes.add(responseReceivedTimestamp - taskSentTimestamp);
        }
        Document expectedDocument = parseDocument(expectedData);

        Document receivedDocument = parseDocument(receivedData);
        if (expectedData.length > 0) {
            SameAsRetrieverUtils.addSameURIsToMarkings(globalRetriever, expectedDocument.getMarkings());
        }
        if (receivedData.length > 0) {
            SameAsRetrieverUtils.addSameURIsToMarkings(globalRetriever, receivedDocument.getMarkings());
        }

        expectedDocuments.add(expectedDocument);
        receivedDocuments.add(receivedDocument);
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
        EvaluationResult evalResult = evaluate(expectedDocuments, receivedDocuments);

        LOGGER.info("results=" + evalResult.toString());
        ExperimentTaskResult expResult = new ExperimentTaskResult("", "", type, matching, new double[6],
                ExperimentDAO.TASK_FINISHED, 0, System.currentTimeMillis());
        transformResults(evalResult, expResult);

        if (isBengal) {
            phaseResults = new StressTestPhaseResult[phases];
            int count = 0;
            int phase = 0;
            long startOfPhase = 0;
            long endOfPhase = Long.MIN_VALUE;
            long endOfTest = Long.MIN_VALUE;
            double f1ScoreSumPhase = 0;
            double overallF1ScoreSum = 0;
            int errorCount = 0;
            for (StressTestDocumentResult r : stressTestData) {
                if (count == 0) {
                    startOfPhase = r.startTime;
                }
                ++count;
                if (r.error) {
                    ++errorCount;
                } else {
                    if (r.endTime > endOfPhase) {
                        endOfPhase = r.endTime;
                    }
                    if (r.endTime > endOfTest) {
                        endOfTest = r.endTime;
                    }
                }
                f1ScoreSumPhase += r.f1;
                overallF1ScoreSum += r.f1;
                if (count == docsPerPhase) {
                    phaseResults[phase] = createPhaseResult(startOfPhase, endOfPhase, f1ScoreSumPhase, errorCount);
                    count = 0;
                    ++phase;
                    f1ScoreSumPhase = 0;
                    endOfPhase = Long.MIN_VALUE;
                    errorCount = 0;
                }
            }
            if (count > 0) {
                LOGGER.warn("Got an unfinished phase #{} (count == {} while {} docs per phase have been expected)",
                        phase, count, docsPerPhase);
                phaseResults[phase] = createPhaseResult(startOfPhase, endOfPhase, f1ScoreSumPhase, errorCount);
            }
            overallStressResult = createPhaseResult(stressTestData.first().startTime, endOfTest, overallF1ScoreSum, 0);
        }

        // DataIDGenerator generator = new
        // DataIDGenerator("http://example.org/MyGerbilBenchmark/");
        // Model model = generator.generateDataIDModel();
        // generator.addToModel(model, Arrays.asList(expResult), experimentUri);
        // model.add(model.getResource(experimentUri), RDF.type,
        // HOBBIT.Experiment);
        return generateModel(expResult);
    }

    private StressTestPhaseResult createPhaseResult(long startOfPhase, long endOfPhase, double f1ScoreSumPhase,
            int errors) {
        long duration = 0;
        double beta = 0;
        if (endOfPhase > 0) {
            duration = endOfPhase - startOfPhase;
            if (duration == 0) {
                LOGGER.error("Got a duration of 0. Setting it to 1ms.");
                duration = 1;
            }
            beta = (f1ScoreSumPhase * 1000d) / duration;
        }
        return new StressTestPhaseResult(duration, f1ScoreSumPhase, beta, errors);
    }

    private EvaluationResult evaluate(List<Document> expectedDocuments, List<Document> receivedDocuments)
            throws GerbilException {
        EvaluationResult evalResult = null;
        switch (type) {
        case D2KB: {
            evalResult = evaluate(Arrays.asList(evaluator), getMarkings(receivedDocuments, MeaningSpan.class),
                    getMarkings(expectedDocuments, MeaningSpan.class));
            break;
        }
        case Sa2KB:
        case A2KB: {
            evalResult = evaluate(Arrays.asList(evaluator), getMarkings(receivedDocuments, MeaningSpan.class),
                    getMarkings(expectedDocuments, MeaningSpan.class));
            break;
        }
        case C2KB: {
            evalResult = evaluate(Arrays.asList(evaluator), getMarkings(receivedDocuments, Meaning.class),
                    getMarkings(expectedDocuments, Meaning.class));
            break;
        }
        case Sc2KB: // Falls through
        case Rc2KB: {
            throw new GerbilException(ErrorTypes.UNEXPECTED_EXCEPTION);
        }
        case ERec: {
            evalResult = evaluate(Arrays.asList(evaluator), getMarkings(receivedDocuments, Span.class),
                    getMarkings(expectedDocuments, Span.class));
            break;
        }
        case ETyping: {
            evalResult = evaluate(Arrays.asList(evaluator), getMarkings(receivedDocuments, TypedSpan.class),
                    getMarkings(expectedDocuments, TypedSpan.class));
            break;
        }
        case OKE_Task1: {
            evalResult = evaluate(Arrays.asList(evaluator), getMarkings(receivedDocuments, TypedNamedEntity.class),
                    getMarkings(expectedDocuments, TypedNamedEntity.class));
            break;
        }
        case OKE_Task2: {
            evalResult = evaluate(Arrays.asList(evaluator), getMarkings(receivedDocuments, TypedNamedEntity.class),
                    getMarkings(expectedDocuments, TypedNamedEntity.class));
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
            ExperimentTaskResult subTask = new ExperimentTaskResult(((SubTaskResult) result).getConfiguration(),
                    new double[6], ExperimentDAO.TASK_FINISHED, 0);
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
                expResult.errorCount = ((IntEvaluationResult) result).getValueAsInt();
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
                    model.addLiteral(phase, GERBIL2.duration, phaseResults[i].duration);
                    model.addLiteral(phase, GERBIL2.f1ScorePoints, phaseResults[i].f1ScoreSum);
                    model.addLiteral(phase, GERBIL2.beta, phaseResults[i].beta);
                    model.addLiteral(phase, GERBIL.errorCount, phaseResults[i].errors);
                }
            }
            if (overallStressResult != null) {
                model.addLiteral(experiment, GERBIL2.duration, overallStressResult.duration);
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

package org.hobbit.benchmark.gerbil;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.aksw.gerbil.datatypes.ExperimentType;
import org.aksw.gerbil.semantic.vocabs.GERBIL;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.hobbit.gerbil.commons.GERBIL2;
import org.hobbit.utils.rdf.RdfHelper;
import org.junit.Assert;
import org.junit.Test;

public class GerbilEvaluationModuleTest extends GerbilEvaluationModule {
    
	@Test
	public void checkErrorCountTest() throws Exception{
		GerbilEvaluationModuleTest eval = new GerbilEvaluationModuleTest();
		eval.type=ExperimentType.A2KB;
		eval.generateMatcher();
		eval.generateEvaluators();
		eval.generateRetriever();
		
		byte[] expectedData=null;
		byte[] receivedData=new byte[0];
		expectedData = FileUtils.readFileToByteArray(new File("src/test/resources/test.nif"));
		//As receivedData has the size of 0 it should count 1 errors here
		eval.evaluateResponse(expectedData, receivedData, 0L, 0L);
		eval.evaluateResponse(expectedData, expectedData, 0L, 0L);
		Model m = eval.summarizeEvaluation();
		NodeIterator it = m.listObjectsOfProperty(GERBIL.errorCount);
		assertTrue(it.next().asLiteral().getInt()==1);
	}

    @Test
    public void checkPhasesTest() throws Exception{
        this.type=ExperimentType.A2KB;
        this.experimentUri = "http://example.org/exp";
        this.isBengal = true;
        this.generateMatcher();
        this.generateEvaluators();
        this.generateRetriever();
        
        this.phases = 3;
        this.docsPerPhase = 2;

        byte[] file1 = FileUtils.readFileToByteArray(new File("src/test/resources/test.nif"));
        byte[] file2 = FileUtils.readFileToByteArray(new File("src/test/resources/test2.nif"));
        byte[] emptyResonse=new byte[0];
        // Phase 0
        this.evaluateResponse(file1, file1, 1L, 11L);
        this.evaluateResponse(file2, file2, 11L, 21L);
        // F1 sum=2.0
        // duration = 20
        // beta = 2.0 * 1000 / 20 = 100
        // errors = 0

        // Phase 1
        this.evaluateResponse(file1, file2, 103L, 151L);
        this.evaluateResponse(file2, file1, 101L, 201L);
        // F1 sum=0.0
        // duration = 100
        // beta = 0.0
        // errors = 0

        // Phase 2
        this.evaluateResponse(file2, file2, 299L, 301L);
        this.evaluateResponse(file1, emptyResonse, 201L, 0L);
        // F1 sum=1.0
        // duration = 100
        // beta = 10
        // errors = 1
        
        // overall
        // F1 sum = 3.0
        // duration = 300
        // beta = 3 * 1000 / 300 = 10
        // errors = 1
        
        Model m = this.summarizeEvaluation();
        String modelString = m.toString();
        Resource phase;
        
        phase = GERBIL2.getPhaseResource(experimentUri, 0);
        Assert.assertNotNull("Phase 0 missing in " + modelString, RdfHelper.getStringValue(m, phase, GERBIL2.duration));
        Assert.assertEquals(20, Long.parseLong(RdfHelper.getStringValue(m, phase, GERBIL2.duration)));
        Assert.assertEquals(2.0, Double.parseDouble(RdfHelper.getStringValue(m, phase, GERBIL2.f1ScorePoints)), 0.0001);
        Assert.assertEquals(100.0, Double.parseDouble(RdfHelper.getStringValue(m, phase, GERBIL2.beta)), 0.0001);
        Assert.assertEquals(0, Long.parseLong(RdfHelper.getStringValue(m, phase, GERBIL.errorCount)));

        phase = GERBIL2.getPhaseResource(experimentUri, 1);
        Assert.assertNotNull("Phase 1 missing in " + modelString, RdfHelper.getStringValue(m, phase, GERBIL2.duration));
        Assert.assertEquals(100, Long.parseLong(RdfHelper.getStringValue(m, phase, GERBIL2.duration)));
        Assert.assertEquals(0.0, Double.parseDouble(RdfHelper.getStringValue(m, phase, GERBIL2.f1ScorePoints)), 0.0001);
        Assert.assertEquals(0.0, Double.parseDouble(RdfHelper.getStringValue(m, phase, GERBIL2.beta)), 0.0001);
        Assert.assertEquals(0, Long.parseLong(RdfHelper.getStringValue(m, phase, GERBIL.errorCount)));

        phase = GERBIL2.getPhaseResource(experimentUri,2);
        Assert.assertNotNull("Phase 2 missing in " + modelString, RdfHelper.getStringValue(m, phase, GERBIL2.duration));
        Assert.assertEquals(100, Long.parseLong(RdfHelper.getStringValue(m, phase, GERBIL2.duration)));
        Assert.assertEquals(1.0, Double.parseDouble(RdfHelper.getStringValue(m, phase, GERBIL2.f1ScorePoints)), 0.0001);
        Assert.assertEquals(10.0, Double.parseDouble(RdfHelper.getStringValue(m, phase, GERBIL2.beta)), 0.0001);
        Assert.assertEquals(1, Long.parseLong(RdfHelper.getStringValue(m, phase, GERBIL.errorCount)));

        phase = m.getResource(experimentUri);
        Assert.assertEquals(300, Long.parseLong(RdfHelper.getStringValue(m, phase, GERBIL2.duration)));
        Assert.assertEquals(3.0, Double.parseDouble(RdfHelper.getStringValue(m, phase, GERBIL2.f1ScorePoints)), 0.0001);
        Assert.assertEquals(10.0, Double.parseDouble(RdfHelper.getStringValue(m, phase, GERBIL2.beta)), 0.0001);
        Assert.assertEquals(1, Long.parseLong(RdfHelper.getStringValue(m, phase, GERBIL.errorCount)));

        Assert.assertEquals(10.0, Double.parseDouble(RdfHelper.getStringValue(m, phase, m.getProperty(GERBIL.getURI() + "avgMillisPerDoc"))), 0.0001);
    }
}

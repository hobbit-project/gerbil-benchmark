package org.hobbit.benchmark.gerbil;

import static org.junit.Assert.*;

import java.io.File;

import org.aksw.gerbil.datatypes.ExperimentType;
import org.aksw.gerbil.semantic.vocabs.GERBIL;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
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
}

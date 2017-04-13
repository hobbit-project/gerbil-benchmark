package org.hobbit.benchmark.gerbil;

import org.aksw.gerbil.datatypes.ExperimentType;
import org.aksw.gerbil.exceptions.GerbilException;
import org.hobbit.benchmark.gerbil.GerbilDataGenerator;
import org.junit.Test;

public class GerbilDataGeneratorTest {

    @Test
    public void test() throws GerbilException{
	GerbilDataGenerator gdg = new GerbilDataGenerator();
	gdg.createConfigurations("Senseval 2", ExperimentType.ERec);
	gdg.testData();
    }

}

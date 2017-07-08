package org.hobbit.benchmark.gerbil.systems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.hobbit.vocab.HOBBIT;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SimpleAdapterCreationTest {

    @Parameters
    public static Collection<Object[]> data() {
        Model systems = ModelFactory.createDefaultModel();
        systems.read("system.ttl");

        List<Object[]> testConfigs = new ArrayList<Object[]>();

        ResIterator systemIterator = systems.listSubjectsWithProperty(RDF.type, HOBBIT.SystemInstance);
        Resource system;
        while (systemIterator.hasNext()) {
            system = systemIterator.next();
            Model systemModel = ModelFactory.createDefaultModel();
            systemModel.add(systems.listStatements(system, null, (RDFNode) null));
            testConfigs.add(new Object[] { systemModel });
        }
        return testConfigs;
    }

    private Model systemModel;

    public SimpleAdapterCreationTest(Model systemModel) {
        this.systemModel = systemModel;
    }

    @Test
    public void test() throws Exception {
        Assert.assertNotNull(SimpleAdapter.create(systemModel));
    }
}

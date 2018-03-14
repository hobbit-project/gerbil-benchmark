/**
 * This file is part of General Entity Annotator Benchmark.
 *
 * General Entity Annotator Benchmark is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * General Entity Annotator Benchmark is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with General Entity Annotator Benchmark.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.aksw.gerbil.semantic.vocabs;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class NIF_SYS {

    protected static final String uri = "http://w3id.org/hobbit/gerbil/nif-sys#";

    /**
     * returns the URI for this schema
     * 
     * @return the URI for this schema
     */
    public static String getURI() {
        return uri;
    }

    protected static final Resource resource(String local) {
        return ResourceFactory.createResource(uri + local);
    }

    protected static final Property property(String local) {
        return ResourceFactory.createProperty(uri, local);
    }

    // public static final Resource Experiment = resource("Experiment");
    // public static final Resource ExperimentTask = resource("ExperimentTask");
    //
    // public static final Resource A2KB = resource("A2KB");
    // public static final Resource C2KB = resource("C2KB");
    // public static final Resource D2KB = resource("D2KB");
    // public static final Resource Rc2KB = resource("Rc2KB");
    // public static final Resource Sa2KB = resource("Sa2KB");
    // public static final Resource Sc2KB = resource("Sc2KB");
    // public static final Resource OKE2015_Task1 = resource("OKE2015_Task1");
    // public static final Resource OKE2015_Task2 = resource("OKE2015_Task2");
    // public static final Resource ERec = resource("ERec");
    // public static final Resource ETyping = resource("ETyping");
    //
    // public static final Resource StrongAnnoMatch =
    // resource("StrongAnnoMatch");
    // public static final Resource WeakAnnoMatch = resource("WeakAnnoMatch");
    // public static final Resource StrongEntityMatch =
    // resource("StrongEntityMatch");
    //
    // public static final Resource DSD = resource("dsd");
    //
    public static final Property instanceImageName = property("instanceImageName");
    public static final Property numberOfInstances = property("numberOfInstances");
    public static final Property webserviceUrl = property("webserviceUrl");
    public static final Property additionalHeader = property("additionalHeader");
    // public static final Property dataset = property("dataset");
    // public static final Property experimentType = property("experimentType");
    // public static final Property errorCount = property("errorCount");
    // public static final Property macroF1 = property("macroF1");
    // public static final Property macroPrecision = property("macroPrecision");
    // public static final Property macroRecall = property("macroRecall");
    // public static final Property matching = property("matching");
    // public static final Property microF1 = property("microF1");
    // public static final Property microPrecision = property("microPrecision");
    // public static final Property microRecall = property("microRecall");
    // public static final Property statusCode = property("statusCode");
    // public static final Property subExperimentOf =
    // property("subExperimentOf");
    // public static final Property timestamp = property("timestamp");
    // public static final Property topic = property("topic");

}

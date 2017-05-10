package org.hobbit.benchmark.gerbil.systems.impl;

import org.aksw.gerbil.annotator.impl.fred.FredAnnotator;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.transfer.nif.Document;
import org.hobbit.benchmark.gerbil.systems.HobbitAnnotator;

public class FREDWrapper extends FredAnnotator implements HobbitAnnotator{

    public FREDWrapper(String serviceUrl) {
	super(serviceUrl);
    }
    
    @Override
    public Document annotate(Document document) throws GerbilException {
	return this.requestAnnotations(document);
    }

}

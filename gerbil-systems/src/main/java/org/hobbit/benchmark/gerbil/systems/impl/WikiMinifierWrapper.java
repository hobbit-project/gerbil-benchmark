package org.hobbit.benchmark.gerbil.systems.impl;

import org.aksw.gerbil.annotator.impl.wikiminer.WikipediaMinerAnnotator;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.transfer.nif.Document;
import org.hobbit.benchmark.gerbil.systems.HobbitAnnotator;

public class WikiMinifierWrapper extends WikipediaMinerAnnotator implements HobbitAnnotator{


    
    public WikiMinifierWrapper(String service) throws GerbilException {
	super(service);
    }
    
    @Override
    public Document annotate(Document document) throws GerbilException {
	return this.requestAnnotations(document);
    }

}

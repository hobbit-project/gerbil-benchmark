package org.hobbit.benchmark.gerbil.systems.impl;

import org.aksw.gerbil.annotator.impl.fox.FOXAnnotator;
import org.aksw.gerbil.annotator.impl.nerd.NERDAnnotator;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.transfer.nif.Document;
import org.hobbit.benchmark.gerbil.systems.HobbitAnnotator;

public class NERDWrapper extends NERDAnnotator implements HobbitAnnotator{

    public NERDWrapper() throws GerbilException {
	super();
    }
    
    public NERDWrapper(String service) throws GerbilException {
	super(service);
    }
    
    @Override
    public Document annotate(Document document) throws GerbilException {
	return this.getNERDAnnotations(document);
    }

}

package org.hobbit.benchmark.gerbil.systems.impl;

import org.aksw.gerbil.annotator.impl.babelfy.BabelfyAnnotator;
import org.aksw.gerbil.annotator.impl.fox.FOXAnnotator;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.transfer.nif.Document;
import org.hobbit.benchmark.gerbil.systems.HobbitAnnotator;

public class BabelfyWrapper extends BabelfyAnnotator implements HobbitAnnotator{

    public BabelfyWrapper() throws GerbilException {
	super();
    }
    
    public BabelfyWrapper(String service) throws GerbilException {
	super(service);
    }
    
    @Override
    public Document annotate(Document document) throws GerbilException {
	//TODO
	return this.sendRequest(document, true);
    }

}

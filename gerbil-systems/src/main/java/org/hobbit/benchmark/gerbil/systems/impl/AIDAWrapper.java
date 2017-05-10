package org.hobbit.benchmark.gerbil.systems.impl;

import org.aksw.gerbil.annotator.impl.aida.AidaAnnotator;
import org.aksw.gerbil.annotator.impl.fox.FOXAnnotator;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.transfer.nif.Document;
import org.hobbit.benchmark.gerbil.systems.HobbitAnnotator;

public class AIDAWrapper extends AidaAnnotator implements HobbitAnnotator{

    
    private boolean containsMentioning;

    public AIDAWrapper(String service, boolean containsMentioning) throws GerbilException {
	super(service);
	this.containsMentioning=containsMentioning;
    }
    
    @Override
    public Document annotate(Document document) throws GerbilException {
	//TODO
	return this.requestAnnotations(document.getDocumentURI(), document.getText(), this.containsMentioning);
    }

}

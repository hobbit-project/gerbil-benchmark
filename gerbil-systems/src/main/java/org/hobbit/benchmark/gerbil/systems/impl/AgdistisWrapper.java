package org.hobbit.benchmark.gerbil.systems.impl;

import org.aksw.gerbil.annotator.impl.agdistis.AgdistisAnnotator;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.MeaningSpan;
import org.hobbit.benchmark.gerbil.systems.HobbitAnnotator;

public class AgdistisWrapper extends AgdistisAnnotator implements HobbitAnnotator{

    
    public AgdistisWrapper(String host, String portString)
	    throws GerbilException {
	super(host, portString);
    }


    @Override
    public Document annotate(Document document) throws GerbilException {
	for(MeaningSpan meaning : this.performD2KBTask(document)){
	     document.addMarking((Marking) meaning);
	}
	return document;
    }

}

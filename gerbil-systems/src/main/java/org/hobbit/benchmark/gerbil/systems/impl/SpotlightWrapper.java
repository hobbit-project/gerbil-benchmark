package org.hobbit.benchmark.gerbil.systems.impl;

import org.aksw.gerbil.annotator.impl.spotlight.SpotlightAnnotator;
import org.aksw.gerbil.annotator.impl.spotlight.SpotlightClient;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.data.TypedNamedEntity;
import org.hobbit.benchmark.gerbil.systems.HobbitAnnotator;

public class SpotlightWrapper extends SpotlightAnnotator implements HobbitAnnotator{

    private SpotlightClient client;
    private int type;
    
    public SpotlightWrapper(String url, int type){
        if (url != null) {
            client = new SpotlightClient(url, this);
        } else {
            client = new SpotlightClient(this);
        }
        this.type=type;
    }
 
    @Override
    public Document annotate(Document document) throws GerbilException {
	switch(this.type){
	case 0:
	    return getAnnotate(document);
	case 1:
	    return getDisambiguate(document);
	case 2:
	    return getSpot(document);
	}
	return null;
	
    }

    private Document getDisambiguate(Document document) throws GerbilException {
	for(TypedNamedEntity tne : client.disambiguate(document)){
	     document.addMarking((Marking) tne);
	}
	return document;
    }

    private Document getSpot(Document document) throws GerbilException {
	for(Span tne : client.spot(document)){
	     document.addMarking((Marking) tne);
	}
	return document;
    }

    private Document getAnnotate(Document document) throws GerbilException{
	for(TypedNamedEntity tne : client.annotate(document)){
	     document.addMarking((Marking) tne);
	}
	return document;
    }

}

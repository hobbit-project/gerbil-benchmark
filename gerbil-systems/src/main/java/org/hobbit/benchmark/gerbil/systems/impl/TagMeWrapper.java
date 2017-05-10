package org.hobbit.benchmark.gerbil.systems.impl;

import org.aksw.gerbil.annotator.impl.fox.FOXAnnotator;
import org.aksw.gerbil.annotator.impl.tagme.TagMeAnnotator;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.transfer.nif.Document;
import org.hobbit.benchmark.gerbil.systems.HobbitAnnotator;

public class TagMeWrapper extends TagMeAnnotator implements HobbitAnnotator{

    
    public TagMeWrapper(String annotationUrl, String spotUrl)
	    throws GerbilException {
	super(annotationUrl, spotUrl);
    }

    @Override
    public Document annotate(Document document) throws GerbilException {
	//TODO
	return this.performRequest(document, true);
    }

}

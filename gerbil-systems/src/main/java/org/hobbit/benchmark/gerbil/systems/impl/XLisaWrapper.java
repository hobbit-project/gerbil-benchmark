package org.hobbit.benchmark.gerbil.systems.impl;

import org.aksw.gerbil.annotator.impl.xlisa.XLisaAnnotator;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.transfer.nif.Document;
import org.hobbit.benchmark.gerbil.systems.HobbitAnnotator;

public class XLisaWrapper extends XLisaAnnotator implements HobbitAnnotator{

    
    public XLisaWrapper(String lang1, String lang2, String kb, String model) {
	super(lang1, lang2, kb, model);
	// TODO Auto-generated constructor stub
    }

    @Override
    public Document annotate(Document document) throws GerbilException {
	return this.sendRequest(document, true);
    }

}

package org.hobbit.benchmark.gerbil.systems;

import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.transfer.nif.Document;

public interface HobbitAnnotator {

    public Document annotate(Document document) throws GerbilException;
    
}

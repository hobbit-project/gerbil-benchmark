package org.hobbit.benchmark.gerbil.comparator;

import java.util.Comparator;

import org.aksw.gerbil.transfer.nif.Document;

public class DocumentComparator implements Comparator<Document> {


    @Override
    public int compare(Document arg0, Document arg1) {
//	Integer docID1 =  Integer.parseInt(arg0.getDocumentURI());
//	Integer docID2 =  Integer.parseInt(arg1.getDocumentURI());
	return arg0.getDocumentURI().compareTo(arg1.getDocumentURI());
    }

}

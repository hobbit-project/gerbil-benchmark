package org.aksw.gerbil.annotator.impl.nif;

import java.io.IOException;

import org.aksw.gerbil.datatypes.ErrorTypes;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.NIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.NIFDocumentParser;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentCreator;
import org.aksw.gerbil.transfer.nif.TurtleNIFDocumentParser;
import org.aksw.gerbil.utils.DocumentTextEditRevoker;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdaptedNIFBasedAnnotatorWebservice extends NIFBasedAnnotatorWebservice {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptedNIFBasedAnnotatorWebservice.class);

    private NIFDocumentCreator nifCreator = new TurtleNIFDocumentCreator();
    private NIFDocumentParser nifParser = new TurtleNIFDocumentParser();

    public AdaptedNIFBasedAnnotatorWebservice(String url) {
        super(url);
    }

    public AdaptedNIFBasedAnnotatorWebservice(String url, String name) {
        super(url, name);
    }
    
    public AdaptedNIFBasedAnnotatorWebservice(String url, String name, String additionalHeader) {
    	super(url, name, additionalHeader);
    }

    /**
     * Overrides the original request method since we don't need HTTP
     * management.
     */
    public Document request(Document document) throws GerbilException {
        // create NIF document
        String nifDocument = nifCreator.getDocumentAsNIFString(document);
        HttpEntity entity = new StringEntity(nifDocument, "UTF-8");
        // send NIF document
        HttpPost request = null;
        try {
            request = new HttpPost(getUrl());
        } catch (IllegalArgumentException e) {
            throw new GerbilException("Couldn't create HTTP request.", e, ErrorTypes.UNEXPECTED_EXCEPTION);
        }
        request.setEntity(entity);
        request.addHeader(HttpHeaders.CONTENT_TYPE, nifCreator.getHttpContentType() + ";charset=UTF-8");
        request.addHeader(HttpHeaders.ACCEPT, nifParser.getHttpContentType());
        request.addHeader(HttpHeaders.ACCEPT_CHARSET, "UTF-8");
        for(Header header : getAdditionalHeader()) {
        	request.addHeader(header);
        }
        entity = null;
        Document resultDoc = null;
        CloseableHttpResponse response = null;
        try {
            response = sendRequest(request, true);
            // receive NIF document
            entity = response.getEntity();
            // read response and parse NIF
            try {
                resultDoc = nifParser.getDocumentFromNIFStream(entity.getContent());
            } catch (Exception e) {
                LOGGER.error("Couldn't parse the response.", e);
                throw new GerbilException("Couldn't parse the response.", e, ErrorTypes.UNEXPECTED_EXCEPTION);
            }
        } finally {
            if (entity != null) {
                try {
                    EntityUtils.consume(entity);
                } catch (IOException e1) {
                }
            }
            IOUtils.closeQuietly(response);
        }
        return DocumentTextEditRevoker.revokeTextEdits(resultDoc, document.getText());
    }

    @Override
    protected void performClose() throws IOException {
        IOUtils.closeQuietly(client);
        client = null;
        super.performClose();
    }
}

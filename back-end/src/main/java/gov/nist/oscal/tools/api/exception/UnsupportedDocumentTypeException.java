package gov.nist.oscal.tools.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedDocumentTypeException extends RuntimeException {

    public UnsupportedDocumentTypeException(String contentType) {
        super("Unsupported document content type: " + contentType
                + ". Allowed: PDF, Office documents, CSV, plain text, common image formats, ZIP, OSCAL JSON/XML/YAML.");
    }
}

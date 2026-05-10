package gov.nist.oscal.tools.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedConMonFormatException extends RuntimeException {
    public UnsupportedConMonFormatException(String filename) {
        super("Unsupported ConMon file: " + filename
                + ". Use OSCAL JSON/XML/YAML or the FedRAMP POA&M Excel template (.xlsx). "
                + "For other artifacts, use the Documents tab.");
    }
}

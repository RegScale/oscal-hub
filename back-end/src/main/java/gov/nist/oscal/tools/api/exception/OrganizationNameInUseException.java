package gov.nist.oscal.tools.api.exception;

public class OrganizationNameInUseException extends RuntimeException {
    public OrganizationNameInUseException(String name) {
        super("Organization name already in use: " + name);
    }
}

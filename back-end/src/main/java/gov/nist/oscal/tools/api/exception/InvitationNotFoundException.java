package gov.nist.oscal.tools.api.exception;

public class InvitationNotFoundException extends RuntimeException {
    public InvitationNotFoundException(String identifier) {
        super("Invitation not found: " + identifier);
    }
}

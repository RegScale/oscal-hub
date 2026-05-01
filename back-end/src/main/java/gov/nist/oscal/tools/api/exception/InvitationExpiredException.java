package gov.nist.oscal.tools.api.exception;

public class InvitationExpiredException extends RuntimeException {
    public InvitationExpiredException(String message) {
        super(message);
    }
}

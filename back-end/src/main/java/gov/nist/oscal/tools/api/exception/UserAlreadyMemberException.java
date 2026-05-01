package gov.nist.oscal.tools.api.exception;

public class UserAlreadyMemberException extends RuntimeException {
    public UserAlreadyMemberException(String email) {
        super("User is already a member of this organization: " + email);
    }
}

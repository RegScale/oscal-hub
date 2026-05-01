package gov.nist.oscal.tools.api.entity;

import java.time.LocalDateTime;

/**
 * STUB — replaced by the real JPA entity in plan Task 9.
 * This minimal class exists only so EmailService and SendGridEmailService compile.
 */
public class Invitation {
    public String getEmail() { return null; }
    public String getToken() { return null; }
    public LocalDateTime getExpiresAt() { return LocalDateTime.now(); }
}

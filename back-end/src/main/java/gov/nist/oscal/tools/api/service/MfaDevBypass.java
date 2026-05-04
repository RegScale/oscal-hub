package gov.nist.oscal.tools.api.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Local-development convenience: skip the MFA challenge during login when set.
 * <p>
 * <b>This bean is annotated {@code @Profile("dev")}, so it is only constructed
 * when the application runs with the {@code dev} Spring profile active.</b>
 * Production (gcp profile), staging, prod, and the default profile do not
 * load this bean at all — even if the {@code security.mfa.dev-bypass.enabled}
 * property is set in those profiles, it is ignored because there is no bean
 * to read it.
 * <p>
 * To opt in locally, set {@code security.mfa.dev-bypass.enabled=true} in
 * {@code application-dev.properties}. A loud WARN is logged on startup so
 * the bypass cannot go unnoticed.
 */
@Component
@Profile("dev")
public class MfaDevBypass {

    private static final Logger log = LoggerFactory.getLogger(MfaDevBypass.class);

    private final boolean enabled;

    public MfaDevBypass(@Value("${security.mfa.dev-bypass.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    @PostConstruct
    void announce() {
        if (enabled) {
            log.warn("================================================================================");
            log.warn("⚠️  MFA BYPASS ACTIVE — login skips MFA challenge entirely.");
            log.warn("⚠️  This is the dev profile (SPRING_PROFILES_ACTIVE=dev). NEVER deploy with this.");
            log.warn("⚠️  Disable by setting security.mfa.dev-bypass.enabled=false.");
            log.warn("================================================================================");
        }
    }

    public boolean isActive() {
        return enabled;
    }
}

package gov.nist.oscal.tools.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hard guarantee: MfaDevBypass is annotated {@code @Profile("dev")} so Spring
 * does not construct the bean under any non-dev profile (gcp, prod, staging,
 * default). Even if a non-dev properties file copied the
 * {@code security.mfa.dev-bypass.enabled=true} line, the bean would not exist
 * and the AuthService bypass branch would be unreachable.
 *
 * <p>This is a static-introspection test rather than a context test because
 * the prod/gcp profiles require runtime env vars (DB_URL etc.) that aren't
 * available in unit tests. The annotation IS the contract — verifying it here
 * locks down the profile gate without booting Spring.
 */
class MfaDevBypassProfileTest {

    @Test
    void mfaDevBypassIsGatedToDevProfileOnly() {
        Profile profile = MfaDevBypass.class.getAnnotation(Profile.class);
        assertThat(profile)
                .as("MfaDevBypass must carry @Profile so it cannot load outside dev")
                .isNotNull();
        assertThat(profile.value())
                .as("MfaDevBypass must be gated exclusively to the 'dev' profile")
                .containsExactly("dev");
    }
}

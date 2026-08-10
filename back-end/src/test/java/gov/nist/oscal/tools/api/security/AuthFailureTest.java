package gov.nist.oscal.tools.api.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The wire contract for authentication failures. These strings are consumed by
 * external integrations (the RegScale Trust Center branches on `code`), so a
 * casual rename here is a breaking API change — hence a test per code.
 */
class AuthFailureTest {

    @Test
    void eachFactory_carriesItsDocumentedCode() {
        assertThat(AuthFailure.missingCredentials().code()).isEqualTo("missing_credentials");
        assertThat(AuthFailure.unsupportedScheme().code()).isEqualTo("unsupported_auth_scheme");
        assertThat(AuthFailure.malformedToken().code()).isEqualTo("malformed_token");
        assertThat(AuthFailure.tokenExpired(null).code()).isEqualTo("token_expired");
        assertThat(AuthFailure.invalidSignature().code()).isEqualTo("invalid_signature");
        assertThat(AuthFailure.invalidToken().code()).isEqualTo("invalid_token");
        assertThat(AuthFailure.serviceTokenRevoked().code()).isEqualTo("service_token_revoked");
        assertThat(AuthFailure.serviceTokenUnknown().code()).isEqualTo("service_token_unknown");
        assertThat(AuthFailure.serviceTokenLegacy().code()).isEqualTo("service_token_legacy");
    }

    @Test
    void everyFactory_hasNonEmptyActionableMessage() {
        AuthFailure[] all = {
                AuthFailure.missingCredentials(), AuthFailure.unsupportedScheme(),
                AuthFailure.malformedToken(), AuthFailure.tokenExpired(null),
                AuthFailure.invalidSignature(), AuthFailure.invalidToken(),
                AuthFailure.serviceTokenRevoked(), AuthFailure.serviceTokenUnknown(),
                AuthFailure.serviceTokenLegacy()
        };
        for (AuthFailure failure : all) {
            assertThat(failure.message()).as("message for %s", failure.code()).isNotBlank();
        }
    }

    @Test
    void missingCredentials_namesTheHeaderItWants() {
        // The failure that actually bit the Trust Center: the message has to say
        // what to send, not merely that something was absent.
        assertThat(AuthFailure.missingCredentials().message()).contains("Bearer");
    }

    @Test
    void tokenExpired_formatsExpiryAsUtcInstant() {
        Date expiry = Date.from(Instant.parse("2026-08-08T21:36:19Z"));
        assertThat(AuthFailure.tokenExpired(expiry).expiredAt()).isEqualTo("2026-08-08T21:36:19Z");
    }

    @Test
    void tokenExpired_toleratesUnknownExpiry() {
        assertThat(AuthFailure.tokenExpired(null).expiredAt()).isNull();
    }

    @Test
    void otherFailures_carryNoExpiry() {
        assertThat(AuthFailure.malformedToken().expiredAt()).isNull();
        assertThat(AuthFailure.invalidSignature().expiredAt()).isNull();
    }

    @Test
    void wwwAuthenticate_isBareBearerWhenNoCredentialWasSent() {
        // RFC 6750: omit the error parameter when the request carried no credential.
        assertThat(AuthFailure.missingCredentials().wwwAuthenticate()).isEqualTo("Bearer");
    }

    @Test
    void wwwAuthenticate_describesTheErrorForRejectedCredentials() {
        String header = AuthFailure.malformedToken().wwwAuthenticate();
        assertThat(header).startsWith("Bearer error=\"invalid_token\"");
        assertThat(header).contains("error_description=");
    }

    @Test
    void wwwAuthenticate_neverEmitsRawQuotesInsideTheQuotedString() {
        // RFC 6750 quoted-string cannot contain a raw double quote; a message
        // containing one would produce an unparseable header.
        AuthFailure quoted = new AuthFailure("invalid_token", "He said \"no\".", null);
        String header = quoted.wwwAuthenticate();
        assertThat(header.indexOf('"', header.indexOf("error_description=\"") + 19))
                .as("description must contain no raw quote before its closing quote")
                .isEqualTo(header.length() - 1);
    }
}

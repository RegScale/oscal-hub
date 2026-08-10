package gov.nist.oscal.tools.api.security;

import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * A single authentication failure, in the form the caller receives it.
 * <p>
 * Every 401 the auth layer produces is one of these. The {@code code} is a
 * stable identifier an integration branches on; the {@code message} is prose a
 * human acts on. Both are part of the public API — external integrations depend
 * on the codes, so treat renames as breaking changes.
 * </p>
 * <p>
 * These strings go only to a caller who already holds the credential and
 * already knows it was rejected, so distinguishing the failures leaks nothing
 * that the bare 401 did not. That is the same judgment already documented for
 * the service-account gate in {@link JwtAuthenticationFilter}.
 * </p>
 *
 * @param code      stable machine-readable identifier
 * @param message   actionable human-readable explanation
 * @param expiredAt ISO-8601 UTC instant, only for {@code token_expired}; null otherwise
 */
public record AuthFailure(String code, String message, String expiredAt) {

    /**
     * Request attribute under which {@code JwtAuthenticationFilter} leaves a
     * failure for {@code SecurityConfig}'s entry point to render. The filter
     * cannot respond itself: public endpoints legitimately arrive with no
     * credential, so only Spring Security may conclude a request was
     * unauthorized.
     */
    public static final String REQUEST_ATTRIBUTE = "gov.nist.oscal.tools.api.authFailure";

    public static AuthFailure missingCredentials() {
        return new AuthFailure("missing_credentials",
                "No Authorization header was provided. Send 'Authorization: Bearer <token>'.", null);
    }

    public static AuthFailure unsupportedScheme() {
        return new AuthFailure("unsupported_auth_scheme",
                "Authorization header must use the Bearer scheme, as in 'Authorization: Bearer <token>'.", null);
    }

    public static AuthFailure malformedToken() {
        return new AuthFailure("malformed_token",
                "The credential is not a well-formed JWT. Check that the whole token value was sent, "
                        + "and that it was not truncated or left encrypted.", null);
    }

    /**
     * @param expiry the moment the token expired, or null when it could not be read
     */
    public static AuthFailure tokenExpired(Date expiry) {
        String expiredAt = expiry == null
                ? null
                : DateTimeFormatter.ISO_INSTANT.format(expiry.toInstant());
        return new AuthFailure("token_expired",
                "The token has expired. Generate a replacement from your Profile page.", expiredAt);
    }

    public static AuthFailure invalidSignature() {
        return new AuthFailure("invalid_signature",
                "Token was signed with a different key. It may have been issued by another environment.", null);
    }

    public static AuthFailure invalidToken() {
        return new AuthFailure("invalid_token", "The token could not be validated.", null);
    }

    public static AuthFailure serviceTokenRevoked() {
        return new AuthFailure("service_token_revoked",
                "This service account token has been revoked.", null);
    }

    public static AuthFailure serviceTokenUnknown() {
        return new AuthFailure("service_token_unknown",
                "Service account token not recognized.", null);
    }

    public static AuthFailure serviceTokenLegacy() {
        return new AuthFailure("service_token_legacy",
                "This service account token predates revocation support. "
                        + "Generate a replacement from your Profile page.", null);
    }

    /**
     * The {@code WWW-Authenticate} challenge for this failure, per RFC 6750.
     * A request that carried no credential gets a bare challenge; anything else
     * gets {@code invalid_token} plus a description.
     */
    public String wwwAuthenticate() {
        if ("missing_credentials".equals(code)) {
            return "Bearer";
        }
        // RFC 6750 quoted-string admits no raw double quote.
        String description = message.replace('"', '\'');
        return "Bearer error=\"invalid_token\", error_description=\"" + description + "\"";
    }
}

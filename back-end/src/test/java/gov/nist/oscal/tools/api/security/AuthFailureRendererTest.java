package gov.nist.oscal.tools.api.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFailureRendererTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void render_returns401WithJsonContentType() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthFailureRenderer.render(response, AuthFailure.malformedToken());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
    }

    @Test
    void render_keepsErrorFieldLiteralForBackwardCompatibility() throws Exception {
        // The frontend and existing clients read `error`; it must not become prose.
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthFailureRenderer.render(response, AuthFailure.invalidSignature());

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.get("error").asText()).isEqualTo("Unauthorized");
        assertThat(body.get("code").asText()).isEqualTo("invalid_signature");
        assertThat(body.get("message").asText()).isEqualTo(AuthFailure.invalidSignature().message());
    }

    @Test
    void render_includesExpiredAtWhenPresent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        Date expiry = Date.from(Instant.parse("2026-08-08T21:36:19Z"));

        AuthFailureRenderer.render(response, AuthFailure.tokenExpired(expiry));

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.get("expiredAt").asText()).isEqualTo("2026-08-08T21:36:19Z");
    }

    @Test
    void render_omitsExpiredAtWhenAbsent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthFailureRenderer.render(response, AuthFailure.missingCredentials());

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.has("expiredAt")).isFalse();
    }

    @Test
    void render_setsWwwAuthenticateChallenge() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthFailureRenderer.render(response, AuthFailure.missingCredentials());

        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }

    @Test
    void render_producesValidJsonWhenMessageContainsQuotes() throws Exception {
        // Guards the bug the old hand-built JSON had: string concatenation
        // emitted broken JSON the moment a message contained a quote.
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthFailureRenderer.render(response, new AuthFailure("invalid_token", "He said \"no\".", null));

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.get("message").asText()).isEqualTo("He said \"no\".");
    }
}

package gov.nist.oscal.tools.api.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.security.AuthFailure;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The entry point is where a recorded failure becomes a response. Instantiating
 * SecurityConfig directly is fine — authenticationEntryPoint() touches none of
 * its injected collaborators.
 */
class AuthenticationEntryPointTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AuthenticationEntryPoint entryPoint = new SecurityConfig().authenticationEntryPoint();

    private static final AuthenticationException NOT_AUTHENTICATED =
            new AuthenticationException("not authenticated") {
            };

    @Test
    void rendersTheRecordedFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthFailure.REQUEST_ATTRIBUTE, AuthFailure.malformedToken());
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, NOT_AUTHENTICATED);

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body.get("code").asText()).isEqualTo("malformed_token");
        assertThat(body.get("error").asText()).isEqualTo("Unauthorized");
    }

    @Test
    void rendersExpiryWhenTheFailureCarriesIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthFailure.REQUEST_ATTRIBUTE,
                AuthFailure.tokenExpired(java.util.Date.from(java.time.Instant.parse("2026-08-08T21:36:19Z"))));
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, NOT_AUTHENTICATED);

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.get("expiredAt").asText()).isEqualTo("2026-08-08T21:36:19Z");
    }

    @Test
    void fallsBackToMissingCredentialsWhenNothingWasRecorded() throws Exception {
        // e.g. a request rejected before the JWT filter ran.
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, NOT_AUTHENTICATED);

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo("missing_credentials");
    }

    @Test
    void alwaysSetsAChallengeHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, NOT_AUTHENTICATED);

        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }
}

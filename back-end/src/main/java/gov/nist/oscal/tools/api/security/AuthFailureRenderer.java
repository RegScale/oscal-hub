package gov.nist.oscal.tools.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes an {@link AuthFailure} to the response as the single 401 shape.
 * <p>
 * Deliberately a static utility rather than a Spring bean: both callers are
 * awkward injection sites — {@code JwtAuthenticationFilter} uses field
 * injection, and {@code SecurityConfig}'s entry point is a lambda in a
 * {@code @Bean} method. Keeping this dependency-free also means it adds nothing
 * to the bean graph that {@code @WebMvcTest} slices would need to mock.
 * </p>
 */
public final class AuthFailureRenderer {

    /** Serialization is stateless here; ObjectMapper is thread-safe once configured. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AuthFailureRenderer() {
    }

    /**
     * Render the failure: 401, JSON body, and an RFC 6750 challenge header.
     * The {@code error} field stays the literal {@code "Unauthorized"} so
     * clients that key off it are unaffected by this diagnostic detail.
     *
     * @param response the response to write
     * @param failure  the failure to report
     * @throws IOException if the response writer fails
     */
    public static void render(HttpServletResponse response, AuthFailure failure) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("WWW-Authenticate", failure.wwwAuthenticate());

        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", "Unauthorized");
        body.put("message", failure.message());
        body.put("code", failure.code());
        if (failure.expiredAt() != null) {
            body.put("expiredAt", failure.expiredAt());
        }

        MAPPER.writeValue(response.getWriter(), body);
    }
}

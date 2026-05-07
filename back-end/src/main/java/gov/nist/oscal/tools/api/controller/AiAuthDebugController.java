package gov.nist.oscal.tools.api.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dev-only diagnostic endpoint that returns what Spring Security currently
 * sees on the request. Useful when the frontend gets 401s and we need to
 * know whether the JWT is being parsed, who Spring thinks the user is, and
 * which authorities are populated.
 *
 * <p>Gated to {@code @Profile("dev")} so it cannot leak in prod/gcp/staging.
 * Whitelisted in SecurityConfig so it works whether or not the JWT validates.
 */
@RestController
@RequestMapping("/api/ai/whoami")
@Profile("dev")
@Hidden
public class AiAuthDebugController {

    @Operation(summary = "Dev-only: return the current Spring Security Authentication state")
    @GetMapping
    public ResponseEntity<Map<String, Object>> whoami() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> body = new HashMap<>();
        if (auth == null) {
            body.put("authenticated", false);
            body.put("reason", "SecurityContextHolder has no Authentication");
            return ResponseEntity.ok(body);
        }
        body.put("authenticated", auth.isAuthenticated());
        body.put("principal", auth.getName());
        body.put("authClass", auth.getClass().getSimpleName());
        body.put("authorities",
                auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
        body.put("isAnonymous", "anonymousUser".equals(auth.getName()));
        return ResponseEntity.ok(body);
    }
}

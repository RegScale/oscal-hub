package gov.nist.oscal.tools.api.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Single source of truth for the client IP behind reverse proxies.
 *
 * <p><b>Why the rightmost X-Forwarded-For entry:</b> proxies APPEND the
 * connection IP they observed, so entries to the left are whatever the client
 * chose to send. The previous per-class implementations took the FIRST entry,
 * which let anyone rotate {@code X-Forwarded-For: <fake>} to bypass per-IP
 * rate limits and IP lockouts, and polluted audit logs with attacker-chosen
 * addresses.</p>
 *
 * <p>{@code security.trusted-proxy-hops} is the number of trusted proxies in
 * front of the app (default 1 = Cloud Run / a single reverse proxy). The
 * client IP is taken that many entries from the right. Behind Cloud Run plus
 * an external HTTPS load balancer set it to 2. With 0 (direct exposure, no
 * proxy) the header is ignored entirely and the socket address is used.</p>
 */
@Component
public class ClientIpResolver {

    private final int trustedProxyHops;

    public ClientIpResolver(@Value("${security.trusted-proxy-hops:1}") int trustedProxyHops) {
        this.trustedProxyHops = Math.max(0, trustedProxyHops);
    }

    /** Client IP for the given request; "unknown" only if nothing is available. */
    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        if (trustedProxyHops > 0) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String[] entries = xff.split(",");
                // The Nth entry from the right was appended by the outermost
                // trusted proxy; clamp for short header values.
                int index = Math.max(0, entries.length - trustedProxyHops);
                String candidate = entries[index].trim();
                if (!candidate.isEmpty()) {
                    return candidate;
                }
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null && !remoteAddr.isBlank() ? remoteAddr : "unknown";
    }

    /**
     * Client IP of the request bound to the current thread, or "unknown" when
     * there is none (async executors, scheduled jobs). Callers on async paths
     * should capture the IP on the request thread and pass it along instead.
     */
    public String resolveCurrent() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? "unknown" : resolve(attributes.getRequest());
    }
}

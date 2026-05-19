package gov.nist.oscal.tools.api.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

/**
 * Fetches the contents of a user-supplied URL for the AI wizard "from URL" flow.
 *
 * <p>Security model — this code accepts a URL from an authenticated user and
 * makes the server reach out to it on the user's behalf. That's a textbook
 * SSRF surface, so the fetcher enforces:
 *
 * <ul>
 *   <li>Scheme must be {@code http} or {@code https} — no {@code file:},
 *       {@code ftp:}, {@code gopher:}, {@code jar:}, {@code javascript:},
 *       {@code data:}, etc.</li>
 *   <li>After DNS resolution, every resolved address must be a "public"
 *       address. Loopback, link-local (incl. cloud metadata
 *       {@code 169.254.169.254}), site-local / RFC1918 private ranges,
 *       multicast, "any-local" / wildcard, CGNAT {@code 100.64.0.0/10}, and
 *       IPv6 unique local {@code fc00::/7} are all rejected.</li>
 *   <li>Redirects are followed manually (max {@value #MAX_REDIRECTS} hops),
 *       re-validating the new target's host on every hop.</li>
 *   <li>Response body is read with a hard size cap of {@value #MAX_BYTES}
 *       bytes; the stream is aborted once the cap is exceeded.</li>
 *   <li>Per-attempt connect + request timeouts.</li>
 * </ul>
 *
 * <p>This does NOT protect against DNS rebinding (a hostname that resolves to
 * a public IP at validation time and a private IP at connect time). For
 * defense in depth, callers should avoid running this from a network that
 * exposes sensitive internal services on routable IPs.
 */
@Service
public class UrlContentFetcher {

    private static final Logger log = LoggerFactory.getLogger(UrlContentFetcher.class);

    /** Hard cap on response body. Matches {@code SourceIngestor#MAX_PDF_BYTES}. */
    public static final long MAX_BYTES = 32L * 1024 * 1024;

    /** Cap on redirect chain length. */
    public static final int MAX_REDIRECTS = 5;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private static final String USER_AGENT = "OSCAL-Hub-Wizard/1.0";
    private static final String ACCEPT =
            "application/pdf, text/html, application/xhtml+xml, text/plain, "
                    + "application/xml, text/xml, application/json, text/markdown, "
                    + "application/octet-stream;q=0.5, */*;q=0.1";

    private final HttpClient httpClient;
    private final HostValidator hostValidator;

    public UrlContentFetcher() {
        this(defaultClient(), new DefaultHostValidator());
    }

    /** Test seam — pass a custom {@link HttpClient} (e.g. one that hits a test server). */
    public UrlContentFetcher(HttpClient httpClient, HostValidator hostValidator) {
        this.httpClient = httpClient;
        this.hostValidator = hostValidator;
    }

    private static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                // We follow redirects manually so we can re-validate the new host
                // on every hop. HttpClient's NORMAL/ALWAYS would skip our checks.
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    /**
     * Fetches the URL's content. The returned bytes can be handed straight to
     * the existing wizard pipeline ({@code orchestrator.start(..., bytes, filename, ...)})
     * which feeds them through {@code DocumentNormalizer} / Tika.
     *
     * @throws IllegalArgumentException if the URL is rejected (bad scheme, private
     *         IP, too many redirects, etc.) — surfaces as a 400 to the client
     * @throws IOException for transport-level failures (DNS, connect, read, size cap)
     */
    public FetchResult fetch(String urlString) throws IOException {
        URI uri = parseAndCheckScheme(urlString);
        int hops = 0;
        while (true) {
            validateHost(uri);

            HttpRequest req = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", ACCEPT)
                    .build();

            HttpResponse<InputStream> resp;
            try {
                resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while fetching " + uri, e);
            }

            int status = resp.statusCode();
            if (status >= 300 && status < 400) {
                // Always drain & close the redirect response body so the connection
                // can be reused (or released) for the next hop.
                try (InputStream in = resp.body()) { in.transferTo(java.io.OutputStream.nullOutputStream()); }
                if (++hops > MAX_REDIRECTS) {
                    throw new IllegalArgumentException("Too many redirects (>" + MAX_REDIRECTS + ") starting from " + urlString);
                }
                String location = resp.headers().firstValue("location")
                        .orElseThrow(() -> new IOException("Redirect " + status + " without Location header"));
                URI next = uri.resolve(location);
                String nextScheme = next.getScheme();
                if (nextScheme == null
                        || !("http".equalsIgnoreCase(nextScheme) || "https".equalsIgnoreCase(nextScheme))) {
                    throw new IllegalArgumentException(
                            "Refusing redirect to non-http(s) scheme: " + nextScheme);
                }
                log.debug("URL fetch redirect {} -> {}", uri, next);
                uri = next;
                continue;
            }

            if (status >= 400) {
                try (InputStream in = resp.body()) { in.transferTo(java.io.OutputStream.nullOutputStream()); }
                throw new IOException("URL fetch failed with HTTP " + status + " for " + uri);
            }

            byte[] bytes;
            try (InputStream in = resp.body()) {
                bytes = readWithCap(in, MAX_BYTES);
            }
            String contentType = resp.headers().firstValue("content-type").orElse(null);
            String filename = deriveFilename(uri, contentType);
            log.info("Fetched URL {} ({} bytes, content-type={})", uri, bytes.length, contentType);
            return new FetchResult(bytes, filename, contentType);
        }
    }

    private static URI parseAndCheckScheme(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            throw new IllegalArgumentException("URL is required");
        }
        URI uri;
        try {
            uri = URI.create(urlString.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Malformed URL: " + urlString, e);
        }
        String scheme = uri.getScheme();
        if (scheme == null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException(
                    "Only http and https URLs are allowed (got scheme: " + scheme + ")");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("URL missing host: " + urlString);
        }
        return uri;
    }

    private void validateHost(URI uri) {
        String host = uri.getHost();
        InetAddress[] resolved;
        try {
            resolved = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Cannot resolve host: " + host, e);
        }
        for (InetAddress addr : resolved) {
            hostValidator.requireAllowed(addr, host);
        }
    }

    /**
     * Reads up to {@code max} bytes from {@code in} and returns the byte array.
     * Throws {@link IOException} (mapped to a 413-style error upstream) if the
     * stream exceeds the cap — we abort eagerly rather than wait for EOF.
     */
    static byte[] readWithCap(InputStream in, long max) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buf)) >= 0) {
            total += n;
            if (total > max) {
                throw new IOException(
                        "URL response exceeds maximum size of " + max + " bytes");
            }
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }

    /**
     * Picks a filename so {@code DocumentNormalizer}/Tika can do format detection.
     * Uses the URL path's last segment if it has an extension; otherwise falls back
     * to a generic name with an extension derived from {@code Content-Type}.
     */
    static String deriveFilename(URI uri, String contentType) {
        String path = uri.getPath();
        if (path != null && !path.isEmpty()) {
            int slash = path.lastIndexOf('/');
            String last = slash >= 0 ? path.substring(slash + 1) : path;
            if (!last.isEmpty() && last.contains(".") && !last.startsWith(".")) {
                return last;
            }
        }
        return "download" + extensionFor(contentType);
    }

    private static String extensionFor(String contentType) {
        if (contentType == null) return ".html";
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.startsWith("application/pdf")) return ".pdf";
        if (ct.startsWith("text/plain")) return ".txt";
        if (ct.contains("markdown")) return ".md";
        if (ct.contains("json")) return ".json";
        if (ct.contains("xml")) return ".xml";
        if (ct.contains("csv")) return ".csv";
        if (ct.contains("html")) return ".html";
        return ".html";
    }

    /** Result of a successful fetch — bytes ready for Tika + the wizard pipeline. */
    public record FetchResult(byte[] bytes, String filename, String contentType) {
        public FetchResult {
            if (bytes == null) throw new IllegalArgumentException("bytes is required");
            if (filename == null || filename.isBlank()) throw new IllegalArgumentException("filename is required");
        }
    }

    /**
     * Decides whether a resolved IP address is safe to fetch from. Injected so
     * tests can authorize a localhost test server while keeping the production
     * path locked down to public addresses only.
     */
    public interface HostValidator {
        void requireAllowed(InetAddress addr, String host);
    }

    /**
     * Production validator — rejects loopback, link-local, site-local, multicast,
     * any-local, CGNAT, and IPv6 ULA addresses. Allows only globally routable IPs.
     */
    public static final class DefaultHostValidator implements HostValidator {
        @Override
        public void requireAllowed(InetAddress addr, String host) {
            String detail = host + " -> " + addr.getHostAddress();
            if (addr.isLoopbackAddress()) {
                throw new IllegalArgumentException("Refusing to fetch loopback address: " + detail);
            }
            if (addr.isAnyLocalAddress()) {
                throw new IllegalArgumentException("Refusing to fetch wildcard / any-local address: " + detail);
            }
            if (addr.isLinkLocalAddress()) {
                // Covers IPv4 169.254/16 (incl. cloud metadata 169.254.169.254)
                // and IPv6 fe80::/10.
                throw new IllegalArgumentException("Refusing to fetch link-local address: " + detail);
            }
            if (addr.isSiteLocalAddress()) {
                // Covers IPv4 RFC1918 ranges 10/8, 172.16/12, 192.168/16.
                throw new IllegalArgumentException("Refusing to fetch private (RFC1918) address: " + detail);
            }
            if (addr.isMulticastAddress()) {
                throw new IllegalArgumentException("Refusing to fetch multicast address: " + detail);
            }
            if (addr instanceof Inet4Address inet4) {
                byte[] b = inet4.getAddress();
                int o1 = b[0] & 0xff;
                int o2 = b[1] & 0xff;
                // RFC 6598 carrier-grade NAT range 100.64.0.0/10.
                if (o1 == 100 && (o2 & 0xc0) == 0x40) {
                    throw new IllegalArgumentException("Refusing to fetch CGNAT (100.64/10) address: " + detail);
                }
                // 0.0.0.0/8 — current network / unspecified ("this" network).
                if (o1 == 0) {
                    throw new IllegalArgumentException("Refusing to fetch 0.0.0.0/8 address: " + detail);
                }
                // 255.255.255.255 broadcast.
                if ((b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xff
                        && (b[2] & 0xff) == 0xff && (b[3] & 0xff) == 0xff) {
                    throw new IllegalArgumentException("Refusing to fetch broadcast address: " + detail);
                }
            }
            if (addr instanceof Inet6Address inet6) {
                byte[] b = inet6.getAddress();
                // IPv6 Unique Local Addresses fc00::/7 — RFC 4193.
                if ((b[0] & 0xfe) == 0xfc) {
                    throw new IllegalArgumentException("Refusing to fetch IPv6 ULA (fc00::/7) address: " + detail);
                }
                // IPv4-mapped IPv6 ::ffff:0:0/96 — re-validate the embedded v4.
                if (inet6.isIPv4CompatibleAddress() || isIPv4Mapped(b)) {
                    byte[] v4 = { b[12], b[13], b[14], b[15] };
                    try {
                        InetAddress mapped = InetAddress.getByAddress(host, v4);
                        requireAllowed(mapped, host);
                    } catch (UnknownHostException e) {
                        throw new IllegalArgumentException("Invalid IPv4-mapped IPv6 address: " + detail, e);
                    }
                }
            }
        }

        private static boolean isIPv4Mapped(byte[] b) {
            for (int i = 0; i < 10; i++) {
                if (b[i] != 0) return false;
            }
            return (b[10] & 0xff) == 0xff && (b[11] & 0xff) == 0xff;
        }
    }

    /** Allow-all validator for tests. */
    public static final class PermissiveHostValidator implements HostValidator {
        @Override
        public void requireAllowed(InetAddress addr, String host) {
            // intentionally empty
        }
    }
}

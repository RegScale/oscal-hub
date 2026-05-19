package gov.nist.oscal.tools.api.service.ai;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlContentFetcherTest {

    /**
     * Standalone test server reachable on 127.0.0.1. Tests pair this with a
     * {@link UrlContentFetcher.PermissiveHostValidator} so the loopback-block
     * doesn't trip on our own test fixtures.
     */
    private HttpServer server;
    private String baseUrl;
    private UrlContentFetcher fetcher;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        fetcher = new UrlContentFetcher(
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
                new UrlContentFetcher.PermissiveHostValidator());
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    // ------------------------------------------------------------------
    // Happy path
    // ------------------------------------------------------------------

    @Test
    void fetchesHtmlAndUsesPathFilename() throws Exception {
        server.createContext("/sp800-53.html", ex -> respond(ex, 200,
                "text/html; charset=utf-8",
                "<html><body><h1>SP 800-53</h1></body></html>"));

        UrlContentFetcher.FetchResult r = fetcher.fetch(baseUrl + "/sp800-53.html");

        assertThat(new String(r.bytes(), StandardCharsets.UTF_8)).contains("<h1>SP 800-53</h1>");
        assertThat(r.filename()).isEqualTo("sp800-53.html");
        assertThat(r.contentType()).startsWith("text/html");
    }

    @Test
    void fetchesPdfBytesIntact() throws Exception {
        byte[] pdfBytes = "%PDF-1.4\n%fake\n%%EOF".getBytes(StandardCharsets.UTF_8);
        server.createContext("/doc.pdf", ex -> respondBytes(ex, 200, "application/pdf", pdfBytes));

        UrlContentFetcher.FetchResult r = fetcher.fetch(baseUrl + "/doc.pdf");

        assertThat(r.bytes()).isEqualTo(pdfBytes);
        assertThat(r.filename()).isEqualTo("doc.pdf");
        assertThat(r.contentType()).startsWith("application/pdf");
    }

    @Test
    void sendsCustomUserAgent() throws Exception {
        AtomicInteger seen = new AtomicInteger();
        List<String> seenUa = new ArrayList<>();
        server.createContext("/", ex -> {
            seenUa.add(ex.getRequestHeaders().getFirst("User-Agent"));
            seen.incrementAndGet();
            respond(ex, 200, "text/plain", "ok");
        });
        fetcher.fetch(baseUrl + "/anything");
        assertThat(seen.get()).isEqualTo(1);
        assertThat(seenUa.getFirst()).startsWith("OSCAL-Hub-Wizard/");
    }

    // ------------------------------------------------------------------
    // Filename derivation
    // ------------------------------------------------------------------

    @Test
    void derivesFilenameFromContentTypeWhenPathHasNoExtension() {
        assertThat(UrlContentFetcher.deriveFilename(URI.create("https://example.com/"), "application/pdf"))
                .isEqualTo("download.pdf");
        assertThat(UrlContentFetcher.deriveFilename(URI.create("https://example.com/docs"), "text/markdown"))
                .isEqualTo("download.md");
        assertThat(UrlContentFetcher.deriveFilename(URI.create("https://example.com/api"), "application/json"))
                .isEqualTo("download.json");
        assertThat(UrlContentFetcher.deriveFilename(URI.create("https://example.com/feed"), "application/xml"))
                .isEqualTo("download.xml");
        assertThat(UrlContentFetcher.deriveFilename(URI.create("https://example.com/page"), "text/html"))
                .isEqualTo("download.html");
        assertThat(UrlContentFetcher.deriveFilename(URI.create("https://example.com/raw"), null))
                .isEqualTo("download.html");
    }

    @Test
    void derivesFilenameFromUrlPathWhenItHasExtension() {
        assertThat(UrlContentFetcher.deriveFilename(URI.create("https://nist.gov/sp800-53r5.pdf"), "application/pdf"))
                .isEqualTo("sp800-53r5.pdf");
        assertThat(UrlContentFetcher.deriveFilename(URI.create("https://example.com/a/b/c.xml"), null))
                .isEqualTo("c.xml");
    }

    @Test
    void ignoresDotfileLikeLastSegment() {
        // Don't pick up ".htaccess" — strip leading-dot segments and fall back.
        assertThat(UrlContentFetcher.deriveFilename(URI.create("https://example.com/.something"), "text/plain"))
                .isEqualTo("download.txt");
    }

    // ------------------------------------------------------------------
    // Scheme validation
    // ------------------------------------------------------------------

    @Test
    void rejectsNullOrBlankUrl() {
        assertThatThrownBy(() -> fetcher.fetch(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
        assertThatThrownBy(() -> fetcher.fetch("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsFileScheme() {
        assertThatThrownBy(() -> fetcher.fetch("file:///etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only http and https");
    }

    @Test
    void rejectsFtpScheme() {
        assertThatThrownBy(() -> fetcher.fetch("ftp://example.com/file"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only http and https");
    }

    @Test
    void rejectsJavascriptScheme() {
        assertThatThrownBy(() -> fetcher.fetch("javascript:alert(1)"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDataScheme() {
        assertThatThrownBy(() -> fetcher.fetch("data:text/plain;base64,SGVsbG8="))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsGopherScheme() {
        assertThatThrownBy(() -> fetcher.fetch("gopher://example.com/"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsJarScheme() {
        assertThatThrownBy(() -> fetcher.fetch("jar:file:/tmp/x.jar!/foo"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingScheme() {
        assertThatThrownBy(() -> fetcher.fetch("//example.com/x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsHostlessUrl() {
        assertThatThrownBy(() -> fetcher.fetch("http:///path"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("host");
    }

    // ------------------------------------------------------------------
    // SSRF — DefaultHostValidator unit tests against synthetic addresses
    // ------------------------------------------------------------------

    private final UrlContentFetcher.HostValidator validator = new UrlContentFetcher.DefaultHostValidator();

    @Test
    void defaultValidatorBlocksLoopbackV4() throws Exception {
        InetAddress addr = InetAddress.getByAddress("evil.example", new byte[]{127, 0, 0, 1});
        assertThatThrownBy(() -> validator.requireAllowed(addr, "evil.example"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loopback");
    }

    @Test
    void defaultValidatorBlocksLoopbackV6() throws Exception {
        byte[] v6 = new byte[16];
        v6[15] = 1; // ::1
        InetAddress addr = InetAddress.getByAddress("evil.example", v6);
        assertThatThrownBy(() -> validator.requireAllowed(addr, "evil.example"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loopback");
    }

    @Test
    void defaultValidatorBlocksAnyLocalV4() throws Exception {
        InetAddress addr = InetAddress.getByAddress("wild.example", new byte[]{0, 0, 0, 0});
        // 0.0.0.0 maps to isAnyLocalAddress() OR our 0.0.0.0/8 rule — both reject.
        assertThatThrownBy(() -> validator.requireAllowed(addr, "wild.example"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultValidatorBlocksGcpMetadataIp() throws Exception {
        // 169.254.169.254 — used by GCP, AWS, Azure IMDS. Falls under link-local.
        InetAddress addr = InetAddress.getByAddress("metadata", new byte[]{(byte)169, (byte)254, (byte)169, (byte)254});
        assertThatThrownBy(() -> validator.requireAllowed(addr, "metadata"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("link-local");
    }

    @Test
    void defaultValidatorBlocksRfc1918Ten() throws Exception {
        InetAddress addr = InetAddress.getByAddress("evil", new byte[]{10, 0, 0, 5});
        assertThatThrownBy(() -> validator.requireAllowed(addr, "evil"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RFC1918");
    }

    @Test
    void defaultValidatorBlocksRfc1918OneSevenTwo() throws Exception {
        InetAddress addr = InetAddress.getByAddress("evil", new byte[]{(byte)172, 16, 0, 1});
        assertThatThrownBy(() -> validator.requireAllowed(addr, "evil"))
                .isInstanceOf(IllegalArgumentException.class);
        // 172.31 is the top of the range
        InetAddress addr2 = InetAddress.getByAddress("evil", new byte[]{(byte)172, 31, (byte)255, (byte)254});
        assertThatThrownBy(() -> validator.requireAllowed(addr2, "evil"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultValidatorBlocksRfc1918OneNineTwo() throws Exception {
        InetAddress addr = InetAddress.getByAddress("evil", new byte[]{(byte)192, (byte)168, 1, 1});
        assertThatThrownBy(() -> validator.requireAllowed(addr, "evil"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultValidatorBlocksCgnatRange() throws Exception {
        InetAddress addr = InetAddress.getByAddress("evil", new byte[]{100, 64, 0, 1});
        assertThatThrownBy(() -> validator.requireAllowed(addr, "evil"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CGNAT");
        // Upper bound of the /10
        InetAddress upper = InetAddress.getByAddress("evil", new byte[]{100, 127, (byte)255, (byte)254});
        assertThatThrownBy(() -> validator.requireAllowed(upper, "evil"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CGNAT");
    }

    @Test
    void defaultValidatorAllowsPublicIpJustAboveCgnat() throws Exception {
        // 100.128.0.0 is outside CGNAT (100.64/10) — should pass.
        InetAddress addr = InetAddress.getByAddress("ok", new byte[]{100, (byte)128, 0, 1});
        validator.requireAllowed(addr, "ok"); // no throw
    }

    @Test
    void defaultValidatorBlocksLinkLocalV6() throws Exception {
        byte[] v6 = new byte[16];
        v6[0] = (byte) 0xfe;
        v6[1] = (byte) 0x80;
        v6[15] = 1; // fe80::1
        InetAddress addr = InetAddress.getByAddress("evil", v6);
        assertThatThrownBy(() -> validator.requireAllowed(addr, "evil"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("link-local");
    }

    @Test
    void defaultValidatorBlocksIpv6UlaRange() throws Exception {
        byte[] v6 = new byte[16];
        v6[0] = (byte) 0xfc; // fc00::/7 boundary
        v6[15] = 1;
        InetAddress addr = InetAddress.getByAddress("evil", v6);
        assertThatThrownBy(() -> validator.requireAllowed(addr, "evil"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ULA");

        byte[] v6b = new byte[16];
        v6b[0] = (byte) 0xfd; // also fc00::/7
        v6b[15] = 1;
        InetAddress addrB = InetAddress.getByAddress("evil", v6b);
        assertThatThrownBy(() -> validator.requireAllowed(addrB, "evil"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ULA");
    }

    @Test
    void defaultValidatorBlocksIpv4MappedLoopback() throws Exception {
        // ::ffff:127.0.0.1
        byte[] v6 = new byte[16];
        v6[10] = (byte) 0xff;
        v6[11] = (byte) 0xff;
        v6[12] = 127;
        v6[15] = 1;
        InetAddress addr = InetAddress.getByAddress("evil", v6);
        // Java sometimes auto-converts this to Inet4Address; either way, the validator
        // must reject because the underlying address is loopback.
        assertThatThrownBy(() -> validator.requireAllowed(addr, "evil"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loopback");
    }

    @Test
    void defaultValidatorBlocksMulticastV4() throws Exception {
        InetAddress addr = InetAddress.getByAddress("evil", new byte[]{(byte)224, 0, 0, 1});
        assertThatThrownBy(() -> validator.requireAllowed(addr, "evil"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("multicast");
    }

    @Test
    void defaultValidatorBlocksZeroSubnet() throws Exception {
        InetAddress addr = InetAddress.getByAddress("evil", new byte[]{0, 1, 2, 3});
        assertThatThrownBy(() -> validator.requireAllowed(addr, "evil"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultValidatorBlocksBroadcastV4() throws Exception {
        InetAddress addr = InetAddress.getByAddress("evil",
                new byte[]{(byte)255, (byte)255, (byte)255, (byte)255});
        assertThatThrownBy(() -> validator.requireAllowed(addr, "evil"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultValidatorAllowsPublicIp() throws Exception {
        // 8.8.8.8 — Google public DNS
        InetAddress addr = InetAddress.getByAddress("dns.google", new byte[]{8, 8, 8, 8});
        validator.requireAllowed(addr, "dns.google"); // no throw
    }

    // ------------------------------------------------------------------
    // SSRF integration — actually try to fetch loopback with the default
    // validator and confirm it short-circuits BEFORE the connect attempt
    // ------------------------------------------------------------------

    @Test
    void fetchWithDefaultValidatorBlocksLoopbackUrl() {
        UrlContentFetcher locked = new UrlContentFetcher(); // default validator
        assertThatThrownBy(() -> locked.fetch(baseUrl + "/anything"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loopback");
    }

    @Test
    void fetchWithDefaultValidatorBlocksGcpMetadata() {
        UrlContentFetcher locked = new UrlContentFetcher();
        assertThatThrownBy(() -> locked.fetch("http://169.254.169.254/computeMetadata/v1/"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("link-local");
    }

    @Test
    void fetchWithDefaultValidatorBlocksLocalhostHostname() {
        UrlContentFetcher locked = new UrlContentFetcher();
        // "localhost" resolves to 127.0.0.1 / ::1 — validator must catch this even
        // though the URL string doesn't contain a numeric private IP.
        assertThatThrownBy(() -> locked.fetch("http://localhost:9999/"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loopback");
    }

    // ------------------------------------------------------------------
    // Redirect handling
    // ------------------------------------------------------------------

    @Test
    void followsRedirectsUpToLimit() throws Exception {
        // 3 hops then a 200.
        server.createContext("/r1", ex -> redirect(ex, "/r2"));
        server.createContext("/r2", ex -> redirect(ex, "/r3"));
        server.createContext("/r3", ex -> redirect(ex, "/final.txt"));
        server.createContext("/final.txt", ex -> respond(ex, 200, "text/plain", "arrived"));

        UrlContentFetcher.FetchResult r = fetcher.fetch(baseUrl + "/r1");
        assertThat(new String(r.bytes(), StandardCharsets.UTF_8)).isEqualTo("arrived");
        assertThat(r.filename()).isEqualTo("final.txt");
    }

    @Test
    void rejectsTooManyRedirects() throws Exception {
        // 6 hops — exceeds MAX_REDIRECTS = 5
        for (int i = 1; i <= 6; i++) {
            final int next = i + 1;
            server.createContext("/h" + i, ex -> redirect(ex, "/h" + next));
        }
        server.createContext("/h7", ex -> respond(ex, 200, "text/plain", "shouldn't reach"));

        assertThatThrownBy(() -> fetcher.fetch(baseUrl + "/h1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Too many redirects");
    }

    @Test
    void rejectsRedirectToNonHttpScheme() throws Exception {
        server.createContext("/jump", ex -> redirect(ex, "file:///etc/passwd"));
        assertThatThrownBy(() -> fetcher.fetch(baseUrl + "/jump"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scheme");
    }

    @Test
    void rejectsRedirectToPrivateIp() throws Exception {
        // Build a fetcher with the production validator but inject the real client.
        UrlContentFetcher locked = new UrlContentFetcher(
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
                new RedirectAwareValidator());
        // The test server is on 127.0.0.1; we use a permissive-then-strict validator
        // to allow the initial request but block on the redirect target.
        server.createContext("/jump", ex -> redirect(ex, "http://10.0.0.5/internal"));
        assertThatThrownBy(() -> locked.fetch(baseUrl + "/jump"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Validator that allows 127.0.0.1 (the test server) but defers to
     * {@link UrlContentFetcher.DefaultHostValidator} for anything else. Lets us
     * exercise the redirect-target validation without the initial connection
     * being rejected.
     */
    static class RedirectAwareValidator implements UrlContentFetcher.HostValidator {
        private final UrlContentFetcher.DefaultHostValidator strict =
                new UrlContentFetcher.DefaultHostValidator();
        @Override
        public void requireAllowed(InetAddress addr, String host) {
            if (addr.isLoopbackAddress()) return; // allow the test server
            strict.requireAllowed(addr, host);
        }
    }

    // ------------------------------------------------------------------
    // Error responses
    // ------------------------------------------------------------------

    @Test
    void surfacesHttp404() {
        server.createContext("/missing", ex -> respond(ex, 404, "text/plain", "not here"));
        assertThatThrownBy(() -> fetcher.fetch(baseUrl + "/missing"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("404");
    }

    @Test
    void surfacesHttp500() {
        server.createContext("/boom", ex -> respond(ex, 500, "text/plain", "boom"));
        assertThatThrownBy(() -> fetcher.fetch(baseUrl + "/boom"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("500");
    }

    // ------------------------------------------------------------------
    // Size cap
    // ------------------------------------------------------------------

    @Test
    void readWithCapAllowsUnderLimit() throws IOException {
        byte[] in = "abcde".getBytes(StandardCharsets.UTF_8);
        byte[] out = UrlContentFetcher.readWithCap(new ByteArrayInputStream(in), 100);
        assertThat(out).isEqualTo(in);
    }

    @Test
    void readWithCapThrowsAtBoundary() {
        byte[] in = new byte[101];
        assertThatThrownBy(() -> UrlContentFetcher.readWithCap(new ByteArrayInputStream(in), 100))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("maximum size");
    }

    @Test
    void fetchRejectsOversizedResponse() throws Exception {
        byte[] big = new byte[(int) UrlContentFetcher.MAX_BYTES + 1];
        server.createContext("/big.bin",
                ex -> respondBytes(ex, 200, "application/octet-stream", big));
        assertThatThrownBy(() -> fetcher.fetch(baseUrl + "/big.bin"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("maximum size");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void respond(HttpExchange ex, int code, String contentType, String body) throws IOException {
        respondBytes(ex, code, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void respondBytes(HttpExchange ex, int code, String contentType, byte[] body) throws IOException {
        ex.getResponseHeaders().add("Content-Type", contentType);
        ex.sendResponseHeaders(code, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    private static void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().add("Location", location);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }
}

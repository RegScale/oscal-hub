package gov.nist.oscal.tools.api.filter;

import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for SecurityHeadersFilter.
 *
 * Coverage focus: every conditional branch in doFilterInternal — disabled
 * bypass, HSTS-only-when-secure, CSP report-only mode, individual header
 * toggles, and the X-Forwarded-Proto / requireHttps fallbacks for the
 * "is this connection secure" check.
 */
class SecurityHeadersFilterTest {

    private SecurityHeadersFilter filter;
    private SecurityHeadersConfig config;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new SecurityHeadersFilter();
        config = new SecurityHeadersConfig();
        config.setEnabled(true);
        ReflectionTestUtils.setField(filter, "config", config);
        chain = mock(FilterChain.class);
    }

    @Test
    void disabled_bypassesAllHeaders_andStillCallsChain() throws Exception {
        config.setEnabled(false);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), res, chain);

        verify(chain, times(1)).doFilter(any(), any());
        // No headers should be set
        assertThat(res.getHeaderNames()).isEmpty();
    }

    @Test
    void enabled_andSecureRequest_addsAllConfiguredHeaders() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setSecure(true);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        // HSTS only on secure connections (request.isSecure() == true here)
        assertThat(res.getHeader("Strict-Transport-Security"))
                .contains("max-age=").contains("includeSubDomains");
        assertThat(res.getHeader("Content-Security-Policy")).contains("default-src 'self'");
        assertThat(res.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(res.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(res.getHeader("X-XSS-Protection")).isEqualTo("1; mode=block");
        assertThat(res.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
        assertThat(res.getHeader("Permissions-Policy")).contains("geolocation=()");
    }

    @Test
    void hsts_onlyAddedWhenConnectionIsSecure() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        // Default isSecure() == false, no X-Forwarded-Proto, requireHttps=false → HSTS skipped
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader("Strict-Transport-Security")).isNull();
        // But other headers still appear
        assertThat(res.getHeader("X-Frame-Options")).isNotNull();
    }

    @Test
    void hsts_addedWhenXForwardedProtoIsHttps_eventOverPlainConnection() throws Exception {
        // Production case: traffic terminates at a load balancer. Connection to
        // the JVM is plaintext but the original request is HTTPS, so HSTS
        // should still be added.
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-Proto", "https");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader("Strict-Transport-Security")).isNotNull();
    }

    @Test
    void hsts_addedWhenRequireHttpsFlagSet_evenOnPlainLocalhost() throws Exception {
        // Dev / forced-HSTS escape hatch.
        MockHttpServletRequest req = new MockHttpServletRequest();
        config.setRequireHttps(true);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader("Strict-Transport-Security")).isNotNull();
    }

    @Test
    void hsts_disabled_omitsHeaderEvenOnSecureConnection() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setSecure(true);
        config.getHsts().setEnabled(false);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader("Strict-Transport-Security")).isNull();
    }

    @Test
    void csp_reportOnly_usesReportOnlyHeaderName() throws Exception {
        // Report-only mode lets ops dial in a CSP without breaking the page.
        // The header NAME changes; the value is the same.
        config.getCsp().setReportOnly(true);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), res, chain);

        assertThat(res.getHeader("Content-Security-Policy")).isNull();
        assertThat(res.getHeader("Content-Security-Policy-Report-Only")).contains("default-src");
    }

    @Test
    void csp_disabled_omitsBothHeaders() throws Exception {
        config.getCsp().setEnabled(false);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), res, chain);

        assertThat(res.getHeader("Content-Security-Policy")).isNull();
        assertThat(res.getHeader("Content-Security-Policy-Report-Only")).isNull();
    }

    @Test
    void contentTypeOptions_disabled_omitsNosniffHeader() throws Exception {
        config.setEnableContentTypeOptions(false);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), res, chain);

        assertThat(res.getHeader("X-Content-Type-Options")).isNull();
    }

    @Test
    void xssProtection_disabled_omitsHeader() throws Exception {
        config.setEnableXssProtection(false);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), res, chain);

        assertThat(res.getHeader("X-XSS-Protection")).isNull();
    }

    @Test
    void referrerPolicy_nullOrEmpty_omitsHeader() throws Exception {
        // Empty string config should NOT emit "Referrer-Policy:" with empty value.
        config.setReferrerPolicy("");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), res, chain);
        assertThat(res.getHeader("Referrer-Policy")).isNull();

        config.setReferrerPolicy(null);
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), res2, chain);
        assertThat(res2.getHeader("Referrer-Policy")).isNull();
    }

    @Test
    void permissionsPolicy_disabled_omitsHeader() throws Exception {
        config.getPermissionsPolicy().setEnabled(false);
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), res, chain);

        assertThat(res.getHeader("Permissions-Policy")).isNull();
    }

    @Test
    void frameOptions_customPolicy_isHonored() throws Exception {
        // The default DENY blocks ALL framing. If an admin needs SAMEORIGIN
        // (e.g., for an in-app preview iframe), the config value should win.
        config.getFrameOptions().setPolicy("SAMEORIGIN");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), res, chain);

        assertThat(res.getHeader("X-Frame-Options")).isEqualTo("SAMEORIGIN");
    }

    @Test
    void xForwardedProto_caseInsensitive_https() throws Exception {
        // Some proxies emit "HTTPS" upper-cased; the filter must accept it.
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-Proto", "HTTPS");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader("Strict-Transport-Security")).isNotNull();
    }

    @Test
    void chain_isAlwaysContinued_evenAfterAllHeadersSet() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain spy = mock(FilterChain.class);

        filter.doFilter(req, res, spy);

        // The filter is non-terminating — request must always proceed downstream.
        verify(spy, times(1)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
        verify(spy, never()).doFilter(null, null);
    }
}

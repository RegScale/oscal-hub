package gov.nist.oscal.tools.api.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the SecurityHeadersConfig builders.
 *
 * The build() methods on each inner class produce HTTP header values; the
 * SecurityHeadersFilter wires them into responses. Errors here are highly
 * visible — a malformed CSP can break a production deploy — so it's worth
 * locking down every flag combination.
 */
class SecurityHeadersConfigTest {

    // ---- HstsConfig ----

    @Test
    void hsts_defaults_includeMaxAgeAndSubdomains_butNotPreload() {
        // Default config: 1-year max-age, includeSubDomains, no preload.
        SecurityHeadersConfig.HstsConfig hsts = new SecurityHeadersConfig.HstsConfig();
        String header = hsts.build();
        assertThat(header).contains("max-age=31536000");
        assertThat(header).contains("includeSubDomains");
        assertThat(header).doesNotContain("preload");
    }

    @Test
    void hsts_disablingSubdomains_omitsToken() {
        SecurityHeadersConfig.HstsConfig hsts = new SecurityHeadersConfig.HstsConfig();
        hsts.setIncludeSubDomains(false);
        assertThat(hsts.build()).doesNotContain("includeSubDomains");
    }

    @Test
    void hsts_enablingPreload_appendsToken() {
        SecurityHeadersConfig.HstsConfig hsts = new SecurityHeadersConfig.HstsConfig();
        hsts.setPreload(true);
        assertThat(hsts.build()).contains("preload");
    }

    @Test
    void hsts_customMaxAge_isHonored() {
        // Config knob exists for short max-age during HSTS rollout.
        SecurityHeadersConfig.HstsConfig hsts = new SecurityHeadersConfig.HstsConfig();
        hsts.setMaxAge(300);
        assertThat(hsts.build()).contains("max-age=300");
    }

    // ---- CspConfig ----

    @Test
    void csp_defaults_emitAllDirectives() {
        // CSP is the OWASP-recommended baseline; default-src + script-src are
        // the most-load-bearing directives so they're worth pinning.
        SecurityHeadersConfig.CspConfig csp = new SecurityHeadersConfig.CspConfig();
        String header = csp.build();
        assertThat(header)
                .contains("default-src 'self'")
                .contains("script-src 'self'")
                .contains("style-src 'self' 'unsafe-inline'")
                .contains("img-src 'self' data: https:")
                .contains("font-src 'self' data:")
                .contains("connect-src 'self'")
                .contains("frame-src 'none'")
                .contains("object-src 'none'")
                .contains("base-uri 'self'")
                .contains("form-action 'self'");
    }

    @Test
    void csp_customDirectives_override() {
        SecurityHeadersConfig.CspConfig csp = new SecurityHeadersConfig.CspConfig();
        csp.setScriptSrc("'self' https://cdn.example.com");
        csp.setConnectSrc("'self' https://api.example.com wss://api.example.com");
        String header = csp.build();
        assertThat(header).contains("script-src 'self' https://cdn.example.com");
        assertThat(header).contains("connect-src 'self' https://api.example.com wss://api.example.com");
    }

    // ---- PermissionsPolicyConfig ----

    @Test
    void permissionsPolicy_defaults_disableAllSensitiveFeatures() {
        // Defaults are deliberately restrictive: empty allowlist () for each
        // sensitive browser feature so a compromised script can't access them.
        SecurityHeadersConfig.PermissionsPolicyConfig p = new SecurityHeadersConfig.PermissionsPolicyConfig();
        String header = p.build();
        assertThat(header)
                .contains("geolocation=()")
                .contains("microphone=()")
                .contains("camera=()")
                .contains("payment=()")
                .contains("usb=()")
                .contains("magnetometer=()")
                .contains("gyroscope=()")
                .contains("accelerometer=()");
    }

    @Test
    void permissionsPolicy_allowingFeature_emitsConfiguredAllowlist() {
        SecurityHeadersConfig.PermissionsPolicyConfig p = new SecurityHeadersConfig.PermissionsPolicyConfig();
        p.setCamera("(self)");
        assertThat(p.build()).contains("camera=(self)");
    }

    // ---- SecurityHeadersConfig top-level ----

    @Test
    void topLevelConfig_defaults_areOpinionatedSecure() {
        // The application's default posture: enabled=false (off until explicitly
        // turned on per env) but every nested toggle is in its secure setting.
        SecurityHeadersConfig c = new SecurityHeadersConfig();
        assertThat(c.isEnabled()).isFalse(); // off-by-default
        assertThat(c.isRequireHttps()).isFalse();
        assertThat(c.getHsts().isEnabled()).isTrue();
        assertThat(c.getCsp().isEnabled()).isTrue();
        assertThat(c.getCsp().isReportOnly()).isFalse();
        assertThat(c.getFrameOptions().getPolicy()).isEqualTo("DENY");
        assertThat(c.isEnableContentTypeOptions()).isTrue();
        assertThat(c.isEnableXssProtection()).isTrue();
        assertThat(c.getReferrerPolicy()).isEqualTo("strict-origin-when-cross-origin");
        assertThat(c.getPermissionsPolicy().isEnabled()).isTrue();
    }
}

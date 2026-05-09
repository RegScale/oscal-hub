package gov.nist.oscal.tools.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for EnvironmentConfig — the startup-time validator that
 * fails fast on dangerous prod configurations (H2 console enabled, H2
 * database, etc.) and warns on softer issues (swagger, no rate limit).
 *
 * Coverage focus: every prod-only critical/warning branch, plus the
 * staging/dev branches that just log without enforcement, plus the
 * password-masking helper.
 */
class EnvironmentConfigTest {

    // ---------- environment classification helpers ----------

    @Test
    void isProductionEnvironment_recognizesBothShortAndLongAlias() {
        EnvironmentConfig c = new EnvironmentConfig(mock(Environment.class));
        ReflectionTestUtils.setField(c, "activeProfile", "prod");
        assertThat(c.isProductionEnvironment()).isTrue();

        ReflectionTestUtils.setField(c, "activeProfile", "production");
        assertThat(c.isProductionEnvironment()).isTrue();

        ReflectionTestUtils.setField(c, "activeProfile", "PROD");
        assertThat(c.isProductionEnvironment()).isTrue();
    }

    @Test
    void isDevelopmentEnvironment_recognizesBothAliases() {
        EnvironmentConfig c = new EnvironmentConfig(mock(Environment.class));
        ReflectionTestUtils.setField(c, "activeProfile", "dev");
        assertThat(c.isDevelopmentEnvironment()).isTrue();
        ReflectionTestUtils.setField(c, "activeProfile", "development");
        assertThat(c.isDevelopmentEnvironment()).isTrue();
    }

    @Test
    void isStagingEnvironment_caseInsensitive() {
        EnvironmentConfig c = new EnvironmentConfig(mock(Environment.class));
        ReflectionTestUtils.setField(c, "activeProfile", "staging");
        assertThat(c.isStagingEnvironment()).isTrue();
        ReflectionTestUtils.setField(c, "activeProfile", "STAGING");
        assertThat(c.isStagingEnvironment()).isTrue();
    }

    @Test
    void environmentClassifiers_areMutuallyExclusive() {
        // No active profile should match more than one classifier.
        EnvironmentConfig c = new EnvironmentConfig(mock(Environment.class));
        for (String p : new String[] {"prod", "staging", "dev", "test", "ci"}) {
            ReflectionTestUtils.setField(c, "activeProfile", p);
            int matched = 0;
            if (c.isProductionEnvironment()) matched++;
            if (c.isStagingEnvironment()) matched++;
            if (c.isDevelopmentEnvironment()) matched++;
            assertThat(matched).as("profile %s", p).isLessThanOrEqualTo(1);
        }
    }

    // ---------- production validation: critical issues ----------

    @Test
    void prodWithH2ConsoleEnabled_throwsBlocking() {
        // H2 console exposes a SQL-execution UI to anyone who can reach it,
        // which in production is everyone. This must fail boot.
        EnvironmentConfig c = configFor("prod");
        ReflectionTestUtils.setField(c, "h2ConsoleEnabled", true);
        ReflectionTestUtils.setField(c, "databaseUrl", "jdbc:postgresql://prod-db:5432/oscal");

        assertThatThrownBy(c::logEnvironmentInfo)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("critical security issues");
    }

    @Test
    void prodWithH2DatabaseUrl_throwsBlocking() {
        // H2 isn't production-grade. Using it in prod is almost always a
        // misconfigured environment promoting dev settings.
        EnvironmentConfig c = configFor("prod");
        ReflectionTestUtils.setField(c, "h2ConsoleEnabled", false);
        ReflectionTestUtils.setField(c, "databaseUrl", "jdbc:h2:mem:testdb");

        assertThatThrownBy(c::logEnvironmentInfo)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void prodWithMultipleCriticals_messageIncludesCount() {
        // Both criticals at once → message should report both, not just one.
        EnvironmentConfig c = configFor("prod");
        ReflectionTestUtils.setField(c, "h2ConsoleEnabled", true);
        ReflectionTestUtils.setField(c, "databaseUrl", "jdbc:h2:mem:test");

        assertThatThrownBy(c::logEnvironmentInfo)
                .hasMessageContaining("2");
    }

    // ---------- production validation: warnings (do not block) ----------

    @Test
    void prodWithWarnings_logsButDoesNotThrow() {
        // Swagger, no security headers, no HTTPS, no rate limit, no audit log
        // — all soft warnings. Boot should still succeed.
        EnvironmentConfig c = configFor("prod");
        ReflectionTestUtils.setField(c, "h2ConsoleEnabled", false);
        ReflectionTestUtils.setField(c, "swaggerEnabled", true);
        ReflectionTestUtils.setField(c, "securityHeadersEnabled", false);
        ReflectionTestUtils.setField(c, "httpsRequired", false);
        ReflectionTestUtils.setField(c, "rateLimitEnabled", false);
        ReflectionTestUtils.setField(c, "auditLoggingEnabled", false);
        ReflectionTestUtils.setField(c, "databaseUrl", "jdbc:postgresql://prod-db:5432/oscal");

        c.logEnvironmentInfo(); // does not throw
    }

    @Test
    void prodFullyHardened_logsCleanly() {
        // The "everything is set correctly" path — make sure it doesn't
        // accidentally throw.
        EnvironmentConfig c = configFor("prod");
        ReflectionTestUtils.setField(c, "h2ConsoleEnabled", false);
        ReflectionTestUtils.setField(c, "swaggerEnabled", false);
        ReflectionTestUtils.setField(c, "securityHeadersEnabled", true);
        ReflectionTestUtils.setField(c, "httpsRequired", true);
        ReflectionTestUtils.setField(c, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(c, "auditLoggingEnabled", true);
        ReflectionTestUtils.setField(c, "databaseUrl", "jdbc:postgresql://prod-db:5432/oscal");

        c.logEnvironmentInfo();
    }

    // ---------- staging / dev profiles: log only, never throw ----------

    @Test
    void staging_doesNotEnforceCriticalChecks() {
        // Staging should let you run with H2 + console for fast iteration —
        // the prod blockers don't apply.
        EnvironmentConfig c = configFor("staging");
        ReflectionTestUtils.setField(c, "h2ConsoleEnabled", true);
        ReflectionTestUtils.setField(c, "swaggerEnabled", true);
        ReflectionTestUtils.setField(c, "databaseUrl", "jdbc:h2:mem:test");

        c.logEnvironmentInfo();
    }

    @Test
    void dev_doesNotEnforceCriticalChecks() {
        EnvironmentConfig c = configFor("dev");
        ReflectionTestUtils.setField(c, "h2ConsoleEnabled", true);
        ReflectionTestUtils.setField(c, "swaggerEnabled", true);
        ReflectionTestUtils.setField(c, "databaseUrl", "jdbc:h2:mem:test");

        c.logEnvironmentInfo();
    }

    @Test
    void unknownProfile_treatedAsDevelopment_noEnforcement() {
        // The validator's else-branch: anything that isn't prod/staging/dev
        // falls through to the "development" log section. Boot must not throw.
        EnvironmentConfig c = configFor("ci-runner");
        ReflectionTestUtils.setField(c, "databaseUrl", "jdbc:h2:mem:test");
        c.logEnvironmentInfo();
    }

    // ---------- maskSensitiveInfo ----------

    @Test
    void maskSensitiveInfo_replacesPasswordParam() throws Exception {
        // Password in a JDBC URL is the most common credential leak in logs.
        EnvironmentConfig c = new EnvironmentConfig(mock(Environment.class));
        String masked = (String) invokeMask(c,
                "jdbc:postgresql://host:5432/db?user=oscal&password=hunter2");

        assertThat(masked).doesNotContain("hunter2");
        assertThat(masked).contains("password=***");
    }

    @Test
    void maskSensitiveInfo_passwordWithSemicolonDelimiter() throws Exception {
        // SQL Server-style URLs use ; instead of & — masker handles both.
        EnvironmentConfig c = new EnvironmentConfig(mock(Environment.class));
        String masked = (String) invokeMask(c,
                "jdbc:sqlserver://host:1433;user=oscal;password=hunter2;database=db");
        assertThat(masked).doesNotContain("hunter2");
    }

    @Test
    void maskSensitiveInfo_nullOrEmpty_returnsConfiguredPlaceholder() throws Exception {
        EnvironmentConfig c = new EnvironmentConfig(mock(Environment.class));
        assertThat(invokeMask(c, null)).isEqualTo("[not configured]");
        assertThat(invokeMask(c, "")).isEqualTo("[not configured]");
    }

    @Test
    void maskSensitiveInfo_urlWithoutPassword_passesThrough() throws Exception {
        // No-op when the URL has no password param.
        EnvironmentConfig c = new EnvironmentConfig(mock(Environment.class));
        String url = "jdbc:postgresql://host:5432/db";
        assertThat(invokeMask(c, url)).isEqualTo(url);
    }

    // ---------- helpers ----------

    private static EnvironmentConfig configFor(String profile) {
        EnvironmentConfig c = new EnvironmentConfig(mock(Environment.class));
        ReflectionTestUtils.setField(c, "activeProfile", profile);
        ReflectionTestUtils.setField(c, "applicationName", "oscal-cli-api");
        ReflectionTestUtils.setField(c, "serverPort", "8080");
        // Sensible defaults — individual tests override what they care about.
        ReflectionTestUtils.setField(c, "databaseUrl", "");
        ReflectionTestUtils.setField(c, "h2ConsoleEnabled", false);
        ReflectionTestUtils.setField(c, "swaggerEnabled", false);
        ReflectionTestUtils.setField(c, "securityHeadersEnabled", false);
        ReflectionTestUtils.setField(c, "httpsRequired", false);
        ReflectionTestUtils.setField(c, "rateLimitEnabled", false);
        ReflectionTestUtils.setField(c, "auditLoggingEnabled", false);
        return c;
    }

    private static Object invokeMask(EnvironmentConfig c, String url) throws Exception {
        Method m = EnvironmentConfig.class.getDeclaredMethod("maskSensitiveInfo", String.class);
        m.setAccessible(true);
        return m.invoke(c, url);
    }
}

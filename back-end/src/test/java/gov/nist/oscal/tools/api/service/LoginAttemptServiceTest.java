package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.config.AccountSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LoginAttemptService — the brute-force / credential-stuffing
 * lockout layer. Coverage focus:
 *  - Threshold-based account lockout (N failures in window → lock)
 *  - Successful login clears the failure counter
 *  - IP-based tracking is independent and configurable
 *  - Manual admin unlock paths
 *  - Disabled-config short-circuits everywhere
 *  - Null/empty username and IP are no-ops (defensive guards)
 */
class LoginAttemptServiceTest {

    private AccountSecurityConfig config;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        config = mock(AccountSecurityConfig.class);
        // Sensible defaults; individual tests override what they care about.
        when(config.isLockoutEnabled()).thenReturn(true);
        when(config.getLockoutMaxAttempts()).thenReturn(5);
        when(config.getIpLockoutMaxAttempts()).thenReturn(20);
        when(config.getLockoutWindowSeconds()).thenReturn(900L);   // 15 min
        when(config.getLockoutDurationSeconds()).thenReturn(1800L); // 30 min
        when(config.isTrackLoginAttemptsByIp()).thenReturn(true);
        service = new LoginAttemptService(config);
    }

    // ---------- account lockout ----------

    @Test
    void belowThreshold_accountIsNotLocked() {
        for (int i = 0; i < 4; i++) {
            service.recordFailedLogin("alice", "1.2.3.4");
        }
        assertThat(service.isAccountLocked("alice")).isFalse();
        // Remaining attempts decreases with each failure.
        assertThat(service.getRemainingAttempts("alice")).isEqualTo(1);
    }

    @Test
    void atThreshold_accountIsLocked() {
        // 5th failed login triggers lock-up. Before the 5th, NOT locked yet.
        for (int i = 0; i < 5; i++) {
            service.recordFailedLogin("alice", "1.2.3.4");
        }
        assertThat(service.isAccountLocked("alice")).isTrue();
        assertThat(service.getRemainingAttempts("alice")).isZero();
    }

    @Test
    void successfulLogin_clearsAttemptCounter() {
        // 4 failed attempts, then success — counter resets so next 5 failures
        // would be needed to lock, not just one.
        for (int i = 0; i < 4; i++) {
            service.recordFailedLogin("alice", "1.2.3.4");
        }
        service.recordSuccessfulLogin("alice", "1.2.3.4");
        assertThat(service.getRemainingAttempts("alice")).isEqualTo(5);

        // One more failure shouldn't lock now.
        service.recordFailedLogin("alice", "1.2.3.4");
        assertThat(service.isAccountLocked("alice")).isFalse();
    }

    @Test
    void getRemainingLockoutTime_returnsPositiveSecondsWhenLocked() {
        for (int i = 0; i < 5; i++) {
            service.recordFailedLogin("alice", "1.2.3.4");
        }
        long remaining = service.getRemainingLockoutTime("alice");
        // ~30 minutes (1800s) — allow a bit of slack for clock granularity.
        assertThat(remaining).isBetween(1700L, 1800L);
    }

    @Test
    void getRemainingLockoutTime_unlockedAccount_returnsZero() {
        assertThat(service.getRemainingLockoutTime("bob")).isZero();
    }

    @Test
    void unlockAccount_clearsLockAndCounter() {
        // Admin override: an admin can manually unlock so a misidentified
        // user isn't stuck waiting 30 minutes.
        for (int i = 0; i < 5; i++) {
            service.recordFailedLogin("alice", "1.2.3.4");
        }
        assertThat(service.isAccountLocked("alice")).isTrue();

        service.unlockAccount("alice");
        assertThat(service.isAccountLocked("alice")).isFalse();
        // Counter must also clear — otherwise the very next failed login
        // would re-lock immediately.
        assertThat(service.getRemainingAttempts("alice")).isEqualTo(5);
    }

    // ---------- IP lockout ----------

    @Test
    void ipLockout_independentFromAccountLockout() {
        // The IP lockout has a separate (typically higher) threshold so that
        // legitimate password resets behind a shared IP don't lock everyone.
        when(config.getIpLockoutMaxAttempts()).thenReturn(10);

        // Lock the account at 5, but the IP only at 10. After 5 attempts:
        for (int i = 0; i < 5; i++) {
            service.recordFailedLogin("alice", "1.2.3.4");
        }
        assertThat(service.isAccountLocked("alice")).isTrue();
        assertThat(service.isIpLocked("1.2.3.4")).isFalse();

        // 5 more failures (across different "users") to hit IP threshold.
        for (int i = 0; i < 5; i++) {
            service.recordFailedLogin("user-" + i, "1.2.3.4");
        }
        assertThat(service.isIpLocked("1.2.3.4")).isTrue();
    }

    @Test
    void ipTrackingDisabled_neverLocksByIp_evenAfterManyFailures() {
        when(config.isTrackLoginAttemptsByIp()).thenReturn(false);
        for (int i = 0; i < 100; i++) {
            service.recordFailedLogin("user-" + i, "1.2.3.4");
        }
        assertThat(service.isIpLocked("1.2.3.4")).isFalse();
    }

    @Test
    void unlockIpAddress_clearsLockAndCounter() {
        when(config.getIpLockoutMaxAttempts()).thenReturn(2);
        service.recordFailedLogin("u", "1.2.3.4");
        service.recordFailedLogin("u", "1.2.3.4");
        assertThat(service.isIpLocked("1.2.3.4")).isTrue();

        service.unlockIpAddress("1.2.3.4");
        assertThat(service.isIpLocked("1.2.3.4")).isFalse();
    }

    // ---------- disabled / null guards ----------

    @Test
    void lockoutDisabled_doesNotRecordOrCheck() {
        // Reconstruct service with disabled config; recordFailedLogin must be a no-op.
        when(config.isLockoutEnabled()).thenReturn(false);
        LoginAttemptService disabled = new LoginAttemptService(config);

        for (int i = 0; i < 100; i++) {
            disabled.recordFailedLogin("alice", "1.2.3.4");
        }
        assertThat(disabled.isAccountLocked("alice")).isFalse();
        // getRemainingAttempts returns -1 when disabled — caller can use that
        // sentinel to hide attempt-counter UX.
        assertThat(disabled.getRemainingAttempts("alice")).isEqualTo(-1);
    }

    @Test
    void nullUsername_recordIsNoOp() {
        service.recordFailedLogin(null, "1.2.3.4");
        // No NPE, no recorded attempt — checking other usernames must remain at full quota.
        assertThat(service.getRemainingAttempts("alice")).isEqualTo(5);
    }

    @Test
    void emptyUsername_recordIsNoOp() {
        service.recordFailedLogin("", "1.2.3.4");
        assertThat(service.getRemainingAttempts("alice")).isEqualTo(5);
    }

    @Test
    void nullIp_recordOnlyTracksUsername() {
        // Some auth flows don't have an IP (server-to-server, scheduled jobs);
        // we must still record the username failure even if IP is null.
        service.recordFailedLogin("alice", null);
        assertThat(service.getRemainingAttempts("alice")).isEqualTo(4);
    }

    @Test
    void isAccountLocked_nullUsername_returnsFalse() {
        assertThat(service.isAccountLocked(null)).isFalse();
        assertThat(service.isAccountLocked("")).isFalse();
    }

    @Test
    void isIpLocked_nullIp_returnsFalse() {
        assertThat(service.isIpLocked(null)).isFalse();
        assertThat(service.isIpLocked("")).isFalse();
    }

    @Test
    void unlockAccount_nullOrEmpty_isNoOp() {
        // Defensive: admin endpoints might pass null if they hit a malformed
        // request body; should not throw.
        service.unlockAccount(null);
        service.unlockAccount("");
    }

    @Test
    void unlockIpAddress_nullOrEmpty_isNoOp() {
        service.unlockIpAddress(null);
        service.unlockIpAddress("");
    }

    // ---------- success path on disabled config ----------

    @Test
    void recordSuccessfulLogin_disabled_isNoOp() {
        when(config.isLockoutEnabled()).thenReturn(false);
        LoginAttemptService disabled = new LoginAttemptService(config);
        // No exceptions, no state changes.
        disabled.recordSuccessfulLogin("alice", "1.2.3.4");
    }

    // ---------- cache stats (smoke) ----------

    @Test
    void getCacheStatistics_returnsHumanReadableSummary() {
        // Used by admin diagnostics; should at least not crash and should
        // produce a string mentioning each cache.
        service.recordFailedLogin("alice", "1.2.3.4");
        String stats = service.getCacheStatistics();
        assertThat(stats).contains("Username Attempts").contains("IP Attempts")
                .contains("Locked Accounts").contains("Locked IPs");
    }
}

/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.integration;

import gov.nist.oscal.tools.api.entity.SecurityPolicy;
import gov.nist.oscal.tools.api.repository.SecurityPolicyRepository;
import gov.nist.oscal.tools.api.service.SecurityPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the nightly audit-log cleanup failure.
 *
 * Production had no security_policy row (never seeded), and
 * createDefaultPolicy() set the id on an IDENTITY entity before save(), so
 * JPA issued an UPDATE against a nonexistent row and failed with
 * "Row was already updated or deleted" — every night at 02:00 UTC, forever.
 *
 * The fix inserts via a native INSERT ... ON CONFLICT DO NOTHING
 * (SecurityPolicyRepository.insertDefaultPolicy), which this test exercises
 * end-to-end against the real database dialect used in tests.
 *
 * Deliberately NOT @Transactional: createDefaultPolicy uses REQUIRES_NEW and
 * the whole point is to verify real commit behavior on an empty table.
 */
@SpringBootTest
@ActiveProfiles("test")
class SecurityPolicyBootstrapIntegrationTest {

    @Autowired SecurityPolicyService service;
    @Autowired SecurityPolicyRepository repo;
    @Autowired(required = false) CacheManager cacheManager;

    @BeforeEach
    void wipePolicy() {
        repo.deleteAll();
        if (cacheManager != null) {
            Cache cache = cacheManager.getCache("securityPolicy");
            if (cache != null) {
                cache.clear();
            }
        }
    }

    @Test
    void getPolicy_createsSingletonRowWhenTableIsEmpty() {
        assertThat(repo.findById(SecurityPolicy.SINGLETON_ID)).isEmpty();

        SecurityPolicy policy = service.getPolicy();

        assertThat(policy.getId()).isEqualTo(SecurityPolicy.SINGLETON_ID);
        assertThat(policy.getAuditLogRetentionDays()).isEqualTo(90);
        assertThat(policy.getPasswordMinLength()).isEqualTo(10);
        assertThat(repo.findById(SecurityPolicy.SINGLETON_ID)).isPresent();
    }

    @Test
    void insertDefaultPolicy_isIdempotent() {
        service.createDefaultPolicy();
        // Second call must be a no-op, not a constraint violation.
        SecurityPolicy again = service.createDefaultPolicy();

        assertThat(again.getId()).isEqualTo(SecurityPolicy.SINGLETON_ID);
        assertThat(repo.count()).isEqualTo(1);
    }

    @Test
    void getAuditLogRetentionDays_worksOnFreshDatabase() {
        // This is the exact call path of the nightly cleanup job.
        assertThat(service.getAuditLogRetentionDays()).isEqualTo(90);
    }
}

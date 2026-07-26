package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.SecurityPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository for SecurityPolicy entity.
 * <p>
 * The security_policy table is a singleton (always ID=1).
 * Use {@link #getPolicy()} to retrieve the current policy.
 * </p>
 */
@Repository
public interface SecurityPolicyRepository extends JpaRepository<SecurityPolicy, Long> {

    /**
     * Get the singleton security policy (ID=1).
     *
     * @return the security policy
     * @throws RuntimeException if the policy doesn't exist (database not migrated)
     */
    default SecurityPolicy getPolicy() {
        return findById(SecurityPolicy.SINGLETON_ID)
                .orElseThrow(() -> new RuntimeException(
                        "Security policy not found. Please run database migration V1.12."));
    }

    /**
     * Insert the singleton default policy row (id=1) if it doesn't exist.
     * <p>
     * A native INSERT is required here: the id column is IDENTITY-generated, so
     * saving an entity with the id pre-set makes JPA issue an UPDATE against a
     * row that doesn't exist ("Row was already updated or deleted"), which is
     * exactly the failure that broke the nightly audit-log cleanup job.
     * </p>
     */
    @Modifying
    @Query(value = "INSERT INTO security_policy "
            + "(id, mfa_required, password_min_length, password_max_length, "
            + " password_rotation_days, audit_log_retention_days, updated_at, updated_by) "
            + "VALUES (1, false, 10, 128, 0, 90, now(), 'system') "
            + "ON CONFLICT DO NOTHING", nativeQuery = true)
    void insertDefaultPolicy();
}

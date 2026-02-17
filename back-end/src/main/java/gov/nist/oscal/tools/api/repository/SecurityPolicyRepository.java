package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.SecurityPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
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
}

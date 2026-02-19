package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.MfaBackupCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for MFA Backup Codes.
 * <p>
 * Backup codes are one-time recovery codes for users who have lost
 * access to their authenticator app. Each user gets 10 codes when
 * they enable MFA, and each code can only be used once.
 * </p>
 */
@Repository
public interface MfaBackupCodeRepository extends JpaRepository<MfaBackupCode, Long> {

    /**
     * Find all unused backup codes for a user.
     *
     * @param userId the user ID
     * @return list of unused backup codes
     */
    List<MfaBackupCode> findByUserIdAndUsedFalse(Long userId);

    /**
     * Find a specific unused backup code by its hash.
     *
     * @param userId   the user ID
     * @param codeHash the SHA-256 hash of the backup code
     * @return the backup code if found and unused
     */
    Optional<MfaBackupCode> findByUserIdAndCodeHashAndUsedFalse(Long userId, String codeHash);

    /**
     * Count remaining unused backup codes for a user.
     *
     * @param userId the user ID
     * @return count of unused backup codes
     */
    int countByUserIdAndUsedFalse(Long userId);

    /**
     * Delete all backup codes for a user.
     * Called when user disables MFA or regenerates backup codes.
     *
     * @param userId the user ID
     */
    @Modifying
    @Query("DELETE FROM MfaBackupCode b WHERE b.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * Find all backup codes for a user (used and unused).
     *
     * @param userId the user ID
     * @return list of all backup codes
     */
    List<MfaBackupCode> findByUserId(Long userId);
}

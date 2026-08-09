package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.ServiceAccountToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ServiceAccountTokenRepository extends JpaRepository<ServiceAccountToken, Long> {

    @Query("SELECT t FROM ServiceAccountToken t JOIN FETCH t.user WHERE t.jti = :jti")
    Optional<ServiceAccountToken> findByJti(@Param("jti") String jti);

    List<ServiceAccountToken> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ServiceAccountToken> findByIdAndUserId(Long id, Long userId);

    /** Revoke every live token for a user — used when an account is archived. */
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE ServiceAccountToken t SET t.revokedAt = :now, t.revokedBy = :revokedBy "
         + "WHERE t.user.id = :userId AND t.revokedAt IS NULL")
    int revokeAllForUser(@Param("userId") Long userId,
                         @Param("now") LocalDateTime now,
                         @Param("revokedBy") String revokedBy);

    /**
     * Called from JwtAuthenticationFilter, which is not transactional — hence
     * the explicit @Transactional, without which @Modifying throws
     * TransactionRequiredException.
     */
    @Transactional
    @Modifying
    @Query("UPDATE ServiceAccountToken t SET t.lastUsedAt = :now WHERE t.id = :id")
    int touchLastUsed(@Param("id") Long id, @Param("now") LocalDateTime now);
}

package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Count users created after a specific date
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countByCreatedAtAfter(@Param("since") LocalDateTime since);

    /**
     * Count users by registration date (for growth trend)
     */
    @Query("SELECT CAST(u.createdAt AS date), COUNT(u) FROM User u WHERE u.createdAt >= :since GROUP BY CAST(u.createdAt AS date) ORDER BY CAST(u.createdAt AS date)")
    List<Object[]> countByCreatedAtGroupByDate(@Param("since") LocalDateTime since);

    /**
     * New users grouped by year/month (e.g. "2026-05") for the last N months.
     * Returns rows of [yearMonthString, count]. Postgres-specific: TO_CHAR.
     */
    @Query(value = "SELECT TO_CHAR(u.created_at, 'YYYY-MM') AS ym, COUNT(*) FROM users u WHERE u.created_at >= :since GROUP BY ym ORDER BY ym", nativeQuery = true)
    List<Object[]> countNewUsersByMonth(@Param("since") LocalDateTime since);

    /** Total count of users whose last_login is null OR older than the cutoff. */
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLogin IS NULL OR u.lastLogin < :cutoff")
    long countStaleUsers(@Param("cutoff") LocalDateTime cutoff);
}

package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.AuditEvent;
import gov.nist.oscal.tools.api.model.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Audit Event Repository
 * <p>
 * Provides data access methods for querying and managing audit events.
 * Supports common audit log queries for security monitoring, compliance
 * reporting, and forensic investigation.
 * </p>
 *
 * <h2>Query Capabilities</h2>
 * <ul>
 *   <li>Find events by user, event type, date range</li>
 *   <li>Find failed login attempts and security events</li>
 *   <li>Find unreviewed high-risk events</li>
 *   <li>Find events by IP address or user agent</li>
 *   <li>Delete old events (retention policy enforcement)</li>
 *   <li>Count events by various criteria</li>
 * </ul>
 *
 * @see AuditEvent
 * @see gov.nist.oscal.tools.api.service.AuditLogService
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    // ========================================
    // Find All (Paginated)
    // ========================================

    /**
     * Find all audit events ordered by timestamp descending
     *
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    Page<AuditEvent> findAllByOrderByTimestampDesc(Pageable pageable);

    /**
     * Find all audit events for "raw" logs (API access - Authentication and Data Access categories)
     *
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.category IN ('Authentication', 'Data Access') ORDER BY a.timestamp DESC")
    Page<AuditEvent> findRawLogs(Pageable pageable);

    /**
     * Find raw logs filtered by username
     *
     * @param username Username to filter by
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.category IN ('Authentication', 'Data Access') AND a.username = :username ORDER BY a.timestamp DESC")
    Page<AuditEvent> findRawLogsByUsername(@Param("username") String username, Pageable pageable);

    /**
     * Find raw logs filtered by risk level
     *
     * @param riskLevel Risk level to filter by
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.category IN ('Authentication', 'Data Access') AND a.riskLevel = :riskLevel ORDER BY a.timestamp DESC")
    Page<AuditEvent> findRawLogsByRiskLevel(@Param("riskLevel") String riskLevel, Pageable pageable);

    /**
     * Search logs by keyword in username, resource, requestUrl, or errorMessage
     *
     * @param keyword Search keyword
     * @param pageable Pagination parameters
     * @return Page of matching audit events
     */
    @Query("SELECT a FROM AuditEvent a WHERE " +
           "LOWER(a.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.resource) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.requestUrl) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.errorMessage) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditEvent> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Count events by outcome
     *
     * @param outcome The outcome to count (SUCCESS, FAILURE, ERROR)
     * @return Number of events with that outcome
     */
    long countByOutcome(String outcome);

    /**
     * Count events by category
     *
     * @param category The category to count
     * @return Number of events in that category
     */
    long countByCategory(String category);

    /**
     * Count security events today (Security category or HIGH risk level)
     *
     * @param since Start of today
     * @return Number of security events
     */
    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE (a.category = 'Security' OR a.riskLevel = 'HIGH') AND a.timestamp >= :since")
    long countSecurityEventsSince(@Param("since") LocalDateTime since);

    /**
     * Count error events today (FAILURE or ERROR outcome)
     *
     * @param since Start of today
     * @return Number of error events
     */
    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.outcome IN ('FAILURE', 'ERROR') AND a.timestamp >= :since")
    long countErrorsSince(@Param("since") LocalDateTime since);

    // ========================================
    // Find by User
    // ========================================

    /**
     * Find all audit events for a specific user (paginated)
     *
     * @param username Username to search for
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    Page<AuditEvent> findByUsernameOrderByTimestampDesc(String username, Pageable pageable);

    /**
     * Find all audit events for a specific user within a date range
     *
     * @param username Username to search for
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    Page<AuditEvent> findByUsernameAndTimestampBetweenOrderByTimestampDesc(
        String username, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable
    );

    /**
     * Find all audit events for a specific user ID
     *
     * @param userId User ID to search for
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    Page<AuditEvent> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    // ========================================
    // Find by Event Type
    // ========================================

    /**
     * Find all audit events of a specific type
     *
     * @param eventType Event type to search for
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    Page<AuditEvent> findByEventTypeOrderByTimestampDesc(AuditEventType eventType, Pageable pageable);

    /**
     * Find all audit events in a specific category
     *
     * @param category Category to search for (Authentication, Authorization, etc.)
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    Page<AuditEvent> findByCategoryOrderByTimestampDesc(String category, Pageable pageable);

    /**
     * Find all audit events of a specific risk level
     *
     * @param riskLevel Risk level (LOW, MEDIUM, HIGH)
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    Page<AuditEvent> findByRiskLevelOrderByTimestampDesc(String riskLevel, Pageable pageable);

    // ========================================
    // Find by Outcome
    // ========================================

    /**
     * Find all failed events (FAILURE or ERROR outcome)
     *
     * @param pageable Pagination parameters
     * @return Page of failed audit events
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.outcome IN ('FAILURE', 'ERROR') ORDER BY a.timestamp DESC")
    Page<AuditEvent> findAllFailedEvents(Pageable pageable);

    /**
     * Find all failed events for a specific user
     *
     * @param username Username to search for
     * @param pageable Pagination parameters
     * @return Page of failed audit events
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.username = :username AND a.outcome IN ('FAILURE', 'ERROR') ORDER BY a.timestamp DESC")
    Page<AuditEvent> findFailedEventsByUsername(@Param("username") String username, Pageable pageable);

    /**
     * Find error logs filtered by username (alias for findFailedEventsByUsername)
     *
     * @param username Username to filter by
     * @param pageable Pagination parameters
     * @return Page of error events
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.outcome IN ('FAILURE', 'ERROR') AND a.username = :username ORDER BY a.timestamp DESC")
    Page<AuditEvent> findErrorsByUsername(@Param("username") String username, Pageable pageable);

    /**
     * Find error logs filtered by risk level
     *
     * @param riskLevel Risk level to filter by
     * @param pageable Pagination parameters
     * @return Page of error events
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.outcome IN ('FAILURE', 'ERROR') AND a.riskLevel = :riskLevel ORDER BY a.timestamp DESC")
    Page<AuditEvent> findErrorsByRiskLevel(@Param("riskLevel") String riskLevel, Pageable pageable);

    // ========================================
    // Find by Date Range
    // ========================================

    /**
     * Find all audit events within a date range
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    Page<AuditEvent> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime startDate, LocalDateTime endDate, Pageable pageable
    );

    /**
     * Find all events of a specific type within a date range
     *
     * @param eventType Event type to search for
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    Page<AuditEvent> findByEventTypeAndTimestampBetweenOrderByTimestampDesc(
        AuditEventType eventType, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable
    );

    // ========================================
    // Find by IP Address
    // ========================================

    /**
     * Find all audit events from a specific IP address
     *
     * @param ipAddress IP address to search for
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    Page<AuditEvent> findByIpAddressOrderByTimestampDesc(String ipAddress, Pageable pageable);

    /**
     * Find all failed events from a specific IP address
     *
     * @param ipAddress IP address to search for
     * @param pageable Pagination parameters
     * @return Page of failed audit events
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.ipAddress = :ipAddress AND a.outcome IN ('FAILURE', 'ERROR') ORDER BY a.timestamp DESC")
    Page<AuditEvent> findFailedEventsByIpAddress(@Param("ipAddress") String ipAddress, Pageable pageable);

    // ========================================
    // Security & High-Risk Events
    // ========================================

    /**
     * Find all high-risk events that haven't been reviewed
     *
     * @param pageable Pagination parameters
     * @return Page of unreviewed high-risk events
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.riskLevel = 'HIGH' AND a.reviewed = false ORDER BY a.timestamp DESC")
    Page<AuditEvent> findUnreviewedHighRiskEvents(Pageable pageable);

    /**
     * Find all security events (category = 'Security' or risk level = 'HIGH')
     *
     * @param pageable Pagination parameters
     * @return Page of security events
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.category = 'Security' OR a.riskLevel = 'HIGH' ORDER BY a.timestamp DESC")
    Page<AuditEvent> findAllSecurityEvents(Pageable pageable);

    /**
     * Find security events filtered by username
     *
     * @param username Username to filter by
     * @param pageable Pagination parameters
     * @return Page of security events
     */
    @Query("SELECT a FROM AuditEvent a WHERE (a.category = 'Security' OR a.riskLevel = 'HIGH') AND a.username = :username ORDER BY a.timestamp DESC")
    Page<AuditEvent> findSecurityEventsByUsername(@Param("username") String username, Pageable pageable);

    /**
     * Find security events filtered by risk level
     *
     * @param riskLevel Risk level to filter by
     * @param pageable Pagination parameters
     * @return Page of security events
     */
    @Query("SELECT a FROM AuditEvent a WHERE (a.category = 'Security' OR a.riskLevel = 'HIGH') AND a.riskLevel = :riskLevel ORDER BY a.timestamp DESC")
    Page<AuditEvent> findSecurityEventsByRiskLevel(@Param("riskLevel") String riskLevel, Pageable pageable);

    /**
     * Find recent failed login attempts (within last N minutes)
     *
     * @param sinceTime Timestamp to search from
     * @return List of failed login attempt events
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.eventType = 'AUTH_LOGIN_FAILURE' AND a.timestamp >= :sinceTime ORDER BY a.timestamp DESC")
    List<AuditEvent> findRecentFailedLoginAttempts(@Param("sinceTime") LocalDateTime sinceTime);

    /**
     * Find failed login attempts for a specific username
     *
     * @param username Username to search for
     * @param sinceTime Timestamp to search from
     * @return List of failed login attempt events
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.eventType = 'AUTH_LOGIN_FAILURE' AND a.username = :username AND a.timestamp >= :sinceTime ORDER BY a.timestamp DESC")
    List<AuditEvent> findFailedLoginAttemptsByUsername(
        @Param("username") String username,
        @Param("sinceTime") LocalDateTime sinceTime
    );

    // ========================================
    // Count Queries
    // ========================================

    /**
     * Count events by user
     *
     * @param username Username to count events for
     * @return Number of events
     */
    long countByUsername(String username);

    /**
     * Count events by event type
     *
     * @param eventType Event type to count
     * @return Number of events
     */
    long countByEventType(AuditEventType eventType);

    /**
     * Count events by risk level
     *
     * @param riskLevel Risk level to count
     * @return Number of events
     */
    long countByRiskLevel(String riskLevel);

    /**
     * Count unreviewed high-risk events
     *
     * @return Number of unreviewed high-risk events
     */
    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.riskLevel = 'HIGH' AND a.reviewed = false")
    long countUnreviewedHighRiskEvents();

    /**
     * Count failed events for a user within a date range
     *
     * @param username Username to count for
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return Number of failed events
     */
    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.username = :username AND a.outcome IN ('FAILURE', 'ERROR') AND a.timestamp BETWEEN :startDate AND :endDate")
    long countFailedEventsByUsernameAndDateRange(
        @Param("username") String username,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    // ========================================
    // Maintenance Queries
    // ========================================

    /**
     * Delete audit events older than a specific date (retention policy)
     *
     * @param cutoffDate Delete events older than this date
     * @return Number of events deleted
     */
    @Modifying
    @Query("DELETE FROM AuditEvent a WHERE a.timestamp < :cutoffDate")
    int deleteEventsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete low-risk events older than a specific date
     *
     * @param cutoffDate Delete events older than this date
     * @return Number of events deleted
     */
    @Modifying
    @Query("DELETE FROM AuditEvent a WHERE a.riskLevel = 'LOW' AND a.timestamp < :cutoffDate")
    int deleteLowRiskEventsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find events that should be deleted according to retention policy
     *
     * @param lowRiskCutoff Cutoff date for LOW risk events (90 days)
     * @param mediumRiskCutoff Cutoff date for MEDIUM risk events (2 years)
     * @param highRiskCutoff Cutoff date for HIGH risk events (7 years)
     * @return List of audit event IDs to delete
     */
    @Query("SELECT a.id FROM AuditEvent a WHERE " +
           "(a.riskLevel = 'LOW' AND a.timestamp < :lowRiskCutoff) OR " +
           "(a.riskLevel = 'MEDIUM' AND a.timestamp < :mediumRiskCutoff) OR " +
           "(a.riskLevel = 'HIGH' AND a.timestamp < :highRiskCutoff)")
    List<Long> findEventIdsToDelete(
        @Param("lowRiskCutoff") LocalDateTime lowRiskCutoff,
        @Param("mediumRiskCutoff") LocalDateTime mediumRiskCutoff,
        @Param("highRiskCutoff") LocalDateTime highRiskCutoff
    );

    // ========================================
    // Resource-Specific Queries
    // ========================================

    /**
     * Find all events related to a specific resource
     *
     * @param resource Resource identifier (file ID, URL, etc.)
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    Page<AuditEvent> findByResourceOrderByTimestampDesc(String resource, Pageable pageable);

    /**
     * Find all events for a specific action
     *
     * @param action Action performed (READ, WRITE, DELETE, etc.)
     * @param pageable Pagination parameters
     * @return Page of audit events
     */
    Page<AuditEvent> findByActionOrderByTimestampDesc(String action, Pageable pageable);

    // ========================================
    // Hash Chain Queries
    // ========================================

    /**
     * Find the most recent audit event (for hash chain initialization)
     *
     * @return The most recent audit event, or null if none exist
     */
    AuditEvent findTopByOrderByIdDesc();

    /**
     * Find events with integrity hash issues (null or empty hash)
     *
     * @return List of events with potential integrity issues
     */
    @Query("SELECT a FROM AuditEvent a WHERE a.integrityHash IS NULL OR a.integrityHash = ''")
    List<AuditEvent> findEventsWithMissingHash();

    // ========================================
    // Analytics Queries
    // ========================================

    /**
     * Count events grouped by event type within a date range
     */
    @Query("SELECT a.eventType, COUNT(a) FROM AuditEvent a WHERE a.timestamp >= :since GROUP BY a.eventType ORDER BY COUNT(a) DESC")
    List<Object[]> countByEventTypeSince(@Param("since") LocalDateTime since);

    /**
     * Count events grouped by category within a date range
     */
    @Query("SELECT a.category, COUNT(a) FROM AuditEvent a WHERE a.timestamp >= :since GROUP BY a.category ORDER BY COUNT(a) DESC")
    List<Object[]> countByCategorySince(@Param("since") LocalDateTime since);

    /**
     * Count events grouped by date (for activity over time chart)
     */
    @Query("SELECT CAST(a.timestamp AS date), COUNT(a) FROM AuditEvent a WHERE a.timestamp >= :since GROUP BY CAST(a.timestamp AS date) ORDER BY CAST(a.timestamp AS date)")
    List<Object[]> countByDateSince(@Param("since") LocalDateTime since);

    /**
     * Count events by username (for most active users)
     */
    @Query("SELECT a.username, COUNT(a) FROM AuditEvent a WHERE a.username IS NOT NULL AND a.timestamp >= :since GROUP BY a.username ORDER BY COUNT(a) DESC")
    List<Object[]> countByUsernameSince(@Param("since") LocalDateTime since);

    /**
     * Count total events within a date range
     */
    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.timestamp >= :since")
    long countEventsSince(@Param("since") LocalDateTime since);

    /**
     * Count unique active users within a date range
     */
    @Query("SELECT COUNT(DISTINCT a.username) FROM AuditEvent a WHERE a.username IS NOT NULL AND a.timestamp >= :since")
    long countUniqueUsersSince(@Param("since") LocalDateTime since);

    // ========================================
    // Organization-scoped analytics queries
    // ========================================

    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.username IN :usernames AND a.eventType = 'AUTH_LOGIN_SUCCESS' AND a.timestamp >= :since")
    long countLoginsByUsernamesSince(@Param("usernames") List<String> usernames, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.username IN :usernames AND a.category != 'Authentication' AND a.timestamp >= :since")
    long countOperationsByUsernamesSince(@Param("usernames") List<String> usernames, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM AuditEvent a WHERE a.username IN :usernames AND a.eventType = 'AUTH_LOGIN_FAILURE' AND a.timestamp >= :since")
    long countFailedLoginsByUsernamesSince(@Param("usernames") List<String> usernames, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT a.username) FROM AuditEvent a WHERE a.username IN :usernames AND a.timestamp >= :since")
    long countActiveUsersByUsernamesSince(@Param("usernames") List<String> usernames, @Param("since") LocalDateTime since);

    @Query("SELECT CAST(a.timestamp AS date), COUNT(a) FROM AuditEvent a WHERE a.username IN :usernames AND a.eventType = 'AUTH_LOGIN_SUCCESS' AND a.timestamp >= :since GROUP BY CAST(a.timestamp AS date) ORDER BY CAST(a.timestamp AS date)")
    List<Object[]> countLoginsPerDayByUsernamesSince(@Param("usernames") List<String> usernames, @Param("since") LocalDateTime since);

    @Query("SELECT a.eventType, COUNT(a) FROM AuditEvent a WHERE a.username IN :usernames AND a.category != 'Authentication' AND a.timestamp >= :since GROUP BY a.eventType ORDER BY COUNT(a) DESC")
    List<Object[]> countOperationsByTypeByUsernamesSince(@Param("usernames") List<String> usernames, @Param("since") LocalDateTime since);

    @Query("SELECT a.username, COUNT(a) FROM AuditEvent a WHERE a.username IN :usernames AND a.category != 'Authentication' AND a.timestamp >= :since GROUP BY a.username ORDER BY COUNT(a) DESC")
    List<Object[]> countTopUsersByUsernamesSince(@Param("usernames") List<String> usernames, @Param("since") LocalDateTime since);

    // ========================================
    // Library download trend queries
    // ========================================

    /**
     * Library item downloads bucketed by ISO week since :since. Powers the
     * public catalog "Downloads over time" chart. Returns rows of
     * [weekStart, count]. Native query so we can use date_trunc — JPQL has
     * no portable equivalent.
     */
    @Query(value = """
        SELECT date_trunc('week', a.timestamp) AS bucket, COUNT(a.id)
        FROM audit_events a
        WHERE a.event_type = 'LIBRARY_ITEM_DOWNLOAD' AND a.timestamp >= :since
        GROUP BY bucket
        ORDER BY bucket
        """, nativeQuery = true)
    List<Object[]> getLibraryDownloadsPerWeek(@Param("since") LocalDateTime since);
}

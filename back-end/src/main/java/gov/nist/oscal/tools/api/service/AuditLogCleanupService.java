package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for cleaning up old audit logs based on retention policy.
 * <p>
 * Runs daily at 2 AM to delete audit events older than the configured
 * retention period in the Security Policy.
 * </p>
 */
@Service
public class AuditLogCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogCleanupService.class);

    private final AuditEventRepository auditEventRepository;
    private final SecurityPolicyService securityPolicyService;

    public AuditLogCleanupService(
            AuditEventRepository auditEventRepository,
            SecurityPolicyService securityPolicyService) {
        this.auditEventRepository = auditEventRepository;
        this.securityPolicyService = securityPolicyService;
    }

    /**
     * Scheduled cleanup job that runs daily at 2 AM.
     * Deletes audit events older than the configured retention period.
     */
    @Scheduled(cron = "${audit.cleanup.schedule:0 0 2 * * ?}")
    @Transactional
    public void cleanupOldAuditLogs() {
        int retentionDays = securityPolicyService.getAuditLogRetentionDays();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

        logger.info("Starting audit log cleanup. Retention period: {} days, cutoff date: {}",
                retentionDays, cutoffDate);

        try {
            int deletedCount = auditEventRepository.deleteEventsOlderThan(cutoffDate);
            logger.info("Audit log cleanup completed. Deleted {} events older than {}",
                    deletedCount, cutoffDate);
        } catch (Exception e) {
            logger.error("Audit log cleanup failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual cleanup trigger (for admin use).
     *
     * @return number of deleted records
     */
    @Transactional
    public int runCleanupNow() {
        int retentionDays = securityPolicyService.getAuditLogRetentionDays();
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

        logger.info("Manual audit log cleanup triggered. Retention: {} days, cutoff: {}",
                retentionDays, cutoffDate);

        int deletedCount = auditEventRepository.deleteEventsOlderThan(cutoffDate);

        logger.info("Manual cleanup completed. Deleted {} audit events", deletedCount);

        return deletedCount;
    }
}

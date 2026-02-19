package gov.nist.oscal.tools.api.model;

import java.util.Map;

/**
 * DTO for audit log statistics.
 * Used by the admin logs dashboard to display summary metrics.
 */
public class AuditLogStats {

    private long totalLogs;
    private long logsToday;
    private long securityEventsToday;
    private long errorsToday;
    private long highRiskUnreviewed;
    private Map<String, Long> byCategory;
    private Map<String, Long> byRiskLevel;
    private Map<String, Long> byOutcome;

    public AuditLogStats() {
    }

    public AuditLogStats(long totalLogs, long logsToday, long securityEventsToday,
                         long errorsToday, long highRiskUnreviewed,
                         Map<String, Long> byCategory, Map<String, Long> byRiskLevel,
                         Map<String, Long> byOutcome) {
        this.totalLogs = totalLogs;
        this.logsToday = logsToday;
        this.securityEventsToday = securityEventsToday;
        this.errorsToday = errorsToday;
        this.highRiskUnreviewed = highRiskUnreviewed;
        this.byCategory = byCategory;
        this.byRiskLevel = byRiskLevel;
        this.byOutcome = byOutcome;
    }

    // Getters and Setters

    public long getTotalLogs() {
        return totalLogs;
    }

    public void setTotalLogs(long totalLogs) {
        this.totalLogs = totalLogs;
    }

    public long getLogsToday() {
        return logsToday;
    }

    public void setLogsToday(long logsToday) {
        this.logsToday = logsToday;
    }

    public long getSecurityEventsToday() {
        return securityEventsToday;
    }

    public void setSecurityEventsToday(long securityEventsToday) {
        this.securityEventsToday = securityEventsToday;
    }

    public long getErrorsToday() {
        return errorsToday;
    }

    public void setErrorsToday(long errorsToday) {
        this.errorsToday = errorsToday;
    }

    public long getHighRiskUnreviewed() {
        return highRiskUnreviewed;
    }

    public void setHighRiskUnreviewed(long highRiskUnreviewed) {
        this.highRiskUnreviewed = highRiskUnreviewed;
    }

    public Map<String, Long> getByCategory() {
        return byCategory;
    }

    public void setByCategory(Map<String, Long> byCategory) {
        this.byCategory = byCategory;
    }

    public Map<String, Long> getByRiskLevel() {
        return byRiskLevel;
    }

    public void setByRiskLevel(Map<String, Long> byRiskLevel) {
        this.byRiskLevel = byRiskLevel;
    }

    public Map<String, Long> getByOutcome() {
        return byOutcome;
    }

    public void setByOutcome(Map<String, Long> byOutcome) {
        this.byOutcome = byOutcome;
    }
}

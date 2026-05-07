package gov.nist.oscal.tools.api.model.conmon;

import gov.nist.oscal.tools.api.entity.ConMonReconciliation;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import gov.nist.oscal.tools.api.entity.ConMonSourceFormat;

import java.time.LocalDateTime;

public class ConMonSnapshotSummary {
    private Long id;
    private Long authorizationId;
    private LocalDateTime uploadedAt;
    private String uploadedByUsername;
    private ConMonSourceFormat sourceFormat;
    private String originalFilename;
    private String oscalUuid;
    private String oscalVersion;
    private String metadataTitle;
    private LocalDateTime metadataLastModified;
    private int openCount;
    private int closedCount;
    private int unknownCount;
    private String notes;

    /** Reconciliation counts vs the prior snapshot, or null if no prior. */
    private ReconciliationCounts reconciliation;

    public ConMonSnapshotSummary() {}

    public ConMonSnapshotSummary(ConMonSnapshot s, ConMonReconciliation rec) {
        this.id = s.getId();
        this.authorizationId = s.getAuthorization().getId();
        this.uploadedAt = s.getUploadedAt();
        this.uploadedByUsername = s.getUploadedBy() != null ? s.getUploadedBy().getUsername() : null;
        this.sourceFormat = s.getSourceFormat();
        this.originalFilename = s.getOriginalFilename();
        this.oscalUuid = s.getOscalUuid();
        this.oscalVersion = s.getOscalVersion();
        this.metadataTitle = s.getMetadataTitle();
        this.metadataLastModified = s.getMetadataLastModified();
        this.openCount = s.getSummaryOpenCount();
        this.closedCount = s.getSummaryClosedCount();
        this.unknownCount = s.getSummaryUnknownCount();
        this.notes = s.getNotes();
        this.reconciliation = rec == null ? null : new ReconciliationCounts(rec);
    }

    public static class ReconciliationCounts {
        private int newCount, closedCount, reopenedCount, stillOpenCount, removedCount, changedCount;
        private Long previousSnapshotId;
        public ReconciliationCounts() {}
        public ReconciliationCounts(ConMonReconciliation r) {
            this.newCount = r.getNewCount();
            this.closedCount = r.getClosedCount();
            this.reopenedCount = r.getReopenedCount();
            this.stillOpenCount = r.getStillOpenCount();
            this.removedCount = r.getRemovedCount();
            this.changedCount = r.getChangedCount();
            this.previousSnapshotId = r.getPreviousSnapshot() != null ? r.getPreviousSnapshot().getId() : null;
        }
        public int getNewCount() { return newCount; } public void setNewCount(int n) { this.newCount = n; }
        public int getClosedCount() { return closedCount; } public void setClosedCount(int n) { this.closedCount = n; }
        public int getReopenedCount() { return reopenedCount; } public void setReopenedCount(int n) { this.reopenedCount = n; }
        public int getStillOpenCount() { return stillOpenCount; } public void setStillOpenCount(int n) { this.stillOpenCount = n; }
        public int getRemovedCount() { return removedCount; } public void setRemovedCount(int n) { this.removedCount = n; }
        public int getChangedCount() { return changedCount; } public void setChangedCount(int n) { this.changedCount = n; }
        public Long getPreviousSnapshotId() { return previousSnapshotId; }
        public void setPreviousSnapshotId(Long id) { this.previousSnapshotId = id; }
    }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getAuthorizationId() { return authorizationId; } public void setAuthorizationId(Long id) { this.authorizationId = id; }
    public LocalDateTime getUploadedAt() { return uploadedAt; } public void setUploadedAt(LocalDateTime t) { this.uploadedAt = t; }
    public String getUploadedByUsername() { return uploadedByUsername; } public void setUploadedByUsername(String s) { this.uploadedByUsername = s; }
    public ConMonSourceFormat getSourceFormat() { return sourceFormat; } public void setSourceFormat(ConMonSourceFormat f) { this.sourceFormat = f; }
    public String getOriginalFilename() { return originalFilename; } public void setOriginalFilename(String s) { this.originalFilename = s; }
    public String getOscalUuid() { return oscalUuid; } public void setOscalUuid(String s) { this.oscalUuid = s; }
    public String getOscalVersion() { return oscalVersion; } public void setOscalVersion(String s) { this.oscalVersion = s; }
    public String getMetadataTitle() { return metadataTitle; } public void setMetadataTitle(String s) { this.metadataTitle = s; }
    public LocalDateTime getMetadataLastModified() { return metadataLastModified; } public void setMetadataLastModified(LocalDateTime t) { this.metadataLastModified = t; }
    public int getOpenCount() { return openCount; } public void setOpenCount(int n) { this.openCount = n; }
    public int getClosedCount() { return closedCount; } public void setClosedCount(int n) { this.closedCount = n; }
    public int getUnknownCount() { return unknownCount; } public void setUnknownCount(int n) { this.unknownCount = n; }
    public String getNotes() { return notes; } public void setNotes(String s) { this.notes = s; }
    public ReconciliationCounts getReconciliation() { return reconciliation; }
    public void setReconciliation(ReconciliationCounts r) { this.reconciliation = r; }
}

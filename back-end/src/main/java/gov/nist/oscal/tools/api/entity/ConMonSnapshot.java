package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conmon_snapshots")
public class ConMonSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "authorization_id", nullable = false)
    private Authorization authorization;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "source_format", nullable = false, length = 32)
    private ConMonSourceFormat sourceFormat;

    @Column(name = "original_filename", nullable = false, length = 512)
    private String originalFilename;

    @Column(name = "file_storage_path", nullable = false, length = 1024)
    private String fileStoragePath;

    @Column(name = "oscal_uuid", length = 64)
    private String oscalUuid;

    @Column(name = "oscal_version", length = 16)
    private String oscalVersion;

    @Column(name = "metadata_title", length = 512)
    private String metadataTitle;

    @Column(name = "metadata_last_modified")
    private LocalDateTime metadataLastModified;

    @Column(name = "summary_open_count", nullable = false)
    private int summaryOpenCount;

    @Column(name = "summary_closed_count", nullable = false)
    private int summaryClosedCount;

    @Column(name = "summary_unknown_count", nullable = false)
    private int summaryUnknownCount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ConMonPoamItem> items = new ArrayList<>();

    public ConMonSnapshot() {}

    // Standard manual getters/setters for all fields above:
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Authorization getAuthorization() { return authorization; }
    public void setAuthorization(Authorization authorization) { this.authorization = authorization; }
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public ConMonSourceFormat getSourceFormat() { return sourceFormat; }
    public void setSourceFormat(ConMonSourceFormat sourceFormat) { this.sourceFormat = sourceFormat; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String s) { this.originalFilename = s; }
    public String getFileStoragePath() { return fileStoragePath; }
    public void setFileStoragePath(String s) { this.fileStoragePath = s; }
    public String getOscalUuid() { return oscalUuid; }
    public void setOscalUuid(String s) { this.oscalUuid = s; }
    public String getOscalVersion() { return oscalVersion; }
    public void setOscalVersion(String s) { this.oscalVersion = s; }
    public String getMetadataTitle() { return metadataTitle; }
    public void setMetadataTitle(String s) { this.metadataTitle = s; }
    public LocalDateTime getMetadataLastModified() { return metadataLastModified; }
    public void setMetadataLastModified(LocalDateTime t) { this.metadataLastModified = t; }
    public int getSummaryOpenCount() { return summaryOpenCount; }
    public void setSummaryOpenCount(int n) { this.summaryOpenCount = n; }
    public int getSummaryClosedCount() { return summaryClosedCount; }
    public void setSummaryClosedCount(int n) { this.summaryClosedCount = n; }
    public int getSummaryUnknownCount() { return summaryUnknownCount; }
    public void setSummaryUnknownCount(int n) { this.summaryUnknownCount = n; }
    public String getNotes() { return notes; }
    public void setNotes(String s) { this.notes = s; }
    public List<ConMonPoamItem> getItems() { return items; }
    public void setItems(List<ConMonPoamItem> items) { this.items = items; }
}

package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "conmon_poam_items",
       indexes = {
           @Index(name = "idx_conmon_poam_items_snap_status", columnList = "snapshot_id, status"),
           @Index(name = "idx_conmon_poam_items_snap_extid", columnList = "snapshot_id, externalId")
       })
public class ConMonPoamItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private ConMonSnapshot snapshot;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Column(nullable = false, length = 1024)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConMonItemStatus status;

    @Column(name = "raw_status", length = 64)
    private String rawStatus;

    @Column(length = 16)
    private String severity;

    @Column(name = "weakness_source", length = 256)
    private String weaknessSource;

    @Column(name = "scheduled_completion_date")
    private LocalDate scheduledCompletionDate;

    @Column(name = "actual_completion_date")
    private LocalDate actualCompletionDate;

    @Column(name = "point_of_contact", length = 256)
    private String pointOfContact;

    @Column(name = "risk_rating", length = 64)
    private String riskRating;

    @Column(name = "extra_props_json", columnDefinition = "TEXT")
    private String extraPropsJson;

    public ConMonPoamItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ConMonSnapshot getSnapshot() { return snapshot; }
    public void setSnapshot(ConMonSnapshot s) { this.snapshot = s; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String s) { this.externalId = s; }
    public String getTitle() { return title; }
    public void setTitle(String s) { this.title = s; }
    public String getDescription() { return description; }
    public void setDescription(String s) { this.description = s; }
    public ConMonItemStatus getStatus() { return status; }
    public void setStatus(ConMonItemStatus s) { this.status = s; }
    public String getRawStatus() { return rawStatus; }
    public void setRawStatus(String s) { this.rawStatus = s; }
    public String getSeverity() { return severity; }
    public void setSeverity(String s) { this.severity = s; }
    public String getWeaknessSource() { return weaknessSource; }
    public void setWeaknessSource(String s) { this.weaknessSource = s; }
    public LocalDate getScheduledCompletionDate() { return scheduledCompletionDate; }
    public void setScheduledCompletionDate(LocalDate d) { this.scheduledCompletionDate = d; }
    public LocalDate getActualCompletionDate() { return actualCompletionDate; }
    public void setActualCompletionDate(LocalDate d) { this.actualCompletionDate = d; }
    public String getPointOfContact() { return pointOfContact; }
    public void setPointOfContact(String s) { this.pointOfContact = s; }
    public String getRiskRating() { return riskRating; }
    public void setRiskRating(String s) { this.riskRating = s; }
    public String getExtraPropsJson() { return extraPropsJson; }
    public void setExtraPropsJson(String s) { this.extraPropsJson = s; }
}

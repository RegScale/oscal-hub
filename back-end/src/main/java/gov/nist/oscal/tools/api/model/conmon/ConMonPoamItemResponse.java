package gov.nist.oscal.tools.api.model.conmon;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;

import java.time.LocalDate;

public class ConMonPoamItemResponse {
    private Long id;
    private String externalId;
    private String title;
    private String description;
    private ConMonItemStatus status;
    private String rawStatus;
    private String severity;
    private String weaknessSource;
    private LocalDate scheduledCompletionDate;
    private LocalDate actualCompletionDate;
    private String pointOfContact;
    private String riskRating;

    public ConMonPoamItemResponse() {}

    public ConMonPoamItemResponse(ConMonPoamItem i) {
        this.id = i.getId();
        this.externalId = i.getExternalId();
        this.title = i.getTitle();
        this.description = i.getDescription();
        this.status = i.getStatus();
        this.rawStatus = i.getRawStatus();
        this.severity = i.getSeverity();
        this.weaknessSource = i.getWeaknessSource();
        this.scheduledCompletionDate = i.getScheduledCompletionDate();
        this.actualCompletionDate = i.getActualCompletionDate();
        this.pointOfContact = i.getPointOfContact();
        this.riskRating = i.getRiskRating();
    }

    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getExternalId() { return externalId; } public void setExternalId(String s) { this.externalId = s; }
    public String getTitle() { return title; } public void setTitle(String s) { this.title = s; }
    public String getDescription() { return description; } public void setDescription(String s) { this.description = s; }
    public ConMonItemStatus getStatus() { return status; } public void setStatus(ConMonItemStatus s) { this.status = s; }
    public String getRawStatus() { return rawStatus; } public void setRawStatus(String s) { this.rawStatus = s; }
    public String getSeverity() { return severity; } public void setSeverity(String s) { this.severity = s; }
    public String getWeaknessSource() { return weaknessSource; } public void setWeaknessSource(String s) { this.weaknessSource = s; }
    public LocalDate getScheduledCompletionDate() { return scheduledCompletionDate; } public void setScheduledCompletionDate(LocalDate d) { this.scheduledCompletionDate = d; }
    public LocalDate getActualCompletionDate() { return actualCompletionDate; } public void setActualCompletionDate(LocalDate d) { this.actualCompletionDate = d; }
    public String getPointOfContact() { return pointOfContact; } public void setPointOfContact(String s) { this.pointOfContact = s; }
    public String getRiskRating() { return riskRating; } public void setRiskRating(String s) { this.riskRating = s; }
}

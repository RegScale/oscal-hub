package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.UserAccessRequest;

import java.time.LocalDateTime;

public class AccessRequestResponse {

    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Long organizationId;
    private String organizationName;
    private String status;
    private String message;
    private LocalDateTime requestDate;
    private Long reviewedBy;
    private String reviewedByUsername;
    private LocalDateTime reviewedDate;
    private String notes;

    // Constructors
    public AccessRequestResponse() {
    }

    public AccessRequestResponse(UserAccessRequest request) {
        this.id = request.getId();
        this.userId = request.getUser() != null ? request.getUser().getId() : null;
        this.username = request.getUser() != null ? request.getUser().getUsername() : request.getUsername();
        this.email = request.getUser() != null ? request.getUser().getEmail() : request.getEmail();
        this.firstName = request.getFirstName();
        this.lastName = request.getLastName();
        this.organizationId = request.getOrganization().getId();
        this.organizationName = request.getOrganization().getName();
        this.status = request.getStatus().toString();
        this.message = request.getMessage();
        this.requestDate = request.getRequestDate();
        this.reviewedBy = request.getReviewedBy() != null ? request.getReviewedBy().getId() : null;
        this.reviewedByUsername = request.getReviewedBy() != null ? request.getReviewedBy().getUsername() : null;
        this.reviewedDate = request.getReviewedDate();
        this.notes = request.getNotes();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewedByUsername() {
        return reviewedByUsername;
    }

    public void setReviewedByUsername(String reviewedByUsername) {
        this.reviewedByUsername = reviewedByUsername;
    }

    public LocalDateTime getReviewedDate() {
        return reviewedDate;
    }

    public void setReviewedDate(LocalDateTime reviewedDate) {
        this.reviewedDate = reviewedDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

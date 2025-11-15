package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.Size;

public class ReviewAccessRequestRequest {

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    // Constructors
    public ReviewAccessRequestRequest() {
    }

    public ReviewAccessRequestRequest(String notes) {
        this.notes = notes;
    }

    // Getters and Setters
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

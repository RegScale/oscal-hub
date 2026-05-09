package gov.nist.oscal.tools.api.dto;

import gov.nist.oscal.tools.api.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateStatusRequest {
    @NotNull private TicketStatus status;
    private String note;

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

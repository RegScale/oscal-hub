package gov.nist.oscal.tools.api.dto;

import gov.nist.oscal.tools.api.entity.Ticket;
import gov.nist.oscal.tools.api.entity.TicketPriority;
import gov.nist.oscal.tools.api.entity.TicketStatus;
import gov.nist.oscal.tools.api.entity.TicketType;
import java.time.LocalDateTime;

public record TicketSummaryResponse(
        Long id,
        TicketType type,
        String title,
        TicketPriority priority,
        TicketStatus status,
        String reporterUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TicketSummaryResponse from(Ticket t) {
        return new TicketSummaryResponse(
            t.getId(), t.getType(), t.getTitle(), t.getPriority(), t.getStatus(),
            t.getReporter().getUsername(), t.getCreatedAt(), t.getUpdatedAt());
    }
}

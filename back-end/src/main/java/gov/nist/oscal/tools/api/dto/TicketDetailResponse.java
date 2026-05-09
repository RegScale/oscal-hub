package gov.nist.oscal.tools.api.dto;

import gov.nist.oscal.tools.api.entity.Ticket;
import gov.nist.oscal.tools.api.entity.TicketPriority;
import gov.nist.oscal.tools.api.entity.TicketStatus;
import gov.nist.oscal.tools.api.entity.TicketType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record TicketDetailResponse(
        Long id,
        TicketType type,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        Map<String, Object> metadata,
        String reporterUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt,
        List<AttachmentResponse> originalAttachments,
        List<CommentResponse> comments) {

    public static TicketDetailResponse from(Ticket t,
                                            List<AttachmentResponse> originalAttachments,
                                            List<CommentResponse> comments) {
        return new TicketDetailResponse(
            t.getId(), t.getType(), t.getTitle(), t.getDescription(),
            t.getPriority(), t.getStatus(), t.getMetadata(),
            t.getReporter().getUsername(),
            t.getCreatedAt(), t.getUpdatedAt(), t.getResolvedAt(),
            originalAttachments, comments);
    }
}

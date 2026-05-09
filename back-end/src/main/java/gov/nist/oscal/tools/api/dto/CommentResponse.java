package gov.nist.oscal.tools.api.dto;

import gov.nist.oscal.tools.api.entity.TicketComment;
import gov.nist.oscal.tools.api.entity.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        String authorUsername,
        String body,
        boolean statusChange,
        TicketStatus oldStatus,
        TicketStatus newStatus,
        LocalDateTime createdAt,
        List<AttachmentResponse> attachments) {

    public static CommentResponse from(TicketComment c, List<AttachmentResponse> atts) {
        return new CommentResponse(
            c.getId(),
            c.getAuthor().getUsername(),
            c.getBody(),
            c.isStatusChange(),
            c.getOldStatus(),
            c.getNewStatus(),
            c.getCreatedAt(),
            atts);
    }
}

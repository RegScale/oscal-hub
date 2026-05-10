package gov.nist.oscal.tools.api.dto;

import gov.nist.oscal.tools.api.entity.TicketAttachment;

public record AttachmentResponse(Long id, String filename, String contentType, long sizeBytes) {
    public static AttachmentResponse from(TicketAttachment a) {
        return new AttachmentResponse(a.getId(), a.getFilename(), a.getContentType(), a.getSizeBytes());
    }
}

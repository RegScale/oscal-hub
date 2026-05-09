package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Ticket;
import gov.nist.oscal.tools.api.entity.TicketAttachment;
import gov.nist.oscal.tools.api.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
    List<TicketAttachment> findByTicket(Ticket ticket);
    List<TicketAttachment> findByComment(TicketComment comment);
    List<TicketAttachment> findByTicketAndCommentIsNull(Ticket ticket);
    List<TicketAttachment> findByCommentIn(java.util.Collection<TicketComment> comments);
}

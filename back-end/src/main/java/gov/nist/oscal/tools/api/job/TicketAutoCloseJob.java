package gov.nist.oscal.tools.api.job;

import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class TicketAutoCloseJob {
    private static final Logger log = LoggerFactory.getLogger(TicketAutoCloseJob.class);
    private static final int IDLE_DAYS = 7;

    private final TicketRepository tickets;
    private final TicketCommentRepository comments;

    public TicketAutoCloseJob(TicketRepository tickets, TicketCommentRepository comments) {
        this.tickets = tickets;
        this.comments = comments;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void run() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(IDLE_DAYS);
        var stale = tickets.findResolvedBefore(TicketStatus.RESOLVED, cutoff);
        for (Ticket t : stale) {
            TicketStatus old = t.getStatus();
            t.setStatus(TicketStatus.CLOSED);
            t.setUpdatedAt(LocalDateTime.now());
            tickets.save(t);
            // Author of system comment is the ticket reporter (avoids schema change for null author).
            TicketComment c = new TicketComment(t, t.getReporter(), "Auto-closed after 7 days in Resolved.");
            c.setStatusChange(true);
            c.setOldStatus(old);
            c.setNewStatus(TicketStatus.CLOSED);
            comments.save(c);
        }
        if (!stale.isEmpty()) log.info("Auto-closed {} resolved tickets", stale.size());
    }
}

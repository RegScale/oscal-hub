package gov.nist.oscal.tools.api.job;

import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TicketAutoCloseJobTest {
    private TicketRepository tickets;
    private TicketCommentRepository comments;
    private TicketAutoCloseJob job;

    @BeforeEach
    void setUp() {
        tickets = mock(TicketRepository.class);
        comments = mock(TicketCommentRepository.class);
        job = new TicketAutoCloseJob(tickets, comments);
    }

    @Test
    void closesResolvedTicketsOlderThan7Days_writesSystemComment_noEmail() {
        User reporter = new User();
        reporter.setId(1L); reporter.setUsername("alice");
        Ticket t = new Ticket(reporter, TicketType.BUG, "x", "y");
        t.setId(1L);
        t.setStatus(TicketStatus.RESOLVED);
        t.setResolvedAt(LocalDateTime.now().minusDays(8));
        when(tickets.findResolvedBefore(eq(TicketStatus.RESOLVED), any()))
            .thenReturn(List.of(t));

        job.run();

        assertThat(t.getStatus()).isEqualTo(TicketStatus.CLOSED);
        verify(tickets).save(t);
        verify(comments).save(any(TicketComment.class));
    }
}

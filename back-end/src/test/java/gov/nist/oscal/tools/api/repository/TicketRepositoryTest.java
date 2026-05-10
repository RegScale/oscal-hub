package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Ticket;
import gov.nist.oscal.tools.api.entity.TicketPriority;
import gov.nist.oscal.tools.api.entity.TicketStatus;
import gov.nist.oscal.tools.api.entity.TicketType;
import gov.nist.oscal.tools.api.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketRepositoryTest {

    @PersistenceContext
    EntityManager em;

    @Autowired
    TicketRepository tickets;

    // --- helpers ---

    private User newUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@test.example");
        u.setPassword("x");
        em.persist(u);
        return u;
    }

    private Ticket newTicket(User reporter, TicketStatus status, LocalDateTime resolvedAt) {
        Ticket t = new Ticket(reporter, TicketType.BUG, "Title for " + status, "Description");
        t.setPriority(TicketPriority.MEDIUM);
        t.setStatus(status);
        t.setResolvedAt(resolvedAt);
        em.persist(t);
        return t;
    }

    // --- tests ---

    @Test
    void findResolvedBeforeReturnsExpiredOnly() {
        User u = newUser("reporter-resolved-test");
        Ticket fresh = newTicket(u, TicketStatus.RESOLVED, LocalDateTime.now());
        Ticket old   = newTicket(u, TicketStatus.RESOLVED, LocalDateTime.now().minusDays(8));
        em.flush();
        em.clear();

        List<Ticket> result = tickets.findResolvedBefore(TicketStatus.RESOLVED,
                LocalDateTime.now().minusDays(7));

        assertThat(result).extracting(Ticket::getId).containsExactly(old.getId());
    }

    @Test
    void findByReporterReturnsOnlyReportersTickets() {
        User alice = newUser("alice-reporter-test");
        User bob   = newUser("bob-reporter-test");
        newTicket(alice, TicketStatus.OPEN, null);
        newTicket(bob,   TicketStatus.OPEN, null);
        em.flush();
        em.clear();

        Page<Ticket> result = tickets.findByReporter(alice, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getReporter().getId()).isEqualTo(alice.getId());
    }
}

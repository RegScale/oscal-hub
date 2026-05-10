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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketSpecificationsTest {

    @PersistenceContext
    EntityManager em;

    @Autowired
    TicketRepository tickets;

    // --- helpers ---

    private User newUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@spec-test.example");
        u.setPassword("x");
        em.persist(u);
        return u;
    }

    private Ticket newTicket(User reporter, TicketType type, String title, String description,
                              TicketStatus status, TicketPriority priority) {
        Ticket t = new Ticket(reporter, type, title, description);
        t.setStatus(status);
        t.setPriority(priority);
        em.persist(t);
        return t;
    }

    // --- tests ---

    @Test
    void searchByQueryMatchesTitleAndDescription() {
        User u = newUser("spec-user-1");
        Ticket titleMatch = newTicket(u, TicketType.BUG, "boom error in login", "some generic problem",
                TicketStatus.OPEN, TicketPriority.MEDIUM);
        Ticket descMatch = newTicket(u, TicketType.BUG, "feature feedback", "the system goes boom unexpectedly",
                TicketStatus.OPEN, TicketPriority.LOW);
        Ticket noMatch = newTicket(u, TicketType.FEATURE, "performance improvement", "make things faster",
                TicketStatus.OPEN, TicketPriority.HIGH);
        em.flush();
        em.clear();

        Specification<Ticket> spec = TicketSpecifications.matches(
                "boom", null, null, null, null, null);
        Page<Ticket> result = tickets.findAll(spec, Pageable.unpaged());

        assertThat(result.getContent())
                .extracting(Ticket::getId)
                .containsExactlyInAnyOrder(titleMatch.getId(), descMatch.getId())
                .doesNotContain(noMatch.getId());
    }

    @Test
    void filterByStatus() {
        User u = newUser("spec-user-2");
        Ticket open = newTicket(u, TicketType.BUG, "Open ticket", "desc",
                TicketStatus.OPEN, TicketPriority.MEDIUM);
        Ticket resolved = newTicket(u, TicketType.BUG, "Resolved ticket", "desc",
                TicketStatus.RESOLVED, TicketPriority.MEDIUM);
        Ticket inProgress = newTicket(u, TicketType.FEATURE, "In progress ticket", "desc",
                TicketStatus.IN_PROGRESS, TicketPriority.HIGH);
        em.flush();
        em.clear();

        Specification<Ticket> spec = TicketSpecifications.matches(
                null, List.of(TicketStatus.OPEN, TicketStatus.RESOLVED), null, null, null, null);
        Page<Ticket> result = tickets.findAll(spec, Pageable.unpaged());

        List<Long> ids = result.getContent().stream().map(Ticket::getId).toList();
        assertThat(ids).contains(open.getId(), resolved.getId());
        assertThat(ids).doesNotContain(inProgress.getId());
    }

    @Test
    void combinedFilters() {
        User u = newUser("spec-user-3");
        Ticket highBug = newTicket(u, TicketType.BUG, "High bug", "desc",
                TicketStatus.OPEN, TicketPriority.HIGH);
        Ticket criticalBug = newTicket(u, TicketType.BUG, "Critical bug", "desc",
                TicketStatus.OPEN, TicketPriority.CRITICAL);
        Ticket lowBug = newTicket(u, TicketType.BUG, "Low priority bug", "desc",
                TicketStatus.OPEN, TicketPriority.LOW);
        Ticket highFeature = newTicket(u, TicketType.FEATURE, "High feature", "desc",
                TicketStatus.OPEN, TicketPriority.HIGH);
        em.flush();
        em.clear();

        Specification<Ticket> spec = TicketSpecifications.matches(
                null, null, TicketType.BUG, List.of(TicketPriority.HIGH, TicketPriority.CRITICAL), null, null);
        Page<Ticket> result = tickets.findAll(spec, Pageable.unpaged());

        List<Long> ids = result.getContent().stream().map(Ticket::getId).toList();
        assertThat(ids).contains(highBug.getId(), criticalBug.getId());
        assertThat(ids).doesNotContain(lowBug.getId(), highFeature.getId());
    }
}

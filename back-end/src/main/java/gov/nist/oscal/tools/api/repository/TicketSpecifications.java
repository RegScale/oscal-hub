package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TicketSpecifications {
    private TicketSpecifications() {}

    public static Specification<Ticket> byReporter(User reporter) {
        return (root, query, cb) -> cb.equal(root.get("reporter"), reporter);
    }

    public static Specification<Ticket> matchesForReporter(
            User reporter, String q, List<TicketStatus> statuses, TicketType type,
            LocalDateTime from, LocalDateTime to) {
        return byReporter(reporter)
            .and(matches(q, statuses, type, null, from, to));
    }

    public static Specification<Ticket> matches(
            String q, List<TicketStatus> statuses, TicketType type,
            List<TicketPriority> priorities, LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                ps.add(cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like)
                ));
            }
            if (statuses != null && !statuses.isEmpty()) {
                ps.add(root.get("status").in(statuses));
            }
            if (type != null) ps.add(cb.equal(root.get("type"), type));
            if (priorities != null && !priorities.isEmpty()) {
                ps.add(root.get("priority").in(priorities));
            }
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) ps.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }
}

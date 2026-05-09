package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Ticket;
import gov.nist.oscal.tools.api.entity.TicketStatus;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository
        extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    Page<Ticket> findByReporter(User reporter, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.status = :status AND t.resolvedAt < :before")
    List<Ticket> findResolvedBefore(@Param("status") TicketStatus status,
                                    @Param("before") LocalDateTime before);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT status, COUNT(*) FROM tickets GROUP BY status",
        nativeQuery = true)
    java.util.List<Object[]> countByStatus();

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT type, COUNT(*) FROM tickets GROUP BY type",
        nativeQuery = true)
    java.util.List<Object[]> countByType();

    @org.springframework.data.jpa.repository.Query(value =
        "SELECT date_trunc('week', created_at)::date AS w, COUNT(*) " +
        "FROM tickets WHERE created_at > now() - interval '12 weeks' " +
        "GROUP BY 1 ORDER BY 1",
        nativeQuery = true)
    java.util.List<Object[]> openedPerWeek();

    @org.springframework.data.jpa.repository.Query(value =
        "SELECT date_trunc('week', resolved_at)::date AS w, COUNT(*) " +
        "FROM tickets WHERE resolved_at IS NOT NULL AND resolved_at > now() - interval '12 weeks' " +
        "GROUP BY 1 ORDER BY 1",
        nativeQuery = true)
    java.util.List<Object[]> resolvedPerWeek();

    @org.springframework.data.jpa.repository.Query(value =
        "SELECT * FROM tickets WHERE status IN ('OPEN','IN_PROGRESS') " +
        "AND created_at < now() - interval '30 days' " +
        "ORDER BY created_at ASC LIMIT 20",
        nativeQuery = true)
    java.util.List<Ticket> staleTickets();
}

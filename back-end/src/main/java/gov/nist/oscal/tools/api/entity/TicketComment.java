package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_comments")
public class TicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_status_change", nullable = false)
    private boolean statusChange = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 16)
    private TicketStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 16)
    private TicketStatus newStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TicketComment() {
        this.createdAt = LocalDateTime.now();
    }

    public TicketComment(Ticket ticket, User author, String body) {
        this();
        this.ticket = ticket;
        this.author = author;
        this.body = body;
    }

    /** Factory for system-generated status-change comments. */
    public static TicketComment statusChange(Ticket ticket, User actor, TicketStatus from, TicketStatus to) {
        TicketComment c = new TicketComment(ticket, actor,
            "Status changed from " + from.name() + " to " + to.name() + ".");
        c.statusChange = true;
        c.oldStatus = from;
        c.newStatus = to;
        return c;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public boolean isStatusChange() { return statusChange; }
    public void setStatusChange(boolean statusChange) { this.statusChange = statusChange; }
    public TicketStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(TicketStatus oldStatus) { this.oldStatus = oldStatus; }
    public TicketStatus getNewStatus() { return newStatus; }
    public void setNewStatus(TicketStatus newStatus) { this.newStatus = newStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

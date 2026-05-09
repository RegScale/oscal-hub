package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class TicketService {

    private final TicketRepository tickets;
    private final TicketCommentRepository comments;
    private final TicketAttachmentRepository attachments;
    private final TicketAttachmentStorageService storage;
    private final EmailService email;
    private final UserRepository users;
    private final String supportEmail;

    public TicketService(TicketRepository tickets,
                         TicketCommentRepository comments,
                         TicketAttachmentRepository attachments,
                         TicketAttachmentStorageService storage,
                         EmailService email,
                         UserRepository users,
                         @Value("${app.support.email}") String supportEmail) {
        this.tickets = tickets;
        this.comments = comments;
        this.attachments = attachments;
        this.storage = storage;
        this.email = email;
        this.users = users;
        this.supportEmail = supportEmail;
    }

    @Transactional
    public Ticket createTicket(User reporter, TicketType type, String title, String description,
                               TicketPriority priority, Map<String, Object> metadata,
                               List<MultipartFile> files) {
        if (files != null && files.size() > TicketAttachmentStorageService.MAX_FILES_PER_REQUEST) {
            throw new IllegalArgumentException(
                "Max " + TicketAttachmentStorageService.MAX_FILES_PER_REQUEST + " files per request");
        }
        Ticket t = new Ticket(reporter, type, title, description);
        t.setPriority(priority == null ? TicketPriority.MEDIUM : priority);
        if (metadata != null) t.setMetadata(metadata);
        Ticket saved = tickets.save(t);

        if (files != null) {
            for (MultipartFile f : files) {
                try {
                    var up = storage.upload(saved.getId(), f);
                    TicketAttachment a = new TicketAttachment();
                    a.setTicket(saved);
                    a.setUploader(reporter);
                    a.setFilename(up.originalFilename());
                    a.setContentType(up.contentType());
                    a.setSizeBytes(up.sizeBytes());
                    a.setStoragePath(up.storagePath());
                    attachments.save(a);
                } catch (IOException e) {
                    throw new RuntimeException("Attachment upload failed", e);
                }
            }
        }

        try { email.sendTicketCreatedToAdmin(saved); } catch (Exception ignored) {}
        try { email.sendTicketCreatedToReporter(saved); } catch (Exception ignored) {}
        return saved;
    }

    @Transactional(readOnly = true)
    public Ticket getTicket(Long id, User caller, boolean isAdmin) {
        Ticket t = tickets.findById(id)
            .orElseThrow(() -> new java.util.NoSuchElementException("Ticket " + id));
        if (!isAdmin && !t.getReporter().getId().equals(caller.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Not your ticket");
        }
        return t;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Ticket> listMyTickets(
            User reporter, org.springframework.data.domain.Pageable pageable) {
        return tickets.findByReporter(reporter, pageable);
    }

    @Transactional
    public TicketComment addComment(Long ticketId, User caller, boolean isAdmin,
                                    String body, List<MultipartFile> files) {
        if (files != null && files.size() > TicketAttachmentStorageService.MAX_FILES_PER_REQUEST) {
            throw new IllegalArgumentException(
                "Max " + TicketAttachmentStorageService.MAX_FILES_PER_REQUEST + " files per request");
        }
        Ticket t = getTicket(ticketId, caller, isAdmin);

        boolean reporterReopening = !isAdmin
            && caller.getId().equals(t.getReporter().getId())
            && t.getStatus().canReopen();

        TicketStatus oldStatus = t.getStatus();
        if (reporterReopening) {
            t.setStatus(TicketStatus.OPEN);
            t.setResolvedAt(null);
        }
        t.setUpdatedAt(java.time.LocalDateTime.now());
        tickets.save(t);

        TicketComment c = comments.save(new TicketComment(t, caller, body));

        if (reporterReopening) {
            comments.save(TicketComment.statusChange(t, caller, oldStatus, TicketStatus.OPEN));
        }

        if (files != null) {
            for (MultipartFile f : files) {
                try {
                    var up = storage.upload(t.getId(), f);
                    TicketAttachment a = new TicketAttachment();
                    a.setTicket(t); a.setComment(c); a.setUploader(caller);
                    a.setFilename(up.originalFilename());
                    a.setContentType(up.contentType());
                    a.setSizeBytes(up.sizeBytes());
                    a.setStoragePath(up.storagePath());
                    attachments.save(a);
                } catch (IOException e) {
                    throw new RuntimeException("Attachment upload failed", e);
                }
            }
        }

        try {
            if (reporterReopening) {
                email.sendTicketReopened(t, c);
            } else {
                String recipientEmail = isAdmin ? t.getReporter().getEmail() : supportEmail;
                email.sendTicketCommentAdded(t, c, recipientEmail);
            }
        } catch (Exception ignored) {}

        return c;
    }
}

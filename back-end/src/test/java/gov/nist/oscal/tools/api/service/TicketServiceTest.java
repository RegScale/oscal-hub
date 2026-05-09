package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.email.EmailService;
import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TicketServiceTest {

    private TicketRepository tickets;
    private TicketCommentRepository comments;
    private TicketAttachmentRepository attachments;
    private TicketAttachmentStorageService storage;
    private EmailService email;
    private UserRepository users;

    private TicketService svc;

    @BeforeEach
    void setUp() {
        tickets = mock(TicketRepository.class);
        comments = mock(TicketCommentRepository.class);
        attachments = mock(TicketAttachmentRepository.class);
        storage = mock(TicketAttachmentStorageService.class);
        email = mock(EmailService.class);
        users = mock(UserRepository.class);
        svc = new TicketService(tickets, comments, attachments, storage, email, users, "support@example.com");
    }

    @Test
    void createTicket_persistsAndSendsBothEmails() {
        User reporter = userWithUsername("alice");
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(42L);
            return t;
        });

        Ticket created = svc.createTicket(
            reporter, TicketType.BUG, "It crashed", "Steps...",
            TicketPriority.HIGH, Map.of("severity", "MAJOR"), List.of());

        assertThat(created.getId()).isEqualTo(42L);
        assertThat(created.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(created.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(created.getMetadata()).containsEntry("severity", "MAJOR");
        verify(email).sendTicketCreatedToAdmin(created);
        verify(email).sendTicketCreatedToReporter(created);
    }

    @Test
    void createTicket_uploadsAttachments() throws Exception {
        User reporter = userWithUsername("alice");
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0); t.setId(1L); return t;
        });
        MultipartFile file = new MockMultipartFile("f", "screenshot.png", "image/png", new byte[10]);
        when(storage.upload(eq(1L), eq(file)))
            .thenReturn(new TicketAttachmentStorageService.AttachmentUpload(
                "tickets/1/x-screenshot.png", 10L, "image/png", "screenshot.png"));

        svc.createTicket(reporter, TicketType.BUG, "t", "d",
            TicketPriority.MEDIUM, Map.of(), List.of(file));

        ArgumentCaptor<TicketAttachment> cap = ArgumentCaptor.forClass(TicketAttachment.class);
        verify(attachments).save(cap.capture());
        assertThat(cap.getValue().getStoragePath()).isEqualTo("tickets/1/x-screenshot.png");
        assertThat(cap.getValue().getComment()).isNull();
    }

    @Test
    void createTicket_rejectsMoreThanFiveFiles() {
        User reporter = userWithUsername("alice");
        var sixFiles = List.<MultipartFile>of(
            mockFile(), mockFile(), mockFile(), mockFile(), mockFile(), mockFile());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> svc.createTicket(reporter, TicketType.BUG, "t", "d",
                TicketPriority.MEDIUM, Map.of(), sixFiles));
        verifyNoInteractions(tickets);
    }

    private MultipartFile mockFile() {
        return new MockMultipartFile("f", "x.png", "image/png", new byte[1]);
    }

    private User userWithUsername(String u) {
        User user = new User();
        user.setId(1L);
        user.setUsername(u);
        user.setEmail(u + "@example.com");
        return user;
    }
}

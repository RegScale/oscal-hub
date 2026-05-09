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

    @Test
    void getTicket_returnsForReporter() {
        User alice = userWithUsername("alice");
        Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
        t.setId(7L);
        when(tickets.findById(7L)).thenReturn(java.util.Optional.of(t));

        Ticket got = svc.getTicket(7L, alice, /*isAdmin*/ false);
        assertThat(got.getId()).isEqualTo(7L);
    }

    @Test
    void getTicket_returnsForAdminEvenIfNotReporter() {
        User alice = userWithUsername("alice");
        User adminBob = new User(); adminBob.setId(2L); adminBob.setUsername("bob"); adminBob.setEmail("b@e.com");
        Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
        t.setId(7L);
        when(tickets.findById(7L)).thenReturn(java.util.Optional.of(t));

        assertThat(svc.getTicket(7L, adminBob, true).getId()).isEqualTo(7L);
    }

    @Test
    void getTicket_throwsForOtherUserNotAdmin() {
        User alice = userWithUsername("alice");
        User mallory = new User(); mallory.setId(3L); mallory.setUsername("mallory"); mallory.setEmail("m@e.com");
        Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
        t.setId(7L);
        when(tickets.findById(7L)).thenReturn(java.util.Optional.of(t));

        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.security.access.AccessDeniedException.class,
            () -> svc.getTicket(7L, mallory, false));
    }

    @Test
    void listMyTickets_filtersToReporter() {
        User alice = userWithUsername("alice");
        org.springframework.data.domain.Pageable p = org.springframework.data.domain.PageRequest.of(0, 25);
        when(tickets.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(p)))
            .thenReturn(org.springframework.data.domain.Page.empty());
        svc.listMyTickets(alice, null, null, null, null, null, p);
        verify(tickets).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(p));
    }

    @Test
    void addComment_byUser_emailsAdmin() {
        User alice = userWithUsername("alice");
        Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
        t.setId(1L); t.setStatus(TicketStatus.OPEN);
        when(tickets.findById(1L)).thenReturn(java.util.Optional.of(t));
        when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketComment c = svc.addComment(1L, alice, /*isAdmin*/ false, "More info", List.of());

        assertThat(c.getBody()).isEqualTo("More info");
        verify(email).sendTicketCommentAdded(eq(t), eq(c), eq("support@example.com"));
        verify(email, never()).sendTicketReopened(any(), any());
    }

    @Test
    void addComment_byAdmin_emailsReporter() {
        User alice = userWithUsername("alice");
        User adminBob = new User(); adminBob.setId(2L); adminBob.setUsername("bob"); adminBob.setEmail("b@e.com");
        Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
        t.setId(1L); t.setStatus(TicketStatus.IN_PROGRESS);
        when(tickets.findById(1L)).thenReturn(java.util.Optional.of(t));
        when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.addComment(1L, adminBob, true, "working on it", List.of());

        verify(email).sendTicketCommentAdded(eq(t), any(), eq(alice.getEmail()));
    }

    @Test
    void addComment_byReporterOnResolved_reopens() {
        User alice = userWithUsername("alice");
        Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
        t.setId(1L); t.setStatus(TicketStatus.RESOLVED);
        t.setResolvedAt(java.time.LocalDateTime.now().minusDays(1));
        when(tickets.findById(1L)).thenReturn(java.util.Optional.of(t));
        when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.addComment(1L, alice, false, "still broken", List.of());

        assertThat(t.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(t.getResolvedAt()).isNull();
        verify(email).sendTicketReopened(eq(t), any());
        verify(email, never()).sendTicketCommentAdded(any(), any(), any(String.class));
    }

    @Test
    void addComment_byReporterOnTerminalState_doesNotReopen() {
        User alice = userWithUsername("alice");
        Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
        t.setId(1L); t.setStatus(TicketStatus.CLOSED);
        when(tickets.findById(1L)).thenReturn(java.util.Optional.of(t));
        when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.addComment(1L, alice, false, "any updates?", List.of());

        assertThat(t.getStatus()).isEqualTo(TicketStatus.CLOSED);
        verify(email, never()).sendTicketReopened(any(), any());
        verify(email).sendTicketCommentAdded(any(), any(), any(String.class));
    }

    @Test
    void changeStatus_writesSystemCommentAndEmailsReporter() {
        User alice = userWithUsername("alice");
        User adminBob = new User(); adminBob.setId(2L); adminBob.setUsername("bob"); adminBob.setEmail("b@e.com");
        Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
        t.setId(1L); t.setStatus(TicketStatus.OPEN);
        when(tickets.findById(1L)).thenReturn(java.util.Optional.of(t));
        when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.changeStatus(1L, adminBob, TicketStatus.IN_PROGRESS, null);

        assertThat(t.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        ArgumentCaptor<TicketComment> cap = ArgumentCaptor.forClass(TicketComment.class);
        verify(comments).save(cap.capture());
        assertThat(cap.getValue().isStatusChange()).isTrue();
        assertThat(cap.getValue().getOldStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(cap.getValue().getNewStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(email).sendTicketStatusChanged(t, TicketStatus.OPEN, TicketStatus.IN_PROGRESS, null);
    }

    @Test
    void changeStatus_terminal_setsResolvedAt() {
        User alice = userWithUsername("alice");
        User adminBob = new User(); adminBob.setId(2L); adminBob.setUsername("bob"); adminBob.setEmail("b@e.com");
        Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
        t.setId(1L); t.setStatus(TicketStatus.IN_PROGRESS);
        when(tickets.findById(1L)).thenReturn(java.util.Optional.of(t));
        when(comments.save(any())).thenAnswer(inv -> inv.getArgument(0));

        svc.changeStatus(1L, adminBob, TicketStatus.RESOLVED, "Fixed in v2.1.4");

        assertThat(t.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(t.getResolvedAt()).isNotNull();
        verify(comments, times(2)).save(any());
        verify(email).sendTicketStatusChanged(t, TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, "Fixed in v2.1.4");
    }

    @Test
    void changeStatus_noopWhenSameStatus() {
        User alice = userWithUsername("alice");
        User adminBob = new User(); adminBob.setId(2L); adminBob.setUsername("bob"); adminBob.setEmail("b@e.com");
        Ticket t = new Ticket(alice, TicketType.BUG, "x", "y");
        t.setId(1L); t.setStatus(TicketStatus.OPEN);
        when(tickets.findById(1L)).thenReturn(java.util.Optional.of(t));

        svc.changeStatus(1L, adminBob, TicketStatus.OPEN, null);

        verify(comments, never()).save(any());
        verify(email, never()).sendTicketStatusChanged(any(), any(), any(), any());
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

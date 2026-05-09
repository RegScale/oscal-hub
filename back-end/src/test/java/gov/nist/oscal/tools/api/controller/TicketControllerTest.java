package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.repository.TicketAttachmentRepository;
import gov.nist.oscal.tools.api.repository.TicketCommentRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.service.TicketAttachmentStorageService;
import gov.nist.oscal.tools.api.service.TicketService;
import gov.nist.oscal.tools.api.telemetry.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TicketService service;
    @MockitoBean private UserRepository users;
    @MockitoBean private TicketAttachmentRepository attachments;
    @MockitoBean private TicketCommentRepository comments;
    @MockitoBean private TicketAttachmentStorageService storage;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private RateLimitService rateLimitService;
    @MockitoBean private RateLimitConfig rateLimitConfig;
    @MockitoBean private SecurityHeadersConfig securityHeadersConfig;
    @MockitoBean private TelemetryService telemetryService;

    private User makeUser(long id, String username, User.GlobalRole role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setGlobalRole(role);
        return u;
    }

    private Ticket makeTicket(long id, User reporter) {
        Ticket t = new Ticket(reporter, TicketType.BUG, "Test title", "Test description");
        t.setId(id);
        t.setPriority(TicketPriority.MEDIUM);
        t.setStatus(TicketStatus.OPEN);
        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        return t;
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void listMyTicketsReturnsPage() throws Exception {
        User alice = makeUser(1L, "alice", User.GlobalRole.USER);
        when(users.findByUsername(eq("alice"))).thenReturn(Optional.of(alice));

        Ticket t = makeTicket(10L, alice);
        when(service.listMyTickets(eq(alice), any(), any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(t)));

        mockMvc.perform(get("/api/tickets/mine"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(10))
            .andExpect(jsonPath("$.content[0].title").value("Test title"))
            .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getTicketDetailReturnsFullResponse() throws Exception {
        User alice = makeUser(1L, "alice", User.GlobalRole.USER);
        when(users.findByUsername(eq("alice"))).thenReturn(Optional.of(alice));

        Ticket t = makeTicket(10L, alice);
        when(service.getTicket(eq(10L), eq(alice), eq(false))).thenReturn(t);
        when(attachments.findByTicketAndCommentIsNull(t)).thenReturn(List.of());
        when(comments.findByTicketOrderByCreatedAtAsc(t)).thenReturn(List.of());

        mockMvc.perform(get("/api/tickets/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.title").value("Test title"))
            .andExpect(jsonPath("$.reporterUsername").value("alice"))
            .andExpect(jsonPath("$.comments").isArray());
    }

    @Test
    @WithMockUser(username = "bob", roles = "USER")
    void getTicketForbiddenForOtherUser() throws Exception {
        User bob = makeUser(2L, "bob", User.GlobalRole.USER);
        when(users.findByUsername(eq("bob"))).thenReturn(Optional.of(bob));

        when(service.getTicket(eq(10L), eq(bob), eq(false)))
            .thenThrow(new org.springframework.security.access.AccessDeniedException("Not your ticket"));

        mockMvc.perform(get("/api/tickets/10"))
            .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/tickets/mine"))
            .andExpect(status().isUnauthorized());
    }
}

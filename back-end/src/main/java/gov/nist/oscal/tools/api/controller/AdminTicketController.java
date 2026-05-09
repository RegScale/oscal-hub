package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.dto.TicketSummaryResponse;
import gov.nist.oscal.tools.api.dto.UpdateStatusRequest;
import gov.nist.oscal.tools.api.entity.Ticket;
import gov.nist.oscal.tools.api.entity.TicketPriority;
import gov.nist.oscal.tools.api.entity.TicketStatus;
import gov.nist.oscal.tools.api.entity.TicketType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.TicketRepository;
import gov.nist.oscal.tools.api.repository.TicketSpecifications;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/tickets")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Tickets", description = "Super-admin-only ticket APIs")
public class AdminTicketController {

    private final TicketService service;
    private final UserRepository users;
    private final TicketRepository tickets;

    public AdminTicketController(TicketService service, UserRepository users, TicketRepository tickets) {
        this.service = service;
        this.users = users;
        this.tickets = tickets;
    }

    @GetMapping
    public Page<TicketSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<TicketStatus> status,
            @RequestParam(required = false) TicketType type,
            @RequestParam(required = false) List<TicketPriority> priority,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "updatedAt,desc") String sort) {

        String[] sp = sort.split(",");
        Sort.Direction dir = sp.length > 1 && sp[1].equalsIgnoreCase("asc")
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pr = PageRequest.of(page, Math.min(size, 100), dir, sp[0]);

        return tickets.findAll(
            TicketSpecifications.matches(q, status, type, priority, from, to), pr)
            .map(TicketSummaryResponse::from);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketSummaryResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest req,
            Principal principal) {
        User admin = users.findByUsername(principal.getName()).orElseThrow();
        Ticket t = service.changeStatus(id, admin, req.getStatus(), req.getNote());
        return ResponseEntity.ok(TicketSummaryResponse.from(t));
    }
}

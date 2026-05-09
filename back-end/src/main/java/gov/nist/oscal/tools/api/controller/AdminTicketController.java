package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.dto.TicketSummaryResponse;
import gov.nist.oscal.tools.api.dto.UpdateStatusRequest;
import gov.nist.oscal.tools.api.entity.Ticket;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin/tickets")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Tickets", description = "Super-admin-only ticket APIs")
public class AdminTicketController {

    private final TicketService service;
    private final UserRepository users;

    public AdminTicketController(TicketService service, UserRepository users) {
        this.service = service;
        this.users = users;
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

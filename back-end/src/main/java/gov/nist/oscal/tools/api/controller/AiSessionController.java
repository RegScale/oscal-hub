package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.model.ai.StartSessionRequest;
import gov.nist.oscal.tools.api.model.ai.StartSessionResponse;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.ai.AiOrchestrator;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai/sessions")
@Tag(name = "AI Sessions", description = "Run an AI wizard and stream progress")
public class AiSessionController {

    private final AiOrchestrator orchestrator;
    private final AiSessionEventStream stream;
    private final UserRepository users;
    private final OrganizationMembershipRepository memberships;
    private final AiSessionRepository sessions;

    public AiSessionController(AiOrchestrator orchestrator, AiSessionEventStream stream,
                               UserRepository users, OrganizationMembershipRepository memberships,
                               AiSessionRepository sessions) {
        this.orchestrator = orchestrator;
        this.stream = stream;
        this.users = users;
        this.memberships = memberships;
        this.sessions = sessions;
    }

    @Operation(summary = "Start an AI wizard session")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<StartSessionResponse> start(@Valid @RequestBody StartSessionRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = users.findByUsername(username).orElseThrow();
        requireOrgMembership(user, req.getOrganizationId());
        UUID id = orchestrator.start(req.getOrganizationId(), user.getId(),
                req.getWizardKind(), req.getMode(), req.getInput());
        return ResponseEntity.ok(new StartSessionResponse(id));
    }

    @Operation(summary = "Start an AI wizard session with a file upload")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StartSessionResponse> startWithUpload(
            @RequestParam Long organizationId,
            @RequestParam WizardKind wizardKind,
            @RequestParam(required = false, defaultValue = "STREAMING") AiSessionMode mode,
            @RequestParam(required = false) String prompt,
            @RequestPart MultipartFile file) throws IOException {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = users.findByUsername(username).orElseThrow();
        requireOrgMembership(user, organizationId);
        UUID id = orchestrator.start(organizationId, user.getId(), wizardKind, mode,
                prompt, file.getBytes(), file.getOriginalFilename());
        return ResponseEntity.ok(new StartSessionResponse(id));
    }

    @Operation(summary = "Subscribe to a session's progress stream")
    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFor(@PathVariable UUID id) {
        return stream.subscribe(id);
    }

    @Operation(summary = "Cancel a running session")
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = users.findByUsername(username).orElseThrow();
        AiSession session = sessions.findById(id).orElse(null);
        if (session != null) {
            if (!session.getUserId().equals(user.getId())
                    && user.getGlobalRole() != User.GlobalRole.SUPER_ADMIN) {
                throw new AccessDeniedException("Not authorized to cancel this session");
            }
            session.setStatus(AiSessionStatus.CANCELLED);
            session.setEndedAt(LocalDateTime.now());
            sessions.save(session);
        }
        stream.close(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Verifies the authenticated user is either a SUPER_ADMIN or an ACTIVE member of
     * the requested organization. Throws AccessDeniedException on mismatch.
     */
    private void requireOrgMembership(User user, Long organizationId) {
        if (user.getGlobalRole() == User.GlobalRole.SUPER_ADMIN) return;
        memberships.findByUserIdAndOrganizationId(user.getId(), organizationId)
                .filter(m -> m.getStatus() == OrganizationMembership.MembershipStatus.ACTIVE)
                .orElseThrow(() -> new AccessDeniedException(
                        "User is not an active member of organization " + organizationId));
    }
}

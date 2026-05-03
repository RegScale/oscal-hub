package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.ai.StartSessionRequest;
import gov.nist.oscal.tools.api.model.ai.StartSessionResponse;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.ai.AiOrchestrator;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai/sessions")
@Tag(name = "AI Sessions", description = "Run an AI wizard and stream progress")
public class AiSessionController {

    private final AiOrchestrator orchestrator;
    private final AiSessionEventStream stream;
    private final UserRepository users;

    public AiSessionController(AiOrchestrator orchestrator, AiSessionEventStream stream, UserRepository users) {
        this.orchestrator = orchestrator;
        this.stream = stream;
        this.users = users;
    }

    @Operation(summary = "Start an AI wizard session")
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<StartSessionResponse> start(@Valid @RequestBody StartSessionRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = users.findByUsername(username).orElseThrow();
        UUID id = orchestrator.start(req.getOrganizationId(), user.getId(),
                req.getWizardKind(), req.getMode(), req.getInput());
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
        stream.close(id);
        return ResponseEntity.noContent().build();
    }
}

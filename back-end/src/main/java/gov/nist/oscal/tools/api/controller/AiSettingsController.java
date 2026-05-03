package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.model.ai.AiSettingsResponse;
import gov.nist.oscal.tools.api.model.ai.UpdateAiSettingsRequest;
import gov.nist.oscal.tools.api.service.ai.AiSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/settings")
@Tag(name = "AI Settings", description = "Per-organization AI configuration")
public class AiSettingsController {

    private final AiSettingsService service;

    public AiSettingsController(AiSettingsService service) {
        this.service = service;
    }

    @Operation(summary = "Get AI settings for an organization")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<AiSettingsResponse> get(@RequestParam Long organizationId) {
        return ResponseEntity.ok(service.getSettings(organizationId));
    }

    @Operation(summary = "Set or rotate the Anthropic API key for an organization")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @PutMapping
    public ResponseEntity<AiSettingsResponse> put(@RequestParam Long organizationId,
                                                  @Valid @RequestBody UpdateAiSettingsRequest req) {
        return ResponseEntity.ok(service.setApiKey(organizationId, req.getApiKey(), req.getDefaultModel()));
    }

    @Operation(summary = "Disable AI for an organization (clears stored key)")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping
    public ResponseEntity<Void> disable(@RequestParam Long organizationId) {
        service.disable(organizationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Capability probe — is AI enabled for this org?")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status(@RequestParam Long organizationId) {
        return ResponseEntity.ok(Map.of("enabled", service.isEnabledFor(organizationId)));
    }
}

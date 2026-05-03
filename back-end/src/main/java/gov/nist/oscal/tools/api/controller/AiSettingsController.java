package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.ai.AiSettingsResponse;
import gov.nist.oscal.tools.api.model.ai.UpdateAiSettingsRequest;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.ai.AiSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/settings")
@Tag(name = "AI Settings", description = "Per-organization AI configuration")
public class AiSettingsController {

    private final AiSettingsService service;
    private final UserRepository users;
    private final OrganizationMembershipRepository memberships;

    public AiSettingsController(AiSettingsService service, UserRepository users,
                                OrganizationMembershipRepository memberships) {
        this.service = service;
        this.users = users;
        this.memberships = memberships;
    }

    @Operation(summary = "Get AI settings for an organization")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @GetMapping
    public ResponseEntity<AiSettingsResponse> get(@RequestParam Long organizationId) {
        requireOrgAdmin(organizationId);
        return ResponseEntity.ok(service.getSettings(organizationId));
    }

    @Operation(summary = "Set or rotate the Anthropic API key for an organization")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @PutMapping
    public ResponseEntity<AiSettingsResponse> put(@RequestParam Long organizationId,
                                                  @Valid @RequestBody UpdateAiSettingsRequest req) {
        requireOrgAdmin(organizationId);
        return ResponseEntity.ok(service.setApiKey(organizationId, req.getApiKey(), req.getDefaultModel()));
    }

    @Operation(summary = "Disable AI for an organization (clears stored key)")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @DeleteMapping
    public ResponseEntity<Void> disable(@RequestParam Long organizationId) {
        requireOrgAdmin(organizationId);
        service.disable(organizationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Capability probe — is AI enabled for this org?")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> status(@RequestParam Long organizationId) {
        return ResponseEntity.ok(Map.of("enabled", service.isEnabledFor(organizationId)));
    }

    /**
     * Verifies the authenticated user is either a SUPER_ADMIN (platform bypass) or an
     * ACTIVE ORG_ADMIN member of the requested organization. Throws AccessDeniedException
     * on mismatch — matching the pattern used in InvitationController.
     */
    private void requireOrgAdmin(Long organizationId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = users.findByUsername(username).orElseThrow();
        if (user.getGlobalRole() == User.GlobalRole.SUPER_ADMIN) return;
        memberships.findByUserIdAndOrganizationId(user.getId(), organizationId)
                .filter(m -> m.getStatus() == OrganizationMembership.MembershipStatus.ACTIVE
                          && m.getRole() == OrganizationMembership.OrganizationRole.ORG_ADMIN)
                .orElseThrow(() -> new AccessDeniedException(
                        "User does not belong to organization " + organizationId + " as ORG_ADMIN"));
    }
}

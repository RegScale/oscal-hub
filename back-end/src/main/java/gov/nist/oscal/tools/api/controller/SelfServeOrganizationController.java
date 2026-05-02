package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.OrganizationNameInUseException;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-serve organization endpoint — any authenticated user can create an organization
 * and automatically become its ORG_ADMIN.
 */
@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organizations", description = "Self-serve organization management")
public class SelfServeOrganizationController {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private UserRepository userRepository;

    public static class CreateOrganizationRequest {
        @NotBlank(message = "Organization name must not be blank")
        @Size(max = 255, message = "Organization name must not exceed 255 characters")
        private String name;

        public String getName() { return name; }
        public void setName(String n) { this.name = n; }
    }

    public static class OrganizationResponse {
        public Long id;
        public String name;

        public OrganizationResponse(Organization o) {
            this.id = o.getId();
            this.name = o.getName();
        }
    }

    @Operation(
        summary = "Create my organization",
        description = "Create a new organization and automatically become its ORG_ADMIN. " +
                      "Available to any authenticated user."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Organization created successfully"),
        @ApiResponse(responseCode = "409", description = "Organization name already in use"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping
    public ResponseEntity<?> createMyOrganization(
            @Valid @RequestBody CreateOrganizationRequest req,
            Authentication auth) {
        try {
            User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
            Organization org = organizationService.createOrganizationForUser(req.getName(), user);
            return ResponseEntity.ok(new OrganizationResponse(org));
        } catch (OrganizationNameInUseException e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "ORGANIZATION_NAME_IN_USE");
            err.put("field", "name");
            err.put("message", "That organization name is already taken. Try another.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
        } catch (IllegalStateException e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }
}

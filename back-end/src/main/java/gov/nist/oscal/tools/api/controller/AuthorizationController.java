package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.model.AuthorizationRequest;
import gov.nist.oscal.tools.api.model.AuthorizationResponse;
import gov.nist.oscal.tools.api.service.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/authorizations")
@Tag(name = "Authorizations", description = "APIs for managing system authorizations")
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    @Autowired
    public AuthorizationController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Operation(
        summary = "Create new authorization",
        description = "Create a new system authorization linked to an SSP, optional SAR, and template"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Authorization created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<AuthorizationResponse> createAuthorization(
            @Valid @RequestBody AuthorizationRequest request,
            Principal principal) {
        try {
            Authorization authorization = authorizationService.createAuthorization(
                    request.getName(),
                    request.getSspItemId(),
                    request.getSarItemId(),
                    request.getTemplateId(),
                    request.getVariableValues(),
                    principal.getName(),
                    request.getDateAuthorized(),
                    request.getDateExpired(),
                    request.getSystemOwner(),
                    request.getSecurityManager(),
                    request.getAuthorizingOfficial(),
                    request.getEditedContent()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthorizationResponse(authorization));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
        summary = "Update authorization",
        description = "Update an existing authorization"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authorization updated successfully"),
        @ApiResponse(responseCode = "404", description = "Authorization not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{id}")
    public ResponseEntity<AuthorizationResponse> updateAuthorization(
            @PathVariable Long id,
            @RequestBody AuthorizationRequest request,
            Principal principal) {
        try {
            Authorization authorization = authorizationService.updateAuthorization(
                    id,
                    request.getName(),
                    request.getVariableValues(),
                    principal.getName()
            );

            return ResponseEntity.ok(new AuthorizationResponse(authorization));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get authorization by ID",
        description = "Retrieve a specific authorization"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authorization found"),
        @ApiResponse(responseCode = "404", description = "Authorization not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AuthorizationResponse> getAuthorization(@PathVariable Long id) {
        try {
            Authorization authorization = authorizationService.getAuthorization(id);
            return ResponseEntity.ok(new AuthorizationResponse(authorization));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get all authorizations",
        description = "Retrieve all system authorizations"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authorizations retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<AuthorizationResponse>> getAllAuthorizations() {
        try {
            List<Authorization> authorizations = authorizationService.getAllAuthorizations();
            List<AuthorizationResponse> responses = authorizations.stream()
                    .map(AuthorizationResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get recently authorized systems",
        description = "Retrieve recently authorized systems"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authorizations retrieved successfully")
    })
    @GetMapping("/recent")
    public ResponseEntity<List<AuthorizationResponse>> getRecentlyAuthorized(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Authorization> authorizations = authorizationService.getRecentlyAuthorized(limit);
            List<AuthorizationResponse> responses = authorizations.stream()
                    .map(AuthorizationResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Get authorizations by SSP",
        description = "Retrieve all authorizations for a specific SSP"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authorizations retrieved successfully")
    })
    @GetMapping("/ssp/{sspItemId}")
    public ResponseEntity<List<AuthorizationResponse>> getAuthorizationsBySsp(@PathVariable String sspItemId) {
        try {
            List<Authorization> authorizations = authorizationService.getAuthorizationsBySsp(sspItemId);
            List<AuthorizationResponse> responses = authorizations.stream()
                    .map(AuthorizationResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Search authorizations",
        description = "Search authorizations by name or SSP item ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<List<AuthorizationResponse>> searchAuthorizations(
            @RequestParam(required = false) String q) {
        try {
            List<Authorization> authorizations = authorizationService.searchAuthorizations(q);
            List<AuthorizationResponse> responses = authorizations.stream()
                    .map(AuthorizationResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Delete authorization",
        description = "Delete an authorization (creator only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authorization deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not the creator"),
        @ApiResponse(responseCode = "404", description = "Authorization not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuthorization(@PathVariable Long id, Principal principal) {
        try {
            authorizationService.deleteAuthorization(id, principal.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Only the creator")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

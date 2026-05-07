package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationGrant;
import gov.nist.oscal.tools.api.entity.AuthorizationRole;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.repository.AuthorizationGrantRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.AuthorizationAccessGuard;
import gov.nist.oscal.tools.api.service.AuthorizationService;
import gov.nist.oscal.tools.api.service.DigitalSignatureService;
import gov.nist.oscal.tools.api.telemetry.EventNames;
import gov.nist.oscal.tools.api.telemetry.TelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/authorizations")
@Tag(name = "Authorizations", description = "APIs for managing system authorizations")
public class AuthorizationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationController.class);

    private final AuthorizationService authorizationService;
    private final DigitalSignatureService digitalSignatureService;
    private final TelemetryService telemetryService;
    private final AuthorizationAccessGuard accessGuard;
    private final AuthorizationGrantRepository grantRepository;
    private final UserRepository userRepository;

    @Autowired
    public AuthorizationController(
            AuthorizationService authorizationService,
            DigitalSignatureService digitalSignatureService,
            TelemetryService telemetryService,
            AuthorizationAccessGuard accessGuard,
            AuthorizationGrantRepository grantRepository,
            UserRepository userRepository) {
        this.authorizationService = authorizationService;
        this.digitalSignatureService = digitalSignatureService;
        this.telemetryService = telemetryService;
        this.accessGuard = accessGuard;
        this.grantRepository = grantRepository;
        this.userRepository = userRepository;
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
                    request.getEditedContent(),
                    request.getConditions()
            );

            try {
                telemetryService.emit(EventNames.AUTHORIZATION_CREATED, Map.of(
                        "authorization_id", authorization.getId() != null ? String.valueOf(authorization.getId()) : "",
                        "template_id", request.getTemplateId() != null ? String.valueOf(request.getTemplateId()) : "",
                        "ssp_item_id", request.getSspItemId() != null ? request.getSspItemId() : ""
                ));
            } catch (Exception telEx) {
                logger.debug("Telemetry emit failed (non-fatal): {}", telEx.getMessage());
            }

            User currentUser = requireCurrentUser(principal);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toResponse(authorization, currentUser));
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
                    principal.getName(),
                    request.getDateAuthorized(),
                    request.getDateExpired(),
                    request.getSystemOwner(),
                    request.getSecurityManager(),
                    request.getAuthorizingOfficial(),
                    request.getEditedContent(),
                    request.getConditions()
            );

            User currentUser = requireCurrentUser(principal);
            return ResponseEntity.ok(toResponse(authorization, currentUser));
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
    public ResponseEntity<AuthorizationResponse> getAuthorization(@PathVariable Long id,
                                                                  Principal principal) {
        try {
            Authorization authorization = authorizationService.getAuthorizationForUser(
                    id, principal.getName());
            User currentUser = requireCurrentUser(principal);
            return ResponseEntity.ok(toResponse(authorization, currentUser));
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
    public ResponseEntity<List<AuthorizationResponse>> getAllAuthorizations(Principal principal) {
        try {
            List<Authorization> authorizations =
                    authorizationService.getAllAuthorizationsForUser(principal.getName());
            User currentUser = requireCurrentUser(principal);
            List<AuthorizationResponse> responses = authorizations.stream()
                    .map(a -> toResponse(a, currentUser))
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
            @RequestParam(defaultValue = "10") int limit,
            Principal principal) {
        try {
            List<Authorization> all =
                    authorizationService.getAllAuthorizationsForUser(principal.getName());
            List<Authorization> authorizations = all.stream()
                    .sorted((a, b) -> b.getAuthorizedAt().compareTo(a.getAuthorizedAt()))
                    .limit(limit)
                    .toList();
            User currentUser = requireCurrentUser(principal);
            List<AuthorizationResponse> responses = authorizations.stream()
                    .map(a -> toResponse(a, currentUser))
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
    public ResponseEntity<List<AuthorizationResponse>> getAuthorizationsBySsp(
            @PathVariable String sspItemId,
            Principal principal) {
        try {
            List<Authorization> authorizations =
                    authorizationService.getAuthorizationsBySspForUser(sspItemId, principal.getName());
            User currentUser = requireCurrentUser(principal);
            List<AuthorizationResponse> responses = authorizations.stream()
                    .map(a -> toResponse(a, currentUser))
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
            @RequestParam(required = false) String q,
            Principal principal) {
        try {
            List<Authorization> authorizations =
                    authorizationService.searchAuthorizationsForUser(principal.getName(), q);
            User currentUser = requireCurrentUser(principal);
            List<AuthorizationResponse> responses = authorizations.stream()
                    .map(a -> toResponse(a, currentUser))
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

    // ===== Digital Signature Endpoints =====

    @Operation(
        summary = "Sign authorization with CAC/PIV certificate",
        description = "Digitally sign an authorization using TLS client certificate from CAC/PIV card"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authorization signed successfully"),
        @ApiResponse(responseCode = "400", description = "Signature failed - invalid certificate"),
        @ApiResponse(responseCode = "401", description = "No client certificate provided"),
        @ApiResponse(responseCode = "404", description = "Authorization not found")
    })
    @PostMapping("/sign-with-cert")
    public ResponseEntity<SignatureResult> signWithClientCertificate(
            @RequestBody SignRequest request,
            HttpServletRequest httpRequest,
            Principal principal) {

        logger.info("Sign request for authorization {}", request.getAuthorizationId());

        // Extract client certificate from TLS connection
        X509Certificate[] certs = (X509Certificate[])
                httpRequest.getAttribute("jakarta.servlet.request.X509Certificate");

        if (certs == null || certs.length == 0) {
            logger.warn("No client certificate provided for signing");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new SignatureResult(false, "No client certificate provided"));
        }

        X509Certificate clientCert = certs[0];
        logger.info("Client certificate received: {}", clientCert.getSubjectX500Principal());

        try {
            // Validate certificate first
            CertificateValidationResult validation =
                    digitalSignatureService.validateCertificate(clientCert);

            if (!validation.isValid()) {
                logger.warn("Certificate validation failed: {}", validation.getNotes());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new SignatureResult(false,
                                "Certificate validation failed: " + validation.getNotes()));
            }

            // Load authorization scoped to the current user's organization — prevents
            // cross-org signing by rejecting IDs that don't belong to this user's org.
            Authorization authorization = authorizationService.getAuthorizationForUser(
                    request.getAuthorizationId(), principal.getName());

            // Sign the authorization
            SignatureResult result = digitalSignatureService.signAuthorization(
                    authorization,
                    clientCert
            );

            logger.info("Authorization {} signed successfully by {}",
                    request.getAuthorizationId(), result.getSignerName());

            try {
                telemetryService.emit(EventNames.AUTHORIZATION_APPROVED, Map.of(
                        "authorization_id", String.valueOf(request.getAuthorizationId()),
                        "signature_method", "CAC_PIV"
                ));
            } catch (Exception telEx) {
                logger.debug("Telemetry emit failed (non-fatal): {}", telEx.getMessage());
            }

            return ResponseEntity.ok(result);

        } catch (jakarta.persistence.EntityNotFoundException e) {
            logger.error("Authorization not found: {}", request.getAuthorizationId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new SignatureResult(false, "Authorization not found"));
        } catch (Exception e) {
            logger.error("Signing failed for authorization {}", request.getAuthorizationId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SignatureResult(false, "Signing failed: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Sign authorization with electronic signature",
        description = "Save an electronic signature (drawn with mouse/touchscreen) for an authorization"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Electronic signature saved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - missing signature data or signer name"),
        @ApiResponse(responseCode = "404", description = "Authorization not found")
    })
    @PostMapping("/sign-electronically")
    public ResponseEntity<SignatureResult> signElectronically(
            @RequestBody ElectronicSignatureRequest request,
            Principal principal) {

        logger.info("Electronic signature request for authorization {}", request.getAuthorizationId());

        // Validate required fields
        if (request.getSignerName() == null || request.getSignerName().trim().isEmpty()) {
            logger.warn("Signer name is required for electronic signature");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new SignatureResult(false, "Signer name is required"));
        }

        if (request.getSignatureImageData() == null || request.getSignatureImageData().trim().isEmpty()) {
            logger.warn("Signature image data is required");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new SignatureResult(false, "Signature image is required"));
        }

        try {
            // Get the authorization scoped to the current user's organization
            Authorization auth = authorizationService.getAuthorizationForUser(
                    request.getAuthorizationId(), principal.getName());

            // Save electronic signature
            auth.setDigitalSignatureMethod("ELECTRONIC");
            auth.setElectronicSignatureImage(request.getSignatureImageData());
            auth.setSignerCommonName(request.getSignerName());
            if (request.getSignerTitle() != null && !request.getSignerTitle().trim().isEmpty()) {
                auth.setSignerEmail(request.getSignerTitle()); // Reuse email field for title
            }
            auth.setSignatureTimestamp(LocalDateTime.now());

            authorizationService.save(auth);

            logger.info("Authorization {} signed electronically by {}",
                    request.getAuthorizationId(), request.getSignerName());

            try {
                telemetryService.emit(EventNames.AUTHORIZATION_APPROVED, Map.of(
                        "authorization_id", String.valueOf(request.getAuthorizationId()),
                        "signature_method", "ELECTRONIC"
                ));
            } catch (Exception telEx) {
                logger.debug("Telemetry emit failed (non-fatal): {}", telEx.getMessage());
            }

            SignatureResult result = new SignatureResult(true, "Electronic signature saved successfully");
            result.setSignerName(request.getSignerName());
            result.setSignerEmail(request.getSignerTitle());
            result.setSignatureTimestamp(LocalDateTime.now());

            return ResponseEntity.ok(result);

        } catch (jakarta.persistence.EntityNotFoundException e) {
            logger.error("Authorization not found: {}", request.getAuthorizationId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new SignatureResult(false, "Authorization not found"));
        } catch (Exception e) {
            logger.error("Electronic signing failed for authorization {}", request.getAuthorizationId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SignatureResult(false, "Electronic signing failed: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Get signature details",
        description = "Get digital signature information for an authorization"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signature details retrieved"),
        @ApiResponse(responseCode = "404", description = "Authorization or signature not found")
    })
    @GetMapping("/{id}/signature")
    public ResponseEntity<SignatureDetailsResponse> getSignatureDetails(@PathVariable Long id,
                                                                        Principal principal) {
        try {
            Authorization auth = authorizationService.getAuthorizationForUser(id, principal.getName());

            if (auth.getSignerCertificate() == null) {
                logger.debug("No signature found for authorization {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new SignatureDetailsResponse("No signature found"));
            }

            SignatureDetailsResponse response = SignatureDetailsResponse.builder()
                    .signed(true)
                    .signerName(auth.getSignerCommonName())
                    .signerEmail(auth.getSignerEmail())
                    .signerEdipi(auth.getSignerEdipi())
                    .signatureTimestamp(auth.getSignatureTimestamp())
                    .certificateIssuer(auth.getCertificateIssuer())
                    .certificateSerial(auth.getCertificateSerial())
                    .certificateNotBefore(auth.getCertificateNotBefore())
                    .certificateNotAfter(auth.getCertificateNotAfter())
                    .certificateVerified(auth.getCertificateVerified())
                    .verificationDate(auth.getCertificateVerificationDate())
                    .verificationNotes(auth.getCertificateVerificationNotes())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get signature details for authorization {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new SignatureDetailsResponse("Authorization not found"));
        }
    }

    @Operation(
        summary = "Verify signature",
        description = "Re-verify the digital signature on an authorization"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signature verified"),
        @ApiResponse(responseCode = "404", description = "Authorization or signature not found"),
        @ApiResponse(responseCode = "500", description = "Verification failed")
    })
    @PostMapping("/{id}/verify-signature")
    public ResponseEntity<SignatureVerificationResponse> verifySignature(@PathVariable Long id,
                                                                         Principal principal) {
        try {
            Authorization auth = authorizationService.getAuthorizationForUser(id, principal.getName());

            if (auth.getSignerCertificate() == null) {
                logger.debug("No signature to verify for authorization {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Re-validate certificate
            CertificateValidationResult result =
                    digitalSignatureService.verifyCertificate(auth.getSignerCertificate());

            // Update verification status
            auth.setCertificateVerified(result.isValid());
            auth.setCertificateVerificationDate(LocalDateTime.now());
            auth.setCertificateVerificationNotes(result.getNotes());
            authorizationService.save(auth);

            logger.info("Signature verification for authorization {}: {}",
                    id, result.isValid() ? "VALID" : "INVALID");

            return ResponseEntity.ok(SignatureVerificationResponse.builder()
                    .valid(result.isValid())
                    .verificationDate(LocalDateTime.now())
                    .notes(result.getNotes())
                    .build());

        } catch (Exception e) {
            logger.error("Verification failed for authorization {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== Grant Management Endpoints =====

    @Operation(
        summary = "List grants for an authorization",
        description = "List all user grants on a specific authorization (requires OWNER role)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Grants retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient role — OWNER required"),
        @ApiResponse(responseCode = "404", description = "Authorization not found")
    })
    @GetMapping("/{id}/grants")
    public ResponseEntity<List<AuthorizationGrantResponse>> listGrants(@PathVariable Long id,
                                                                       Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(id, principal.getName());
        User currentUser = requireCurrentUser(principal);
        accessGuard.requireManageGrants(authorization, currentUser);

        List<AuthorizationGrantResponse> grants = grantRepository.findByAuthorization(authorization).stream()
                .map(AuthorizationGrantResponse::new)
                .toList();
        return ResponseEntity.ok(grants);
    }

    @Operation(
        summary = "Add a grant to an authorization",
        description = "Grant a user a specific role on an authorization (requires OWNER role)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Grant created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or user not in org"),
        @ApiResponse(responseCode = "403", description = "Insufficient role — OWNER required"),
        @ApiResponse(responseCode = "404", description = "Authorization or user not found")
    })
    @PostMapping("/{id}/grants")
    public ResponseEntity<AuthorizationGrantResponse> addGrant(@PathVariable Long id,
                                                               @Valid @RequestBody AuthorizationGrantRequest request,
                                                               Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(id, principal.getName());
        User currentUser = requireCurrentUser(principal);
        accessGuard.requireManageGrants(authorization, currentUser);

        User grantee = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User " + request.getUserId() + " not found."));

        // Reject grants for users not in the authorization's organization.
        if (!isInSameOrg(authorization, grantee)) {
            throw new IllegalArgumentException("User is not a member of this authorization's organization.");
        }

        AuthorizationGrant grant = grantRepository.findByAuthorizationAndUser(authorization, grantee)
                .orElseGet(() -> new AuthorizationGrant(authorization, grantee, request.getRole(), currentUser));
        grant.setRole(request.getRole());
        grant.setGrantedBy(currentUser);
        grantRepository.save(grant);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthorizationGrantResponse(grant));
    }

    @Operation(
        summary = "Update a grant on an authorization",
        description = "Change the role for an existing grant (requires OWNER role)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Grant updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Insufficient role — OWNER required"),
        @ApiResponse(responseCode = "404", description = "Authorization or grant not found")
    })
    @PatchMapping("/{id}/grants/{grantId}")
    public ResponseEntity<AuthorizationGrantResponse> updateGrant(@PathVariable Long id,
                                                                  @PathVariable Long grantId,
                                                                  @Valid @RequestBody AuthorizationGrantRequest request,
                                                                  Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(id, principal.getName());
        User currentUser = requireCurrentUser(principal);
        accessGuard.requireManageGrants(authorization, currentUser);

        AuthorizationGrant grant = grantRepository.findById(grantId)
                .filter(g -> g.getAuthorization().getId().equals(id))
                .orElseThrow(() -> new IllegalArgumentException("Grant " + grantId + " not found on authorization " + id));

        grant.setRole(request.getRole());
        grant.setGrantedBy(currentUser);
        grantRepository.save(grant);

        return ResponseEntity.ok(new AuthorizationGrantResponse(grant));
    }

    @Operation(
        summary = "Remove a grant from an authorization",
        description = "Revoke a user's access grant on an authorization (requires OWNER role)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Grant removed successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient role — OWNER required"),
        @ApiResponse(responseCode = "404", description = "Authorization or grant not found")
    })
    @DeleteMapping("/{id}/grants/{grantId}")
    public ResponseEntity<Void> removeGrant(@PathVariable Long id,
                                            @PathVariable Long grantId,
                                            Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(id, principal.getName());
        User currentUser = requireCurrentUser(principal);
        accessGuard.requireManageGrants(authorization, currentUser);

        AuthorizationGrant grant = grantRepository.findById(grantId)
                .filter(g -> g.getAuthorization().getId().equals(id))
                .orElseThrow(() -> new IllegalArgumentException("Grant " + grantId + " not found on authorization " + id));

        grantRepository.delete(grant);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Set or clear the share-with-org default role",
        description = "Set a default role for all org members on this authorization (requires OWNER role). "
                + "Pass null to clear. Allowed values: VIEWER, CONTRIBUTOR, EDITOR."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Share-with-org setting updated"),
        @ApiResponse(responseCode = "400", description = "Invalid role (OWNER not allowed as default)"),
        @ApiResponse(responseCode = "403", description = "Insufficient role — OWNER required"),
        @ApiResponse(responseCode = "404", description = "Authorization not found")
    })
    @PatchMapping("/{id}/share-with-org")
    public ResponseEntity<AuthorizationResponse> setShareWithOrg(@PathVariable Long id,
                                                                 @RequestBody ShareWithOrgRequest request,
                                                                 Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(id, principal.getName());
        User currentUser = requireCurrentUser(principal);
        accessGuard.requireManageGrants(authorization, currentUser);

        if (request.getRole() != null && !AuthorizationRole.isAssignableAsShareDefault(request.getRole())) {
            throw new IllegalArgumentException("Cannot set share-with-org default to " + request.getRole()
                    + ". Allowed: VIEWER, CONTRIBUTOR, EDITOR.");
        }

        authorization.setShareWithOrgDefaultRole(request.getRole());
        authorizationService.save(authorization);

        return ResponseEntity.ok(toResponse(authorization, currentUser));
    }

    // ===== Private helpers =====

    private AuthorizationResponse toResponse(Authorization authorization, User currentUser) {
        AuthorizationResponse response = new AuthorizationResponse(authorization);
        response.setEffectiveRole(accessGuard.effectiveRole(authorization, currentUser));
        response.setShareWithOrgDefaultRole(authorization.getShareWithOrgDefaultRole());
        return response;
    }

    private User requireCurrentUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User '" + principal.getName() + "' not found."));
    }

    private boolean isInSameOrg(Authorization authorization, User user) {
        return user.getOrganizationMemberships().stream()
                .anyMatch(m -> m.getOrganization().getId().equals(authorization.getOrganization().getId())
                        && m.getStatus() == OrganizationMembership.MembershipStatus.ACTIVE);
    }
}

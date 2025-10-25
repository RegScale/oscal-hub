package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.service.AuthorizationService;
import gov.nist.oscal.tools.api.service.DigitalSignatureService;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/authorizations")
@Tag(name = "Authorizations", description = "APIs for managing system authorizations")
public class AuthorizationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationController.class);

    private final AuthorizationService authorizationService;
    private final DigitalSignatureService digitalSignatureService;

    @Autowired
    public AuthorizationController(
            AuthorizationService authorizationService,
            DigitalSignatureService digitalSignatureService) {
        this.authorizationService = authorizationService;
        this.digitalSignatureService = digitalSignatureService;
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
                    principal.getName(),
                    request.getDateAuthorized(),
                    request.getDateExpired(),
                    request.getSystemOwner(),
                    request.getSecurityManager(),
                    request.getAuthorizingOfficial(),
                    request.getEditedContent(),
                    request.getConditions()
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
            HttpServletRequest httpRequest) {

        logger.info("Sign request for authorization {}", request.getAuthorizationId());

        // Extract client certificate from TLS connection
        X509Certificate[] certs = (X509Certificate[])
                httpRequest.getAttribute("javax.servlet.request.X509Certificate");

        if (certs == null || certs.length == 0) {
            logger.warn("No client certificate provided for signing");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new SignatureResult(false, "No client certificate provided"));
        }

        X509Certificate clientCert = certs[0];
        logger.info("Client certificate received: {}", clientCert.getSubjectDN());

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

            // Sign the authorization
            SignatureResult result = digitalSignatureService.signAuthorization(
                    request.getAuthorizationId(),
                    clientCert
            );

            logger.info("Authorization {} signed successfully by {}",
                    request.getAuthorizationId(), result.getSignerName());

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
        summary = "Get signature details",
        description = "Get digital signature information for an authorization"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signature details retrieved"),
        @ApiResponse(responseCode = "404", description = "Authorization or signature not found")
    })
    @GetMapping("/{id}/signature")
    public ResponseEntity<SignatureDetailsResponse> getSignatureDetails(@PathVariable Long id) {
        try {
            Authorization auth = authorizationService.getAuthorization(id);

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
    public ResponseEntity<SignatureVerificationResponse> verifySignature(@PathVariable Long id) {
        try {
            Authorization auth = authorizationService.getAuthorization(id);

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
}

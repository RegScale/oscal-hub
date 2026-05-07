package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationDocument;
import gov.nist.oscal.tools.api.entity.DocumentType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.AuthorizationDocumentResponse;
import gov.nist.oscal.tools.api.model.PackageCompletenessResponse;
import gov.nist.oscal.tools.api.model.UpdateDocumentMetadataRequest;
import gov.nist.oscal.tools.api.repository.AuthorizationDocumentRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.AuthorizationAccessGuard;
import gov.nist.oscal.tools.api.service.AuthorizationDocumentService;
import gov.nist.oscal.tools.api.service.AuthorizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/authorizations/{authorizationId}/documents")
@Tag(name = "Authorization Documents", description = "Upload and manage supporting documents on an authorization")
public class AuthorizationDocumentsController {

    /**
     * "Core" document types whose presence is checked by the package-completeness
     * panel. Per the spec, these seven are the most-asked-for in an audit package.
     */
    private static final List<DocumentType> CORE_DOCUMENT_TYPES = List.of(
            DocumentType.VULNERABILITY_SCAN,
            DocumentType.PENETRATION_TEST,
            DocumentType.SSP,
            DocumentType.SAR,
            DocumentType.CONTINGENCY_PLAN,
            DocumentType.AUTHORIZATION_LETTER,
            DocumentType.RISK_ASSESSMENT
    );

    private final AuthorizationService authorizationService;
    private final AuthorizationDocumentService documentService;
    private final AuthorizationDocumentRepository documentRepository;
    private final AuthorizationAccessGuard accessGuard;
    private final UserRepository userRepository;

    public AuthorizationDocumentsController(AuthorizationService authorizationService,
                                            AuthorizationDocumentService documentService,
                                            AuthorizationDocumentRepository documentRepository,
                                            AuthorizationAccessGuard accessGuard,
                                            UserRepository userRepository) {
        this.authorizationService = authorizationService;
        this.documentService = documentService;
        this.documentRepository = documentRepository;
        this.accessGuard = accessGuard;
        this.userRepository = userRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuthorizationDocumentResponse> upload(
            @PathVariable Long authorizationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "version", required = false) String version,
            @RequestParam(value = "effectiveDate", required = false) String effectiveDate,
            @RequestParam(value = "expiresAt", required = false) String expiresAt,
            Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        User currentUser = requireCurrentUser(principal);
        accessGuard.requireUploadDocument(authorization, currentUser);

        AuthorizationDocument doc = documentService.upload(
                authorization, currentUser, file, documentType,
                description, tags, version,
                parseDate(effectiveDate), parseDate(expiresAt));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthorizationDocumentResponse(doc));
    }

    @GetMapping
    public ResponseEntity<List<AuthorizationDocumentResponse>> list(
            @PathVariable Long authorizationId,
            @RequestParam(value = "type", required = false) DocumentType type,
            @RequestParam(value = "q", required = false) String q,
            Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());

        List<AuthorizationDocumentResponse> result = documentService.list(authorization, type, q).stream()
                .map(AuthorizationDocumentResponse::new)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<AuthorizationDocumentResponse> get(
            @PathVariable Long authorizationId,
            @PathVariable Long documentId,
            Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        AuthorizationDocument doc = requireDocument(authorization, documentId);
        return ResponseEntity.ok(new AuthorizationDocumentResponse(doc));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable Long authorizationId,
            @PathVariable Long documentId,
            Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        AuthorizationDocument doc = requireDocument(authorization, documentId);

        byte[] bytes = documentService.download(doc);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(doc.getContentType()));
        headers.setContentDisposition(org.springframework.http.ContentDisposition
                .attachment()
                .filename(doc.getOriginalFilename())
                .build());
        headers.setContentLength(bytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(new ByteArrayResource(bytes));
    }

    @PatchMapping("/{documentId}")
    public ResponseEntity<AuthorizationDocumentResponse> updateMetadata(
            @PathVariable Long authorizationId,
            @PathVariable Long documentId,
            @RequestBody UpdateDocumentMetadataRequest body,
            Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        User currentUser = requireCurrentUser(principal);
        accessGuard.requireUploadDocument(authorization, currentUser);
        AuthorizationDocument doc = requireDocument(authorization, documentId);

        AuthorizationDocument updated = documentService.updateMetadata(
                doc, body.getDocumentType(), body.getDescription(), body.getTags(),
                body.getVersion(), body.getEffectiveDate(), body.getExpiresAt());
        return ResponseEntity.ok(new AuthorizationDocumentResponse(updated));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long authorizationId,
            @PathVariable Long documentId,
            Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        User currentUser = requireCurrentUser(principal);
        AuthorizationDocument doc = requireDocument(authorization, documentId);
        accessGuard.requireDeleteOwnedItem(authorization, currentUser, doc.getUploadedBy().getId());

        documentService.delete(doc);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/completeness")
    public ResponseEntity<PackageCompletenessResponse> completeness(
            @PathVariable Long authorizationId,
            Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());

        LocalDate today = LocalDate.now();
        List<PackageCompletenessResponse.Item> items = CORE_DOCUMENT_TYPES.stream()
                .map(type -> {
                    long present = documentRepository.findByAuthorizationAndType(authorization, type).stream()
                            .filter(d -> d.getExpiresAt() == null || !d.getExpiresAt().isBefore(today))
                            .count();
                    return new PackageCompletenessResponse.Item(type, (int) present);
                })
                .toList();
        return ResponseEntity.ok(new PackageCompletenessResponse(items));
    }

    private AuthorizationDocument requireDocument(Authorization authorization, Long documentId) {
        return documentRepository.findByIdAndAuthorization(documentId, authorization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Document " + documentId + " not found on authorization " + authorization.getId()));
    }

    private User requireCurrentUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User '" + principal.getName() + "' not found."));
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format: " + raw);
        }
    }
}

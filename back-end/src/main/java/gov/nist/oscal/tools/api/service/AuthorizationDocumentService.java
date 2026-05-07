package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationDocument;
import gov.nist.oscal.tools.api.entity.DocumentType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.UnsupportedDocumentTypeException;
import gov.nist.oscal.tools.api.repository.AuthorizationDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Manages document upload, listing, download, metadata edit, and deletion
 * for documents attached to an authorization. Path scheme:
 *   authorizations/{authorizationId}/documents/{uuid}-{originalFilename}
 *
 * Access control is enforced upstream in the controller via
 * AuthorizationAccessGuard — this service trusts the caller and operates on
 * already-resolved Authorization + User entities.
 */
@Service
public class AuthorizationDocumentService {

    /**
     * Allowlist of acceptable content types. Excludes executables and other
     * formats the browser would interpret as code.
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/csv",
            "text/plain",
            "text/markdown",
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/svg+xml",
            "application/zip",
            "application/json",
            "application/xml",
            "text/xml",
            "application/x-yaml",
            "text/yaml"
    );

    private final AuthorizationDocumentRepository repository;
    private final FileStorageService fileStorageService;

    public AuthorizationDocumentService(AuthorizationDocumentRepository repository,
                                        FileStorageService fileStorageService) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public AuthorizationDocument upload(Authorization authorization,
                                        User uploader,
                                        MultipartFile file,
                                        DocumentType type,
                                        String description,
                                        String tags,
                                        String version,
                                        LocalDate effectiveDate,
                                        LocalDate expiresAt) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new UnsupportedDocumentTypeException(contentType);
        }

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String storagePath = "authorizations/" + authorization.getId()
                + "/documents/" + UUID.randomUUID() + "-" + originalFilename;

        AuthorizationDocument doc = new AuthorizationDocument();
        doc.setAuthorization(authorization);
        doc.setUploadedBy(uploader);
        doc.setOriginalFilename(originalFilename);
        doc.setFileSize(file.getSize());
        doc.setContentType(contentType);
        doc.setStoragePath(storagePath);
        doc.setDocumentType(type);
        doc.setDescription(description);
        doc.setTags(tags);
        doc.setVersion(version);
        doc.setEffectiveDate(effectiveDate);
        doc.setExpiresAt(expiresAt);

        AuthorizationDocument saved = repository.save(doc);

        try {
            fileStorageService.saveBinary(storagePath, file.getBytes(), contentType);
        } catch (IOException e) {
            // @Transactional will roll back the metadata insert on RuntimeException.
            throw new RuntimeException("Failed to read uploaded file bytes", e);
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<AuthorizationDocument> list(Authorization authorization,
                                            DocumentType typeFilter,
                                            String searchTerm) {
        List<AuthorizationDocument> docs;
        if (typeFilter != null) {
            docs = repository.findByAuthorizationAndType(authorization, typeFilter);
        } else if (searchTerm != null && !searchTerm.isBlank()) {
            docs = repository.searchInAuthorization(authorization, searchTerm.trim());
        } else {
            docs = repository.findByAuthorizationOrderByUploadedAtDesc(authorization);
        }
        // Force-load LAZY associations while still inside the transaction so the
        // controller can safely call new AuthorizationDocumentResponse(doc) after
        // the transaction boundary closes (preventing LazyInitializationException).
        for (AuthorizationDocument doc : docs) {
            if (doc.getAuthorization() != null) doc.getAuthorization().getId();
            if (doc.getUploadedBy() != null) doc.getUploadedBy().getUsername();
        }
        return docs;
    }

    /**
     * Fetches a single document by id and authorization, eagerly initializing
     * the LAZY {@code authorization} and {@code uploadedBy} associations so that
     * the controller can safely build response DTOs after the transaction closes.
     * Returns an empty Optional if the document does not exist or belongs to a
     * different authorization.
     */
    @Transactional(readOnly = true)
    public Optional<AuthorizationDocument> findByIdAndAuthorization(
            Long documentId, Authorization authorization) {
        return repository.findByIdAndAuthorization(documentId, authorization)
                .map(doc -> {
                    if (doc.getAuthorization() != null) doc.getAuthorization().getId();
                    if (doc.getUploadedBy() != null) doc.getUploadedBy().getUsername();
                    return doc;
                });
    }

    public byte[] download(AuthorizationDocument doc) {
        byte[] bytes = fileStorageService.loadBinary(doc.getStoragePath());
        if (bytes == null) {
            throw new RuntimeException("File blob missing for document " + doc.getId()
                    + " at " + doc.getStoragePath());
        }
        return bytes;
    }

    @Transactional
    public AuthorizationDocument updateMetadata(AuthorizationDocument doc,
                                                DocumentType type,
                                                String description,
                                                String tags,
                                                String version,
                                                LocalDate effectiveDate,
                                                LocalDate expiresAt) {
        if (type != null) doc.setDocumentType(type);
        doc.setDescription(description);
        doc.setTags(tags);
        doc.setVersion(version);
        doc.setEffectiveDate(effectiveDate);
        doc.setExpiresAt(expiresAt);
        return repository.save(doc);
    }

    @Transactional
    public void delete(AuthorizationDocument doc) {
        repository.delete(doc);
        fileStorageService.deleteBinary(doc.getStoragePath());
    }

    private static String normalizeContentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "application/octet-stream";
        }
        // Strip any parameter portion (e.g., "text/csv; charset=UTF-8")
        int semi = raw.indexOf(';');
        return (semi < 0 ? raw : raw.substring(0, semi)).trim().toLowerCase();
    }

    private static String sanitizeFilename(String raw) {
        if (raw == null || raw.isBlank()) {
            return "file";
        }
        String trimmed = raw.replace("\\", "/");
        int slash = trimmed.lastIndexOf('/');
        String basename = slash < 0 ? trimmed : trimmed.substring(slash + 1);
        // Replace anything that's not a safe filename character.
        return basename.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}

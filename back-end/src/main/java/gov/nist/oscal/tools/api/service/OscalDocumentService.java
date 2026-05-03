package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.OscalDocument;
import gov.nist.oscal.tools.api.entity.OscalModelType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.OscalDocumentRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CRUD service for OSCAL documents that share the unified storage
 * (SSP, Assessment Plan, Assessment Results, POA&amp;M).
 */
@Service
public class OscalDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(OscalDocumentService.class);

    @Autowired
    private OscalDocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    @Transactional
    public OscalDocument create(
            OscalModelType modelType, String title, String description, String version,
            String oscalVersion, String filename, String jsonContent, String oscalUuid,
            String statsJson, Boolean draft, String username) {

        logger.info("Creating {} '{}' for user {}", modelType, title, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (oscalUuid == null || oscalUuid.trim().isEmpty()) {
            oscalUuid = UUID.randomUUID().toString();
        }
        if (documentRepository.findByOscalUuid(oscalUuid).isPresent()) {
            throw new RuntimeException("Document with UUID " + oscalUuid + " already exists");
        }

        String storagePath = storageService.buildPath(username, filename);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("title", title);
        metadata.put("oscalVersion", oscalVersion);
        metadata.put("uploadedBy", username);
        metadata.put("docType", modelType.slug());
        storageService.uploadComponent(username, filename, jsonContent, metadata);

        long fileSize = storageService.getFileSize(storagePath);

        OscalDocument doc = new OscalDocument(oscalUuid, modelType, title, storagePath, user);
        doc.setDescription(description);
        doc.setVersion(version);
        doc.setOscalVersion(oscalVersion);
        doc.setFilename(filename);
        doc.setFileSize(fileSize);
        doc.setStatsJson(statsJson);
        doc.setDraft(Boolean.TRUE.equals(draft));
        doc.setLastUpdatedBy(user);

        doc = documentRepository.save(doc);
        logger.info("Created {} id={} uuid={}", modelType, doc.getId(), oscalUuid);
        return doc;
    }

    @Transactional
    public OscalDocument update(
            Long documentId, String title, String description, String version,
            String jsonContent, String statsJson, Boolean draft, String username) {

        OscalDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (!doc.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can update this document");
        }

        if (title != null) doc.setTitle(title);
        if (description != null) doc.setDescription(description);
        if (version != null) doc.setVersion(version);
        if (statsJson != null) doc.setStatsJson(statsJson);
        if (draft != null) doc.setDraft(draft);

        if (jsonContent != null) {
            storageService.uploadComponent(username, doc.getFilename(), jsonContent, null);
            doc.setFileSize(storageService.getFileSize(doc.getStoragePath()));
        }

        doc.setLastUpdatedBy(user);
        return documentRepository.save(doc);
    }

    public OscalDocument get(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
    }

    public OscalDocument getByUuid(String oscalUuid) {
        return documentRepository.findByOscalUuid(oscalUuid)
                .orElseThrow(() -> new RuntimeException("Document not found with UUID: " + oscalUuid));
    }

    public String getContent(Long id) {
        OscalDocument doc = get(id);
        return storageService.downloadComponent(doc.getStoragePath());
    }

    public List<OscalDocument> listByUserAndType(String username, OscalModelType modelType) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return documentRepository.findByUserAndType(user, modelType);
    }

    public List<OscalDocument> search(String username, OscalModelType modelType, String term) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        if (term == null || term.trim().isEmpty()) {
            return documentRepository.findByUserAndType(user, modelType);
        }
        return documentRepository.searchByUserAndType(user, modelType, term);
    }

    @Transactional
    public void delete(Long id, String username) {
        OscalDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        if (!doc.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can delete this document");
        }
        storageService.deleteComponent(doc.getStoragePath());
        documentRepository.delete(doc);
    }
}

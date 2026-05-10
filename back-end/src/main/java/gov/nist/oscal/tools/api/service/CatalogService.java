package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Catalog;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.CatalogRepository;
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
import java.util.stream.Collectors;

/**
 * Service for managing OSCAL catalogs created via the builder.
 * Reuses StorageService for file persistence; storage layer is generic
 * even though method names mention "component".
 */
@Service
public class CatalogService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogService.class);

    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    @Transactional
    public Catalog createCatalog(
            String title, String description, String version, String oscalVersion,
            String filename, String jsonContent, String oscalUuid,
            Integer groupCount, Integer controlCount, Integer paramCount,
            Boolean draft, String username) {

        logger.info("Creating catalog: {} by user: {}", title, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (oscalUuid == null || oscalUuid.trim().isEmpty()) {
            oscalUuid = UUID.randomUUID().toString();
        }

        if (catalogRepository.findByOscalUuid(oscalUuid).isPresent()) {
            throw new RuntimeException("Catalog with UUID " + oscalUuid + " already exists");
        }

        String storagePath = storageService.buildPath(username, filename);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("title", title);
        metadata.put("oscalVersion", oscalVersion);
        metadata.put("uploadedBy", username);
        metadata.put("docType", "catalog");

        storageService.uploadComponent(username, filename, jsonContent, metadata);

        long fileSize = storageService.getFileSize(storagePath);

        Catalog catalog = new Catalog(oscalUuid, title, storagePath, user);
        catalog.setDescription(description);
        catalog.setVersion(version);
        catalog.setOscalVersion(oscalVersion);
        catalog.setFilename(filename);
        catalog.setFileSize(fileSize);
        catalog.setGroupCount(groupCount);
        catalog.setControlCount(controlCount);
        catalog.setParamCount(paramCount);
        catalog.setDraft(Boolean.TRUE.equals(draft));
        catalog.setLastUpdatedBy(user);

        catalog = catalogRepository.save(catalog);
        logger.info("Created catalog id={} uuid={}", catalog.getId(), oscalUuid);
        return catalog;
    }

    @Transactional
    public Catalog updateCatalog(
            Long catalogId, String title, String description, String version,
            String jsonContent, Integer groupCount, Integer controlCount, Integer paramCount,
            Boolean draft, String username) {

        Catalog catalog = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new RuntimeException("Catalog not found: " + catalogId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (!catalog.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can update this catalog");
        }

        if (title != null) catalog.setTitle(title);
        if (description != null) catalog.setDescription(description);
        if (version != null) catalog.setVersion(version);
        if (groupCount != null) catalog.setGroupCount(groupCount);
        if (controlCount != null) catalog.setControlCount(controlCount);
        if (paramCount != null) catalog.setParamCount(paramCount);
        if (draft != null) catalog.setDraft(draft);

        if (jsonContent != null) {
            storageService.uploadComponent(username, catalog.getFilename(), jsonContent, null);
            catalog.setFileSize(storageService.getFileSize(catalog.getStoragePath()));
        }

        catalog.setLastUpdatedBy(user);
        return catalogRepository.save(catalog);
    }

    public Catalog getCatalog(Long catalogId) {
        return catalogRepository.findById(catalogId)
                .orElseThrow(() -> new RuntimeException("Catalog not found: " + catalogId));
    }

    public Catalog getCatalogByUuid(String oscalUuid) {
        return catalogRepository.findByOscalUuid(oscalUuid)
                .orElseThrow(() -> new RuntimeException("Catalog not found with UUID: " + oscalUuid));
    }

    public String getCatalogContent(Long catalogId) {
        Catalog catalog = getCatalog(catalogId);
        return storageService.downloadComponent(catalog.getStoragePath());
    }

    public List<Catalog> getUserCatalogs(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return catalogRepository.findByCreatedByOrderByCreatedAtDesc(user);
    }

    public List<Catalog> searchCatalogs(String username, String searchTerm) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return catalogRepository.findByCreatedByOrderByCreatedAtDesc(user);
        }
        return catalogRepository.findByCreatedByAndSearch(user, searchTerm);
    }

    @Transactional
    public void deleteCatalog(Long catalogId, String username) {
        Catalog catalog = catalogRepository.findById(catalogId)
                .orElseThrow(() -> new RuntimeException("Catalog not found: " + catalogId));
        if (!catalog.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can delete this catalog");
        }
        storageService.deleteComponent(catalog.getStoragePath());
        catalogRepository.delete(catalog);
    }

    public Map<String, Object> getStatistics(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        List<Catalog> all = catalogRepository.findByCreatedBy(user);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCatalogs", all.size());
        stats.put("totalControls", all.stream().mapToInt(c -> c.getControlCount() == null ? 0 : c.getControlCount()).sum());
        stats.put("totalGroups", all.stream().mapToInt(c -> c.getGroupCount() == null ? 0 : c.getGroupCount()).sum());
        stats.put("totalStorageBytes", all.stream().mapToLong(c -> c.getFileSize() == null ? 0L : c.getFileSize()).sum());
        Map<String, Long> versions = all.stream()
                .filter(c -> c.getOscalVersion() != null)
                .collect(Collectors.groupingBy(Catalog::getOscalVersion, Collectors.counting()));
        stats.put("oscalVersions", versions);
        return stats;
    }
}

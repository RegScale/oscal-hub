package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.ComponentDefinition;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.ComponentDefinitionRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing OSCAL component definitions
 * Provides CRUD operations, search, and analytics for component builder
 */
@Service
public class ComponentDefinitionService {

    private static final Logger logger = LoggerFactory.getLogger(ComponentDefinitionService.class);

    @Autowired
    private ComponentDefinitionRepository componentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AzureBlobService blobService;

    /**
     * Create a new component definition
     */
    @Transactional
    public ComponentDefinition createComponentDefinition(
            String title, String description, String version, String oscalVersion,
            String filename, String jsonContent, String oscalUuid,
            Integer componentCount, Integer capabilityCount, Integer controlCount, String username) {

        logger.info("Creating component definition: {} by user: {}", title, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Generate OSCAL UUID if not provided
        if (oscalUuid == null || oscalUuid.trim().isEmpty()) {
            oscalUuid = UUID.randomUUID().toString();
        }

        // Check if component with this UUID already exists
        if (componentRepository.findByOscalUuid(oscalUuid).isPresent()) {
            throw new RuntimeException("Component definition with UUID " + oscalUuid + " already exists");
        }

        // Upload JSON content to Azure Blob Storage
        String blobPath = blobService.buildBlobPath(username, filename);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("title", title);
        metadata.put("oscalVersion", oscalVersion);
        metadata.put("uploadedBy", username);

        blobService.uploadComponent(username, filename, jsonContent, metadata);

        // Get file size
        long fileSize = blobService.getFileSize(blobPath);

        // Create component definition entity
        ComponentDefinition component = new ComponentDefinition(oscalUuid, title, blobPath, user);
        component.setDescription(description);
        component.setVersion(version);
        component.setOscalVersion(oscalVersion);
        component.setFilename(filename);
        component.setFileSize(fileSize);
        component.setComponentCount(componentCount);
        component.setCapabilityCount(capabilityCount);
        component.setControlCount(controlCount);
        component.setLastUpdatedBy(user);

        component = componentRepository.save(component);
        logger.info("Created component definition with ID: {} and UUID: {}", component.getId(), oscalUuid);

        return component;
    }

    /**
     * Update an existing component definition
     */
    @Transactional
    public ComponentDefinition updateComponentDefinition(
            Long componentId, String title, String description, String version,
            String jsonContent, Integer componentCount, Integer capabilityCount, Integer controlCount, String username) {

        logger.info("Updating component definition: {} by user: {}", componentId, username);

        ComponentDefinition component = componentRepository.findById(componentId)
                .orElseThrow(() -> new RuntimeException("Component definition not found: " + componentId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Verify user owns this component
        if (!component.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can update this component definition");
        }

        // Update metadata
        if (title != null) {
            component.setTitle(title);
        }
        if (description != null) {
            component.setDescription(description);
        }
        if (version != null) {
            component.setVersion(version);
        }
        if (componentCount != null) {
            component.setComponentCount(componentCount);
        }
        if (capabilityCount != null) {
            component.setCapabilityCount(capabilityCount);
        }
        if (controlCount != null) {
            component.setControlCount(controlCount);
        }

        // Update JSON content if provided
        if (jsonContent != null) {
            blobService.uploadComponent(username, component.getFilename(), jsonContent, null);
            long fileSize = blobService.getFileSize(component.getAzureBlobPath());
            component.setFileSize(fileSize);
        }

        component.setLastUpdatedBy(user);
        component = componentRepository.save(component);

        logger.info("Updated component definition: {}", componentId);
        return component;
    }

    /**
     * Get a component definition by ID
     */
    public ComponentDefinition getComponentDefinition(Long componentId) {
        return componentRepository.findById(componentId)
                .orElseThrow(() -> new RuntimeException("Component definition not found: " + componentId));
    }

    /**
     * Get a component definition by OSCAL UUID
     */
    public ComponentDefinition getComponentDefinitionByUuid(String oscalUuid) {
        return componentRepository.findByOscalUuid(oscalUuid)
                .orElseThrow(() -> new RuntimeException("Component definition not found with UUID: " + oscalUuid));
    }

    /**
     * Get component definition JSON content
     */
    public String getComponentContent(Long componentId) {
        ComponentDefinition component = getComponentDefinition(componentId);
        return blobService.downloadComponent(component.getAzureBlobPath());
    }

    /**
     * Get all component definitions for a specific user
     */
    public List<ComponentDefinition> getUserComponents(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return componentRepository.findByCreatedBy(user);
    }

    /**
     * Get recently created components for a user
     */
    public List<ComponentDefinition> getRecentComponents(String username, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return componentRepository.findByCreatedByOrderByCreatedAtDesc(user)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Search component definitions
     */
    public List<ComponentDefinition> searchComponents(String username, String searchTerm) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return componentRepository.findByCreatedBy(user);
        }

        return componentRepository.findByCreatedByAndSearch(user, searchTerm);
    }

    /**
     * Delete a component definition
     */
    @Transactional
    public void deleteComponentDefinition(Long componentId, String username) {
        logger.info("Deleting component definition: {} by user: {}", componentId, username);

        ComponentDefinition component = componentRepository.findById(componentId)
                .orElseThrow(() -> new RuntimeException("Component definition not found: " + componentId));

        // Verify user owns this component
        if (!component.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can delete this component definition");
        }

        // Delete from Azure Blob Storage
        blobService.deleteComponent(component.getAzureBlobPath());

        // Delete from database
        componentRepository.delete(component);

        logger.info("Deleted component definition: {}", componentId);
    }

    /**
     * Get component definition statistics for a user
     */
    public Map<String, Object> getComponentStatistics(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Map<String, Object> stats = new HashMap<>();

        List<ComponentDefinition> allComponents = componentRepository.findByCreatedBy(user);

        // Total components
        stats.put("totalComponents", allComponents.size());

        // Total controls implemented across all components
        int totalControls = allComponents.stream()
                .mapToInt(c -> c.getControlCount() != null ? c.getControlCount() : 0)
                .sum();
        stats.put("totalControls", totalControls);

        // Total component count across all definitions
        int totalComponentCount = allComponents.stream()
                .mapToInt(c -> c.getComponentCount() != null ? c.getComponentCount() : 0)
                .sum();
        stats.put("totalComponentCount", totalComponentCount);

        // Total storage used
        long totalStorage = allComponents.stream()
                .mapToLong(c -> c.getFileSize() != null ? c.getFileSize() : 0)
                .sum();
        stats.put("totalStorageBytes", totalStorage);

        // OSCAL versions distribution
        Map<String, Long> versionDistribution = allComponents.stream()
                .filter(c -> c.getOscalVersion() != null)
                .collect(Collectors.groupingBy(
                        ComponentDefinition::getOscalVersion,
                        Collectors.counting()
                ));
        stats.put("oscalVersions", versionDistribution);

        // Recent components
        List<Map<String, Object>> recentComponents = getRecentComponents(username, 5).stream()
                .map(component -> {
                    Map<String, Object> componentMap = new HashMap<>();
                    componentMap.put("id", component.getId());
                    componentMap.put("title", component.getTitle());
                    componentMap.put("oscalUuid", component.getOscalUuid());
                    componentMap.put("createdAt", component.getCreatedAt().toString());
                    return componentMap;
                })
                .collect(Collectors.toList());
        stats.put("recentComponents", recentComponents);

        return stats;
    }

    /**
     * Check if component exists
     */
    public boolean componentExists(Long componentId) {
        return componentRepository.existsById(componentId);
    }

    /**
     * Check if component with UUID exists
     */
    public boolean componentExistsByUuid(String oscalUuid) {
        return componentRepository.findByOscalUuid(oscalUuid).isPresent();
    }
}

package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.ReusableElement;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.ReusableElementRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Service for managing reusable OSCAL elements
 * Provides CRUD operations, search, and analytics for element library
 */
@Service
public class ReusableElementService {

    private static final Logger logger = LoggerFactory.getLogger(ReusableElementService.class);

    @Autowired
    private ReusableElementRepository elementRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new reusable element
     */
    @Transactional
    public ReusableElement createElement(ReusableElement.ElementType type, String name,
                                        String jsonContent, String description,
                                        boolean isShared, String username) {
        logger.info("Creating reusable element: {} (type: {}) by user: {}", name, type, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        ReusableElement element = new ReusableElement(type, name, jsonContent, user);
        element.setDescription(description);
        element.setShared(isShared);

        element = elementRepository.save(element);
        logger.info("Created reusable element with ID: {}", element.getId());

        return element;
    }

    /**
     * Update an existing reusable element
     */
    @Transactional
    public ReusableElement updateElement(Long elementId, String name, String jsonContent,
                                        String description, Boolean isShared, String username) {
        logger.info("Updating reusable element: {} by user: {}", elementId, username);

        ReusableElement element = elementRepository.findById(elementId)
                .orElseThrow(() -> new RuntimeException("Element not found: " + elementId));

        // Verify user owns this element
        if (!element.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can update this element");
        }

        // Update fields if provided
        if (name != null) {
            element.setName(name);
        }
        if (jsonContent != null) {
            element.setJsonContent(jsonContent);
        }
        if (description != null) {
            element.setDescription(description);
        }
        if (isShared != null) {
            element.setShared(isShared);
        }

        element = elementRepository.save(element);
        logger.info("Updated reusable element: {}", elementId);

        return element;
    }

    /**
     * Get a reusable element by ID
     */
    public ReusableElement getElement(Long elementId) {
        return elementRepository.findById(elementId)
                .orElseThrow(() -> new RuntimeException("Element not found: " + elementId));
    }

    /**
     * Get all elements for a specific user
     */
    public List<ReusableElement> getUserElements(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return elementRepository.findByCreatedBy(user);
    }

    /**
     * Get elements by type for a specific user
     */
    public List<ReusableElement> getUserElementsByType(String username, ReusableElement.ElementType type) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return elementRepository.findByCreatedByAndType(user, type);
    }

    /**
     * Search elements by name, type, and user
     */
    public List<ReusableElement> searchElements(String username, String searchTerm, ReusableElement.ElementType type) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // If no search term, just filter by type
            if (type == null) {
                return elementRepository.findByCreatedBy(user);
            } else {
                return elementRepository.findByCreatedByAndType(user, type);
            }
        }

        return elementRepository.searchByUserNameAndType(user, searchTerm, type);
    }

    /**
     * Delete a reusable element
     */
    @Transactional
    public void deleteElement(Long elementId, String username) {
        logger.info("Deleting reusable element: {} by user: {}", elementId, username);

        ReusableElement element = elementRepository.findById(elementId)
                .orElseThrow(() -> new RuntimeException("Element not found: " + elementId));

        // Verify user owns this element
        if (!element.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can delete this element");
        }

        elementRepository.delete(element);
        logger.info("Deleted reusable element: {}", elementId);
    }

    /**
     * Increment use count when an element is used in a component
     */
    @Transactional
    public void incrementUseCount(Long elementId) {
        ReusableElement element = elementRepository.findById(elementId)
                .orElseThrow(() -> new RuntimeException("Element not found: " + elementId));

        element.incrementUseCount();
        elementRepository.save(element);
    }

    /**
     * Get recently created elements by user
     */
    public List<ReusableElement> getRecentElements(String username, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return elementRepository.findByCreatedByOrderByCreatedAtDesc(user)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get most used elements by user
     */
    public List<ReusableElement> getMostUsedElements(String username, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return elementRepository.findByCreatedByOrderByUseCountDesc(user)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get element library statistics for a user
     */
    public Map<String, Object> getElementStatistics(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Map<String, Object> stats = new HashMap<>();

        // Total elements
        List<ReusableElement> allElements = elementRepository.findByCreatedBy(user);
        stats.put("totalElements", allElements.size());

        // Count by type
        Map<String, Long> countByType = new HashMap<>();
        for (ReusableElement.ElementType type : ReusableElement.ElementType.values()) {
            long count = elementRepository.countByCreatedByAndType(user, type);
            countByType.put(type.name(), count);
        }
        stats.put("countByType", countByType);

        // Total use count
        int totalUseCount = allElements.stream()
                .mapToInt(ReusableElement::getUseCount)
                .sum();
        stats.put("totalUses", totalUseCount);

        // Most used elements
        List<Map<String, Object>> mostUsed = getMostUsedElements(username, 5).stream()
                .map(element -> {
                    Map<String, Object> elementMap = new HashMap<>();
                    elementMap.put("id", element.getId());
                    elementMap.put("name", element.getName());
                    elementMap.put("type", element.getType().name());
                    elementMap.put("useCount", element.getUseCount());
                    return elementMap;
                })
                .collect(Collectors.toList());
        stats.put("mostUsed", mostUsed);

        return stats;
    }

    /**
     * Get shared elements (for future implementation)
     */
    public List<ReusableElement> getSharedElements(ReusableElement.ElementType type) {
        if (type == null) {
            return elementRepository.findAll().stream()
                    .filter(ReusableElement::isShared)
                    .collect(Collectors.toList());
        }
        return elementRepository.findByTypeAndIsShared(type, true);
    }
}

package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryTag;
import gov.nist.oscal.tools.api.entity.LibraryVersion;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import gov.nist.oscal.tools.api.repository.LibraryTagRepository;
import gov.nist.oscal.tools.api.repository.LibraryVersionRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing library items, versions, and tags
 * Provides CRUD operations, search, and analytics
 */
@Service
public class LibraryService {

    private static final Logger logger = LoggerFactory.getLogger(LibraryService.class);

    @Autowired
    private LibraryItemRepository libraryItemRepository;

    @Autowired
    private LibraryVersionRepository libraryVersionRepository;

    @Autowired
    private LibraryTagRepository libraryTagRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LibraryStorageService storageService;

    /**
     * Create a new library item with initial version
     */
    @Transactional
    public LibraryItem createLibraryItem(String title, String description, String oscalType,
                                         String fileName, String format, String fileContent,
                                         Set<String> tagNames, String username) {
        logger.info("Creating new library item: {} by user: {}", title, username);

        // Get user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Generate IDs
        String itemId = UUID.randomUUID().toString();
        String versionId = UUID.randomUUID().toString();

        // Create library item
        LibraryItem libraryItem = new LibraryItem(itemId, title, description, oscalType, user);

        // Process tags
        Set<LibraryTag> tags = processTags(tagNames);
        libraryItem.setTags(tags);

        // Save library item first to get the ID
        libraryItem = libraryItemRepository.save(libraryItem);

        // Create initial version
        String blobPath = storageService.buildBlobPath(itemId, versionId, fileName);

        // Save file to storage
        Map<String, String> metadata = new HashMap<>();
        metadata.put("itemId", itemId);
        metadata.put("versionId", versionId);
        metadata.put("versionNumber", "1");
        metadata.put("oscalType", oscalType);
        metadata.put("format", format);
        metadata.put("uploadedBy", username);

        storageService.saveLibraryFile(fileContent, blobPath, metadata);

        // Create version record
        LibraryVersion version = new LibraryVersion(
                versionId, libraryItem, 1, fileName, format,
                (long) fileContent.getBytes().length, blobPath, user,
                "Initial version"
        );
        version = libraryVersionRepository.save(version);

        // Set current version
        libraryItem.setCurrentVersion(version);
        libraryItem = libraryItemRepository.save(libraryItem);

        logger.info("Created library item with ID: {} and initial version", itemId);
        return libraryItem;
    }

    /**
     * Update library item metadata
     */
    @Transactional
    public LibraryItem updateLibraryItem(String itemId, String title, String description,
                                         Set<String> tagNames, String username) {
        logger.info("Updating library item: {} by user: {}", itemId, username);

        LibraryItem libraryItem = libraryItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Library item not found: " + itemId));

        // Update metadata
        if (title != null) {
            libraryItem.setTitle(title);
        }
        if (description != null) {
            libraryItem.setDescription(description);
        }
        if (tagNames != null) {
            Set<LibraryTag> tags = processTags(tagNames);
            libraryItem.setTags(tags);
        }

        return libraryItemRepository.save(libraryItem);
    }

    /**
     * Add a new version to an existing library item
     */
    @Transactional
    public LibraryVersion addVersion(String itemId, String fileName, String format,
                                     String fileContent, String changeDescription, String username) {
        logger.info("Adding new version to library item: {} by user: {}", itemId, username);

        LibraryItem libraryItem = libraryItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Library item not found: " + itemId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Generate version ID and get next version number
        String versionId = UUID.randomUUID().toString();
        Integer nextVersionNumber = libraryVersionRepository.getNextVersionNumber(libraryItem);

        // Save file to storage
        String blobPath = storageService.buildBlobPath(itemId, versionId, fileName);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("itemId", itemId);
        metadata.put("versionId", versionId);
        metadata.put("versionNumber", nextVersionNumber.toString());
        metadata.put("oscalType", libraryItem.getOscalType());
        metadata.put("format", format);
        metadata.put("uploadedBy", username);

        storageService.saveLibraryFile(fileContent, blobPath, metadata);

        // Create version record
        LibraryVersion version = new LibraryVersion(
                versionId, libraryItem, nextVersionNumber, fileName, format,
                (long) fileContent.getBytes().length, blobPath, user,
                changeDescription != null ? changeDescription : "Version " + nextVersionNumber
        );
        version = libraryVersionRepository.save(version);

        // Update current version
        libraryItem.setCurrentVersion(version);
        libraryItemRepository.save(libraryItem);

        logger.info("Added version {} to library item: {}", nextVersionNumber, itemId);
        return version;
    }

    /**
     * Get a library item by ID
     */
    @Transactional
    public LibraryItem getLibraryItem(String itemId) {
        LibraryItem item = libraryItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Library item not found: " + itemId));

        // Increment view count
        item.incrementViewCount();
        libraryItemRepository.save(item);

        return item;
    }

    /**
     * Get file content for a specific version
     */
    public String getVersionContent(String versionId) {
        LibraryVersion version = libraryVersionRepository.findByVersionId(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionId));

        return storageService.getLibraryFileContent(version.getFilePath());
    }

    /**
     * Get file content for current version of a library item
     */
    public String getCurrentVersionContent(String itemId) {
        LibraryItem item = libraryItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Library item not found: " + itemId));

        if (item.getCurrentVersion() == null) {
            throw new RuntimeException("No current version found for library item: " + itemId);
        }

        // Increment download count
        item.incrementDownloadCount();
        libraryItemRepository.save(item);

        return storageService.getLibraryFileContent(item.getCurrentVersion().getFilePath());
    }

    /**
     * Get version history for a library item
     */
    public List<LibraryVersion> getVersionHistory(String itemId) {
        LibraryItem item = libraryItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Library item not found: " + itemId));

        return libraryVersionRepository.findByLibraryItemOrderByVersionNumberDesc(item);
    }

    /**
     * Delete a library item and all its versions
     */
    @Transactional
    public void deleteLibraryItem(String itemId, String username) {
        logger.info("Deleting library item: {} by user: {}", itemId, username);

        LibraryItem item = libraryItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Library item not found: " + itemId));

        // Check if user is the creator (optional - you may want to add admin role check)
        if (!item.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can delete this library item");
        }

        // Delete all versions from storage
        List<LibraryVersion> versions = libraryVersionRepository.findByLibraryItem(item);
        for (LibraryVersion version : versions) {
            storageService.deleteLibraryFile(version.getFilePath());
        }

        // Delete from database (versions will be cascade deleted)
        libraryItemRepository.delete(item);

        logger.info("Deleted library item: {}", itemId);
    }

    /**
     * Search library items
     */
    public List<LibraryItem> searchLibrary(String searchTerm, String oscalType, String tagName) {
        if (searchTerm == null && oscalType == null && tagName == null) {
            // Return all items if no filters
            return libraryItemRepository.findAll();
        }

        return libraryItemRepository.advancedSearch(searchTerm, oscalType, tagName);
    }

    /**
     * Get all library items
     */
    public List<LibraryItem> getAllLibraryItems() {
        return libraryItemRepository.findAll();
    }

    /**
     * Get library items by OSCAL type
     */
    public List<LibraryItem> getLibraryItemsByOscalType(String oscalType) {
        return libraryItemRepository.findByOscalType(oscalType);
    }

    /**
     * Get most popular library items (by download count)
     */
    public List<LibraryItem> getMostPopular(int limit) {
        List<LibraryItem> items = libraryItemRepository.findMostDownloaded();
        return items.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Get recently updated library items
     */
    public List<LibraryItem> getRecentlyUpdated(int limit) {
        List<LibraryItem> items = libraryItemRepository.findRecentlyUpdated();
        return items.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Get library analytics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        // Total counts
        analytics.put("totalItems", libraryItemRepository.count());
        analytics.put("totalVersions", libraryVersionRepository.count());
        analytics.put("totalTags", libraryTagRepository.count());

        // Count by OSCAL type
        List<Object[]> typeCounts = libraryItemRepository.countByOscalType();
        Map<String, Long> typeCountMap = new HashMap<>();
        for (Object[] row : typeCounts) {
            typeCountMap.put((String) row[0], (Long) row[1]);
        }
        analytics.put("itemsByType", typeCountMap);

        // Most popular tags
        List<Object[]> popularTagsData = libraryTagRepository.findMostPopularWithCounts();
        List<Map<String, Object>> tagStats = popularTagsData.stream()
                .limit(10)
                .map(row -> {
                    Map<String, Object> tagMap = new HashMap<>();
                    tagMap.put("name", row[1]); // name is at index 1
                    tagMap.put("count", row[3]); // usageCount is at index 3
                    return tagMap;
                })
                .collect(Collectors.toList());
        analytics.put("popularTags", tagStats);

        // Most downloaded items
        List<LibraryItem> mostDownloaded = getMostPopular(5);
        analytics.put("mostDownloaded", mostDownloaded.stream()
                .map(item -> Map.of(
                        "itemId", item.getItemId(),
                        "title", item.getTitle(),
                        "downloadCount", item.getDownloadCount()
                ))
                .collect(Collectors.toList()));

        return analytics;
    }

    /**
     * Process tag names and create/retrieve tags
     */
    private Set<LibraryTag> processTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }

        Set<LibraryTag> tags = new HashSet<>();
        for (String tagName : tagNames) {
            String normalizedName = tagName.toLowerCase().trim();
            LibraryTag tag = libraryTagRepository.findByName(normalizedName)
                    .orElseGet(() -> {
                        LibraryTag newTag = new LibraryTag(normalizedName);
                        return libraryTagRepository.save(newTag);
                    });
            tags.add(tag);
        }
        return tags;
    }

    /**
     * Get all tags with usage counts
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllTags() {
        List<Object[]> tagsData = libraryTagRepository.findAllWithCountsOrderByNameAsc();
        return tagsData.stream()
                .map(row -> {
                    Map<String, Object> tagMap = new HashMap<>();
                    tagMap.put("name", row[1]); // name is at index 1
                    tagMap.put("usageCount", row[3]); // usageCount is at index 3
                    return tagMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get popular tags with usage counts
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPopularTags(int limit) {
        List<Object[]> tagsData = libraryTagRepository.findMostPopularWithCounts();
        return tagsData.stream()
                .limit(limit)
                .map(row -> {
                    Map<String, Object> tagMap = new HashMap<>();
                    tagMap.put("name", row[1]); // name is at index 1
                    tagMap.put("usageCount", row[3]); // usageCount is at index 3
                    return tagMap;
                })
                .collect(Collectors.toList());
    }
}

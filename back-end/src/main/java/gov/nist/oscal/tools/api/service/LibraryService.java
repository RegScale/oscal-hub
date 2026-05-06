package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryTag;
import gov.nist.oscal.tools.api.entity.LibraryVersion;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.Visibility;
import gov.nist.oscal.tools.api.model.AuditEventType;
import gov.nist.oscal.tools.api.model.library.PublicCatalogAnalytics;
import gov.nist.oscal.tools.api.model.library.PublicCatalogTopContributors;
import gov.nist.oscal.tools.api.model.library.VisibilityChangeRequest;
import gov.nist.oscal.tools.api.repository.AuditEventRepository;
import gov.nist.oscal.tools.api.repository.LibraryItemRatingRepository;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import gov.nist.oscal.tools.api.repository.LibraryTagRepository;
import gov.nist.oscal.tools.api.repository.LibraryVersionRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Autowired(required = false)
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired(required = false)
    private AuditLogService auditLogService;

    @Autowired
    private LibraryStorageService storageService;

    @Autowired
    private LibraryItemRatingRepository libraryItemRatingRepository;

    @Autowired(required = false)
    private AuditEventRepository auditEventRepository;

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
     * Get a library item by ID. Enforces visibility — items not readable by the
     * caller surface as "not found" (404 at the controller) to avoid leaking
     * existence.
     */
    @Transactional
    public LibraryItem getLibraryItem(String itemId, User caller) {
        LibraryItem item = libraryItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Library item not found: " + itemId));

        if (!canRead(item, caller)) {
            throw new RuntimeException("Library item not found: " + itemId);
        }

        // Increment view count
        item.incrementViewCount();
        libraryItemRepository.save(item);

        return item;
    }

    /**
     * Backwards-compatible overload — only used by callers that don't yet have a
     * caller User to thread through. Treats as anonymous (PUBLIC-only).
     * @deprecated prefer {@link #getLibraryItem(String, User)}
     */
    @Deprecated
    @Transactional
    public LibraryItem getLibraryItem(String itemId) {
        return getLibraryItem(itemId, null);
    }

    /**
     * Get file content for a specific version. Enforces visibility on the parent
     * library item — version not readable surfaces as "not found".
     */
    public String getVersionContent(String versionId, User caller) {
        LibraryVersion version = libraryVersionRepository.findByVersionId(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionId));

        if (version.getLibraryItem() != null && !canRead(version.getLibraryItem(), caller)) {
            throw new RuntimeException("Version not found: " + versionId);
        }

        return storageService.getLibraryFileContent(version.getFilePath());
    }

    /**
     * @deprecated prefer {@link #getVersionContent(String, User)}
     */
    @Deprecated
    public String getVersionContent(String versionId) {
        return getVersionContent(versionId, null);
    }

    /**
     * Get file content for current version of a library item. Enforces visibility.
     */
    public String getCurrentVersionContent(String itemId, User caller) {
        LibraryItem item = libraryItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Library item not found: " + itemId));

        if (!canRead(item, caller)) {
            throw new RuntimeException("Library item not found: " + itemId);
        }

        if (item.getCurrentVersion() == null) {
            throw new RuntimeException("No current version found for library item: " + itemId);
        }

        // Increment download count
        item.incrementDownloadCount();
        libraryItemRepository.save(item);

        logDownloadAudit(item, item.getCurrentVersion(), caller, "AUTHENTICATED");

        return storageService.getLibraryFileContent(item.getCurrentVersion().getFilePath());
    }

    /**
     * Record a LIBRARY_ITEM_DOWNLOAD audit event. Best-effort — never let an
     * audit failure block the actual download.
     */
    private void logDownloadAudit(LibraryItem item, LibraryVersion version, User caller, String channel) {
        if (auditLogService == null) {
            return;
        }
        try {
            String username = caller != null ? caller.getUsername() : "anonymous";
            Long userId = caller != null ? caller.getId() : null;

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("itemId", item.getItemId());
            metadata.put("title", item.getTitle());
            metadata.put("oscalType", item.getOscalType());
            metadata.put("channel", channel); // PUBLIC_LATEST, PUBLIC_VERSION, AUTHENTICATED
            if (version != null) {
                metadata.put("versionId", version.getVersionId());
                metadata.put("versionNumber", version.getVersionNumber());
                metadata.put("format", version.getFormat());
            }
            if (item.getOrganization() != null) {
                metadata.put("organizationId", item.getOrganization().getId());
                metadata.put("organizationName", item.getOrganization().getName());
            }

            auditLogService.logEvent(
                    AuditEventType.LIBRARY_ITEM_DOWNLOAD,
                    username,
                    userId,
                    "SUCCESS",
                    item.getItemId(),
                    "DOWNLOAD",
                    metadata
            );
        } catch (RuntimeException e) {
            logger.warn("Failed to record download audit for item {}: {}", item.getItemId(), e.getMessage());
        }
    }

    /**
     * @deprecated prefer {@link #getCurrentVersionContent(String, User)}
     */
    @Deprecated
    public String getCurrentVersionContent(String itemId) {
        return getCurrentVersionContent(itemId, null);
    }

    /**
     * Get version history for a library item. Enforces visibility.
     */
    public List<LibraryVersion> getVersionHistory(String itemId, User caller) {
        LibraryItem item = libraryItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Library item not found: " + itemId));

        if (!canRead(item, caller)) {
            throw new RuntimeException("Library item not found: " + itemId);
        }

        return libraryVersionRepository.findByLibraryItemOrderByVersionNumberDesc(item);
    }

    /**
     * @deprecated prefer {@link #getVersionHistory(String, User)}
     */
    @Deprecated
    public List<LibraryVersion> getVersionHistory(String itemId) {
        return getVersionHistory(itemId, null);
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
     * Cached for 5 minutes
     */
    @Cacheable(value = "libraryAnalytics", key = "'all'")
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
     * Cached for 10 minutes
     */
    @Cacheable(value = "libraryTags", key = "'all'")
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
     * Cached for 10 minutes
     */
    @Cacheable(value = "popularTags", key = "'library_' + #limit")
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

    // ==================== PAGINATED METHODS ====================

    /**
     * Get all library items (paginated)
     */
    @Transactional(readOnly = true)
    public Page<LibraryItem> getAllLibraryItemsPaged(Pageable pageable) {
        return libraryItemRepository.findAllPaged(pageable);
    }

    /**
     * Get library items by OSCAL type (paginated)
     */
    @Transactional(readOnly = true)
    public Page<LibraryItem> getLibraryItemsByOscalTypePaged(String oscalType, Pageable pageable) {
        return libraryItemRepository.findByOscalTypePaged(oscalType, pageable);
    }

    /**
     * Search library items (paginated)
     */
    @Transactional(readOnly = true)
    public Page<LibraryItem> searchLibraryPaged(String searchTerm, String oscalType, String tagName, Pageable pageable) {
        return libraryItemRepository.advancedSearchPaged(searchTerm, oscalType, tagName, pageable);
    }

    // ==================== VISIBILITY-AWARE LIST METHODS ====================

    /**
     * Resolve a User by username. Returns null if username is null/blank or not found.
     * Used by controllers to translate Principal.getName() into a User entity for
     * visibility filtering.
     */
    @Transactional(readOnly = true)
    public User resolveCaller(String username) {
        if (username == null || username.isBlank()) return null;
        return userRepository.findByUsername(username).orElse(null);
    }

    /** Get all library items visible to the caller (paginated). */
    @Transactional(readOnly = true)
    public Page<LibraryItem> getAllLibraryItemsVisibleToPaged(User caller, Pageable pageable) {
        Long userId = caller != null ? caller.getId() : null;
        Long orgId = resolveOrgId(caller);
        return libraryItemRepository.findAllVisibleTo(userId, orgId, pageable);
    }

    /** Get library items by OSCAL type visible to the caller (paginated). */
    @Transactional(readOnly = true)
    public Page<LibraryItem> getLibraryItemsByOscalTypeVisibleToPaged(String oscalType, User caller, Pageable pageable) {
        Long userId = caller != null ? caller.getId() : null;
        Long orgId = resolveOrgId(caller);
        return libraryItemRepository.findByOscalTypeVisibleTo(oscalType, userId, orgId, pageable);
    }

    /** Search library visible to caller (paginated). */
    @Transactional(readOnly = true)
    public Page<LibraryItem> searchLibraryVisibleToPaged(String searchTerm, String oscalType, String tagName,
                                                         User caller, Pageable pageable) {
        Long userId = caller != null ? caller.getId() : null;
        Long orgId = resolveOrgId(caller);
        return libraryItemRepository.advancedSearchPagedVisibleTo(searchTerm, oscalType, tagName, userId, orgId, pageable);
    }

    /** Most popular items visible to caller, capped at limit. */
    @Transactional(readOnly = true)
    public List<LibraryItem> getMostPopularVisibleTo(User caller, int limit) {
        Long userId = caller != null ? caller.getId() : null;
        Long orgId = resolveOrgId(caller);
        return libraryItemRepository.findMostDownloadedVisibleTo(userId, orgId)
                .stream().limit(limit).collect(Collectors.toList());
    }

    /** Recently updated items visible to caller, capped at limit. */
    @Transactional(readOnly = true)
    public List<LibraryItem> getRecentlyUpdatedVisibleTo(User caller, int limit) {
        Long userId = caller != null ? caller.getId() : null;
        Long orgId = resolveOrgId(caller);
        return libraryItemRepository.findRecentlyUpdatedVisibleTo(userId, orgId)
                .stream().limit(limit).collect(Collectors.toList());
    }

    // ==================== VISIBILITY MUTATION ====================

    /**
     * Change a library item's visibility. Permitted to either the creator or a
     * platform SUPER_ADMIN (force-unpublish path). All other callers see a 404,
     * not 403 — we hide existence to avoid leaking the item id.
     * <p>
     * Side effects:
     *   - Sets/clears the {@code organization} reference based on the target visibility
     *   - Stamps {@code publishedAt}/{@code lastPublishedAt} on transitions to PUBLIC
     *   - Writes an audit event categorising the change (publish, unpublish,
     *     force-unpublish by admin, or generic visibility change)
     */
    @Transactional
    public LibraryItem changeVisibility(String itemId,
                                         VisibilityChangeRequest req,
                                         User caller) {
        LibraryItem item = libraryItemRepository.findByItemId(itemId)
            .orElseThrow(() -> new RuntimeException("library item not found"));

        boolean isCreator = caller != null
            && item.getCreatedBy() != null
            && item.getCreatedBy().getId() != null
            && item.getCreatedBy().getId().equals(caller.getId());
        boolean isSuperAdmin = caller != null
            && caller.getGlobalRole() == User.GlobalRole.SUPER_ADMIN;

        if (!isCreator && !isSuperAdmin) {
            // Hide existence — return 404 not 403.
            throw new RuntimeException("library item not found");
        }

        Visibility prev = item.getVisibility();
        Visibility next = req.getVisibility();

        if (next == Visibility.ORGANIZATION) {
            if (req.getOrganizationId() == null) {
                throw new IllegalArgumentException("organizationId required when visibility=ORGANIZATION");
            }
            if (!isSuperAdmin) {
                Long callerOrg = resolveOrgId(caller);
                if (!req.getOrganizationId().equals(callerOrg)) {
                    throw new SecurityException("cannot share to organization you don't belong to");
                }
            }
            Organization o = organizationRepository.findById(req.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("unknown organizationId"));
            item.setOrganization(o);
        } else {
            item.setOrganization(null);
        }

        item.setVisibility(next);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (next == Visibility.PUBLIC) {
            if (item.getPublishedAt() == null) item.setPublishedAt(now);
            item.setLastPublishedAt(now);
        }

        item = libraryItemRepository.save(item);

        // Audit — non-fatal if AuditLogService isn't wired (e.g. minimal tests).
        if (auditLogService != null) {
            try {
                AuditEventType type = chooseAuditType(prev, next, isCreator, isSuperAdmin);
                java.util.Map<String, Object> metadata = new java.util.HashMap<>();
                metadata.put("itemId", item.getItemId());
                metadata.put("previousVisibility", prev != null ? prev.name() : null);
                metadata.put("newVisibility", next != null ? next.name() : null);
                if (req.getReason() != null && !req.getReason().isBlank()) {
                    metadata.put("reason", req.getReason());
                }
                if (req.getOrganizationId() != null) {
                    metadata.put("organizationId", req.getOrganizationId());
                }
                auditLogService.logEvent(
                    type,
                    caller != null ? caller.getUsername() : null,
                    caller != null ? caller.getId() : null,
                    "SUCCESS",
                    item.getItemId(),
                    "VISIBILITY_CHANGE",
                    metadata
                );
            } catch (RuntimeException ex) {
                logger.warn("Failed to write audit event for visibility change on {}: {}",
                            item.getItemId(), ex.getMessage());
            }
        }

        return item;
    }

    private AuditEventType chooseAuditType(Visibility prev, Visibility next,
                                           boolean isCreator, boolean isSuperAdmin) {
        if (next == Visibility.PUBLIC && prev != Visibility.PUBLIC) {
            return AuditEventType.LIBRARY_ITEM_PUBLISHED;
        }
        if (prev == Visibility.PUBLIC && next != Visibility.PUBLIC) {
            return (isSuperAdmin && !isCreator)
                ? AuditEventType.LIBRARY_ITEM_FORCE_UNPUBLISHED
                : AuditEventType.LIBRARY_ITEM_UNPUBLISHED;
        }
        return AuditEventType.LIBRARY_ITEM_VISIBILITY_CHANGED;
    }

    // ==================== VISIBILITY ====================

    /**
     * Visibility predicate used by every read path. PUBLIC items are readable by
     * anyone (including null caller). Otherwise, only creator or same-org members
     * (the latter only when visibility == ORGANIZATION).
     */
    public boolean canRead(LibraryItem item, User caller) {
        if (item.getVisibility() == Visibility.PUBLIC) return true;
        if (caller == null) return false;
        if (item.getCreatedBy() != null
                && item.getCreatedBy().getId() != null
                && item.getCreatedBy().getId().equals(caller.getId())) return true;
        if (item.getVisibility() == Visibility.ORGANIZATION) {
            Long itemOrg = item.getOrganization() != null ? item.getOrganization().getId() : null;
            Long callerOrg = resolveOrgId(caller);
            return itemOrg != null && itemOrg.equals(callerOrg);
        }
        return false;
    }

    /**
     * Resolve the caller's organization id. Users have a
     * {@code Set<OrganizationMembership>}; we pick the first ACTIVE membership.
     * Returns null if the user has no active membership.
     * <p>
     * The caller may be a detached entity (loaded in one transaction, passed into
     * another), in which case the lazy collection will not have a session to
     * fault in — fall back to a fresh query against the membership repository
     * (when wired in). For pure unit tests where the collection is preinitialised
     * and the membership repo isn't available, the in-memory traversal still
     * works.
     */
    Long resolveOrgId(User user) {
        if (user == null || user.getId() == null) return null;
        // Try membership repo first when wired (production / @SpringBootTest paths).
        if (membershipRepository != null) {
            try {
                List<OrganizationMembership> memberships =
                        membershipRepository.findByUserAndStatus(user,
                                OrganizationMembership.MembershipStatus.ACTIVE);
                for (OrganizationMembership m : memberships) {
                    if (m.getOrganization() != null) {
                        return m.getOrganization().getId();
                    }
                }
                return null;
            } catch (RuntimeException ignored) {
                // fall through to the in-memory traversal below
            }
        }
        if (user.getOrganizationMemberships() == null) return null;
        for (OrganizationMembership m : user.getOrganizationMemberships()) {
            if (m.getStatus() == OrganizationMembership.MembershipStatus.ACTIVE
                    && m.getOrganization() != null) {
                return m.getOrganization().getId();
            }
        }
        return null;
    }

    // ==================== PUBLIC CATALOG ====================
    // Anonymous, PUBLIC-only browse and download.

    /** Result type for content downloads (latest or by version). */
    public record VersionDownload(String content, String filename, String format) {}

    @Transactional(readOnly = true)
    public Page<gov.nist.oscal.tools.api.model.library.PublicItemSummary>
            searchPublic(String q, String type, String tag, Pageable pageable) {
        return libraryItemRepository.searchPublic(q, type, tag, pageable)
            .map(item -> {
                Double avg = libraryItemRatingRepository.averageRatingForItem(item.getId());
                Long total = libraryItemRatingRepository.countRatingsForItem(item.getId());
                return gov.nist.oscal.tools.api.model.library.PublicItemSummary
                        .fromEntity(item, avg, total);
            });
    }

    @Transactional
    public Optional<gov.nist.oscal.tools.api.model.library.PublicItemSummary> getPublic(String itemId) {
        return libraryItemRepository.findPublicByItemId(itemId)
            .map(item -> {
                Double avg = libraryItemRatingRepository.averageRatingForItem(item.getId());
                Long total = libraryItemRatingRepository.countRatingsForItem(item.getId());
                // Increment view count on detail load.
                item.incrementViewCount();
                libraryItemRepository.save(item);
                return gov.nist.oscal.tools.api.model.library.PublicItemSummary
                        .fromEntity(item, avg, total);
            });
    }

    @Transactional
    public Optional<VersionDownload> getPublicLatestContent(String itemId) {
        return getPublicLatestContent(itemId, null);
    }

    @Transactional
    public Optional<VersionDownload> getPublicLatestContent(String itemId, User caller) {
        return libraryItemRepository.findPublicByItemId(itemId)
            .filter(item -> item.getCurrentVersion() != null)
            .map(item -> {
                LibraryVersion v = item.getCurrentVersion();
                String content = storageService.getLibraryFileContent(v.getFilePath());
                item.incrementDownloadCount();
                libraryItemRepository.save(item);
                logDownloadAudit(item, v, caller, "PUBLIC_LATEST");
                return new VersionDownload(content == null ? "" : content,
                                            v.getFileName(), v.getFormat());
            });
    }

    @Transactional
    public Optional<VersionDownload> getPublicVersionContent(String itemId, String versionId) {
        return getPublicVersionContent(itemId, versionId, null);
    }

    @Transactional
    public Optional<VersionDownload> getPublicVersionContent(String itemId, String versionId, User caller) {
        return libraryItemRepository.findPublicByItemId(itemId)
            .flatMap(item -> item.getVersions().stream()
                .filter(v -> versionId.equals(v.getVersionId()))
                .findFirst()
                .map(v -> {
                    String content = storageService.getLibraryFileContent(v.getFilePath());
                    item.incrementDownloadCount();
                    libraryItemRepository.save(item);
                    logDownloadAudit(item, v, caller, "PUBLIC_VERSION");
                    return new VersionDownload(content == null ? "" : content,
                                                v.getFileName(), v.getFormat());
                }));
    }

    // ==================== PUBLIC CATALOG ANALYTICS ====================
    // Aggregations powering the /catalog tabs (Highest Rated, Most Downloaded,
    // Top Contributors, Analytics). All scope to PUBLIC items only — nothing
    // private leaks.

    @Transactional(readOnly = true)
    public List<gov.nist.oscal.tools.api.model.library.PublicItemSummary> getMostDownloadedPublic(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return libraryItemRepository.findMostDownloadedPublic(
                org.springframework.data.domain.PageRequest.of(0, safeLimit))
            .stream()
            .map(item -> {
                Double avg = libraryItemRatingRepository.averageRatingForItem(item.getId());
                Long total = libraryItemRatingRepository.countRatingsForItem(item.getId());
                return gov.nist.oscal.tools.api.model.library.PublicItemSummary
                        .fromEntity(item, avg, total);
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<gov.nist.oscal.tools.api.model.library.PublicItemSummary> getTopRatedPublic(int limit, long minRatings) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        long safeMin = Math.max(minRatings, 1);
        return libraryItemRepository.findTopRatedPublic(safeMin,
                org.springframework.data.domain.PageRequest.of(0, safeLimit))
            .stream()
            .map(row -> {
                LibraryItem item = (LibraryItem) row[0];
                Double avg = row[1] == null ? null : ((Number) row[1]).doubleValue();
                Long total = row[2] == null ? null : ((Number) row[2]).longValue();
                return gov.nist.oscal.tools.api.model.library.PublicItemSummary
                        .fromEntity(item, avg, total);
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PublicCatalogTopContributors getTopContributorsPublic(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        var pageable = org.springframework.data.domain.PageRequest.of(0, safeLimit);

        List<PublicCatalogTopContributors.UserContributor> users =
                libraryItemRepository.findTopUserContributorsPublic(pageable).stream()
                        .map(row -> {
                            Long userId = row[0] == null ? null : ((Number) row[0]).longValue();
                            String username = (String) row[1];
                            String firstName = (String) row[2];
                            String lastName = (String) row[3];
                            long uploads = row[4] == null ? 0L : ((Number) row[4]).longValue();
                            long downloads = row[5] == null ? 0L : ((Number) row[5]).longValue();
                            String displayName = buildDisplayName(firstName, lastName, username);
                            return new PublicCatalogTopContributors.UserContributor(
                                    userId, username, displayName, uploads, downloads);
                        })
                        .collect(Collectors.toList());

        List<PublicCatalogTopContributors.OrgContributor> orgs =
                libraryItemRepository.findTopOrgContributorsPublic(pageable).stream()
                        .map(row -> {
                            Long orgId = row[0] == null ? null : ((Number) row[0]).longValue();
                            String name = (String) row[1];
                            String logoUrl = (String) row[2];
                            long uploads = row[3] == null ? 0L : ((Number) row[3]).longValue();
                            long downloads = row[4] == null ? 0L : ((Number) row[4]).longValue();
                            return new PublicCatalogTopContributors.OrgContributor(
                                    orgId, name, logoUrl, uploads, downloads);
                        })
                        .collect(Collectors.toList());

        return new PublicCatalogTopContributors(users, orgs);
    }

    private static String buildDisplayName(String firstName, String lastName, String username) {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) sb.append(firstName.trim());
        if (lastName != null && !lastName.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(lastName.trim());
        }
        return sb.length() > 0 ? sb.toString() : username;
    }

    @Transactional(readOnly = true)
    public PublicCatalogAnalytics getPublicAnalytics(int weeksBack) {
        int weeks = Math.min(Math.max(weeksBack, 1), 104); // cap at 2 years
        java.time.LocalDateTime since = java.time.LocalDateTime.now().minusWeeks(weeks);

        long totalItems = libraryItemRepository.countPublic();
        long totalDownloads = libraryItemRepository.sumDownloadsPublic();

        // Contributor / org counts come from the per-leaderboard queries with
        // a generous limit — public-catalog cardinality is small.
        var unbounded = org.springframework.data.domain.PageRequest.of(0, 1000);
        long contributorCount = libraryItemRepository.findTopUserContributorsPublic(unbounded).size();
        long organizationCount = libraryItemRepository.findTopOrgContributorsPublic(unbounded).size();

        List<PublicCatalogAnalytics.TypeStat> byType =
                libraryItemRepository.getTypeStatsPublic().stream()
                        .map(row -> new PublicCatalogAnalytics.TypeStat(
                                (String) row[0],
                                row[1] == null ? 0L : ((Number) row[1]).longValue(),
                                row[2] == null ? 0.0 : ((Number) row[2]).doubleValue(),
                                row[3] == null ? 0.0 : ((Number) row[3]).doubleValue()))
                        .collect(Collectors.toList());

        List<PublicCatalogAnalytics.TimeBucket> uploadsOverTime =
                libraryItemRepository.getUploadsPerWeekPublic(since).stream()
                        .map(LibraryService::toTimeBucket)
                        .collect(Collectors.toList());

        // Downloads-over-time is sourced from the audit log so we get one
        // row per actual download event (we only started recording these
        // events after the audit-log change shipped, so historical buckets
        // before that date will read as zero).
        List<PublicCatalogAnalytics.TimeBucket> downloadsOverTime =
                auditEventRepository == null
                        ? List.of()
                        : auditEventRepository.getLibraryDownloadsPerWeek(since).stream()
                                .map(LibraryService::toTimeBucket)
                                .collect(Collectors.toList());

        return new PublicCatalogAnalytics(
                new PublicCatalogAnalytics.Totals(
                        totalItems, totalDownloads, contributorCount, organizationCount),
                byType,
                uploadsOverTime,
                downloadsOverTime);
    }

    /**
     * Shared converter for native time-bucket queries. Postgres returns
     * date_trunc as java.sql.Timestamp; convert to LocalDate (the start of
     * the week) which round-trips cleanly to JSON.
     */
    private static PublicCatalogAnalytics.TimeBucket toTimeBucket(Object[] row) {
        Object ts = row[0];
        java.time.LocalDate weekStart;
        if (ts instanceof java.sql.Timestamp t) {
            weekStart = t.toLocalDateTime().toLocalDate();
        } else if (ts instanceof java.time.LocalDateTime ldt) {
            weekStart = ldt.toLocalDate();
        } else if (ts instanceof java.time.LocalDate ld) {
            weekStart = ld;
        } else {
            weekStart = java.time.LocalDate.now();
        }
        long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
        return new PublicCatalogAnalytics.TimeBucket(weekStart, count);
    }
}

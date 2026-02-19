package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.entity.Artifact.ArtifactVisibility;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing artifacts, versions, and tags.
 * Provides CRUD operations, search with visibility filtering, and analytics.
 */
@Service
public class ArtifactService {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactService.class);

    // Pattern for extracting {{ variable }} syntax
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*\\}\\}");

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    private ArtifactVersionRepository artifactVersionRepository;

    @Autowired
    private ArtifactTagRepository artifactTagRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private ArtifactStorageService storageService;

    /**
     * Get list of organizations the user belongs to (for visibility filtering)
     */
    public List<Organization> getUserOrganizations(User user) {
        List<OrganizationMembership> memberships = membershipRepository.findByUserAndStatus(user, MembershipStatus.ACTIVE);
        return memberships.stream()
                .map(OrganizationMembership::getOrganization)
                .collect(Collectors.toList());
    }

    /**
     * Extract variables from Markdown content using {{ variable }} syntax
     */
    public List<String> extractVariables(String content) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> variables = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        while (matcher.find()) {
            variables.add(matcher.group(1).trim());
        }
        return new ArrayList<>(variables);
    }

    /**
     * Convert variables list to JSON string for storage
     */
    private String variablesToJson(List<String> variables) {
        if (variables == null || variables.isEmpty()) {
            return "[]";
        }
        return "[" + variables.stream()
                .map(v -> "\"" + v.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    /**
     * Create a new artifact with initial version
     */
    @Transactional
    public Artifact createArtifact(String title, String description, ArtifactVisibility visibility,
                                   Long organizationId, String content, Set<String> tagNames, String username) {
        logger.info("Creating new artifact: {} by user: {}", title, username);

        // Get user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Get organization if visibility is ORGANIZATION
        Organization organization = null;
        if (visibility == ArtifactVisibility.ORGANIZATION) {
            if (organizationId == null) {
                throw new RuntimeException("Organization ID is required for ORGANIZATION visibility");
            }
            organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));

            // Verify user belongs to the organization
            List<Organization> userOrgs = getUserOrganizations(user);
            if (!userOrgs.contains(organization)) {
                throw new RuntimeException("User does not belong to the specified organization");
            }
        }

        // Generate IDs
        String artifactId = UUID.randomUUID().toString();
        String versionId = UUID.randomUUID().toString();

        // Extract variables from content
        List<String> variables = extractVariables(content);
        String variablesJson = variablesToJson(variables);

        // Create artifact
        Artifact artifact = new Artifact(artifactId, title, description, visibility, organization, user);
        artifact.setExtractedVariables(variablesJson);

        // Process tags
        Set<ArtifactTag> tags = processTags(tagNames);
        artifact.setTags(tags);

        // Save artifact first to get the ID
        artifact = artifactRepository.save(artifact);

        // Create initial version
        String blobPath = storageService.buildBlobPath(artifactId, versionId, "content.md");

        // Save file to storage
        Map<String, String> metadata = new HashMap<>();
        metadata.put("artifactId", artifactId);
        metadata.put("versionId", versionId);
        metadata.put("versionNumber", "1");
        metadata.put("visibility", visibility.name());
        metadata.put("uploadedBy", username);

        storageService.saveArtifactFile(content, blobPath, metadata);

        // Create version record
        ArtifactVersion version = new ArtifactVersion(
                versionId, artifact, 1, (long) content.getBytes().length, blobPath, user,
                "Initial version"
        );
        version.setExtractedVariablesSnapshot(variablesJson);
        version = artifactVersionRepository.save(version);

        // Set current version
        artifact.setCurrentVersion(version);
        artifact = artifactRepository.save(artifact);

        logger.info("Created artifact with ID: {} and initial version", artifactId);
        return artifact;
    }

    /**
     * Update artifact metadata
     */
    @Transactional
    public Artifact updateArtifact(String artifactId, String title, String description,
                                   ArtifactVisibility visibility, Long organizationId,
                                   Set<String> tagNames, String username) {
        logger.info("Updating artifact: {} by user: {}", artifactId, username);

        Artifact artifact = artifactRepository.findByArtifactId(artifactId)
                .orElseThrow(() -> new RuntimeException("Artifact not found: " + artifactId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Verify ownership or check if user has access
        if (!artifact.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can update this artifact");
        }

        // Update metadata
        if (title != null) {
            artifact.setTitle(title);
        }
        if (description != null) {
            artifact.setDescription(description);
        }
        if (visibility != null) {
            artifact.setVisibility(visibility);
            if (visibility == ArtifactVisibility.ORGANIZATION && organizationId != null) {
                Organization organization = organizationRepository.findById(organizationId)
                        .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));
                artifact.setOrganization(organization);
            } else if (visibility != ArtifactVisibility.ORGANIZATION) {
                artifact.setOrganization(null);
            }
        }
        if (tagNames != null) {
            Set<ArtifactTag> tags = processTags(tagNames);
            artifact.setTags(tags);
        }

        return artifactRepository.save(artifact);
    }

    /**
     * Add a new version to an existing artifact
     */
    @Transactional
    public ArtifactVersion addVersion(String artifactId, String content, String changeDescription, String username) {
        logger.info("Adding new version to artifact: {} by user: {}", artifactId, username);

        Artifact artifact = artifactRepository.findByArtifactId(artifactId)
                .orElseThrow(() -> new RuntimeException("Artifact not found: " + artifactId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Extract variables from new content
        List<String> variables = extractVariables(content);
        String variablesJson = variablesToJson(variables);

        // Update artifact's extracted variables
        artifact.setExtractedVariables(variablesJson);

        // Generate version ID and get next version number
        String versionId = UUID.randomUUID().toString();
        Integer nextVersionNumber = artifactVersionRepository.getNextVersionNumber(artifact);

        // Save file to storage
        String blobPath = storageService.buildBlobPath(artifactId, versionId, "content.md");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("artifactId", artifactId);
        metadata.put("versionId", versionId);
        metadata.put("versionNumber", nextVersionNumber.toString());
        metadata.put("visibility", artifact.getVisibility().name());
        metadata.put("uploadedBy", username);

        storageService.saveArtifactFile(content, blobPath, metadata);

        // Create version record
        ArtifactVersion version = new ArtifactVersion(
                versionId, artifact, nextVersionNumber, (long) content.getBytes().length, blobPath, user,
                changeDescription != null ? changeDescription : "Version " + nextVersionNumber
        );
        version.setExtractedVariablesSnapshot(variablesJson);
        version = artifactVersionRepository.save(version);

        // Update current version
        artifact.setCurrentVersion(version);
        artifactRepository.save(artifact);

        logger.info("Added version {} to artifact: {}", nextVersionNumber, artifactId);
        return version;
    }

    /**
     * Get an artifact by ID with visibility check
     */
    @Transactional
    public Artifact getArtifact(String artifactId, String username) {
        Artifact artifact = artifactRepository.findByArtifactId(artifactId)
                .orElseThrow(() -> new RuntimeException("Artifact not found: " + artifactId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Check access
        if (!hasAccess(artifact, user)) {
            throw new RuntimeException("Access denied to artifact: " + artifactId);
        }

        // Increment view count
        artifact.incrementViewCount();
        artifactRepository.save(artifact);

        return artifact;
    }

    /**
     * Check if user has access to artifact based on visibility
     */
    public boolean hasAccess(Artifact artifact, User user) {
        // Owner always has access
        if (artifact.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        // Public artifacts are accessible to all
        if (artifact.getVisibility() == ArtifactVisibility.PUBLIC) {
            return true;
        }

        // Organization artifacts are accessible to org members
        if (artifact.getVisibility() == ArtifactVisibility.ORGANIZATION && artifact.getOrganization() != null) {
            List<Organization> userOrgs = getUserOrganizations(user);
            return userOrgs.contains(artifact.getOrganization());
        }

        // Private artifacts only accessible to owner
        return false;
    }

    /**
     * Get file content for a specific version
     */
    public String getVersionContent(String versionId) {
        ArtifactVersion version = artifactVersionRepository.findByVersionId(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionId));

        return storageService.getArtifactFileContent(version.getFilePath());
    }

    /**
     * Get file content for current version of an artifact
     */
    @Transactional
    public String getCurrentVersionContent(String artifactId, String username) {
        Artifact artifact = artifactRepository.findByArtifactId(artifactId)
                .orElseThrow(() -> new RuntimeException("Artifact not found: " + artifactId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (!hasAccess(artifact, user)) {
            throw new RuntimeException("Access denied to artifact: " + artifactId);
        }

        if (artifact.getCurrentVersion() == null) {
            throw new RuntimeException("No current version found for artifact: " + artifactId);
        }

        // Increment download count
        artifact.incrementDownloadCount();
        artifactRepository.save(artifact);

        return storageService.getArtifactFileContent(artifact.getCurrentVersion().getFilePath());
    }

    /**
     * Get version history for an artifact
     */
    public List<ArtifactVersion> getVersionHistory(String artifactId) {
        Artifact artifact = artifactRepository.findByArtifactId(artifactId)
                .orElseThrow(() -> new RuntimeException("Artifact not found: " + artifactId));

        return artifactVersionRepository.findByArtifactOrderByVersionNumberDesc(artifact);
    }

    /**
     * Delete an artifact and all its versions
     */
    @Transactional
    public void deleteArtifact(String artifactId, String username) {
        logger.info("Deleting artifact: {} by user: {}", artifactId, username);

        Artifact artifact = artifactRepository.findByArtifactId(artifactId)
                .orElseThrow(() -> new RuntimeException("Artifact not found: " + artifactId));

        // Check if user is the creator
        if (!artifact.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can delete this artifact");
        }

        // Delete all versions from storage
        List<ArtifactVersion> versions = artifactVersionRepository.findByArtifact(artifact);
        for (ArtifactVersion version : versions) {
            storageService.deleteArtifactFile(version.getFilePath());
        }

        // Delete from database (versions will be cascade deleted)
        artifactRepository.delete(artifact);

        logger.info("Deleted artifact: {}", artifactId);
    }

    /**
     * Get all visible artifacts for user
     */
    public List<Artifact> getAllVisibleArtifacts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Organization> userOrgs = getUserOrganizations(user);
        return artifactRepository.findVisibleToUser(user, userOrgs);
    }

    /**
     * Get user's own artifacts
     */
    public List<Artifact> getMyArtifacts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return artifactRepository.findMyArtifacts(user);
    }

    /**
     * Get public artifacts only
     */
    public List<Artifact> getPublicArtifacts() {
        return artifactRepository.findPublicArtifacts();
    }

    /**
     * Get organization artifacts
     */
    public List<Artifact> getOrganizationArtifacts(Long organizationId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));

        // Verify user belongs to organization
        List<Organization> userOrgs = getUserOrganizations(user);
        if (!userOrgs.contains(organization)) {
            throw new RuntimeException("User does not belong to this organization");
        }

        return artifactRepository.findOrganizationArtifacts(organization);
    }

    /**
     * Search artifacts with visibility filter
     */
    public List<Artifact> searchArtifacts(String searchTerm, ArtifactVisibility visibility, String tagName, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Organization> userOrgs = getUserOrganizations(user);

        if (searchTerm == null && visibility == null && tagName == null) {
            return artifactRepository.findVisibleToUser(user, userOrgs);
        }

        return artifactRepository.advancedSearchWithVisibility(searchTerm, visibility, tagName, user, userOrgs);
    }

    /**
     * Get most popular artifacts (by download count)
     */
    public List<Artifact> getMostPopular(int limit, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Organization> userOrgs = getUserOrganizations(user);
        List<Artifact> artifacts = artifactRepository.findMostDownloadedWithVisibility(user, userOrgs);
        return artifacts.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Get recently updated artifacts
     */
    public List<Artifact> getRecentlyUpdated(int limit, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Organization> userOrgs = getUserOrganizations(user);
        List<Artifact> artifacts = artifactRepository.findRecentlyUpdatedWithVisibility(user, userOrgs);
        return artifacts.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Get artifact analytics
     * Cached for 5 minutes
     */
    @Cacheable(value = "artifactAnalytics", key = "#username")
    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Organization> userOrgs = getUserOrganizations(user);

        Map<String, Object> analytics = new HashMap<>();

        // Total counts
        analytics.put("totalArtifacts", artifactRepository.countVisibleToUser(user, userOrgs));
        analytics.put("totalVersions", artifactVersionRepository.countTotalVersions());
        analytics.put("totalTags", artifactTagRepository.count());

        // Count by visibility
        List<Object[]> visibilityCounts = artifactRepository.countByVisibilityWithFilter(user, userOrgs);
        Map<String, Long> visibilityCountMap = new HashMap<>();
        for (Object[] row : visibilityCounts) {
            visibilityCountMap.put(row[0].toString(), (Long) row[1]);
        }
        analytics.put("artifactsByVisibility", visibilityCountMap);

        // Most popular tags
        List<Object[]> popularTagsData = artifactTagRepository.findMostPopularWithCounts();
        List<Map<String, Object>> tagStats = popularTagsData.stream()
                .limit(10)
                .map(row -> {
                    Map<String, Object> tagMap = new HashMap<>();
                    tagMap.put("name", row[1]);
                    tagMap.put("count", row[3]);
                    return tagMap;
                })
                .collect(Collectors.toList());
        analytics.put("popularTags", tagStats);

        // Most downloaded artifacts
        List<Artifact> mostDownloaded = getMostPopular(5, username);
        analytics.put("mostDownloaded", mostDownloaded.stream()
                .map(artifact -> Map.of(
                        "artifactId", artifact.getArtifactId(),
                        "title", artifact.getTitle(),
                        "downloadCount", artifact.getDownloadCount()
                ))
                .collect(Collectors.toList()));

        return analytics;
    }

    /**
     * Process tag names and create/retrieve tags
     */
    private Set<ArtifactTag> processTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }

        Set<ArtifactTag> tags = new HashSet<>();
        for (String tagName : tagNames) {
            String normalizedName = tagName.toLowerCase().trim();
            ArtifactTag tag = artifactTagRepository.findByName(normalizedName)
                    .orElseGet(() -> {
                        ArtifactTag newTag = new ArtifactTag(normalizedName);
                        return artifactTagRepository.save(newTag);
                    });
            tags.add(tag);
        }
        return tags;
    }

    /**
     * Get all tags with usage counts
     * Cached for 10 minutes as tags change infrequently
     */
    @Cacheable(value = "artifactTags", key = "'all'")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllTags() {
        List<Object[]> tagsData = artifactTagRepository.findAllWithCountsOrderByNameAsc();
        return tagsData.stream()
                .map(row -> {
                    Map<String, Object> tagMap = new HashMap<>();
                    tagMap.put("name", row[1]);
                    tagMap.put("usageCount", row[3]);
                    return tagMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get popular tags with usage counts
     * Cached for 10 minutes as tags change infrequently
     */
    @Cacheable(value = "popularTags", key = "'artifact_' + #limit")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPopularTags(int limit) {
        List<Object[]> tagsData = artifactTagRepository.findMostPopularWithCounts();
        return tagsData.stream()
                .limit(limit)
                .map(row -> {
                    Map<String, Object> tagMap = new HashMap<>();
                    tagMap.put("name", row[1]);
                    tagMap.put("usageCount", row[3]);
                    return tagMap;
                })
                .collect(Collectors.toList());
    }

    // ==================== PAGINATED METHODS ====================

    /**
     * Get all visible artifacts for user (paginated)
     */
    @Transactional(readOnly = true)
    public Page<Artifact> getAllVisibleArtifactsPaged(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Organization> userOrgs = getUserOrganizations(user);
        // Use different query when user has no organizations to avoid empty IN clause
        if (userOrgs.isEmpty()) {
            return artifactRepository.findVisibleToUserPagedNoOrgs(user, pageable);
        }
        return artifactRepository.findVisibleToUserPaged(user, userOrgs, pageable);
    }

    /**
     * Get user's own artifacts (paginated)
     */
    @Transactional(readOnly = true)
    public Page<Artifact> getMyArtifactsPaged(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return artifactRepository.findMyArtifactsPaged(user, pageable);
    }

    /**
     * Get public artifacts only (paginated)
     */
    @Transactional(readOnly = true)
    public Page<Artifact> getPublicArtifactsPaged(Pageable pageable) {
        return artifactRepository.findPublicArtifactsPaged(pageable);
    }

    /**
     * Get organization artifacts (paginated)
     */
    @Transactional(readOnly = true)
    public Page<Artifact> getOrganizationArtifactsPaged(Long organizationId, String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + organizationId));

        // Verify user belongs to organization
        List<Organization> userOrgs = getUserOrganizations(user);
        if (!userOrgs.contains(organization)) {
            throw new RuntimeException("User does not belong to this organization");
        }

        return artifactRepository.findOrganizationArtifactsPaged(organization, pageable);
    }

    /**
     * Search artifacts with visibility filter (paginated)
     */
    @Transactional(readOnly = true)
    public Page<Artifact> searchArtifactsPaged(String searchTerm, ArtifactVisibility visibility,
                                               String tagName, String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Normalize empty strings to null
        if (searchTerm != null && searchTerm.trim().isEmpty()) {
            searchTerm = null;
        }
        if (tagName != null && tagName.trim().isEmpty()) {
            tagName = null;
        }

        // If no search criteria, just return all visible artifacts
        if (searchTerm == null && visibility == null && tagName == null) {
            return getAllVisibleArtifactsPaged(username, pageable);
        }

        List<Organization> userOrgs = getUserOrganizations(user);
        boolean hasOrgs = !userOrgs.isEmpty();

        // Tag-only search (most common case from the UI)
        if (tagName != null && searchTerm == null && visibility == null) {
            if (hasOrgs) {
                return artifactRepository.findByTag(tagName, user, userOrgs, pageable);
            }
            return artifactRepository.findByTagNoOrgs(tagName, user, pageable);
        }

        // Text search only
        if (searchTerm != null && tagName == null && visibility == null) {
            if (hasOrgs) {
                return artifactRepository.searchByText(searchTerm, user, userOrgs, pageable);
            }
            return artifactRepository.searchByTextNoOrgs(searchTerm, user, pageable);
        }

        // Complex search with multiple criteria - use the general query
        if (hasOrgs) {
            return artifactRepository.advancedSearchWithVisibilityPaged(searchTerm, visibility, tagName, user, userOrgs, pageable);
        }
        return artifactRepository.advancedSearchWithVisibilityPagedNoOrgs(searchTerm, visibility, tagName, user, pageable);
    }
}

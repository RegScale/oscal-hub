package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Artifact;
import gov.nist.oscal.tools.api.entity.Artifact.ArtifactVisibility;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, Long> {

    Optional<Artifact> findByArtifactId(String artifactId);

    /**
     * Leaderboard: artifacts created per user since the cutoff.
     * Rows are [userId, count]. Pass epoch for all-time.
     */
    @Query("SELECT a.createdBy.id, COUNT(a) FROM Artifact a "
            + "WHERE a.createdAt >= :cutoff GROUP BY a.createdBy.id")
    List<Object[]> countCreatedPerUserSince(@Param("cutoff") java.time.LocalDateTime cutoff);

    // Find artifact by ID with tags eagerly loaded (for single artifact fetches)
    @Query("SELECT a FROM Artifact a LEFT JOIN FETCH a.tags WHERE a.artifactId = :artifactId")
    Optional<Artifact> findByArtifactIdWithTags(@Param("artifactId") String artifactId);

    List<Artifact> findByCreatedBy(User user);

    List<Artifact> findByVisibility(ArtifactVisibility visibility);

    List<Artifact> findByOrganization(Organization organization);

    // Find all artifacts visible to user (their own + public + their organizations)
    @Query("SELECT DISTINCT a FROM Artifact a WHERE " +
           "a.createdBy = :user OR " +
           "a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations)")
    List<Artifact> findVisibleToUser(@Param("user") User user,
                                     @Param("organizations") List<Organization> organizations);

    // Find user's own artifacts
    @Query("SELECT a FROM Artifact a WHERE a.createdBy = :user ORDER BY a.updatedAt DESC")
    List<Artifact> findMyArtifacts(@Param("user") User user);

    // Find public artifacts only
    @Query("SELECT a FROM Artifact a WHERE a.visibility = 'PUBLIC' ORDER BY a.updatedAt DESC")
    List<Artifact> findPublicArtifacts();

    // Find organization artifacts (for a specific organization)
    @Query("SELECT a FROM Artifact a WHERE a.visibility = 'ORGANIZATION' AND a.organization = :organization ORDER BY a.updatedAt DESC")
    List<Artifact> findOrganizationArtifacts(@Param("organization") Organization organization);

    // Search by title (case-insensitive, contains)
    List<Artifact> findByTitleContainingIgnoreCase(String title);

    // Search by description (case-insensitive, contains)
    List<Artifact> findByDescriptionContainingIgnoreCase(String description);

    // Full-text search across title and description with visibility filter
    @Query("SELECT DISTINCT a FROM Artifact a WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations)) AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Artifact> searchByTitleOrDescriptionWithVisibility(
        @Param("searchTerm") String searchTerm,
        @Param("user") User user,
        @Param("organizations") List<Organization> organizations);

    // Search by tag name with visibility filter
    @Query("SELECT DISTINCT a FROM Artifact a JOIN a.tags t WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations)) AND " +
           "LOWER(t.name) = LOWER(:tagName)")
    List<Artifact> findByTagNameWithVisibility(
        @Param("tagName") String tagName,
        @Param("user") User user,
        @Param("organizations") List<Organization> organizations);

    // Advanced search with multiple criteria and visibility filter
    @Query("SELECT DISTINCT a FROM Artifact a LEFT JOIN a.tags t WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations)) AND " +
           "(:searchTerm IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%')) OR " +
           "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%'))) AND " +
           "(:visibility IS NULL OR a.visibility = :visibility) AND " +
           "(:tagName IS NULL OR LOWER(t.name) = LOWER(COALESCE(:tagName, '')))")
    List<Artifact> advancedSearchWithVisibility(
        @Param("searchTerm") String searchTerm,
        @Param("visibility") ArtifactVisibility visibility,
        @Param("tagName") String tagName,
        @Param("user") User user,
        @Param("organizations") List<Organization> organizations);

    // Get most downloaded artifacts (visible to user)
    @Query("SELECT a FROM Artifact a WHERE " +
           "a.createdBy = :user OR a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations) " +
           "ORDER BY a.downloadCount DESC")
    List<Artifact> findMostDownloadedWithVisibility(
        @Param("user") User user,
        @Param("organizations") List<Organization> organizations);

    // Get most viewed artifacts (visible to user)
    @Query("SELECT a FROM Artifact a WHERE " +
           "a.createdBy = :user OR a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations) " +
           "ORDER BY a.viewCount DESC")
    List<Artifact> findMostViewedWithVisibility(
        @Param("user") User user,
        @Param("organizations") List<Organization> organizations);

    // Get recently updated artifacts (visible to user)
    @Query("SELECT a FROM Artifact a WHERE " +
           "a.createdBy = :user OR a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations) " +
           "ORDER BY a.updatedAt DESC")
    List<Artifact> findRecentlyUpdatedWithVisibility(
        @Param("user") User user,
        @Param("organizations") List<Organization> organizations);

    // Count artifacts by visibility (for analytics)
    @Query("SELECT a.visibility, COUNT(a) FROM Artifact a WHERE " +
           "a.createdBy = :user OR a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations) " +
           "GROUP BY a.visibility")
    List<Object[]> countByVisibilityWithFilter(
        @Param("user") User user,
        @Param("organizations") List<Organization> organizations);

    // Total count of visible artifacts
    @Query("SELECT COUNT(a) FROM Artifact a WHERE " +
           "a.createdBy = :user OR a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations)")
    Long countVisibleToUser(
        @Param("user") User user,
        @Param("organizations") List<Organization> organizations);

    // ==================== Performance Optimized Queries ====================
    // These queries use JOIN FETCH to avoid N+1 problems with tags

    // Find all visible artifacts with tags eagerly loaded
    @Query("SELECT DISTINCT a FROM Artifact a LEFT JOIN FETCH a.tags WHERE " +
           "a.createdBy = :user OR " +
           "a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations)")
    List<Artifact> findVisibleToUserWithTags(@Param("user") User user,
                                             @Param("organizations") List<Organization> organizations);

    // Find user's artifacts with tags
    @Query("SELECT DISTINCT a FROM Artifact a LEFT JOIN FETCH a.tags WHERE a.createdBy = :user ORDER BY a.updatedAt DESC")
    List<Artifact> findMyArtifactsWithTags(@Param("user") User user);

    // Find public artifacts with tags
    @Query("SELECT DISTINCT a FROM Artifact a LEFT JOIN FETCH a.tags WHERE a.visibility = 'PUBLIC' ORDER BY a.updatedAt DESC")
    List<Artifact> findPublicArtifactsWithTags();

    // Find organization artifacts with tags
    @Query("SELECT DISTINCT a FROM Artifact a LEFT JOIN FETCH a.tags WHERE a.visibility = 'ORGANIZATION' AND a.organization = :organization ORDER BY a.updatedAt DESC")
    List<Artifact> findOrganizationArtifactsWithTags(@Param("organization") Organization organization);

    // ==================== Paginated Queries ====================
    // These queries support pagination for large datasets

    // Paginated: Find all visible artifacts (own + public only, no org filtering)
    @Query(value = "SELECT DISTINCT a FROM Artifact a WHERE " +
           "a.createdBy = :user OR " +
           "a.visibility = 'PUBLIC'",
           countQuery = "SELECT COUNT(DISTINCT a) FROM Artifact a WHERE " +
           "a.createdBy = :user OR " +
           "a.visibility = 'PUBLIC'")
    Page<Artifact> findVisibleToUserPagedNoOrgs(@Param("user") User user, Pageable pageable);

    // Paginated: Find all visible artifacts (with org filtering)
    @Query(value = "SELECT DISTINCT a FROM Artifact a WHERE " +
           "a.createdBy = :user OR " +
           "a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations)",
           countQuery = "SELECT COUNT(DISTINCT a) FROM Artifact a WHERE " +
           "a.createdBy = :user OR " +
           "a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations)")
    Page<Artifact> findVisibleToUserPaged(@Param("user") User user,
                                          @Param("organizations") List<Organization> organizations,
                                          Pageable pageable);

    // Paginated: Find user's artifacts
    @Query(value = "SELECT a FROM Artifact a WHERE a.createdBy = :user",
           countQuery = "SELECT COUNT(a) FROM Artifact a WHERE a.createdBy = :user")
    Page<Artifact> findMyArtifactsPaged(@Param("user") User user, Pageable pageable);

    // Paginated: Find public artifacts
    @Query(value = "SELECT a FROM Artifact a WHERE a.visibility = 'PUBLIC'",
           countQuery = "SELECT COUNT(a) FROM Artifact a WHERE a.visibility = 'PUBLIC'")
    Page<Artifact> findPublicArtifactsPaged(Pageable pageable);

    // Paginated: Find organization artifacts
    @Query(value = "SELECT a FROM Artifact a WHERE a.visibility = 'ORGANIZATION' AND a.organization = :organization",
           countQuery = "SELECT COUNT(a) FROM Artifact a WHERE a.visibility = 'ORGANIZATION' AND a.organization = :organization")
    Page<Artifact> findOrganizationArtifactsPaged(@Param("organization") Organization organization, Pageable pageable);

    // Paginated: Search by tag only (no org filtering)
    @Query(value = "SELECT DISTINCT a FROM Artifact a JOIN a.tags t WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC') AND " +
           "LOWER(t.name) = LOWER(:tagName)",
           countQuery = "SELECT COUNT(DISTINCT a) FROM Artifact a JOIN a.tags t WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC') AND " +
           "LOWER(t.name) = LOWER(:tagName)")
    Page<Artifact> findByTagNoOrgs(@Param("tagName") String tagName, @Param("user") User user, Pageable pageable);

    // Paginated: Search by tag only (with org filtering)
    @Query(value = "SELECT DISTINCT a FROM Artifact a JOIN a.tags t WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations)) AND " +
           "LOWER(t.name) = LOWER(:tagName)",
           countQuery = "SELECT COUNT(DISTINCT a) FROM Artifact a JOIN a.tags t WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations)) AND " +
           "LOWER(t.name) = LOWER(:tagName)")
    Page<Artifact> findByTag(@Param("tagName") String tagName, @Param("user") User user,
                             @Param("organizations") List<Organization> organizations, Pageable pageable);

    // Paginated: Search with visibility filter (no org filtering)
    @Query(value = "SELECT DISTINCT a FROM Artifact a LEFT JOIN a.tags t WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC') AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')))",
           countQuery = "SELECT COUNT(DISTINCT a) FROM Artifact a LEFT JOIN a.tags t WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC') AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Artifact> searchByTextNoOrgs(@Param("searchTerm") String searchTerm, @Param("user") User user, Pageable pageable);

    // Paginated: Search with visibility filter (with org filtering)
    @Query(value = "SELECT DISTINCT a FROM Artifact a LEFT JOIN a.tags t WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations)) AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')))",
           countQuery = "SELECT COUNT(DISTINCT a) FROM Artifact a LEFT JOIN a.tags t WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations)) AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Artifact> searchByText(@Param("searchTerm") String searchTerm, @Param("user") User user,
                                @Param("organizations") List<Organization> organizations, Pageable pageable);

    // Keep the complex search for backwards compatibility but it may not work well with all null combinations
    @Query(value = "SELECT DISTINCT a FROM Artifact a LEFT JOIN a.tags t WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC') AND " +
           "(:searchTerm IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%')) OR " +
           "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%'))) AND " +
           "(:visibility IS NULL OR a.visibility = :visibility) AND " +
           "(:tagName IS NULL OR LOWER(t.name) = LOWER(COALESCE(:tagName, '')))",
           countQuery = "SELECT COUNT(DISTINCT a) FROM Artifact a LEFT JOIN a.tags t WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC') AND " +
           "(:searchTerm IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%')) OR " +
           "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%'))) AND " +
           "(:visibility IS NULL OR a.visibility = :visibility) AND " +
           "(:tagName IS NULL OR LOWER(t.name) = LOWER(COALESCE(:tagName, '')))")
    Page<Artifact> advancedSearchWithVisibilityPagedNoOrgs(
        @Param("searchTerm") String searchTerm,
        @Param("visibility") ArtifactVisibility visibility,
        @Param("tagName") String tagName,
        @Param("user") User user,
        Pageable pageable);

    // Paginated: Search with visibility filter (with org filtering)
    @Query(value = "SELECT DISTINCT a FROM Artifact a LEFT JOIN a.tags t WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations)) AND " +
           "(:searchTerm IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%')) OR " +
           "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%'))) AND " +
           "(:visibility IS NULL OR a.visibility = :visibility) AND " +
           "(:tagName IS NULL OR LOWER(t.name) = LOWER(COALESCE(:tagName, '')))",
           countQuery = "SELECT COUNT(DISTINCT a) FROM Artifact a LEFT JOIN a.tags t WHERE " +
           "(a.createdBy = :user OR a.visibility = 'PUBLIC' OR " +
           "(a.visibility = 'ORGANIZATION' AND a.organization IN :organizations)) AND " +
           "(:searchTerm IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%')) OR " +
           "LOWER(COALESCE(a.description, '')) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%'))) AND " +
           "(:visibility IS NULL OR a.visibility = :visibility) AND " +
           "(:tagName IS NULL OR LOWER(t.name) = LOWER(COALESCE(:tagName, '')))")
    Page<Artifact> advancedSearchWithVisibilityPaged(
        @Param("searchTerm") String searchTerm,
        @Param("visibility") ArtifactVisibility visibility,
        @Param("tagName") String tagName,
        @Param("user") User user,
        @Param("organizations") List<Organization> organizations,
        Pageable pageable);
}

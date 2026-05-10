package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.LibraryItem;
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
public interface LibraryItemRepository extends JpaRepository<LibraryItem, Long> {

    Optional<LibraryItem> findByItemId(String itemId);

    /**
     * Looks up a library item by its creator and the builder source it was saved from.
     * Used by LibraryIngestService to decide between create-new vs append-version.
     */
    java.util.Optional<LibraryItem> findByCreatedBy_IdAndSourceTypeAndSourceId(
        Long createdById,
        gov.nist.oscal.tools.api.entity.SourceType sourceType,
        java.util.UUID sourceId);

    // Find library item by ID with tags eagerly loaded (for single item fetches)
    @Query("SELECT li FROM LibraryItem li LEFT JOIN FETCH li.tags WHERE li.itemId = :itemId")
    Optional<LibraryItem> findByItemIdWithTags(@Param("itemId") String itemId);

    List<LibraryItem> findByCreatedBy(User user);

    List<LibraryItem> findByOscalType(String oscalType);

    // Search by title (case-insensitive, contains)
    List<LibraryItem> findByTitleContainingIgnoreCase(String title);

    // Search by description (case-insensitive, contains)
    List<LibraryItem> findByDescriptionContainingIgnoreCase(String description);

    // Search by OSCAL type
    List<LibraryItem> findByOscalTypeIn(List<String> oscalTypes);

    // Full-text search across title and description
    @Query("SELECT li FROM LibraryItem li WHERE " +
           "LOWER(li.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(li.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<LibraryItem> searchByTitleOrDescription(@Param("searchTerm") String searchTerm);

    // Search by tag name
    @Query("SELECT DISTINCT li FROM LibraryItem li JOIN li.tags t WHERE LOWER(t.name) = LOWER(:tagName)")
    List<LibraryItem> findByTagName(@Param("tagName") String tagName);

    // Advanced search with multiple criteria
    // Note: COALESCE prevents PostgreSQL bytea error when LOWER() receives NULL parameter
    // See: https://github.com/spring-projects/spring-data-jpa/issues/3928
    @Query("SELECT DISTINCT li FROM LibraryItem li LEFT JOIN li.tags t WHERE " +
           "(:searchTerm IS NULL OR LOWER(li.title) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%')) OR LOWER(COALESCE(li.description, '')) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%'))) AND " +
           "(:oscalType IS NULL OR li.oscalType = :oscalType) AND " +
           "(:tagName IS NULL OR LOWER(t.name) = LOWER(COALESCE(:tagName, '')))")
    List<LibraryItem> advancedSearch(
        @Param("searchTerm") String searchTerm,
        @Param("oscalType") String oscalType,
        @Param("tagName") String tagName
    );

    // Get most downloaded items
    @Query("SELECT li FROM LibraryItem li ORDER BY li.downloadCount DESC")
    List<LibraryItem> findMostDownloaded();

    // Get most viewed items
    @Query("SELECT li FROM LibraryItem li ORDER BY li.viewCount DESC")
    List<LibraryItem> findMostViewed();

    // Get recently updated items
    @Query("SELECT li FROM LibraryItem li ORDER BY li.updatedAt DESC")
    List<LibraryItem> findRecentlyUpdated();

    // Count items by OSCAL type (for analytics)
    @Query("SELECT li.oscalType, COUNT(li) FROM LibraryItem li GROUP BY li.oscalType")
    List<Object[]> countByOscalType();

    // ==================== Performance Optimized Queries ====================
    // These queries use JOIN FETCH to avoid N+1 problems with tags

    // Find all library items with tags eagerly loaded
    @Query("SELECT DISTINCT li FROM LibraryItem li LEFT JOIN FETCH li.tags ORDER BY li.updatedAt DESC")
    List<LibraryItem> findAllWithTags();

    // Find items by OSCAL type with tags
    @Query("SELECT DISTINCT li FROM LibraryItem li LEFT JOIN FETCH li.tags WHERE li.oscalType = :oscalType ORDER BY li.updatedAt DESC")
    List<LibraryItem> findByOscalTypeWithTags(@Param("oscalType") String oscalType);

    // Find items by creator with tags
    @Query("SELECT DISTINCT li FROM LibraryItem li LEFT JOIN FETCH li.tags WHERE li.createdBy = :user ORDER BY li.updatedAt DESC")
    List<LibraryItem> findByCreatedByWithTags(@Param("user") User user);

    // Get most downloaded with tags
    @Query("SELECT DISTINCT li FROM LibraryItem li LEFT JOIN FETCH li.tags ORDER BY li.downloadCount DESC")
    List<LibraryItem> findMostDownloadedWithTags();

    // Get most viewed with tags
    @Query("SELECT DISTINCT li FROM LibraryItem li LEFT JOIN FETCH li.tags ORDER BY li.viewCount DESC")
    List<LibraryItem> findMostViewedWithTags();

    // Get recently updated with tags
    @Query("SELECT DISTINCT li FROM LibraryItem li LEFT JOIN FETCH li.tags ORDER BY li.updatedAt DESC")
    List<LibraryItem> findRecentlyUpdatedWithTags();

    // ==================== Paginated Queries ====================
    // These queries support pagination for large datasets

    // Paginated: Find all library items
    @Query(value = "SELECT li FROM LibraryItem li",
           countQuery = "SELECT COUNT(li) FROM LibraryItem li")
    Page<LibraryItem> findAllPaged(Pageable pageable);

    // Paginated: Find by OSCAL type
    @Query(value = "SELECT li FROM LibraryItem li WHERE li.oscalType = :oscalType",
           countQuery = "SELECT COUNT(li) FROM LibraryItem li WHERE li.oscalType = :oscalType")
    Page<LibraryItem> findByOscalTypePaged(@Param("oscalType") String oscalType, Pageable pageable);

    // Paginated: Advanced search
    @Query(value = "SELECT DISTINCT li FROM LibraryItem li LEFT JOIN li.tags t WHERE " +
           "(:searchTerm IS NULL OR LOWER(li.title) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%')) OR LOWER(COALESCE(li.description, '')) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%'))) AND " +
           "(:oscalType IS NULL OR li.oscalType = :oscalType) AND " +
           "(:tagName IS NULL OR LOWER(t.name) = LOWER(COALESCE(:tagName, '')))",
           countQuery = "SELECT COUNT(DISTINCT li) FROM LibraryItem li LEFT JOIN li.tags t WHERE " +
           "(:searchTerm IS NULL OR LOWER(li.title) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%')) OR LOWER(COALESCE(li.description, '')) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%'))) AND " +
           "(:oscalType IS NULL OR li.oscalType = :oscalType) AND " +
           "(:tagName IS NULL OR LOWER(t.name) = LOWER(COALESCE(:tagName, '')))")
    Page<LibraryItem> advancedSearchPaged(
        @Param("searchTerm") String searchTerm,
        @Param("oscalType") String oscalType,
        @Param("tagName") String tagName,
        Pageable pageable
    );

    // ==================== Visibility-Aware Queries ====================
    // These queries filter results based on visibility:
    //   - PUBLIC items are visible to everyone (including anonymous, userId == null)
    //   - PRIVATE/ORGANIZATION items are visible only to the creator
    //   - ORGANIZATION items are also visible to same-org members (orgId match)
    // Pass userId == null and orgId == null for anonymous callers (returns PUBLIC only).

    @Query(value = "SELECT li FROM LibraryItem li WHERE " +
           "(li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC " +
           " OR (:userId IS NOT NULL AND li.createdBy.id = :userId) " +
           " OR (li.visibility = gov.nist.oscal.tools.api.entity.Visibility.ORGANIZATION " +
           "     AND :orgId IS NOT NULL AND li.organization.id = :orgId))",
           countQuery = "SELECT COUNT(li) FROM LibraryItem li WHERE " +
           "(li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC " +
           " OR (:userId IS NOT NULL AND li.createdBy.id = :userId) " +
           " OR (li.visibility = gov.nist.oscal.tools.api.entity.Visibility.ORGANIZATION " +
           "     AND :orgId IS NOT NULL AND li.organization.id = :orgId))")
    Page<LibraryItem> findAllVisibleTo(
        @Param("userId") Long userId,
        @Param("orgId") Long orgId,
        Pageable pageable);

    @Query(value = "SELECT li FROM LibraryItem li WHERE li.oscalType = :oscalType AND " +
           "(li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC " +
           " OR (:userId IS NOT NULL AND li.createdBy.id = :userId) " +
           " OR (li.visibility = gov.nist.oscal.tools.api.entity.Visibility.ORGANIZATION " +
           "     AND :orgId IS NOT NULL AND li.organization.id = :orgId))",
           countQuery = "SELECT COUNT(li) FROM LibraryItem li WHERE li.oscalType = :oscalType AND " +
           "(li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC " +
           " OR (:userId IS NOT NULL AND li.createdBy.id = :userId) " +
           " OR (li.visibility = gov.nist.oscal.tools.api.entity.Visibility.ORGANIZATION " +
           "     AND :orgId IS NOT NULL AND li.organization.id = :orgId))")
    Page<LibraryItem> findByOscalTypeVisibleTo(
        @Param("oscalType") String oscalType,
        @Param("userId") Long userId,
        @Param("orgId") Long orgId,
        Pageable pageable);

    @Query(value = "SELECT DISTINCT li FROM LibraryItem li LEFT JOIN li.tags t WHERE " +
           "(:searchTerm IS NULL OR LOWER(li.title) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%')) OR LOWER(COALESCE(li.description, '')) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%'))) AND " +
           "(:oscalType IS NULL OR li.oscalType = :oscalType) AND " +
           "(:tagName IS NULL OR LOWER(t.name) = LOWER(COALESCE(:tagName, ''))) AND " +
           "(li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC " +
           " OR (:userId IS NOT NULL AND li.createdBy.id = :userId) " +
           " OR (li.visibility = gov.nist.oscal.tools.api.entity.Visibility.ORGANIZATION " +
           "     AND :orgId IS NOT NULL AND li.organization.id = :orgId))",
           countQuery = "SELECT COUNT(DISTINCT li) FROM LibraryItem li LEFT JOIN li.tags t WHERE " +
           "(:searchTerm IS NULL OR LOWER(li.title) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%')) OR LOWER(COALESCE(li.description, '')) LIKE LOWER(CONCAT('%', COALESCE(:searchTerm, ''), '%'))) AND " +
           "(:oscalType IS NULL OR li.oscalType = :oscalType) AND " +
           "(:tagName IS NULL OR LOWER(t.name) = LOWER(COALESCE(:tagName, ''))) AND " +
           "(li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC " +
           " OR (:userId IS NOT NULL AND li.createdBy.id = :userId) " +
           " OR (li.visibility = gov.nist.oscal.tools.api.entity.Visibility.ORGANIZATION " +
           "     AND :orgId IS NOT NULL AND li.organization.id = :orgId))")
    Page<LibraryItem> advancedSearchPagedVisibleTo(
        @Param("searchTerm") String searchTerm,
        @Param("oscalType") String oscalType,
        @Param("tagName") String tagName,
        @Param("userId") Long userId,
        @Param("orgId") Long orgId,
        Pageable pageable);

    @Query("SELECT li FROM LibraryItem li WHERE " +
           "(li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC " +
           " OR (:userId IS NOT NULL AND li.createdBy.id = :userId) " +
           " OR (li.visibility = gov.nist.oscal.tools.api.entity.Visibility.ORGANIZATION " +
           "     AND :orgId IS NOT NULL AND li.organization.id = :orgId)) " +
           "ORDER BY li.downloadCount DESC")
    List<LibraryItem> findMostDownloadedVisibleTo(
        @Param("userId") Long userId,
        @Param("orgId") Long orgId);

    @Query("SELECT li FROM LibraryItem li WHERE " +
           "(li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC " +
           " OR (:userId IS NOT NULL AND li.createdBy.id = :userId) " +
           " OR (li.visibility = gov.nist.oscal.tools.api.entity.Visibility.ORGANIZATION " +
           "     AND :orgId IS NOT NULL AND li.organization.id = :orgId)) " +
           "ORDER BY li.updatedAt DESC")
    List<LibraryItem> findRecentlyUpdatedVisibleTo(
        @Param("userId") Long userId,
        @Param("orgId") Long orgId);

    // ==================== Public Catalog Queries ====================
    // Anonymous-readable, PUBLIC-only views.

    /**
     * Public catalog search. Visibility filter pinned to PUBLIC.
     * FTS keyword applies to title+description via the GIN tsvector index;
     * type and tag are optional exact filters.
     */
    @Query(value = """
        SELECT li.* FROM library_items li
        LEFT JOIN library_item_tags lit ON lit.library_item_id = li.id
        LEFT JOIN library_tags lt ON lt.id = lit.tag_id
        WHERE li.visibility = 'PUBLIC'
          AND (:q IS NULL OR :q = ''
               OR to_tsvector('english',
                  coalesce(li.title,'') || ' ' || coalesce(li.description,''))
                  @@ plainto_tsquery('english', :q))
          AND (:type IS NULL OR :type = '' OR li.oscal_type = :type)
          AND (:tag  IS NULL OR :tag  = '' OR lt.name = :tag)
        GROUP BY li.id
        """,
        countQuery = """
            SELECT COUNT(DISTINCT li.id) FROM library_items li
            LEFT JOIN library_item_tags lit ON lit.library_item_id = li.id
            LEFT JOIN library_tags lt ON lt.id = lit.tag_id
            WHERE li.visibility = 'PUBLIC'
              AND (:q IS NULL OR :q = ''
                   OR to_tsvector('english',
                      coalesce(li.title,'') || ' ' || coalesce(li.description,''))
                      @@ plainto_tsquery('english', :q))
              AND (:type IS NULL OR :type = '' OR li.oscal_type = :type)
              AND (:tag  IS NULL OR :tag  = '' OR lt.name = :tag)
            """,
        nativeQuery = true)
    Page<LibraryItem> searchPublic(
        @Param("q") String q,
        @Param("type") String type,
        @Param("tag") String tag,
        Pageable pageable);

    @Query("""
        SELECT li FROM LibraryItem li
        WHERE li.itemId = :itemId AND li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC
        """)
    Optional<LibraryItem> findPublicByItemId(@Param("itemId") String itemId);

    // ==================== Public Analytics Queries ====================
    // Aggregations over PUBLIC items only — these power the /catalog tabs.

    /**
     * Most-downloaded PUBLIC items, ordered by downloadCount desc.
     * Pageable controls limit (e.g., PageRequest.of(0, 10)).
     */
    @Query("SELECT li FROM LibraryItem li " +
           "WHERE li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC " +
           "ORDER BY li.downloadCount DESC, li.lastPublishedAt DESC")
    List<LibraryItem> findMostDownloadedPublic(Pageable pageable);

    /**
     * Top-rated PUBLIC items. Items with at least :minRatings ratings only —
     * a single 5-star rating shouldn't dominate the leaderboard. Tie-break by
     * total rating count so well-loved items beat lightly-rated ones at the
     * same average. Returns rows of [LibraryItem, avgRating, totalRatings].
     */
    @Query(value = """
        SELECT li, AVG(r.rating) AS avgRating, COUNT(r) AS totalRatings
        FROM LibraryItem li
        JOIN LibraryItemRating r ON r.libraryItem.id = li.id
        WHERE li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC
        GROUP BY li.id
        HAVING COUNT(r) >= :minRatings
        ORDER BY AVG(r.rating) DESC, COUNT(r) DESC, li.lastPublishedAt DESC
        """)
    List<Object[]> findTopRatedPublic(@Param("minRatings") long minRatings, Pageable pageable);

    /**
     * Top user contributors — counts of PUBLIC items they've published, with
     * total downloads across those items. Returns rows of:
     *   [userId, username, firstName, lastName, uploadCount, totalDownloads]
     */
    @Query("SELECT li.createdBy.id, li.createdBy.username, " +
           "       li.createdBy.firstName, li.createdBy.lastName, " +
           "       COUNT(li), COALESCE(SUM(li.downloadCount), 0) " +
           "FROM LibraryItem li " +
           "WHERE li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC " +
           "  AND li.createdBy IS NOT NULL " +
           "GROUP BY li.createdBy.id, li.createdBy.username, li.createdBy.firstName, li.createdBy.lastName " +
           "ORDER BY COUNT(li) DESC, COALESCE(SUM(li.downloadCount), 0) DESC")
    List<Object[]> findTopUserContributorsPublic(Pageable pageable);

    /**
     * Top organization contributors — counts of PUBLIC items their members
     * have published. Returns rows of:
     *   [organizationId, organizationName, logoUrl, uploadCount, totalDownloads]
     */
    @Query("SELECT li.organization.id, li.organization.name, li.organization.logoUrl, " +
           "       COUNT(li), COALESCE(SUM(li.downloadCount), 0) " +
           "FROM LibraryItem li " +
           "WHERE li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC " +
           "  AND li.organization IS NOT NULL " +
           "GROUP BY li.organization.id, li.organization.name, li.organization.logoUrl " +
           "ORDER BY COUNT(li) DESC, COALESCE(SUM(li.downloadCount), 0) DESC")
    List<Object[]> findTopOrgContributorsPublic(Pageable pageable);

    /**
     * Per-OSCAL-type stats over PUBLIC items: count, average downloads, and
     * average rating. Average rating is computed via a correlated subquery
     * over LibraryItemRating so items without ratings don't drag the mean
     * to zero. Returns rows of:
     *   [oscalType, itemCount, avgDownloads, avgRating]
     */
    @Query(value = """
        SELECT li.oscal_type,
               COUNT(li.id),
               COALESCE(AVG(li.download_count), 0),
               COALESCE((
                   SELECT AVG(r.rating)
                   FROM library_item_ratings r
                   JOIN library_items li2 ON li2.id = r.library_item_id
                   WHERE li2.visibility = 'PUBLIC' AND li2.oscal_type = li.oscal_type
               ), 0)
        FROM library_items li
        WHERE li.visibility = 'PUBLIC'
        GROUP BY li.oscal_type
        ORDER BY COUNT(li.id) DESC
        """, nativeQuery = true)
    List<Object[]> getTypeStatsPublic();

    /**
     * Uploads-over-time on PUBLIC items, bucketed by ISO week, since :since.
     * Native Postgres query (uses date_trunc). Returns rows of [weekStart, count].
     */
    @Query(value = """
        SELECT date_trunc('week', li.created_at) AS bucket, COUNT(li.id)
        FROM library_items li
        WHERE li.visibility = 'PUBLIC' AND li.created_at >= :since
        GROUP BY bucket
        ORDER BY bucket
        """, nativeQuery = true)
    List<Object[]> getUploadsPerWeekPublic(@Param("since") java.time.LocalDateTime since);

    /**
     * Top-level public-catalog totals.
     */
    @Query("SELECT COUNT(li) FROM LibraryItem li " +
           "WHERE li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC")
    long countPublic();

    @Query("SELECT COALESCE(SUM(li.downloadCount), 0) FROM LibraryItem li " +
           "WHERE li.visibility = gov.nist.oscal.tools.api.entity.Visibility.PUBLIC")
    long sumDownloadsPublic();
}

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
}

package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LibraryItemRepository extends JpaRepository<LibraryItem, Long> {

    Optional<LibraryItem> findByItemId(String itemId);

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
}

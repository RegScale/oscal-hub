package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryItemRating;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for LibraryItemRating entity.
 * Provides methods for managing user ratings on library items.
 */
@Repository
public interface LibraryItemRatingRepository extends JpaRepository<LibraryItemRating, Long> {

    /**
     * Find a user's rating for a specific library item.
     */
    Optional<LibraryItemRating> findByLibraryItemAndUser(LibraryItem libraryItem, User user);

    /**
     * Find a user's rating by item ID and user.
     */
    @Query("SELECT r FROM LibraryItemRating r WHERE r.libraryItem.itemId = :itemId AND r.user = :user")
    Optional<LibraryItemRating> findByItemIdAndUser(@Param("itemId") String itemId, @Param("user") User user);

    /**
     * Get all ratings for a specific library item.
     */
    List<LibraryItemRating> findByLibraryItem(LibraryItem libraryItem);

    /**
     * Calculate average rating for an item by its UUID.
     */
    @Query("SELECT AVG(r.rating) FROM LibraryItemRating r WHERE r.libraryItem.itemId = :itemId")
    Double getAverageRatingByItemId(@Param("itemId") String itemId);

    /**
     * Count total ratings for an item by its UUID.
     */
    @Query("SELECT COUNT(r) FROM LibraryItemRating r WHERE r.libraryItem.itemId = :itemId")
    Long countByItemId(@Param("itemId") String itemId);

    /**
     * Batch query to get rating statistics for multiple items.
     * Returns [itemId, averageRating, totalRatings] for each item.
     */
    @Query("SELECT r.libraryItem.itemId, AVG(r.rating), COUNT(r) FROM LibraryItemRating r " +
           "WHERE r.libraryItem.itemId IN :itemIds GROUP BY r.libraryItem.itemId")
    List<Object[]> getRatingStatsByItemIds(@Param("itemIds") List<String> itemIds);

    /**
     * Check if a user has already rated an item.
     */
    boolean existsByLibraryItemAndUser(LibraryItem libraryItem, User user);

    /**
     * Delete a user's rating for an item.
     */
    @Query("DELETE FROM LibraryItemRating r WHERE r.libraryItem.itemId = :itemId AND r.user = :user")
    void deleteByItemIdAndUser(@Param("itemId") String itemId, @Param("user") User user);
}

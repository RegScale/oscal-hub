package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryItemComment;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for LibraryItemComment entity.
 * Provides methods for managing threaded comments on library items.
 */
@Repository
public interface LibraryItemCommentRepository extends JpaRepository<LibraryItemComment, Long> {

    /**
     * Find a comment by its UUID.
     * Uses JOIN FETCH to eagerly load user to avoid LazyInitializationException.
     */
    @Query("SELECT c FROM LibraryItemComment c JOIN FETCH c.user WHERE c.commentId = :commentId")
    Optional<LibraryItemComment> findByCommentId(@Param("commentId") String commentId);

    /**
     * Get top-level comments (no parent) for a library item, excluding deleted.
     * Ordered by creation date descending (newest first).
     * Uses JOIN FETCH to eagerly load user to avoid LazyInitializationException.
     */
    @Query("SELECT c FROM LibraryItemComment c JOIN FETCH c.user WHERE c.libraryItem.itemId = :itemId " +
           "AND c.parentComment IS NULL AND c.deleted = false ORDER BY c.createdAt DESC")
    List<LibraryItemComment> findTopLevelCommentsByItemId(@Param("itemId") String itemId);

    /**
     * Get all comments for an item (including replies), excluding deleted.
     * Ordered by creation date ascending for proper thread ordering.
     * Uses JOIN FETCH to eagerly load user to avoid LazyInitializationException.
     */
    @Query("SELECT c FROM LibraryItemComment c JOIN FETCH c.user WHERE c.libraryItem.itemId = :itemId " +
           "AND c.deleted = false ORDER BY c.createdAt ASC")
    List<LibraryItemComment> findAllByItemId(@Param("itemId") String itemId);

    /**
     * Count total non-deleted comments for an item (including replies).
     */
    @Query("SELECT COUNT(c) FROM LibraryItemComment c WHERE c.libraryItem.itemId = :itemId " +
           "AND c.deleted = false")
    Long countByItemId(@Param("itemId") String itemId);

    /**
     * Batch query to get comment counts for multiple items.
     * Returns [itemId, commentCount] for each item.
     */
    @Query("SELECT c.libraryItem.itemId, COUNT(c) FROM LibraryItemComment c " +
           "WHERE c.libraryItem.itemId IN :itemIds AND c.deleted = false GROUP BY c.libraryItem.itemId")
    List<Object[]> countByItemIds(@Param("itemIds") List<String> itemIds);

    /**
     * Find replies to a specific comment, excluding deleted.
     * Uses JOIN FETCH to eagerly load user to avoid LazyInitializationException.
     */
    @Query("SELECT c FROM LibraryItemComment c JOIN FETCH c.user WHERE c.parentComment = :parentComment " +
           "AND c.deleted = false ORDER BY c.createdAt ASC")
    List<LibraryItemComment> findRepliesByParentComment(@Param("parentComment") LibraryItemComment parentComment);

    /**
     * Find comments by a specific user for a library item.
     * Uses JOIN FETCH to eagerly load user to avoid LazyInitializationException.
     */
    @Query("SELECT c FROM LibraryItemComment c JOIN FETCH c.user WHERE c.libraryItem.itemId = :itemId " +
           "AND c.user = :user AND c.deleted = false ORDER BY c.createdAt DESC")
    List<LibraryItemComment> findByItemIdAndUser(@Param("itemId") String itemId, @Param("user") User user);

    /**
     * Check if a comment exists and belongs to a user.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM LibraryItemComment c " +
           "WHERE c.commentId = :commentId AND c.user = :user AND c.deleted = false")
    boolean existsByCommentIdAndUser(@Param("commentId") String commentId, @Param("user") User user);

    /**
     * Count replies to a specific comment.
     */
    @Query("SELECT COUNT(c) FROM LibraryItemComment c WHERE c.parentComment.commentId = :parentCommentId " +
           "AND c.deleted = false")
    Long countRepliesByParentCommentId(@Param("parentCommentId") String parentCommentId);
}

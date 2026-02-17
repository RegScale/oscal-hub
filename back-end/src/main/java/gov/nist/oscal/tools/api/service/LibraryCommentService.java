package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryItemComment;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.CommentResponse;
import gov.nist.oscal.tools.api.repository.LibraryItemCommentRepository;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing library item comments.
 */
@Service
public class LibraryCommentService {

    private static final Logger logger = LoggerFactory.getLogger(LibraryCommentService.class);

    @Autowired
    private LibraryItemCommentRepository commentRepository;

    @Autowired
    private LibraryItemRepository libraryItemRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new comment on a library item.
     *
     * @param itemId          The library item UUID
     * @param content         The comment content
     * @param parentCommentId Optional parent comment UUID for replies
     * @param username        The username of the commenter
     * @return The created comment response
     */
    @Transactional
    public CommentResponse createComment(String itemId, String content, String parentCommentId, String username) {
        logger.info("User {} creating comment on item {}", username, itemId);

        // Find the library item
        LibraryItem libraryItem = libraryItemRepository.findByItemId(itemId)
                .orElseThrow(() -> new RuntimeException("Library item not found: " + itemId));

        // Find the user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Find parent comment if this is a reply
        LibraryItemComment parentComment = null;
        if (parentCommentId != null && !parentCommentId.isEmpty()) {
            parentComment = commentRepository.findByCommentId(parentCommentId)
                    .orElseThrow(() -> new RuntimeException("Parent comment not found: " + parentCommentId));

            // Verify parent comment belongs to same item
            if (!parentComment.getLibraryItem().getItemId().equals(itemId)) {
                throw new RuntimeException("Parent comment does not belong to this library item");
            }
        }

        // Create the comment
        String commentId = UUID.randomUUID().toString();
        LibraryItemComment comment = new LibraryItemComment(commentId, libraryItem, user, content, parentComment);
        comment = commentRepository.save(comment);

        logger.info("Created comment {} on item {} by user {}", commentId, itemId, username);

        return CommentResponse.fromEntity(comment);
    }

    /**
     * Get all comments for a library item with threaded structure.
     *
     * @param itemId The library item UUID
     * @return List of top-level comments with nested replies
     */
    public List<CommentResponse> getComments(String itemId) {
        // Get top-level comments
        List<LibraryItemComment> topLevelComments = commentRepository.findTopLevelCommentsByItemId(itemId);

        // Convert to response with recursive reply loading
        return topLevelComments.stream()
                .map(comment -> buildCommentTree(comment))
                .collect(Collectors.toList());
    }

    /**
     * Recursively build comment tree with replies.
     */
    private CommentResponse buildCommentTree(LibraryItemComment comment) {
        CommentResponse response = CommentResponse.fromEntity(comment, false);

        // Load replies recursively
        List<LibraryItemComment> replies = commentRepository.findRepliesByParentComment(comment);
        List<CommentResponse> replyResponses = replies.stream()
                .map(this::buildCommentTree)
                .collect(Collectors.toList());

        response.setReplies(replyResponses);
        response.setReplyCount(replyResponses.size());

        return response;
    }

    /**
     * Update a comment's content.
     *
     * @param commentId The comment UUID
     * @param content   The new content
     * @param username  The username of the requester (must be owner)
     * @return The updated comment response
     */
    @Transactional
    public CommentResponse updateComment(String commentId, String content, String username) {
        logger.info("User {} updating comment {}", username, commentId);

        LibraryItemComment comment = commentRepository.findByCommentId(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        // Verify ownership
        if (!comment.getUser().getUsername().equals(username)) {
            throw new RuntimeException("User is not authorized to edit this comment");
        }

        // Update content
        comment.setContent(content);
        comment = commentRepository.save(comment);

        logger.info("Updated comment {}", commentId);

        return CommentResponse.fromEntity(comment);
    }

    /**
     * Soft delete a comment.
     *
     * @param commentId The comment UUID
     * @param username  The username of the requester (must be owner)
     */
    @Transactional
    public void deleteComment(String commentId, String username) {
        logger.info("User {} deleting comment {}", username, commentId);

        LibraryItemComment comment = commentRepository.findByCommentId(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        // Verify ownership
        if (!comment.getUser().getUsername().equals(username)) {
            throw new RuntimeException("User is not authorized to delete this comment");
        }

        // Soft delete
        comment.setDeleted(true);
        commentRepository.save(comment);

        logger.info("Soft deleted comment {}", commentId);
    }

    /**
     * Get comment count for a library item.
     *
     * @param itemId The library item UUID
     * @return Total comment count (including replies)
     */
    public Long getCommentCount(String itemId) {
        Long count = commentRepository.countByItemId(itemId);
        return count != null ? count : 0L;
    }

    /**
     * Get comment counts for multiple library items (for efficient card display).
     *
     * @param itemIds List of library item UUIDs
     * @return Map of itemId to comment count
     */
    public Map<String, Long> getBatchCommentCounts(List<String> itemIds) {
        Map<String, Long> result = new HashMap<>();

        if (itemIds == null || itemIds.isEmpty()) {
            return result;
        }

        // Initialize all items with zero count
        for (String itemId : itemIds) {
            result.put(itemId, 0L);
        }

        // Batch query for comment counts
        List<Object[]> counts = commentRepository.countByItemIds(itemIds);
        for (Object[] count : counts) {
            String itemId = (String) count[0];
            Long commentCount = (Long) count[1];
            result.put(itemId, commentCount);
        }

        return result;
    }
}

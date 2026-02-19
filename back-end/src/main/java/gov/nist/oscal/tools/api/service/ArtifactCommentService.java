package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Artifact;
import gov.nist.oscal.tools.api.entity.ArtifactComment;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.CommentResponse;
import gov.nist.oscal.tools.api.repository.ArtifactCommentRepository;
import gov.nist.oscal.tools.api.repository.ArtifactRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing artifact comments.
 */
@Service
public class ArtifactCommentService {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactCommentService.class);

    @Autowired
    private ArtifactCommentRepository commentRepository;

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new comment on an artifact.
     *
     * @param artifactId      The artifact UUID
     * @param content         The comment content
     * @param parentCommentId Optional parent comment UUID for replies
     * @param username        The username of the commenter
     * @return The created comment response
     */
    @Transactional
    public CommentResponse createComment(String artifactId, String content, String parentCommentId, String username) {
        logger.info("User {} creating comment on artifact {}", username, artifactId);

        // Find the artifact
        Artifact artifact = artifactRepository.findByArtifactId(artifactId)
                .orElseThrow(() -> new RuntimeException("Artifact not found: " + artifactId));

        // Find the user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Find parent comment if this is a reply
        ArtifactComment parentComment = null;
        if (parentCommentId != null && !parentCommentId.isEmpty()) {
            parentComment = commentRepository.findByCommentId(parentCommentId)
                    .orElseThrow(() -> new RuntimeException("Parent comment not found: " + parentCommentId));

            // Verify parent comment belongs to same artifact
            if (!parentComment.getArtifact().getArtifactId().equals(artifactId)) {
                throw new RuntimeException("Parent comment does not belong to this artifact");
            }
        }

        // Create the comment
        String commentId = UUID.randomUUID().toString();
        ArtifactComment comment = new ArtifactComment(commentId, artifact, user, content, parentComment);
        comment = commentRepository.save(comment);

        logger.info("Created comment {} on artifact {} by user {}", commentId, artifactId, username);

        return CommentResponse.fromArtifactComment(comment);
    }

    /**
     * Get all comments for an artifact with threaded structure.
     *
     * @param artifactId The artifact UUID
     * @return List of top-level comments with nested replies
     */
    public List<CommentResponse> getComments(String artifactId) {
        // Get top-level comments
        List<ArtifactComment> topLevelComments = commentRepository.findTopLevelCommentsByArtifactId(artifactId);

        // Convert to response with recursive reply loading
        return topLevelComments.stream()
                .map(this::buildCommentTree)
                .collect(Collectors.toList());
    }

    /**
     * Recursively build comment tree with replies.
     */
    private CommentResponse buildCommentTree(ArtifactComment comment) {
        CommentResponse response = CommentResponse.fromArtifactComment(comment, false);

        // Load replies recursively
        List<ArtifactComment> replies = commentRepository.findRepliesByParentComment(comment);
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

        ArtifactComment comment = commentRepository.findByCommentId(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found: " + commentId));

        // Verify ownership
        if (!comment.getUser().getUsername().equals(username)) {
            throw new RuntimeException("User is not authorized to edit this comment");
        }

        // Update content
        comment.setContent(content);
        comment = commentRepository.save(comment);

        logger.info("Updated comment {}", commentId);

        return CommentResponse.fromArtifactComment(comment);
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

        ArtifactComment comment = commentRepository.findByCommentId(commentId)
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
     * Get comment count for an artifact.
     *
     * @param artifactId The artifact UUID
     * @return Total comment count (including replies)
     */
    public Long getCommentCount(String artifactId) {
        Long count = commentRepository.countByArtifactId(artifactId);
        return count != null ? count : 0L;
    }

    /**
     * Get comment counts for multiple artifacts (for efficient card display).
     *
     * @param artifactIds List of artifact UUIDs
     * @return Map of artifactId to comment count
     */
    public Map<String, Long> getBatchCommentCounts(List<String> artifactIds) {
        Map<String, Long> result = new HashMap<>();

        if (artifactIds == null || artifactIds.isEmpty()) {
            return result;
        }

        // Initialize all artifacts with zero count
        for (String artifactId : artifactIds) {
            result.put(artifactId, 0L);
        }

        // Batch query for comment counts
        List<Object[]> counts = commentRepository.countByArtifactIds(artifactIds);
        for (Object[] count : counts) {
            String artifactId = (String) count[0];
            Long commentCount = (Long) count[1];
            result.put(artifactId, commentCount);
        }

        return result;
    }
}

package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.LibraryItemComment;
import gov.nist.oscal.tools.api.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for a comment with optional nested replies.
 */
public class CommentResponse {

    private String commentId;
    private String content;
    private String username;
    private String userDisplayName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isEdited;
    private String parentCommentId;
    private List<CommentResponse> replies;
    private Integer replyCount;

    // Constructors
    public CommentResponse() {
        this.replies = new ArrayList<>();
    }

    /**
     * Create a CommentResponse from an entity without loading replies.
     */
    public static CommentResponse fromEntity(LibraryItemComment comment) {
        return fromEntity(comment, false);
    }

    /**
     * Create a CommentResponse from an entity with optional recursive reply loading.
     */
    public static CommentResponse fromEntity(LibraryItemComment comment, boolean loadReplies) {
        CommentResponse response = new CommentResponse();
        response.setCommentId(comment.getCommentId());
        response.setContent(comment.getContent());
        response.setUsername(comment.getUser().getUsername());
        response.setUserDisplayName(getUserDisplayName(comment.getUser()));
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        response.setIsEdited(comment.isEdited());

        if (comment.getParentComment() != null) {
            response.setParentCommentId(comment.getParentComment().getCommentId());
        }

        if (loadReplies && comment.getReplies() != null) {
            List<CommentResponse> replyResponses = comment.getReplies().stream()
                    .filter(r -> !r.getDeleted())
                    .map(r -> fromEntity(r, true))
                    .collect(Collectors.toList());
            response.setReplies(replyResponses);
            response.setReplyCount(replyResponses.size());
        } else {
            response.setReplies(new ArrayList<>());
            response.setReplyCount(0);
        }

        return response;
    }

    private static String getUserDisplayName(User user) {
        if (user.getFirstName() != null && user.getLastName() != null) {
            return user.getFirstName() + " " + user.getLastName();
        } else if (user.getFirstName() != null) {
            return user.getFirstName();
        }
        return user.getUsername();
    }

    // Getters and Setters
    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsEdited() {
        return isEdited;
    }

    public void setIsEdited(Boolean isEdited) {
        this.isEdited = isEdited;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public List<CommentResponse> getReplies() {
        return replies;
    }

    public void setReplies(List<CommentResponse> replies) {
        this.replies = replies;
    }

    public Integer getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }
}

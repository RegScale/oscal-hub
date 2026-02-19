package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a comment.
 */
public class CommentRequest {

    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Comment must not exceed 5000 characters")
    private String content;

    private String parentCommentId; // Optional - for replies

    // Constructors
    public CommentRequest() {
    }

    public CommentRequest(String content) {
        this.content = content;
    }

    public CommentRequest(String content, String parentCommentId) {
        this.content = content;
        this.parentCommentId = parentCommentId;
    }

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
    }
}

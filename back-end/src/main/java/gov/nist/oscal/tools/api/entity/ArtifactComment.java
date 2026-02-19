package gov.nist.oscal.tools.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a comment on an artifact.
 * Supports threaded replies via self-referential relationship.
 * Uses soft delete to preserve thread structure.
 */
@Entity
@Table(name = "artifact_comments")
public class ArtifactComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false, unique = true, length = 100)
    private String commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artifact_id", nullable = false)
    private Artifact artifact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private ArtifactComment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @JsonIgnore
    private List<ArtifactComment> replies = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean deleted = false;

    // Constructors
    public ArtifactComment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ArtifactComment(String commentId, Artifact artifact, User user, String content) {
        this();
        this.commentId = commentId;
        this.artifact = artifact;
        this.user = user;
        this.content = content;
    }

    public ArtifactComment(String commentId, Artifact artifact, User user,
                           String content, ArtifactComment parentComment) {
        this(commentId, artifact, user, content);
        this.parentComment = parentComment;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public void addReply(ArtifactComment reply) {
        replies.add(reply);
        reply.setParentComment(this);
    }

    public void removeReply(ArtifactComment reply) {
        replies.remove(reply);
        reply.setParentComment(null);
    }

    public boolean isEdited() {
        return !createdAt.equals(updatedAt);
    }

    public boolean isTopLevel() {
        return parentComment == null;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ArtifactComment getParentComment() {
        return parentComment;
    }

    public void setParentComment(ArtifactComment parentComment) {
        this.parentComment = parentComment;
    }

    public List<ArtifactComment> getReplies() {
        return replies;
    }

    public void setReplies(List<ArtifactComment> replies) {
        this.replies = replies;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
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

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}

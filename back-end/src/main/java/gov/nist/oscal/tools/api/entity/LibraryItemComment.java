package gov.nist.oscal.tools.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a comment on a library item.
 * Supports threaded replies via self-referential relationship.
 * Uses soft delete to preserve thread structure.
 */
@Entity
@Table(name = "library_item_comments")
public class LibraryItemComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false, unique = true, length = 100)
    private String commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "library_item_id", nullable = false)
    private LibraryItem libraryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private LibraryItemComment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @JsonIgnore
    private List<LibraryItemComment> replies = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean deleted = false;

    // Constructors
    public LibraryItemComment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public LibraryItemComment(String commentId, LibraryItem libraryItem, User user, String content) {
        this();
        this.commentId = commentId;
        this.libraryItem = libraryItem;
        this.user = user;
        this.content = content;
    }

    public LibraryItemComment(String commentId, LibraryItem libraryItem, User user,
                              String content, LibraryItemComment parentComment) {
        this(commentId, libraryItem, user, content);
        this.parentComment = parentComment;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public void addReply(LibraryItemComment reply) {
        replies.add(reply);
        reply.setParentComment(this);
    }

    public void removeReply(LibraryItemComment reply) {
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

    public LibraryItem getLibraryItem() {
        return libraryItem;
    }

    public void setLibraryItem(LibraryItem libraryItem) {
        this.libraryItem = libraryItem;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LibraryItemComment getParentComment() {
        return parentComment;
    }

    public void setParentComment(LibraryItemComment parentComment) {
        this.parentComment = parentComment;
    }

    public List<LibraryItemComment> getReplies() {
        return replies;
    }

    public void setReplies(List<LibraryItemComment> replies) {
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

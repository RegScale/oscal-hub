package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a reusable OSCAL element that can be used in multiple component definitions
 * Stores JSON snippets for roles, parties, links, back matter, and responsible parties
 */
@Entity
@Table(name = "reusable_elements")
public class ReusableElement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ElementType type;

    @Column(nullable = false)
    private String name; // User-friendly display name

    @Column(columnDefinition = "TEXT", nullable = false)
    private String jsonContent; // The actual JSON snippet

    @Column(columnDefinition = "TEXT")
    private String description; // Optional description of the element

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_shared")
    private boolean isShared = false; // Future: allow sharing elements with other users

    @Column(name = "use_count")
    private Integer useCount = 0; // Track how many times this element has been used

    /**
     * Types of reusable elements in OSCAL component definitions
     */
    public enum ElementType {
        ROLE,                // Roles in metadata
        PARTY,               // Parties (organizations/individuals) in metadata
        LINK,                // Links in metadata or other locations
        BACK_MATTER,         // Back matter resources and citations
        RESPONSIBLE_PARTY    // Responsible parties in metadata
    }

    // Constructors
    public ReusableElement() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.useCount = 0;
    }

    public ReusableElement(ElementType type, String name, String jsonContent, User createdBy) {
        this();
        this.type = type;
        this.name = name;
        this.jsonContent = jsonContent;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ElementType getType() {
        return type;
    }

    public void setType(ElementType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJsonContent() {
        return jsonContent;
    }

    public void setJsonContent(String jsonContent) {
        this.jsonContent = jsonContent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
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

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean shared) {
        isShared = shared;
    }

    public Integer getUseCount() {
        return useCount;
    }

    public void setUseCount(Integer useCount) {
        this.useCount = useCount;
    }

    public void incrementUseCount() {
        this.useCount++;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

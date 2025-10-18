package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a shared OSCAL document in the library
 * Accessible by all authenticated users
 */
@Entity
@Table(name = "library_items")
public class LibraryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String itemId; // UUID for external reference

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "oscal_type", nullable = false, length = 50)
    private String oscalType; // catalog, profile, ssp, etc.

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne
    @JoinColumn(name = "current_version_id")
    private LibraryVersion currentVersion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "library_item_tags",
        joinColumns = @JoinColumn(name = "library_item_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<LibraryTag> tags = new HashSet<>();

    @OneToMany(mappedBy = "libraryItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<LibraryVersion> versions = new HashSet<>();

    @Column(nullable = false)
    private Long downloadCount = 0L;

    @Column(nullable = false)
    private Long viewCount = 0L;

    // Constructors
    public LibraryItem() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public LibraryItem(String itemId, String title, String description, String oscalType, User createdBy) {
        this();
        this.itemId = itemId;
        this.title = title;
        this.description = description;
        this.oscalType = oscalType;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public String getOscalType() {
        return oscalType;
    }

    public void setOscalType(String oscalType) {
        this.oscalType = oscalType;
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

    public LibraryVersion getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(LibraryVersion currentVersion) {
        this.currentVersion = currentVersion;
        this.updatedAt = LocalDateTime.now();
    }

    public Set<LibraryTag> getTags() {
        return tags;
    }

    public void setTags(Set<LibraryTag> tags) {
        this.tags = tags;
    }

    public void addTag(LibraryTag tag) {
        this.tags.add(tag);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeTag(LibraryTag tag) {
        this.tags.remove(tag);
        this.updatedAt = LocalDateTime.now();
    }

    public Set<LibraryVersion> getVersions() {
        return versions;
    }

    public void setVersions(Set<LibraryVersion> versions) {
        this.versions = versions;
    }

    public Long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }
}

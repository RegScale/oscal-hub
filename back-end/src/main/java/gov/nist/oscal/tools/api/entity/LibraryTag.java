package gov.nist.oscal.tools.api.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a tag/keyword that can be associated with library items
 * Tags are reusable across multiple library items
 */
@Entity
@Table(name = "library_tags")
public class LibraryTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // Tag name (e.g., "NIST800-53", "FedRAMP", "encryption")

    @Column(length = 500)
    private String description;

    @ManyToMany(mappedBy = "tags")
    private Set<LibraryItem> libraryItems = new HashSet<>();

    // Constructors
    public LibraryTag() {
    }

    public LibraryTag(String name) {
        this.name = name.toLowerCase().trim(); // Normalize tag names
    }

    public LibraryTag(String name, String description) {
        this(name);
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.toLowerCase().trim(); // Normalize tag names
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<LibraryItem> getLibraryItems() {
        return libraryItems;
    }

    public void setLibraryItems(Set<LibraryItem> libraryItems) {
        this.libraryItems = libraryItems;
    }

    /**
     * Get the usage count (number of library items using this tag)
     */
    public int getUsageCount() {
        return libraryItems != null ? libraryItems.size() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryTag that = (LibraryTag) o;
        return name != null && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}

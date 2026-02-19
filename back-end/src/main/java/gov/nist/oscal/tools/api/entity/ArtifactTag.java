package gov.nist.oscal.tools.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a tag/keyword that can be associated with artifacts.
 * Tags are reusable across multiple artifacts.
 */
@Entity
@Table(name = "artifact_tags")
public class ArtifactTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name; // Tag name (e.g., "compliance", "policy", "template")

    @Column(length = 500)
    private String description;

    @ManyToMany(mappedBy = "tags")
    @JsonIgnore
    private Set<Artifact> artifacts = new HashSet<>();

    // Constructors
    public ArtifactTag() {
    }

    public ArtifactTag(String name) {
        this.name = name.toLowerCase().trim(); // Normalize tag names
    }

    public ArtifactTag(String name, String description) {
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

    public Set<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(Set<Artifact> artifacts) {
        this.artifacts = artifacts;
    }

    /**
     * Get the usage count (number of artifacts using this tag)
     */
    public int getUsageCount() {
        return artifacts != null ? artifacts.size() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArtifactTag that = (ArtifactTag) o;
        return name != null && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}

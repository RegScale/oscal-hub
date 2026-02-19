package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Artifact;
import gov.nist.oscal.tools.api.entity.ArtifactVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtifactVersionRepository extends JpaRepository<ArtifactVersion, Long> {

    Optional<ArtifactVersion> findByVersionId(String versionId);

    List<ArtifactVersion> findByArtifact(Artifact artifact);

    List<ArtifactVersion> findByArtifactOrderByVersionNumberDesc(Artifact artifact);

    Optional<ArtifactVersion> findByArtifactAndVersionNumber(Artifact artifact, Integer versionNumber);

    // Get latest version for an artifact
    @Query("SELECT av FROM ArtifactVersion av WHERE av.artifact = :artifact ORDER BY av.versionNumber DESC")
    Optional<ArtifactVersion> findLatestVersion(@Param("artifact") Artifact artifact);

    // Get next version number for an artifact
    @Query("SELECT COALESCE(MAX(av.versionNumber), 0) + 1 FROM ArtifactVersion av WHERE av.artifact = :artifact")
    Integer getNextVersionNumber(@Param("artifact") Artifact artifact);

    // Count versions for an artifact
    @Query("SELECT COUNT(av) FROM ArtifactVersion av WHERE av.artifact = :artifact")
    Long countVersions(@Param("artifact") Artifact artifact);

    // Get version history by artifact ID
    @Query("SELECT av FROM ArtifactVersion av WHERE av.artifact.artifactId = :artifactId ORDER BY av.versionNumber DESC")
    List<ArtifactVersion> findByArtifactIdOrderByVersionNumberDesc(@Param("artifactId") String artifactId);

    // Count total versions (for analytics)
    @Query("SELECT COUNT(av) FROM ArtifactVersion av")
    Long countTotalVersions();
}

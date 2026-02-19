package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.ArtifactTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtifactTagRepository extends JpaRepository<ArtifactTag, Long> {

    Optional<ArtifactTag> findByName(String name);

    boolean existsByName(String name);

    List<ArtifactTag> findByNameContainingIgnoreCase(String name);

    // Get most popular tags (by usage count) with count
    @Query("SELECT t.id, t.name, t.description, COUNT(a.id) as usageCount " +
           "FROM ArtifactTag t LEFT JOIN t.artifacts a " +
           "GROUP BY t.id, t.name, t.description " +
           "ORDER BY COUNT(a.id) DESC")
    List<Object[]> findMostPopularWithCounts();

    // Get all tags sorted by name with counts
    @Query("SELECT t.id, t.name, t.description, COUNT(a.id) as usageCount " +
           "FROM ArtifactTag t LEFT JOIN t.artifacts a " +
           "GROUP BY t.id, t.name, t.description " +
           "ORDER BY t.name ASC")
    List<Object[]> findAllWithCountsOrderByNameAsc();
}

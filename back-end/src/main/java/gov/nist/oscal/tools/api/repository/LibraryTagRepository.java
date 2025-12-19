package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.LibraryTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LibraryTagRepository extends JpaRepository<LibraryTag, Long> {

    Optional<LibraryTag> findByName(String name);

    boolean existsByName(String name);

    List<LibraryTag> findByNameContainingIgnoreCase(String name);

    // Get most popular tags (by usage count) with count
    @Query("SELECT t.id, t.name, t.description, COUNT(lit.id) as usageCount " +
           "FROM LibraryTag t LEFT JOIN t.libraryItems lit " +
           "GROUP BY t.id, t.name, t.description " +
           "ORDER BY COUNT(lit.id) DESC")
    List<Object[]> findMostPopularWithCounts();

    // Get all tags sorted by name with counts
    @Query("SELECT t.id, t.name, t.description, COUNT(lit.id) as usageCount " +
           "FROM LibraryTag t LEFT JOIN t.libraryItems lit " +
           "GROUP BY t.id, t.name, t.description " +
           "ORDER BY t.name ASC")
    List<Object[]> findAllWithCountsOrderByNameAsc();
}

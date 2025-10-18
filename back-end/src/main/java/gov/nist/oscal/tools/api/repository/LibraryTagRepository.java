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

    // Get most popular tags (by usage count)
    @Query("SELECT t FROM LibraryTag t ORDER BY SIZE(t.libraryItems) DESC")
    List<LibraryTag> findMostPopular();

    // Get all tags sorted by name
    List<LibraryTag> findAllByOrderByNameAsc();
}

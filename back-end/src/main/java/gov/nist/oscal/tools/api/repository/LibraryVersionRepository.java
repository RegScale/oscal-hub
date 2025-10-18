package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LibraryVersionRepository extends JpaRepository<LibraryVersion, Long> {

    Optional<LibraryVersion> findByVersionId(String versionId);

    List<LibraryVersion> findByLibraryItem(LibraryItem libraryItem);

    List<LibraryVersion> findByLibraryItemOrderByVersionNumberDesc(LibraryItem libraryItem);

    Optional<LibraryVersion> findByLibraryItemAndVersionNumber(LibraryItem libraryItem, Integer versionNumber);

    // Get latest version for a library item
    @Query("SELECT lv FROM LibraryVersion lv WHERE lv.libraryItem = :libraryItem ORDER BY lv.versionNumber DESC")
    Optional<LibraryVersion> findLatestVersion(@Param("libraryItem") LibraryItem libraryItem);

    // Get next version number for a library item
    @Query("SELECT COALESCE(MAX(lv.versionNumber), 0) + 1 FROM LibraryVersion lv WHERE lv.libraryItem = :libraryItem")
    Integer getNextVersionNumber(@Param("libraryItem") LibraryItem libraryItem);

    // Count versions for a library item
    @Query("SELECT COUNT(lv) FROM LibraryVersion lv WHERE lv.libraryItem = :libraryItem")
    Long countVersions(@Param("libraryItem") LibraryItem libraryItem);
}

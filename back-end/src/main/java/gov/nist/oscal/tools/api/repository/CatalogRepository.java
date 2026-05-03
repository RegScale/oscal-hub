package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Catalog;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogRepository extends JpaRepository<Catalog, Long> {

    List<Catalog> findByCreatedBy(User user);

    Optional<Catalog> findByOscalUuid(String oscalUuid);

    Optional<Catalog> findByStoragePath(String storagePath);

    @Query("SELECT c FROM Catalog c WHERE c.createdBy = :user ORDER BY c.createdAt DESC")
    List<Catalog> findByCreatedByOrderByCreatedAtDesc(@Param("user") User user);

    @Query("SELECT c FROM Catalog c WHERE c.createdBy = :user AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY c.createdAt DESC")
    List<Catalog> findByCreatedByAndSearch(@Param("user") User user,
                                           @Param("searchTerm") String searchTerm);
}

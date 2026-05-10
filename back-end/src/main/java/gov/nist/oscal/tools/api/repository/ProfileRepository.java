package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Profile;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    List<Profile> findByCreatedBy(User user);

    Optional<Profile> findByOscalUuid(String oscalUuid);

    Optional<Profile> findByStoragePath(String storagePath);

    @Query("SELECT p FROM Profile p WHERE p.createdBy = :user ORDER BY p.createdAt DESC")
    List<Profile> findByCreatedByOrderByCreatedAtDesc(@Param("user") User user);

    @Query("SELECT p FROM Profile p WHERE p.createdBy = :user AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<Profile> findByCreatedByAndSearch(@Param("user") User user,
                                           @Param("searchTerm") String searchTerm);
}

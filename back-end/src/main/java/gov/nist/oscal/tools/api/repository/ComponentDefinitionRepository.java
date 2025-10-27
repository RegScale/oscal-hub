package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.ComponentDefinition;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComponentDefinitionRepository extends JpaRepository<ComponentDefinition, Long> {

    /**
     * Find all component definitions created by a specific user
     */
    List<ComponentDefinition> findByCreatedBy(User user);

    /**
     * Find component definition by OSCAL UUID
     */
    Optional<ComponentDefinition> findByOscalUuid(String oscalUuid);

    /**
     * Find component definitions by title (case-insensitive partial match)
     */
    List<ComponentDefinition> findByTitleContainingIgnoreCase(String title);

    /**
     * Find component definitions by storage path
     */
    Optional<ComponentDefinition> findByStoragePath(String storagePath);

    /**
     * Find recently created component definitions
     */
    @Query("SELECT c FROM ComponentDefinition c ORDER BY c.createdAt DESC")
    List<ComponentDefinition> findRecentlyCreated();

    /**
     * Find component definitions created by a specific user, ordered by most recent
     */
    @Query("SELECT c FROM ComponentDefinition c WHERE c.createdBy = :user ORDER BY c.createdAt DESC")
    List<ComponentDefinition> findByCreatedByOrderByCreatedAtDesc(@Param("user") User user);

    /**
     * Search component definitions by title or description
     */
    @Query("SELECT c FROM ComponentDefinition c WHERE " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<ComponentDefinition> searchByTitleOrDescription(@Param("searchTerm") String searchTerm);

    /**
     * Find component definitions by user with optional search filter
     */
    @Query("SELECT c FROM ComponentDefinition c WHERE c.createdBy = :user AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY c.createdAt DESC")
    List<ComponentDefinition> findByCreatedByAndSearch(@Param("user") User user,
                                                        @Param("searchTerm") String searchTerm);
}

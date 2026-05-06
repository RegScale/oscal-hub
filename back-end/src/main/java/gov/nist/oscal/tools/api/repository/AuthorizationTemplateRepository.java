package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorizationTemplateRepository extends JpaRepository<AuthorizationTemplate, Long> {

    List<AuthorizationTemplate> findByCreatedBy(User user);

    List<AuthorizationTemplate> findByNameContainingIgnoreCase(String name);

    // Get recently updated templates
    @Query("SELECT t FROM AuthorizationTemplate t ORDER BY t.lastUpdatedAt DESC")
    List<AuthorizationTemplate> findRecentlyUpdated();

    // Get templates created by a specific user
    List<AuthorizationTemplate> findByCreatedByOrderByCreatedAtDesc(User user);

    // --- Org-scoped queries ---

    List<AuthorizationTemplate> findByOrganization(Organization organization);

    Optional<AuthorizationTemplate> findByIdAndOrganization(Long id, Organization organization);

    List<AuthorizationTemplate> findByOrganizationOrderByLastUpdatedAtDesc(
            Organization organization);

    @Query("SELECT t FROM AuthorizationTemplate t WHERE t.organization = :organization AND " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<AuthorizationTemplate> searchByNameAndOrganization(
            @Param("searchTerm") String searchTerm,
            @Param("organization") Organization organization);
}

package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorizationRepository extends JpaRepository<Authorization, Long> {

    List<Authorization> findByAuthorizedBy(User user);

    List<Authorization> findBySspItemId(String sspItemId);

    List<Authorization> findByTemplate(AuthorizationTemplate template);

    List<Authorization> findByNameContainingIgnoreCase(String name);

    // Get recently authorized systems
    @Query("SELECT a FROM Authorization a ORDER BY a.authorizedAt DESC")
    List<Authorization> findRecentlyAuthorized();

    // Find authorizations by SSP and template
    List<Authorization> findBySspItemIdAndTemplate(String sspItemId, AuthorizationTemplate template);

    // Search by name or SSP item ID
    @Query("SELECT a FROM Authorization a WHERE " +
           "LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.sspItemId) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Authorization> searchByNameOrSspItemId(@Param("searchTerm") String searchTerm);
}

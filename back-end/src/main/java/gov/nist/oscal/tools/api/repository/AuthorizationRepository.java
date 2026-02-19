package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

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

    // ==================== Performance Optimized Queries ====================
    // These queries use JOIN FETCH to avoid N+1 problems with conditions and variables

    // Find authorization by ID with conditions and variables eagerly loaded
    @Query("SELECT DISTINCT a FROM Authorization a " +
           "LEFT JOIN FETCH a.conditions " +
           "LEFT JOIN FETCH a.variableValues " +
           "WHERE a.id = :id")
    Optional<Authorization> findByIdWithConditionsAndVariables(@Param("id") Long id);

    // Find all authorizations with conditions loaded
    @Query("SELECT DISTINCT a FROM Authorization a LEFT JOIN FETCH a.conditions ORDER BY a.authorizedAt DESC")
    List<Authorization> findAllWithConditions();

    // Find authorizations by user with conditions
    @Query("SELECT DISTINCT a FROM Authorization a LEFT JOIN FETCH a.conditions WHERE a.authorizedBy = :user ORDER BY a.authorizedAt DESC")
    List<Authorization> findByAuthorizedByWithConditions(@Param("user") User user);

    // Find authorizations by SSP with conditions
    @Query("SELECT DISTINCT a FROM Authorization a LEFT JOIN FETCH a.conditions WHERE a.sspItemId = :sspItemId ORDER BY a.authorizedAt DESC")
    List<Authorization> findBySspItemIdWithConditions(@Param("sspItemId") String sspItemId);

    // Get recently authorized with conditions
    @Query("SELECT DISTINCT a FROM Authorization a LEFT JOIN FETCH a.conditions ORDER BY a.authorizedAt DESC")
    List<Authorization> findRecentlyAuthorizedWithConditions();

    // ==================== Paginated Queries ====================
    // These queries support pagination for large datasets

    // Paginated: Find all authorizations
    @Query(value = "SELECT a FROM Authorization a",
           countQuery = "SELECT COUNT(a) FROM Authorization a")
    Page<Authorization> findAllPaged(Pageable pageable);

    // Paginated: Find by user
    @Query(value = "SELECT a FROM Authorization a WHERE a.authorizedBy = :user",
           countQuery = "SELECT COUNT(a) FROM Authorization a WHERE a.authorizedBy = :user")
    Page<Authorization> findByAuthorizedByPaged(@Param("user") User user, Pageable pageable);

    // Paginated: Find by SSP
    @Query(value = "SELECT a FROM Authorization a WHERE a.sspItemId = :sspItemId",
           countQuery = "SELECT COUNT(a) FROM Authorization a WHERE a.sspItemId = :sspItemId")
    Page<Authorization> findBySspItemIdPaged(@Param("sspItemId") String sspItemId, Pageable pageable);

    // Paginated: Search
    @Query(value = "SELECT a FROM Authorization a WHERE " +
           "(:searchTerm IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.sspItemId) LIKE LOWER(CONCAT('%', :searchTerm, '%')))",
           countQuery = "SELECT COUNT(a) FROM Authorization a WHERE " +
           "(:searchTerm IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.sspItemId) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Authorization> searchByNameOrSspItemIdPaged(@Param("searchTerm") String searchTerm, Pageable pageable);
}

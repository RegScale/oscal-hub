package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {

    List<OrganizationMembership> findByUser(User user);

    List<OrganizationMembership> findByUserAndStatus(User user, MembershipStatus status);

    List<OrganizationMembership> findByOrganization(Organization organization);

    List<OrganizationMembership> findByOrganizationAndStatus(Organization organization, MembershipStatus status);

    Optional<OrganizationMembership> findByUserAndOrganization(User user, Organization organization);

    @Query("SELECT m FROM OrganizationMembership m WHERE m.user.id = :userId AND m.organization.id = :orgId")
    Optional<OrganizationMembership> findByUserIdAndOrganizationId(
        @Param("userId") Long userId,
        @Param("orgId") Long orgId
    );

    @Query("SELECT m FROM OrganizationMembership m WHERE m.organization.id = :orgId AND m.role = :role AND m.status = :status")
    List<OrganizationMembership> findByOrganizationIdAndRoleAndStatus(
        @Param("orgId") Long orgId,
        @Param("role") OrganizationRole role,
        @Param("status") MembershipStatus status
    );

    @Query("SELECT m FROM OrganizationMembership m WHERE m.user.id = :userId")
    List<OrganizationMembership> findByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM OrganizationMembership m JOIN FETCH m.user WHERE m.organization = :organization")
    List<OrganizationMembership> findByOrganizationWithUser(@Param("organization") Organization organization);

    @Query("SELECT m FROM OrganizationMembership m JOIN FETCH m.user WHERE m.organization = :organization AND m.status = :status")
    List<OrganizationMembership> findByOrganizationAndStatusWithUser(
        @Param("organization") Organization organization,
        @Param("status") MembershipStatus status
    );

    boolean existsByUserAndOrganization(User user, Organization organization);

    @Query("SELECT COUNT(m) FROM OrganizationMembership m WHERE m.organization.id = :orgId")
    int countByOrganizationId(@Param("orgId") Long orgId);

    /**
     * Get the first organization name for a user (for analytics display)
     */
    @Query("SELECT m.organization.name FROM OrganizationMembership m WHERE m.user.id = :userId ORDER BY m.joinedAt ASC")
    String findFirstOrganizationNameByUserId(@Param("userId") Long userId);

    // ==================== Batch Query Optimizations ====================

    /**
     * Batch count members by organization - returns all org member counts in single query
     * Prevents N+1 queries when loading organization summaries
     * @return List of Object[] where [0] = organizationId (Long), [1] = memberCount (Long)
     */
    @Query("SELECT m.organization.id, COUNT(m) FROM OrganizationMembership m GROUP BY m.organization.id")
    List<Object[]> countMembersByOrganization();

    /**
     * Batch count members by organization with status filter
     * @return List of Object[] where [0] = organizationId (Long), [1] = memberCount (Long)
     */
    @Query("SELECT m.organization.id, COUNT(m) FROM OrganizationMembership m WHERE m.status = :status GROUP BY m.organization.id")
    List<Object[]> countMembersByOrganizationAndStatus(@Param("status") MembershipStatus status);

    /**
     * Count members for multiple organizations at once
     * @param orgIds List of organization IDs
     * @return List of Object[] where [0] = organizationId (Long), [1] = memberCount (Long)
     */
    @Query("SELECT m.organization.id, COUNT(m) FROM OrganizationMembership m WHERE m.organization.id IN :orgIds GROUP BY m.organization.id")
    List<Object[]> countMembersByOrganizationIds(@Param("orgIds") List<Long> orgIds);
}

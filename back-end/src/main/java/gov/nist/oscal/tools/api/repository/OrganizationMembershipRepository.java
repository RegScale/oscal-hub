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

    boolean existsByUserAndOrganization(User user, Organization organization);
}

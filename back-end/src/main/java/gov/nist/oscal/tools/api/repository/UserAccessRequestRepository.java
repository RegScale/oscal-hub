package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.entity.UserAccessRequest;
import gov.nist.oscal.tools.api.entity.UserAccessRequest.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAccessRequestRepository extends JpaRepository<UserAccessRequest, Long> {

    List<UserAccessRequest> findByOrganizationAndStatus(Organization organization, RequestStatus status);

    List<UserAccessRequest> findByOrganization(Organization organization);

    List<UserAccessRequest> findByUserAndStatus(User user, RequestStatus status);

    @Query("SELECT r FROM UserAccessRequest r WHERE r.organization.id = :orgId AND r.status = 'PENDING'")
    List<UserAccessRequest> findPendingByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(r) FROM UserAccessRequest r WHERE r.organization.id = :orgId AND r.status = 'PENDING'")
    long countPendingByOrganizationId(@Param("orgId") Long orgId);

    @Query("SELECT r FROM UserAccessRequest r WHERE r.email = :email AND r.organization.id = :orgId AND r.status = 'PENDING'")
    List<UserAccessRequest> findPendingByEmailAndOrganization(
        @Param("email") String email,
        @Param("orgId") Long orgId
    );
}

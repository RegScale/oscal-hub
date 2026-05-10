package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationGrant;
import gov.nist.oscal.tools.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorizationGrantRepository extends JpaRepository<AuthorizationGrant, Long> {

    List<AuthorizationGrant> findByAuthorization(Authorization authorization);

    Optional<AuthorizationGrant> findByAuthorizationAndUser(Authorization authorization, User user);

    @Query("SELECT g FROM AuthorizationGrant g " +
           "WHERE g.user = :user AND g.authorization.organization.id = :organizationId")
    List<AuthorizationGrant> findByUserInOrganization(
            @Param("user") User user,
            @Param("organizationId") Long organizationId);

    void deleteByAuthorizationAndUser(Authorization authorization, User user);
}

package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Invitation;
import gov.nist.oscal.tools.api.entity.Invitation.Status;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    /**
     * organization and invitedBy are LAZY and get serialized into
     * InvitationResponse outside the service transaction (OSIV is off), so
     * they must be fetch-joined here or mapping throws LazyInitializationException.
     */
    @Query("SELECT i FROM Invitation i JOIN FETCH i.organization JOIN FETCH i.invitedBy WHERE i.token = :token")
    Optional<Invitation> findByToken(@Param("token") String token);

    @Query("SELECT i FROM Invitation i JOIN FETCH i.organization JOIN FETCH i.invitedBy "
        + "WHERE i.organization.id = :organizationId AND i.status = :status")
    List<Invitation> findByOrganizationIdAndStatus(@Param("organizationId") Long organizationId,
                                                   @Param("status") Status status);

    List<Invitation> findByEmailAndOrganizationIdAndStatus(String email, Long organizationId, Status status);
}

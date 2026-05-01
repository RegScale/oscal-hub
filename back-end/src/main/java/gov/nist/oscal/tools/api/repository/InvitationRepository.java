package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Invitation;
import gov.nist.oscal.tools.api.entity.Invitation.Status;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByToken(String token);
    List<Invitation> findByOrganizationIdAndStatus(Long organizationId, Status status);
    List<Invitation> findByEmailAndOrganizationIdAndStatus(String email, Long organizationId, Status status);
}

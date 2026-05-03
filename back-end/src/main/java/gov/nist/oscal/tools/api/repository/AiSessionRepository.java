package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.AiSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiSessionRepository extends JpaRepository<AiSession, UUID> {
}

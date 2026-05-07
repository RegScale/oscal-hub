package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.ConMonReconciliation;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConMonReconciliationRepository extends JpaRepository<ConMonReconciliation, Long> {
    Optional<ConMonReconciliation> findBySnapshot(ConMonSnapshot snapshot);
}

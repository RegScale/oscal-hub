package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConMonSnapshotRepository extends JpaRepository<ConMonSnapshot, Long> {
    List<ConMonSnapshot> findByAuthorizationOrderByUploadedAtDesc(Authorization authorization);
    Optional<ConMonSnapshot> findByIdAndAuthorization(Long id, Authorization authorization);
    Optional<ConMonSnapshot> findFirstByAuthorizationOrderByUploadedAtDesc(Authorization authorization);
}

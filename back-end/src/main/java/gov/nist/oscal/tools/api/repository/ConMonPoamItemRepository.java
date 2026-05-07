package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConMonPoamItemRepository extends JpaRepository<ConMonPoamItem, Long> {
    List<ConMonPoamItem> findBySnapshot(ConMonSnapshot snapshot);

    @Query("SELECT i FROM ConMonPoamItem i WHERE i.snapshot = :snapshot " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:severity IS NULL OR i.severity = :severity) " +
           "AND (:q IS NULL OR LOWER(i.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "                OR LOWER(i.externalId) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<ConMonPoamItem> search(
            @Param("snapshot") ConMonSnapshot snapshot,
            @Param("status") ConMonItemStatus status,
            @Param("severity") String severity,
            @Param("q") String q,
            Pageable pageable);
}

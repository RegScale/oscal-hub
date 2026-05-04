package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.AiSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface AiSessionRepository extends JpaRepository<AiSession, UUID> {

    List<AiSession> findByOrganizationIdOrderByStartedAtDesc(Long organizationId, Pageable pageable);

    @Query("SELECT new map(" +
           "COUNT(s) as count, SUM(s.tokensIn) as ti, SUM(s.tokensOut) as to_, SUM(s.costUsdMicros) as cost) " +
           "FROM AiSession s WHERE s.organizationId = :orgId")
    Map<String, Object> sumForOrg(@Param("orgId") Long orgId);

    @Query("SELECT new map(" +
           "COUNT(s) as count, SUM(s.costUsdMicros) as cost) " +
           "FROM AiSession s WHERE s.organizationId = :orgId AND s.startedAt >= :since")
    Map<String, Object> sumForOrgSince(@Param("orgId") Long orgId, @Param("since") LocalDateTime since);
}

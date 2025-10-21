package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConditionOfApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConditionOfApprovalRepository extends JpaRepository<ConditionOfApproval, Long> {

    List<ConditionOfApproval> findByAuthorization(Authorization authorization);

    List<ConditionOfApproval> findByAuthorizationId(Long authorizationId);

    List<ConditionOfApproval> findByConditionType(ConditionOfApproval.ConditionType conditionType);

    List<ConditionOfApproval> findByAuthorizationIdAndConditionType(Long authorizationId, ConditionOfApproval.ConditionType conditionType);
}

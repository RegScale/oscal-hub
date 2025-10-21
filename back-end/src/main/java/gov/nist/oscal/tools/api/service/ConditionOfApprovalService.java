package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConditionOfApproval;
import gov.nist.oscal.tools.api.repository.AuthorizationRepository;
import gov.nist.oscal.tools.api.repository.ConditionOfApprovalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service for managing conditions of approval
 * Provides CRUD operations for authorization conditions
 */
@Service
public class ConditionOfApprovalService {

    private static final Logger logger = LoggerFactory.getLogger(ConditionOfApprovalService.class);

    @Autowired
    private ConditionOfApprovalRepository conditionRepository;

    @Autowired
    private AuthorizationRepository authorizationRepository;

    /**
     * Create a new condition of approval
     */
    @Transactional
    public ConditionOfApproval createCondition(Long authorizationId, String condition,
                                              ConditionOfApproval.ConditionType conditionType,
                                              LocalDate dueDate) {
        logger.info("Creating new condition for authorization: {}", authorizationId);

        Authorization authorization = authorizationRepository.findById(authorizationId)
                .orElseThrow(() -> new RuntimeException("Authorization not found: " + authorizationId));

        ConditionOfApproval conditionOfApproval = new ConditionOfApproval();
        conditionOfApproval.setAuthorization(authorization);
        conditionOfApproval.setCondition(condition);
        conditionOfApproval.setConditionType(conditionType);
        conditionOfApproval.setDueDate(dueDate);

        return conditionRepository.save(conditionOfApproval);
    }

    /**
     * Update an existing condition
     */
    @Transactional
    public ConditionOfApproval updateCondition(Long id, String condition,
                                              ConditionOfApproval.ConditionType conditionType,
                                              LocalDate dueDate) {
        logger.info("Updating condition: {}", id);

        ConditionOfApproval conditionOfApproval = conditionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Condition not found: " + id));

        if (condition != null) {
            conditionOfApproval.setCondition(condition);
        }
        if (conditionType != null) {
            conditionOfApproval.setConditionType(conditionType);
        }
        conditionOfApproval.setDueDate(dueDate);

        return conditionRepository.save(conditionOfApproval);
    }

    /**
     * Get a condition by ID
     */
    public ConditionOfApproval getCondition(Long id) {
        return conditionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Condition not found: " + id));
    }

    /**
     * Get all conditions for an authorization
     */
    public List<ConditionOfApproval> getConditionsByAuthorization(Long authorizationId) {
        return conditionRepository.findByAuthorizationId(authorizationId);
    }

    /**
     * Get all conditions of a specific type for an authorization
     */
    public List<ConditionOfApproval> getConditionsByAuthorizationAndType(Long authorizationId,
                                                                         ConditionOfApproval.ConditionType type) {
        return conditionRepository.findByAuthorizationIdAndConditionType(authorizationId, type);
    }

    /**
     * Delete a condition
     */
    @Transactional
    public void deleteCondition(Long id) {
        logger.info("Deleting condition: {}", id);

        ConditionOfApproval condition = conditionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Condition not found: " + id));

        conditionRepository.delete(condition);
        logger.info("Deleted condition: {}", id);
    }

    /**
     * Delete all conditions for an authorization
     */
    @Transactional
    public void deleteConditionsByAuthorization(Long authorizationId) {
        logger.info("Deleting all conditions for authorization: {}", authorizationId);

        List<ConditionOfApproval> conditions = conditionRepository.findByAuthorizationId(authorizationId);
        conditionRepository.deleteAll(conditions);

        logger.info("Deleted {} conditions for authorization: {}", conditions.size(), authorizationId);
    }
}

package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.CustomValidationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomValidationRuleRepository extends JpaRepository<CustomValidationRule, Long> {

    /**
     * Find a custom rule by its unique rule ID
     */
    Optional<CustomValidationRule> findByRuleId(String ruleId);

    /**
     * Find all enabled custom rules
     */
    List<CustomValidationRule> findByEnabledTrue();

    /**
     * Find custom rules by category
     */
    List<CustomValidationRule> findByCategory(String category);

    /**
     * Find custom rules by severity
     */
    List<CustomValidationRule> findBySeverity(String severity);

    /**
     * Find custom rules by rule type
     */
    List<CustomValidationRule> findByRuleType(String ruleType);

    /**
     * Check if a rule ID already exists
     */
    boolean existsByRuleId(String ruleId);

    /**
     * Find enabled rules applicable to a specific model type
     */
    @Query("SELECT r FROM CustomValidationRule r WHERE r.enabled = true AND " +
           "(r.applicableModelTypes LIKE %?1% OR r.applicableModelTypes IS NULL)")
    List<CustomValidationRule> findEnabledRulesForModelType(String modelType);

    /**
     * Find custom rules by user
     */
    List<CustomValidationRule> findByUserIdOrderByCreatedDateDesc(Long userId);

    /**
     * Find a custom rule by ruleId and userId
     */
    Optional<CustomValidationRule> findByRuleIdAndUserId(String ruleId, Long userId);

    /**
     * Find enabled custom rules for a specific user
     */
    List<CustomValidationRule> findByUserIdAndEnabledTrue(Long userId);
}

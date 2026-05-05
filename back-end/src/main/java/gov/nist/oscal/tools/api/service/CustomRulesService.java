package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.CustomValidationRule;
import gov.nist.oscal.tools.api.model.CustomRuleRequest;
import gov.nist.oscal.tools.api.model.CustomRuleResponse;
import gov.nist.oscal.tools.api.repository.CustomValidationRuleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomRulesService {

    private static final Logger logger = LoggerFactory.getLogger(CustomRulesService.class);
    private final CustomValidationRuleRepository repository;
    private final MetapathConstraintService constraintService;

    @Autowired
    public CustomRulesService(CustomValidationRuleRepository repository,
                              MetapathConstraintService constraintService) {
        this.repository = repository;
        this.constraintService = constraintService;
    }

    private static Long ownerId(CustomValidationRule rule) {
        return rule == null || rule.getUser() == null ? null : rule.getUser().getId();
    }

    /**
     * Get all custom rules
     */
    public List<CustomRuleResponse> getAllCustomRules() {
        return repository.findAll().stream()
            .map(CustomRuleResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get a custom rule by ID
     */
    public Optional<CustomRuleResponse> getCustomRuleById(Long id) {
        return repository.findById(id)
            .map(CustomRuleResponse::fromEntity);
    }

    /**
     * Get a custom rule by rule ID
     */
    public Optional<CustomRuleResponse> getCustomRuleByRuleId(String ruleId) {
        return repository.findByRuleId(ruleId)
            .map(CustomRuleResponse::fromEntity);
    }

    /**
     * Get all enabled custom rules
     */
    public List<CustomRuleResponse> getEnabledCustomRules() {
        return repository.findByEnabledTrue().stream()
            .map(CustomRuleResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Create a new custom rule
     */
    @Transactional
    public CustomRuleResponse createCustomRule(CustomRuleRequest request) {
        // Check if rule ID already exists
        if (repository.existsByRuleId(request.getRuleId())) {
            throw new IllegalArgumentException("Rule ID already exists: " + request.getRuleId());
        }

        CustomValidationRule entity = new CustomValidationRule();
        updateEntityFromRequest(entity, request);

        CustomValidationRule saved = repository.save(entity);
        constraintService.evictForUser(ownerId(saved));
        logger.info("Created custom validation rule: {}", saved.getRuleId());

        return CustomRuleResponse.fromEntity(saved);
    }

    /**
     * Update an existing custom rule
     */
    @Transactional
    public CustomRuleResponse updateCustomRule(Long id, CustomRuleRequest request) {
        CustomValidationRule entity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Custom rule not found with ID: " + id));

        // Check if changing rule ID would conflict
        if (!entity.getRuleId().equals(request.getRuleId()) &&
            repository.existsByRuleId(request.getRuleId())) {
            throw new IllegalArgumentException("Rule ID already exists: " + request.getRuleId());
        }

        updateEntityFromRequest(entity, request);

        CustomValidationRule saved = repository.save(entity);
        constraintService.evictForUser(ownerId(saved));
        logger.info("Updated custom validation rule: {}", saved.getRuleId());

        return CustomRuleResponse.fromEntity(saved);
    }

    /**
     * Delete a custom rule
     */
    @Transactional
    public void deleteCustomRule(Long id) {
        CustomValidationRule entity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Custom rule not found with ID: " + id));
        Long uid = ownerId(entity);
        repository.deleteById(id);
        constraintService.evictForUser(uid);
        logger.info("Deleted custom validation rule with ID: {}", id);
    }

    /**
     * Toggle rule enabled status
     */
    @Transactional
    public CustomRuleResponse toggleRuleEnabled(Long id) {
        CustomValidationRule entity = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Custom rule not found with ID: " + id));

        entity.setEnabled(!entity.getEnabled());
        CustomValidationRule saved = repository.save(entity);
        constraintService.evictForUser(ownerId(saved));

        logger.info("Toggled custom validation rule {} to enabled: {}",
            saved.getRuleId(), saved.getEnabled());

        return CustomRuleResponse.fromEntity(saved);
    }

    /**
     * Get custom rules by category
     */
    public List<CustomRuleResponse> getCustomRulesByCategory(String category) {
        return repository.findByCategory(category).stream()
            .map(CustomRuleResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get custom rules for a specific model type
     */
    public List<CustomRuleResponse> getCustomRulesForModelType(String modelType) {
        return repository.findEnabledRulesForModelType(modelType).stream()
            .map(CustomRuleResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Helper method to update entity from request
     */
    private void updateEntityFromRequest(CustomValidationRule entity, CustomRuleRequest request) {
        entity.setRuleId(request.getRuleId());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setRuleType(request.getRuleType());
        entity.setSeverity(request.getSeverity());
        entity.setCategory(request.getCategory());
        entity.setFieldPath(request.getFieldPath());
        entity.setRuleExpression(request.getRuleExpression());
        entity.setConstraintDetails(request.getConstraintDetails());

        // Convert list to comma-separated string
        if (request.getApplicableModelTypes() != null && !request.getApplicableModelTypes().isEmpty()) {
            entity.setApplicableModelTypes(String.join(",", request.getApplicableModelTypes()));
        } else {
            entity.setApplicableModelTypes(null);
        }

        entity.setAiGenerated(Boolean.TRUE.equals(request.getAiGenerated()));
        entity.setGenerationPrompt(request.getGenerationPrompt());
        entity.setGenerationModel(request.getGenerationModel());

        entity.setEnabled(request.getEnabled());
    }
}

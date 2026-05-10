package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.CustomValidationRule;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.airulegen.EditProposalRequest;
import gov.nist.oscal.tools.api.model.airulegen.RuleGenTurnRequest;
import gov.nist.oscal.tools.api.model.airulegen.RuleGenTurnResponse;
import gov.nist.oscal.tools.api.model.airulegen.RuleProposal;
import gov.nist.oscal.tools.api.model.airulegen.SaveRuleRequest;
import gov.nist.oscal.tools.api.model.airulegen.StartRuleGenRequest;
import gov.nist.oscal.tools.api.model.airulegen.StartRuleGenResponse;
import gov.nist.oscal.tools.api.repository.CustomValidationRuleRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.MetapathConstraintService;
import gov.nist.oscal.tools.api.service.ai.rulegen.AiRuleGenService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST endpoints for the AI rule-gen wizard. Each session is owned by a
 * single authenticated user; turns and edits are validated against
 * the session owner. Persisting a generated rule snapshots the current
 * proposal into the {@code custom_validation_rules} table.
 */
@RestController
@RequestMapping("/api/rules/ai-generate")
@ConditionalOnProperty(
    name = "app.features.ai-rule-gen.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AiRuleGenController {

    private final AiRuleGenService service;
    private final UserRepository userRepository;
    private final CustomValidationRuleRepository ruleRepository;
    private final MetapathConstraintService constraintService;

    public AiRuleGenController(AiRuleGenService service,
                               UserRepository userRepository,
                               CustomValidationRuleRepository ruleRepository,
                               MetapathConstraintService constraintService) {
        this.service = service;
        this.userRepository = userRepository;
        this.ruleRepository = ruleRepository;
        this.constraintService = constraintService;
    }

    @PostMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StartRuleGenResponse> start(@Valid @RequestBody StartRuleGenRequest req,
                                                      Principal principal) {
        User user = requireUser(principal);
        UUID id = service.start(req.organizationId(), user.getId(), req.modelType());
        return ResponseEntity.ok(new StartRuleGenResponse(id));
    }

    @PostMapping("/sessions/{id}/turn")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RuleGenTurnResponse> turn(@PathVariable UUID id,
                                                    @Valid @RequestBody RuleGenTurnRequest req,
                                                    Principal principal) {
        requireOwnership(id, principal);
        return ResponseEntity.ok(service.turn(id, req.userMessage()));
    }

    @PostMapping("/sessions/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RuleGenTurnResponse> edit(@PathVariable UUID id,
                                                    @Valid @RequestBody EditProposalRequest req,
                                                    Principal principal) {
        requireOwnership(id, principal);
        return ResponseEntity.ok(service.rerunTests(id, req.constraintXml()));
    }

    @PostMapping("/sessions/{id}/save")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> save(@PathVariable UUID id,
                                     @Valid @RequestBody SaveRuleRequest req,
                                     Principal principal) {
        var session = service.session(id);
        User user = requireUser(principal);
        if (session.userId() != user.getId()) {
            return ResponseEntity.status(403).build();
        }
        RuleProposal p = session.currentProposal();
        if (p == null) {
            return ResponseEntity.badRequest().build();
        }
        if (ruleRepository.existsByRuleId(req.ruleId())) {
            return ResponseEntity.status(409).build();
        }
        CustomValidationRule rule = new CustomValidationRule();
        rule.setRuleId(req.ruleId());
        rule.setName(p.name());
        rule.setDescription(p.description());
        rule.setRuleType("metapath");
        rule.setSeverity(p.severity());
        rule.setCategory(req.category());
        rule.setFieldPath(p.fieldPath());
        rule.setRuleExpression(p.constraintXml());
        rule.setApplicableModelTypes(session.modelType());
        rule.setEnabled(req.enabled() == null ? Boolean.TRUE : req.enabled());
        rule.setCreatedDate(LocalDateTime.now());
        rule.setUpdatedDate(LocalDateTime.now());
        rule.setCreatedBy(user.getUsername());
        rule.setUser(user);
        rule.setAiGenerated(true);
        rule.setGenerationModel(session.anthropicModel());
        rule.setGenerationPrompt(session.transcript().isEmpty()
            ? null
            : session.transcript().get(0).text());

        ruleRepository.save(rule);
        constraintService.evictForUser(user.getId());
        service.close(id);
        return ResponseEntity.ok(rule.getId());
    }

    @DeleteMapping("/sessions/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> abandon(@PathVariable UUID id, Principal principal) {
        requireOwnership(id, principal);
        service.close(id);
        return ResponseEntity.noContent().build();
    }

    private User requireUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private void requireOwnership(UUID id, Principal principal) {
        long userId = requireUser(principal).getId();
        if (service.session(id).userId() != userId) {
            throw new org.springframework.security.access.AccessDeniedException("Not your session");
        }
    }
}

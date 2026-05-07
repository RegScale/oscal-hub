package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.ConditionOfApproval;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.AuthorizationNotFoundException;
import gov.nist.oscal.tools.api.model.ConditionOfApprovalRequest;
import gov.nist.oscal.tools.api.repository.AuthorizationRepository;
import gov.nist.oscal.tools.api.repository.AuthorizationTemplateRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing system authorizations
 * Provides CRUD operations and template rendering
 */
@Service
public class AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    @Autowired
    private AuthorizationRepository authorizationRepository;

    @Autowired
    private AuthorizationTemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthorizationOrgContext orgContext;

    @Autowired
    private AuthorizationAccessGuard accessGuard;

    /**
     * Create a new authorization
     */
    @Transactional
    public Authorization createAuthorization(String name, String sspItemId, String sarItemId, Long templateId,
                                            Map<String, String> variableValues, String username,
                                            String dateAuthorized, String dateExpired,
                                            String systemOwner, String securityManager,
                                            String authorizingOfficial, String editedContent,
                                            List<ConditionOfApprovalRequest> conditionRequests) {
        logger.info("Creating new authorization: {} for SSP: {} SAR: {} by user: {}", name, sspItemId, sarItemId, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        Organization userOrg = orgContext.requirePrimaryOrganization(user);
        AuthorizationTemplate template = templateRepository.findByIdAndOrganization(templateId, userOrg)
                .orElseThrow(() -> new AuthorizationNotFoundException(templateId));

        // Create authorization
        Authorization authorization = new Authorization(name, sspItemId, template, user);
        authorization.setOrganization(userOrg);
        authorization.setSarItemId(sarItemId);
        authorization.setVariableValues(variableValues);

        // Set metadata
        if (dateAuthorized != null && !dateAuthorized.isEmpty()) {
            authorization.setDateAuthorized(LocalDate.parse(dateAuthorized));
        }
        if (dateExpired != null && !dateExpired.isEmpty()) {
            authorization.setDateExpired(LocalDate.parse(dateExpired));
        }
        authorization.setSystemOwner(systemOwner);
        authorization.setSecurityManager(securityManager);
        authorization.setAuthorizingOfficial(authorizingOfficial);

        // Render the template with variable values
        // Use editedContent if provided, otherwise use original template content
        String contentToRender = (editedContent != null && !editedContent.isEmpty())
                ? editedContent
                : template.getContent();
        String completedContent = renderTemplate(contentToRender, variableValues, user);
        authorization.setCompletedContent(completedContent);

        // Save authorization first to get the ID
        authorization = authorizationRepository.save(authorization);

        // Create conditions of approval if provided
        if (conditionRequests != null && !conditionRequests.isEmpty()) {
            for (ConditionOfApprovalRequest conditionRequest : conditionRequests) {
                ConditionOfApproval condition = new ConditionOfApproval();
                condition.setAuthorization(authorization);
                condition.setCondition(conditionRequest.getCondition());
                condition.setConditionType(conditionRequest.getConditionType());
                if (conditionRequest.getDueDate() != null && !conditionRequest.getDueDate().isEmpty()) {
                    condition.setDueDate(LocalDate.parse(conditionRequest.getDueDate()));
                }
                authorization.addCondition(condition);
            }
            // Save again to persist conditions
            authorization = authorizationRepository.save(authorization);
        }

        return authorization;
    }

    /**
     * Update an existing authorization
     */
    @Transactional
    public Authorization updateAuthorization(Long id, String name, Map<String, String> variableValues,
                                            String username, String dateAuthorized, String dateExpired,
                                            String systemOwner, String securityManager,
                                            String authorizingOfficial, String editedContent,
                                            List<ConditionOfApprovalRequest> conditionRequests) {
        logger.info("Updating authorization: {} by user: {}", id, username);

        Organization userOrg = resolveUserOrg(username);
        Authorization authorization = authorizationRepository.findByIdAndOrganization(id, userOrg)
                .orElseThrow(() -> new AuthorizationNotFoundException(id));

        accessGuard.requireWriteDetails(authorization,
                userRepository.findByUsername(username)
                        .orElseThrow(() -> new IllegalArgumentException("User '" + username + "' not found.")));

        // Update name
        if (name != null) {
            authorization.setName(name);
        }

        // Update metadata fields
        if (dateAuthorized != null && !dateAuthorized.isEmpty()) {
            authorization.setDateAuthorized(LocalDate.parse(dateAuthorized));
        }
        if (dateExpired != null && !dateExpired.isEmpty()) {
            authorization.setDateExpired(LocalDate.parse(dateExpired));
        }
        if (systemOwner != null) {
            authorization.setSystemOwner(systemOwner);
        }
        if (securityManager != null) {
            authorization.setSecurityManager(securityManager);
        }
        if (authorizingOfficial != null) {
            authorization.setAuthorizingOfficial(authorizingOfficial);
        }

        // Update variable values and re-render content
        if (variableValues != null) {
            authorization.setVariableValues(variableValues);

            // Use editedContent if provided, otherwise use original template content
            String contentToRender = (editedContent != null && !editedContent.isEmpty())
                    ? editedContent
                    : authorization.getTemplate().getContent();
            String completedContent = renderTemplate(contentToRender, variableValues, authorization.getAuthorizedBy());
            authorization.setCompletedContent(completedContent);
        }

        // Update conditions of approval if provided
        if (conditionRequests != null) {
            // Remove existing conditions
            authorization.getConditions().clear();

            // Add new conditions
            for (ConditionOfApprovalRequest conditionRequest : conditionRequests) {
                ConditionOfApproval condition = new ConditionOfApproval();
                condition.setAuthorization(authorization);
                condition.setCondition(conditionRequest.getCondition());
                condition.setConditionType(conditionRequest.getConditionType());
                if (conditionRequest.getDueDate() != null && !conditionRequest.getDueDate().isEmpty()) {
                    condition.setDueDate(LocalDate.parse(conditionRequest.getDueDate()));
                }
                authorization.addCondition(condition);
            }
        }

        return authorizationRepository.save(authorization);
    }

    /**
     * Get an authorization by ID
     */
    @Transactional(readOnly = true)
    public Authorization getAuthorization(Long id) {
        return authorizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Authorization not found: " + id));
    }

    /**
     * Get all authorizations
     */
    @Transactional(readOnly = true)
    public List<Authorization> getAllAuthorizations() {
        return authorizationRepository.findAll();
    }

    /**
     * Get recently authorized systems
     */
    @Transactional(readOnly = true)
    public List<Authorization> getRecentlyAuthorized(int limit) {
        List<Authorization> authorizations = authorizationRepository.findRecentlyAuthorized();
        return authorizations.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Get authorizations for a specific SSP
     */
    @Transactional(readOnly = true)
    public List<Authorization> getAuthorizationsBySsp(String sspItemId) {
        return authorizationRepository.findBySspItemId(sspItemId);
    }

    /**
     * Get authorizations by user
     */
    @Transactional(readOnly = true)
    public List<Authorization> getAuthorizationsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return authorizationRepository.findByAuthorizedBy(user);
    }

    /**
     * Search authorizations
     */
    @Transactional(readOnly = true)
    public List<Authorization> searchAuthorizations(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return getAllAuthorizations();
        }
        return authorizationRepository.searchByNameOrSspItemId(searchTerm);
    }

    /**
     * Delete an authorization
     */
    @Transactional
    public void deleteAuthorization(Long id, String username) {
        logger.info("Deleting authorization: {} by user: {}", id, username);

        Organization userOrg = resolveUserOrg(username);
        Authorization authorization = authorizationRepository.findByIdAndOrganization(id, userOrg)
                .orElseThrow(() -> new AuthorizationNotFoundException(id));

        accessGuard.requireDelete(authorization,
                userRepository.findByUsername(username)
                        .orElseThrow(() -> new IllegalArgumentException("User '" + username + "' not found.")));

        authorizationRepository.delete(authorization);
        logger.info("Deleted authorization: {}", id);
    }

    /**
     * Save an authorization (used by digital signature service)
     */
    @Transactional
    public Authorization save(Authorization authorization) {
        return authorizationRepository.save(authorization);
    }

    // ==================== Org-scoped methods (multi-tenant isolation) ====================

    /**
     * Get all authorizations scoped to the current user's primary organization
     */
    @Transactional(readOnly = true)
    public List<Authorization> getAllAuthorizationsForUser(String username) {
        Organization org = resolveUserOrg(username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User '" + username + "' not found."));
        // TODO(perf): replace with a single JPQL/SQL query that joins grants
        // and applies the access predicate when authorization counts grow large.
        return authorizationRepository.findByOrganization(org).stream()
                .filter(a -> accessGuard.effectiveRole(a, user) != null)
                .toList();
    }

    /**
     * Get a single authorization by ID, scoped to the current user's primary organization
     */
    @Transactional(readOnly = true)
    public Authorization getAuthorizationForUser(Long id, String username) {
        Organization org = resolveUserOrg(username);
        return authorizationRepository.findByIdAndOrganization(id, org)
                .orElseThrow(() -> new AuthorizationNotFoundException(id));
    }

    /**
     * Search authorizations scoped to the current user's primary organization
     */
    @Transactional(readOnly = true)
    public List<Authorization> searchAuthorizationsForUser(String username, String searchTerm) {
        Organization org = resolveUserOrg(username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User '" + username + "' not found."));
        List<Authorization> raw = (searchTerm == null || searchTerm.isBlank())
                ? authorizationRepository.findByOrganization(org)
                : authorizationRepository.searchByNameOrSspItemIdAndOrganization(searchTerm, org);
        return raw.stream()
                .filter(a -> accessGuard.effectiveRole(a, user) != null)
                .toList();
    }

    /**
     * Get authorizations for a specific SSP, scoped to the current user's primary organization
     */
    @Transactional(readOnly = true)
    public List<Authorization> getAuthorizationsBySspForUser(String sspItemId, String username) {
        Organization org = resolveUserOrg(username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User '" + username + "' not found."));
        return authorizationRepository.findBySspItemIdAndOrganization(sspItemId, org).stream()
                .filter(a -> accessGuard.effectiveRole(a, user) != null)
                .toList();
    }

    /**
     * Resolve the primary organization for a username
     */
    private Organization resolveUserOrg(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User '" + username + "' not found."));
        return orgContext.requirePrimaryOrganization(user);
    }

    /**
     * Render a template with variable values
     * Replaces {{ variable }} with actual values
     * Automatically handles {{ logo }} by injecting the user's logo
     */
    public String renderTemplate(String template, Map<String, String> variableValues) {
        if (template == null) {
            return template;
        }

        String result = template;

        // Handle regular variables
        if (variableValues != null) {
            for (Map.Entry<String, String> entry : variableValues.entrySet()) {
                String variableName = entry.getKey();
                String value = entry.getValue() != null ? entry.getValue() : "";

                // Replace {{ variableName }} with the value (handles spaces around variable name)
                String pattern = "\\{\\{\\s*" + Pattern.quote(variableName) + "\\s*\\}\\}";
                result = result.replaceAll(pattern, Matcher.quoteReplacement(value));
            }
        }

        return result;
    }

    /**
     * Render a template with variable values and user-specific data
     * Replaces {{ variable }} with actual values
     * Automatically handles {{ logo }} by injecting the user's logo
     */
    public String renderTemplate(String template, Map<String, String> variableValues, User user) {
        if (template == null) {
            return template;
        }

        String result = template;

        // Handle user-specific variables first ({{ logo }})
        if (user != null && user.getLogo() != null) {
            String logoPattern = "\\{\\{\\s*logo\\s*\\}\\}";

            // For markdown, wrap the logo in an img tag
            String logoReplacement = "![Logo](" + user.getLogo() + ")";
            result = result.replaceAll(logoPattern, Matcher.quoteReplacement(logoReplacement));
        }

        // Handle regular variables
        if (variableValues != null) {
            for (Map.Entry<String, String> entry : variableValues.entrySet()) {
                String variableName = entry.getKey();
                String value = entry.getValue() != null ? entry.getValue() : "";

                // Replace {{ variableName }} with the value (handles spaces around variable name)
                String pattern = "\\{\\{\\s*" + Pattern.quote(variableName) + "\\s*\\}\\}";
                result = result.replaceAll(pattern, Matcher.quoteReplacement(value));
            }
        }

        return result;
    }
}

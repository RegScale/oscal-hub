package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.User;
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

/**
 * Service for managing system authorizations
 * Provides CRUD operations and template rendering
 */
@Service
public class AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);
    // Pattern to match {{ anything }} - allows any content except closing braces
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*\\}\\}");

    @Autowired
    private AuthorizationRepository authorizationRepository;

    @Autowired
    private AuthorizationTemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new authorization
     */
    @Transactional
    public Authorization createAuthorization(String name, String sspItemId, String sarItemId, Long templateId,
                                            Map<String, String> variableValues, String username,
                                            String dateAuthorized, String dateExpired,
                                            String systemOwner, String securityManager,
                                            String authorizingOfficial, String editedContent) {
        logger.info("Creating new authorization: {} for SSP: {} SAR: {} by user: {}", name, sspItemId, sarItemId, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        AuthorizationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        // Create authorization
        Authorization authorization = new Authorization(name, sspItemId, template, user);
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

        return authorizationRepository.save(authorization);
    }

    /**
     * Update an existing authorization
     */
    @Transactional
    public Authorization updateAuthorization(Long id, String name, Map<String, String> variableValues,
                                            String username) {
        logger.info("Updating authorization: {} by user: {}", id, username);

        Authorization authorization = authorizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Authorization not found: " + id));

        if (name != null) {
            authorization.setName(name);
        }

        if (variableValues != null) {
            authorization.setVariableValues(variableValues);

            // Re-render the template with updated variables
            String completedContent = renderTemplate(
                authorization.getTemplate().getContent(),
                variableValues,
                authorization.getAuthorizedBy()
            );
            authorization.setCompletedContent(completedContent);
        }

        return authorizationRepository.save(authorization);
    }

    /**
     * Get an authorization by ID
     */
    public Authorization getAuthorization(Long id) {
        return authorizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Authorization not found: " + id));
    }

    /**
     * Get all authorizations
     */
    public List<Authorization> getAllAuthorizations() {
        return authorizationRepository.findAll();
    }

    /**
     * Get recently authorized systems
     */
    public List<Authorization> getRecentlyAuthorized(int limit) {
        List<Authorization> authorizations = authorizationRepository.findRecentlyAuthorized();
        return authorizations.stream().limit(limit).toList();
    }

    /**
     * Get authorizations for a specific SSP
     */
    public List<Authorization> getAuthorizationsBySsp(String sspItemId) {
        return authorizationRepository.findBySspItemId(sspItemId);
    }

    /**
     * Get authorizations by user
     */
    public List<Authorization> getAuthorizationsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return authorizationRepository.findByAuthorizedBy(user);
    }

    /**
     * Search authorizations
     */
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

        Authorization authorization = authorizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Authorization not found: " + id));

        // Check if user is the creator (you may want to add admin role check)
        if (!authorization.getAuthorizedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can delete this authorization");
        }

        authorizationRepository.delete(authorization);
        logger.info("Deleted authorization: {}", id);
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

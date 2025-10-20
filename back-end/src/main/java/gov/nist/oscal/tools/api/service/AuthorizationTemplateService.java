package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.AuthorizationTemplate;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.AuthorizationTemplateRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;

/**
 * Service for managing authorization templates
 * Provides CRUD operations and variable extraction
 */
@Service
public class AuthorizationTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationTemplateService.class);
    // Pattern to match {{ anything }} - allows any content except closing braces
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([^}]+?)\\s*\\}\\}");

    @Autowired
    private AuthorizationTemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new authorization template
     */
    @Transactional
    public AuthorizationTemplate createTemplate(String name, String content, String username) {
        logger.info("Creating new authorization template: {} by user: {}", name, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        AuthorizationTemplate template = new AuthorizationTemplate(name, content, user);
        return templateRepository.save(template);
    }

    /**
     * Update an existing authorization template
     */
    @Transactional
    public AuthorizationTemplate updateTemplate(Long id, String name, String content, String username) {
        logger.info("Updating authorization template: {} by user: {}", id, username);

        AuthorizationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (name != null) {
            template.setName(name);
        }
        if (content != null) {
            template.setContent(content);
        }
        template.setLastUpdatedBy(user);

        return templateRepository.save(template);
    }

    /**
     * Get a template by ID
     */
    public AuthorizationTemplate getTemplate(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
    }

    /**
     * Get all templates
     */
    public List<AuthorizationTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }

    /**
     * Get recently updated templates
     */
    public List<AuthorizationTemplate> getRecentlyUpdated(int limit) {
        List<AuthorizationTemplate> templates = templateRepository.findRecentlyUpdated();
        return templates.stream().limit(limit).toList();
    }

    /**
     * Get templates by user
     */
    public List<AuthorizationTemplate> getTemplatesByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return templateRepository.findByCreatedBy(user);
    }

    /**
     * Search templates by name
     */
    public List<AuthorizationTemplate> searchTemplates(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return getAllTemplates();
        }
        return templateRepository.findByNameContainingIgnoreCase(searchTerm);
    }

    /**
     * Delete a template
     */
    @Transactional
    public void deleteTemplate(Long id, String username) {
        logger.info("Deleting authorization template: {} by user: {}", id, username);

        AuthorizationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));

        // Check if user is the creator (you may want to add admin role check)
        if (!template.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can delete this template");
        }

        templateRepository.delete(template);
        logger.info("Deleted authorization template: {}", id);
    }

    /**
     * Extract variables from template content
     * Returns a set of variable names found in {{ variable }} format
     */
    public Set<String> extractVariables(String content) {
        Set<String> variables = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(content);

        while (matcher.find()) {
            variables.add(matcher.group(1).trim());
        }

        return variables;
    }

    /**
     * Extract variables from a template by ID
     */
    public Set<String> extractVariablesFromTemplate(Long id) {
        AuthorizationTemplate template = getTemplate(id);
        return extractVariables(template.getContent());
    }
}

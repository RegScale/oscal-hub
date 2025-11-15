package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing organizations in the multi-tenant system
 * Provides CRUD operations for organizations and logo management
 */
@Service
public class OrganizationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationService.class);
    private static final String LOGO_UPLOAD_DIR = "uploads/org-logos";
    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String[] ALLOWED_LOGO_TYPES = {"image/png", "image/jpeg", "image/jpg", "image/svg+xml"};

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new organization
     */
    @Transactional
    public Organization createOrganization(String name, String description, String logoUrl) {
        logger.info("Creating new organization: {}", name);

        // Check if organization name already exists
        if (organizationRepository.existsByName(name)) {
            throw new RuntimeException("Organization with name '" + name + "' already exists");
        }

        Organization organization = new Organization(name, description);
        organization.setLogoUrl(logoUrl);
        organization.setActive(true);

        organization = organizationRepository.save(organization);
        logger.info("Created organization: {} with ID: {}", name, organization.getId());

        return organization;
    }

    /**
     * Update an existing organization
     */
    @Transactional
    public Organization updateOrganization(Long id, String name, String description, Boolean active) {
        logger.info("Updating organization: {}", id);

        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + id));

        // Check if new name conflicts with another organization
        if (name != null && !name.equals(organization.getName())) {
            if (organizationRepository.existsByName(name)) {
                throw new RuntimeException("Organization with name '" + name + "' already exists");
            }
            organization.setName(name);
        }

        if (description != null) {
            organization.setDescription(description);
        }

        if (active != null) {
            organization.setActive(active);
        }

        organization.setUpdatedAt(LocalDateTime.now());
        organization = organizationRepository.save(organization);

        logger.info("Updated organization: {}", id);
        return organization;
    }

    /**
     * Get organization by ID
     */
    public Organization getOrganization(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Organization not found: " + id));
    }

    /**
     * Get all organizations
     */
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    /**
     * Get only active organizations
     */
    public List<Organization> getActiveOrganizations() {
        return organizationRepository.findByActiveTrue();
    }

    /**
     * Deactivate an organization (soft delete)
     */
    @Transactional
    public Organization deactivateOrganization(Long id) {
        logger.info("Deactivating organization: {}", id);

        Organization organization = getOrganization(id);
        organization.setActive(false);
        organization.setUpdatedAt(LocalDateTime.now());

        organization = organizationRepository.save(organization);
        logger.info("Deactivated organization: {}", id);

        return organization;
    }

    /**
     * Upload organization logo
     */
    @Transactional
    public String uploadLogo(Long organizationId, MultipartFile file) throws IOException {
        logger.info("Uploading logo for organization: {}", organizationId);

        Organization organization = getOrganization(organizationId);

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_LOGO_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 2MB");
        }

        String contentType = file.getContentType();
        boolean validType = false;
        for (String allowedType : ALLOWED_LOGO_TYPES) {
            if (allowedType.equals(contentType)) {
                validType = true;
                break;
            }
        }

        if (!validType) {
            throw new IllegalArgumentException("Invalid file type. Allowed types: PNG, JPG, JPEG, SVG");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(LOGO_UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".png";
        String filename = "org-" + organizationId + "-" + UUID.randomUUID().toString() + extension;

        // Delete old logo if exists
        if (organization.getLogoUrl() != null) {
            deleteLogo(organization.getLogoUrl());
        }

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Update organization logo URL
        String logoUrl = "/api/files/org-logos/" + filename;
        organization.setLogoUrl(logoUrl);
        organization.setUpdatedAt(LocalDateTime.now());
        organizationRepository.save(organization);

        logger.info("Uploaded logo for organization: {} at: {}", organizationId, logoUrl);
        return logoUrl;
    }

    /**
     * Delete organization logo
     */
    @Transactional
    public void deleteLogo(Long organizationId) {
        logger.info("Deleting logo for organization: {}", organizationId);

        Organization organization = getOrganization(organizationId);

        if (organization.getLogoUrl() != null) {
            deleteLogo(organization.getLogoUrl());
            organization.setLogoUrl(null);
            organization.setUpdatedAt(LocalDateTime.now());
            organizationRepository.save(organization);
        }

        logger.info("Deleted logo for organization: {}", organizationId);
    }

    /**
     * Delete logo file from filesystem
     */
    private void deleteLogo(String logoUrl) {
        try {
            String filename = logoUrl.substring(logoUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(LOGO_UPLOAD_DIR, filename);
            Files.deleteIfExists(filePath);
            logger.info("Deleted logo file: {}", filename);
        } catch (IOException e) {
            logger.error("Failed to delete logo file: {}", logoUrl, e);
        }
    }

    /**
     * Assign a user as organization administrator
     */
    @Transactional
    public OrganizationMembership assignOrganizationAdmin(Long organizationId, Long userId) {
        logger.info("Assigning user {} as admin of organization {}", userId, organizationId);

        Organization organization = getOrganization(organizationId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Check if membership already exists
        OrganizationMembership membership = membershipRepository
                .findByUserAndOrganization(user, organization)
                .orElse(null);

        if (membership != null) {
            // Update existing membership to ORG_ADMIN
            membership.setRole(OrganizationRole.ORG_ADMIN);
            membership.setStatus(MembershipStatus.ACTIVE);
            membership.setUpdatedAt(LocalDateTime.now());
        } else {
            // Create new membership
            membership = new OrganizationMembership(user, organization, OrganizationRole.ORG_ADMIN);
            membership.setStatus(MembershipStatus.ACTIVE);
        }

        membership = membershipRepository.save(membership);
        logger.info("Assigned user {} as admin of organization {}", userId, organizationId);

        return membership;
    }

    /**
     * Add a user to an organization with specified role
     */
    @Transactional
    public OrganizationMembership addUserToOrganization(Long organizationId, Long userId, OrganizationRole role) {
        logger.info("Adding user {} to organization {} with role {}", userId, organizationId, role);

        Organization organization = getOrganization(organizationId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Check if membership already exists
        if (membershipRepository.existsByUserAndOrganization(user, organization)) {
            throw new RuntimeException("User is already a member of this organization");
        }

        OrganizationMembership membership = new OrganizationMembership(user, organization, role);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership = membershipRepository.save(membership);

        logger.info("Added user {} to organization {}", userId, organizationId);
        return membership;
    }

    /**
     * Get all members of an organization
     */
    public List<OrganizationMembership> getOrganizationMembers(Long organizationId) {
        Organization organization = getOrganization(organizationId);
        return membershipRepository.findByOrganizationWithUser(organization);
    }

    /**
     * Get active members of an organization
     */
    public List<OrganizationMembership> getActiveOrganizationMembers(Long organizationId) {
        Organization organization = getOrganization(organizationId);
        return membershipRepository.findByOrganizationAndStatusWithUser(organization, MembershipStatus.ACTIVE);
    }
}

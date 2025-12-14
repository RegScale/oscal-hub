package gov.nist.oscal.tools.api.config;

import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database initializer that runs on application startup.
 * Creates default admin user and organization if no users exist in the database.
 */
@Component
public class DatabaseInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "password";
    private static final String DEFAULT_ADMIN_EMAIL = "admin@oscal-tools.local";
    private static final String DEFAULT_ORG_NAME = "Default Organization";
    private static final String DEFAULT_ORG_DESCRIPTION = "Default organization for system administrators";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long userCount = userRepository.count();

        if (userCount == 0) {
            logger.info("================================================================================");
            logger.info("Database is empty. Creating default admin user and organization...");
            logger.info("================================================================================");

            try {
                // Create default organization
                Organization defaultOrg = createDefaultOrganization();
                logger.info("✓ Created default organization: {}", defaultOrg.getName());

                // Create admin user
                User adminUser = createAdminUser();
                logger.info("✓ Created admin user: {}", adminUser.getUsername());

                // Link admin to default organization
                OrganizationMembership membership = new OrganizationMembership(
                        adminUser,
                        defaultOrg,
                        OrganizationMembership.OrganizationRole.ORG_ADMIN
                );
                adminUser.getOrganizationMemberships().add(membership);
                userRepository.save(adminUser);
                logger.info("✓ Linked admin user to default organization");

                logger.info("================================================================================");
                logger.info("Default Admin Credentials:");
                logger.info("  Username: {}", DEFAULT_ADMIN_USERNAME);
                logger.info("  Password: {}", DEFAULT_ADMIN_PASSWORD);
                logger.info("  Email:    {}", DEFAULT_ADMIN_EMAIL);
                logger.info("================================================================================");
                logger.warn("⚠️  IMPORTANT: Change the admin password immediately after first login!");
                logger.info("================================================================================");

            } catch (Exception e) {
                logger.error("Failed to initialize default admin user and organization", e);
                throw new RuntimeException("Database initialization failed", e);
            }
        } else {
            logger.debug("Database already contains {} user(s). Skipping default admin creation.", userCount);
        }
    }

    /**
     * Creates the default organization.
     */
    private Organization createDefaultOrganization() {
        Organization org = new Organization(DEFAULT_ORG_NAME, DEFAULT_ORG_DESCRIPTION);
        return organizationRepository.save(org);
    }

    /**
     * Creates the default admin user with SUPER_ADMIN role.
     */
    private User createAdminUser() {
        User admin = new User();
        admin.setUsername(DEFAULT_ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
        admin.setEmail(DEFAULT_ADMIN_EMAIL);
        admin.setGlobalRole(User.GlobalRole.SUPER_ADMIN);
        admin.setEnabled(true);
        admin.setMustChangePassword(false);
        admin.getRoles().add("ROLE_ADMIN"); // Add admin role for backward compatibility

        return userRepository.save(admin);
    }
}

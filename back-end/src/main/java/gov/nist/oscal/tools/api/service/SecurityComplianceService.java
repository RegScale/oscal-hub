package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.model.security.*;
import gov.nist.oscal.tools.api.model.security.ComplianceSummary.CategorySummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for SOC 2 security compliance reporting.
 * Control data is hardcoded as it represents the application's security posture
 * and should be version-controlled rather than database-driven.
 */
@Service
public class SecurityComplianceService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityComplianceService.class);

    private final List<Soc2Control> controls;
    private final List<GapAnalysis> gaps;

    public SecurityComplianceService() {
        this.controls = initializeControls();
        this.gaps = initializeGaps();
    }

    /**
     * Get compliance summary with overall statistics.
     */
    public ComplianceSummary getComplianceSummary() {
        int total = controls.size();
        int implemented = (int) controls.stream()
                .filter(c -> c.getStatus() == ControlStatus.IMPLEMENTED)
                .count();
        int partial = (int) controls.stream()
                .filter(c -> c.getStatus() == ControlStatus.PARTIAL)
                .count();
        int gapCount = (int) controls.stream()
                .filter(c -> c.getStatus() == ControlStatus.GAP)
                .count();

        // Calculate compliance percentage: full implementation = 1.0, partial = 0.5, gap = 0
        double percentage = ((implemented * 1.0) + (partial * 0.5)) / total * 100;

        // Build category summaries
        Map<String, CategorySummary> byCategory = new LinkedHashMap<>();
        for (ControlCategory category : ControlCategory.values()) {
            List<Soc2Control> categoryControls = controls.stream()
                    .filter(c -> c.getCategory() == category)
                    .collect(Collectors.toList());

            if (!categoryControls.isEmpty()) {
                int catImplemented = (int) categoryControls.stream()
                        .filter(c -> c.getStatus() == ControlStatus.IMPLEMENTED)
                        .count();
                int catPartial = (int) categoryControls.stream()
                        .filter(c -> c.getStatus() == ControlStatus.PARTIAL)
                        .count();
                int catGaps = (int) categoryControls.stream()
                        .filter(c -> c.getStatus() == ControlStatus.GAP)
                        .count();

                byCategory.put(category.getCode(), new CategorySummary(
                        category.getDisplayName(),
                        categoryControls.size(),
                        catImplemented,
                        catPartial,
                        catGaps
                ));
            }
        }

        return new ComplianceSummary(
                total,
                implemented,
                partial,
                gapCount,
                Math.round(percentage * 10.0) / 10.0,
                Instant.now(),
                byCategory
        );
    }

    /**
     * Get all SOC 2 controls.
     */
    public List<Soc2Control> getAllControls() {
        return new ArrayList<>(controls);
    }

    /**
     * Get controls filtered by category.
     */
    public List<Soc2Control> getControlsByCategory(String categoryCode) {
        ControlCategory category;
        try {
            category = ControlCategory.fromCode(categoryCode);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid category code: {}", categoryCode);
            return Collections.emptyList();
        }

        return controls.stream()
                .filter(c -> c.getCategory() == category)
                .collect(Collectors.toList());
    }

    /**
     * Get all identified gaps.
     */
    public List<GapAnalysis> getGapAnalysis() {
        return new ArrayList<>(gaps);
    }

    /**
     * Initialize all SOC 2 controls with current implementation status.
     */
    private List<Soc2Control> initializeControls() {
        List<Soc2Control> list = new ArrayList<>();

        // ========== CC6 - Logical and Physical Access Controls ==========
        list.add(Soc2Control.builder()
                .controlId("CC6.1")
                .name("Logical Access Security Software")
                .description("The entity implements logical access security software to support achievement of the entity's objectives.")
                .category(ControlCategory.CC6)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("JWT-based authentication with secure token generation and validation. " +
                        "JwtAuthenticationFilter validates tokens on every request. " +
                        "Tokens include user roles and organization context.")
                .evidence(Arrays.asList(
                        "JwtAuthenticationFilter.java",
                        "JwtService.java",
                        "SecurityConfig.java"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("CC6.2")
                .name("Prior to Registration, Access Removal")
                .description("Prior to registration, new users are authorized, and access is removed when user access is no longer required.")
                .category(ControlCategory.CC6)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("User registration requires approval. Users can be disabled or deleted by administrators. " +
                        "Organization membership can be revoked. Access requests require admin approval.")
                .evidence(Arrays.asList(
                        "AdminController.java - user management endpoints",
                        "OrganizationController.java - member removal",
                        "User entity - enabled/disabled flag"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("CC6.3")
                .name("Role-Based Access Control")
                .description("The entity authorizes, modifies, or removes access based on roles and responsibilities.")
                .category(ControlCategory.CC6)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("Three-tier role system: SUPER_ADMIN (global), ORG_ADMIN (organization), USER (member). " +
                        "Spring Security @PreAuthorize annotations enforce role-based access on all endpoints. " +
                        "Organization-scoped data access prevents cross-tenant access.")
                .evidence(Arrays.asList(
                        "GlobalRole.java enum",
                        "OrganizationRole.java enum",
                        "@PreAuthorize annotations on controllers",
                        "Multi-tenant data filtering in services"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("CC6.6")
                .name("Logical Access Restrictions")
                .description("The entity restricts logical access to system components.")
                .category(ControlCategory.CC6)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("Multi-tenant architecture with organization-scoped data. " +
                        "All API endpoints validate user's organization membership before returning data. " +
                        "Service layer enforces data isolation between organizations.")
                .evidence(Arrays.asList(
                        "LibraryService.java - org-scoped queries",
                        "AuthorizationService.java - org-scoped data",
                        "SecurityConfig.java - endpoint protection"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("CC6.7")
                .name("Information Transmission")
                .description("The entity restricts the transmission, movement, and removal of information.")
                .category(ControlCategory.CC6)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("HTTPS/TLS enforced for all communications. " +
                        "CORS configuration restricts allowed origins. " +
                        "API responses do not include sensitive internal data.")
                .evidence(Arrays.asList(
                        "SecurityConfig.java - CORS configuration",
                        "application.properties - server.ssl settings",
                        "Cloud Run/deployment HTTPS enforcement"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("CC6.8")
                .name("Multi-Factor Authentication")
                .description("The entity implements MFA for systems supporting authentication of users.")
                .category(ControlCategory.CC6)
                .status(ControlStatus.GAP)
                .implementation("Not currently implemented. Users authenticate with username and password only.")
                .evidence(Collections.emptyList())
                .build());

        // ========== CC7 - System Operations ==========
        list.add(Soc2Control.builder()
                .controlId("CC7.1")
                .name("Vulnerability Detection")
                .description("The entity detects vulnerabilities and implements controls to mitigate risks.")
                .category(ControlCategory.CC7)
                .status(ControlStatus.PARTIAL)
                .implementation("Rate limiting (100 requests/minute) prevents abuse. " +
                        "Input validation on all API endpoints. File type and size restrictions on uploads. " +
                        "OSCAL schema validation prevents malformed document processing.")
                .evidence(Arrays.asList(
                        "RateLimitFilter.java - rate limiting",
                        "FileValidationService.java - file validation",
                        "OscalValidationService.java - schema validation"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("CC7.2")
                .name("Anomaly Detection and Monitoring")
                .description("The entity monitors system components for anomalies and security events.")
                .category(ControlCategory.CC7)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("Comprehensive audit logging with 27 event types. " +
                        "All security-relevant actions are logged with user, timestamp, IP, and details. " +
                        "SIEM integration via webhooks enables real-time monitoring. " +
                        "SHA-256 integrity hashing prevents log tampering.")
                .evidence(Arrays.asList(
                        "AuditService.java",
                        "AuditEventType.java - 27 event types",
                        "SiemService.java - SIEM integration",
                        "AuditLog entity - integrity hash"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("CC7.3")
                .name("Security Event Evaluation")
                .description("The entity evaluates security events to determine whether they could impact the system.")
                .category(ControlCategory.CC7)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("Health check system monitors all critical components: database, storage, memory, CPU, disk space. " +
                        "Secrets/configuration health check validates required environment variables. " +
                        "Admin dashboard provides real-time visibility into system health.")
                .evidence(Arrays.asList(
                        "HealthCheckService.java",
                        "HealthController.java",
                        "/admin/health dashboard"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("CC7.4")
                .name("Incident Response")
                .description("The entity responds to identified security incidents.")
                .category(ControlCategory.CC7)
                .status(ControlStatus.PARTIAL)
                .implementation("Audit logs capture all security events for investigation. " +
                        "SIEM integration enables alerting on suspicious activity. " +
                        "Account lockout (5 failed attempts = 15 minute lockout) prevents brute force attacks.")
                .evidence(Arrays.asList(
                        "AuditService.java - event logging",
                        "SiemService.java - alert delivery",
                        "AuthService.java - account lockout"
                ))
                .build());

        // ========== CC8 - Change Management ==========
        list.add(Soc2Control.builder()
                .controlId("CC8.1")
                .name("Change Authorization")
                .description("The entity authorizes, designs, develops or acquires, implements, and operates changes.")
                .category(ControlCategory.CC8)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("Administrative functions restricted to SUPER_ADMIN role. " +
                        "Organization management restricted to ORG_ADMIN and above. " +
                        "All configuration changes are audit logged.")
                .evidence(Arrays.asList(
                        "@PreAuthorize annotations",
                        "AdminController.java - admin-only endpoints",
                        "Audit logging of configuration changes"
                ))
                .build());

        // ========== CC9 - Risk Mitigation ==========
        list.add(Soc2Control.builder()
                .controlId("CC9.1")
                .name("Risk Identification and Analysis")
                .description("The entity identifies and analyzes risks that could impact objectives.")
                .category(ControlCategory.CC9)
                .status(ControlStatus.GAP)
                .implementation("File uploads are validated for type and size, but no malware scanning is performed.")
                .evidence(Arrays.asList(
                        "FileValidationService.java - basic validation only"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("CC9.2")
                .name("Business Continuity")
                .description("The entity implements controls to enable recovery following a disaster or incident.")
                .category(ControlCategory.CC9)
                .status(ControlStatus.PARTIAL)
                .implementation("Health monitoring provides visibility into system status. " +
                        "Cloud deployment (GCP Cloud Run) provides infrastructure resilience. " +
                        "Documented DR procedures not yet established.")
                .evidence(Arrays.asList(
                        "HealthCheckService.java",
                        "GCP deployment configuration",
                        "docker-compose.yml for local recovery"
                ))
                .build());

        // ========== Data Protection ==========
        list.add(Soc2Control.builder()
                .controlId("DP.1")
                .name("Encryption at Rest")
                .description("Data is encrypted when stored.")
                .category(ControlCategory.DATA_PROTECTION)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("Cloud storage providers (Azure Blob, GCS) provide encryption at rest by default. " +
                        "Database encryption enabled via cloud provider settings.")
                .evidence(Arrays.asList(
                        "AzureBlobService.java - Azure encryption",
                        "GcsStorageService.java - GCS encryption",
                        "Cloud provider documentation"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("DP.2")
                .name("Encryption in Transit")
                .description("Data is encrypted during transmission.")
                .category(ControlCategory.DATA_PROTECTION)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("All API communications use HTTPS/TLS. " +
                        "Cloud Run enforces HTTPS. " +
                        "Database connections use SSL.")
                .evidence(Arrays.asList(
                        "SecurityConfig.java",
                        "Cloud Run deployment settings",
                        "application.properties - database SSL"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("DP.3")
                .name("Input Validation")
                .description("Input is validated before processing.")
                .category(ControlCategory.DATA_PROTECTION)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("File uploads validated for type (XML, JSON, YAML only) and size (50MB limit). " +
                        "OSCAL documents validated against schema. " +
                        "API inputs validated with Spring validation annotations.")
                .evidence(Arrays.asList(
                        "FileValidationService.java",
                        "OscalValidationService.java",
                        "@Valid annotations on request DTOs"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("DP.4")
                .name("Password Policy")
                .description("Password requirements enforce strong passwords.")
                .category(ControlCategory.DATA_PROTECTION)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("Password requirements: minimum 8 characters, uppercase, lowercase, number, special character. " +
                        "Passwords stored using BCrypt hashing. " +
                        "Password change functionality available.")
                .evidence(Arrays.asList(
                        "AuthService.java - password validation",
                        "BCryptPasswordEncoder configuration",
                        "ChangePasswordRequest.java"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("DP.5")
                .name("Account Lockout")
                .description("Accounts are locked after failed authentication attempts.")
                .category(ControlCategory.DATA_PROTECTION)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("After 5 failed login attempts, account is locked for 15 minutes. " +
                        "Failed attempts and lockouts are audit logged. " +
                        "SIEM notification on account lockout events.")
                .evidence(Arrays.asList(
                        "AuthService.java - lockout logic",
                        "User entity - failedAttempts, lockoutTime fields",
                        "AuditEventType.ACCOUNT_LOCKOUT"
                ))
                .build());

        // ========== Audit and Monitoring ==========
        list.add(Soc2Control.builder()
                .controlId("AM.1")
                .name("Audit Logging")
                .description("Security-relevant events are logged.")
                .category(ControlCategory.AUDIT)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("27 distinct audit event types covering authentication, authorization, data access, " +
                        "configuration changes, and security events. " +
                        "Logs include timestamp, user, IP address, and event details. " +
                        "SHA-256 integrity hash prevents tampering.")
                .evidence(Arrays.asList(
                        "AuditEventType.java - 27 event types",
                        "AuditService.java",
                        "AuditLog entity"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("AM.2")
                .name("Log Retention")
                .description("Audit logs are retained for an appropriate period.")
                .category(ControlCategory.AUDIT)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("Configurable retention period (default 90 days). " +
                        "Automatic cleanup of old logs. " +
                        "Export functionality for compliance archival.")
                .evidence(Arrays.asList(
                        "application.properties - retention settings",
                        "AuditLogCleanupService.java",
                        "AuditController.java - export endpoint"
                ))
                .build());

        list.add(Soc2Control.builder()
                .controlId("AM.3")
                .name("SIEM Integration")
                .description("Security events are forwarded to a SIEM for monitoring.")
                .category(ControlCategory.AUDIT)
                .status(ControlStatus.IMPLEMENTED)
                .implementation("Webhook-based SIEM integration. " +
                        "Async event delivery prevents performance impact. " +
                        "Configurable webhook URL and retry logic.")
                .evidence(Arrays.asList(
                        "SiemService.java",
                        "application.properties - siem.webhook.url",
                        "SiemWebhookPayload.java"
                ))
                .build());

        logger.info("Initialized {} SOC 2 controls", list.size());
        return list;
    }

    /**
     * Initialize identified gaps with recommendations.
     */
    private List<GapAnalysis> initializeGaps() {
        List<GapAnalysis> list = new ArrayList<>();

        list.add(GapAnalysis.builder()
                .gapId("GAP-001")
                .controlId("CC6.8")
                .title("Multi-Factor Authentication Not Implemented")
                .description("Users authenticate with username and password only. " +
                        "No second factor is required, which increases risk of unauthorized access " +
                        "if credentials are compromised.")
                .severity(GapSeverity.HIGH)
                .recommendation("Implement TOTP-based MFA using Google Authenticator or similar. " +
                        "Require MFA for all admin accounts (SUPER_ADMIN, ORG_ADMIN). " +
                        "Consider optional MFA for regular users.")
                .effort("Medium-High")
                .priority(1)
                .build());

        list.add(GapAnalysis.builder()
                .gapId("GAP-002")
                .controlId("CC9.1")
                .title("No Malware Scanning for File Uploads")
                .description("File uploads are validated for type and size, but no malware scanning is performed. " +
                        "Malicious files could be uploaded and stored.")
                .severity(GapSeverity.HIGH)
                .recommendation("Integrate ClamAV for on-premise scanning or use a cloud-based " +
                        "malware scanning service (e.g., VirusTotal API, Google Safe Browsing). " +
                        "Quarantine suspicious files before processing.")
                .effort("Medium")
                .priority(2)
                .build());

        list.add(GapAnalysis.builder()
                .gapId("GAP-003")
                .controlId("CC7.4")
                .title("Incomplete Incident Response Procedures")
                .description("While audit logging and SIEM integration exist, documented incident response " +
                        "procedures and runbooks are not established.")
                .severity(GapSeverity.MEDIUM)
                .recommendation("Create incident response runbook covering: escalation procedures, " +
                        "common attack scenarios, communication templates, recovery steps. " +
                        "Conduct periodic incident response drills.")
                .effort("Low")
                .priority(3)
                .build());

        list.add(GapAnalysis.builder()
                .gapId("GAP-004")
                .controlId("CC9.2")
                .title("Disaster Recovery Not Documented")
                .description("Health monitoring exists but formal DR procedures are not documented. " +
                        "Recovery time objectives (RTO) and recovery point objectives (RPO) are not defined.")
                .severity(GapSeverity.MEDIUM)
                .recommendation("Document disaster recovery procedures including: backup strategies, " +
                        "recovery steps, RTO/RPO targets, testing schedule. " +
                        "Implement automated database backups.")
                .effort("Medium")
                .priority(4)
                .build());

        list.add(GapAnalysis.builder()
                .gapId("GAP-005")
                .controlId("CC7.1")
                .title("Limited Vulnerability Scanning")
                .description("While input validation exists, no automated vulnerability scanning or " +
                        "dependency checking is in place.")
                .severity(GapSeverity.MEDIUM)
                .recommendation("Implement dependency scanning in CI/CD pipeline (e.g., Snyk, Dependabot). " +
                        "Add SAST tools for code security analysis. " +
                        "Conduct periodic penetration testing.")
                .effort("Medium")
                .priority(5)
                .build());

        logger.info("Initialized {} compliance gaps", list.size());
        return list;
    }
}

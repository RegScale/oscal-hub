package gov.nist.oscal.tools.api.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.google.cloud.storage.*;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import gov.nist.oscal.tools.api.entity.Organization;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.MembershipStatus;
import gov.nist.oscal.tools.api.entity.OrganizationMembership.OrganizationRole;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.OrganizationRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.util.PathSanitizer;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Value("${storage.provider:azure}")
    private String storageProvider;

    // Azure Blob Storage configuration
    @Value("${azure.storage.connection-string:}")
    private String azureConnectionString;

    @Value("${azure.storage.container-name:oscal-files}")
    private String azureContainerName;

    // Google Cloud Storage configuration
    @Value("${gcp.project-id:}")
    private String gcpProjectId;

    @Value("${gcp.storage.logo-bucket:oscal-logos}")
    private String gcsLogoBucket;

    // AWS S3 configuration
    @Value("${aws.s3.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.s3.logo-bucket:oscal-logos}")
    private String s3LogoBucket;

    // Storage clients
    private BlobContainerClient azureContainerClient;
    private Storage gcsStorage;
    private S3Client s3Client;
    private boolean useCloudStorage = false;

    @PostConstruct
    public void init() {
        logger.info("Initializing organization logo storage (provider: {})...", storageProvider);

        if ("gcs".equalsIgnoreCase(storageProvider)) {
            initializeGcsStorage();
        } else if ("s3".equalsIgnoreCase(storageProvider)) {
            initializeS3Storage();
        } else {
            // Default to Azure
            initializeAzureStorage();
        }
    }

    private void initializeAzureStorage() {
        if (azureConnectionString == null || azureConnectionString.trim().isEmpty()) {
            logger.info("Azure Blob Storage not configured, using local filesystem only for organization logos");
            return;
        }

        try {
            logger.info("Initializing Azure Blob Storage for organization logos...");
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(azureConnectionString)
                    .buildClient();

            azureContainerClient = blobServiceClient.getBlobContainerClient(azureContainerName);

            // Create container if it doesn't exist
            // NOTE: We use PRIVATE access because all images are served through the authenticated proxy endpoint
            // Public access is typically disabled at the storage account level for security
            if (!azureContainerClient.exists()) {
                azureContainerClient.create();
                logger.info("Created Azure blob container for organization logos (private access - served via proxy): {}", azureContainerName);
            } else {
                logger.info("Using existing Azure blob container for organization logos: {}", azureContainerName);
            }

            useCloudStorage = true;
            logger.info("Azure Blob Storage initialized for organization logos");
        } catch (Exception e) {
            logger.error("Failed to initialize Azure Blob Storage for organization logos, using local filesystem only: {}", e.getMessage());
            useCloudStorage = false;
        }
    }

    private void initializeGcsStorage() {
        try {
            logger.info("Initializing Google Cloud Storage for organization logos...");
            logger.info("GCP Project ID: {}", gcpProjectId);
            logger.info("GCS Logo Bucket: {}", gcsLogoBucket);

            // Create GCS client (uses Application Default Credentials)
            gcsStorage = StorageOptions.newBuilder()
                    .setProjectId(gcpProjectId)
                    .build()
                    .getService();

            // Check if bucket exists
            com.google.cloud.storage.Bucket bucket = gcsStorage.get(gcsLogoBucket);

            if (bucket == null) {
                logger.warn("GCS bucket '{}' does not exist. Attempting to create...", gcsLogoBucket);
                com.google.cloud.storage.BucketInfo bucketInfo = com.google.cloud.storage.BucketInfo.newBuilder(gcsLogoBucket)
                        .setLocation("US")
                        .setStorageClass(com.google.cloud.storage.StorageClass.STANDARD)
                        .setIamConfiguration(
                                com.google.cloud.storage.BucketInfo.IamConfiguration.newBuilder()
                                        .setIsUniformBucketLevelAccessEnabled(true)
                                        .build()
                        )
                        .build();
                gcsStorage.create(bucketInfo);
                logger.info("Created GCS bucket for organization logos: {}", gcsLogoBucket);
            } else {
                logger.info("Using existing GCS bucket for organization logos: {}", gcsLogoBucket);
            }

            useCloudStorage = true;
            logger.info("Google Cloud Storage initialized for organization logos");
        } catch (Exception e) {
            logger.error("Failed to initialize GCS for organization logos, using local filesystem only: {}", e.getMessage(), e);
            logger.info("Ensure Google Cloud credentials are configured:");
            logger.info("  1. Running on Cloud Run with service account, OR");
            logger.info("  2. GOOGLE_APPLICATION_CREDENTIALS environment variable set, OR");
            logger.info("  3. gcloud auth application-default login executed");
            useCloudStorage = false;
        }
    }

    private void initializeS3Storage() {
        try {
            logger.info("Initializing AWS S3 for organization logos...");
            logger.info("AWS Region: {}", awsRegion);
            logger.info("S3 Logo Bucket: {}", s3LogoBucket);

            // Create S3 client (uses IAM role credentials or AWS credentials chain)
            s3Client = S3Client.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            // Check if bucket exists
            try {
                HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                        .bucket(s3LogoBucket)
                        .build();
                s3Client.headBucket(headBucketRequest);
                logger.info("Using existing S3 bucket for organization logos: {}", s3LogoBucket);
            } catch (NoSuchBucketException e) {
                logger.warn("S3 bucket '{}' does not exist. Attempting to create...", s3LogoBucket);
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(s3LogoBucket)
                        .build();
                s3Client.createBucket(createBucketRequest);
                logger.info("Created S3 bucket for organization logos: {}", s3LogoBucket);
            }

            useCloudStorage = true;
            logger.info("AWS S3 initialized for organization logos");
        } catch (Exception e) {
            logger.error("Failed to initialize S3 for organization logos, using local filesystem only: {}", e.getMessage(), e);
            logger.info("Ensure AWS credentials are configured:");
            logger.info("  1. IAM role attached to EC2/ECS/Lambda, OR");
            logger.info("  2. AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables, OR");
            logger.info("  3. ~/.aws/credentials file configured");
            useCloudStorage = false;
        }
    }

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

        // Read file content into byte array (so we can upload to cloud and local)
        byte[] fileBytes = file.getBytes();

        // Upload to cloud storage if configured
        if (useCloudStorage) {
            uploadToCloudStorage(filename, fileBytes, contentType, organizationId, originalFilename);
        }

        // Always save to local filesystem as backup/fallback
        Path uploadPath = Paths.get(LOGO_UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Use PathSanitizer to prevent path traversal attacks
        Path filePath = PathSanitizer.safeResolve(uploadPath, filename);
        Files.write(filePath, fileBytes);
        logger.info("Saved logo to local filesystem: {}", filePath);

        // Update organization logo URL - use proxy endpoint for authenticated access
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
     * Delete logo file from cloud storage and local filesystem
     */
    private void deleteLogo(String logoUrl) {
        try {
            String filename = logoUrl.substring(logoUrl.lastIndexOf("/") + 1);

            // Delete from cloud storage if configured
            if (useCloudStorage) {
                deleteFromCloudStorage(filename);
            }

            // Delete from local filesystem
            Path filePath = Paths.get(LOGO_UPLOAD_DIR, filename);
            if (Files.deleteIfExists(filePath)) {
                logger.info("Deleted logo file from local filesystem: {}", filename);
            }
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

    /**
     * Update member role in an organization
     */
    @Transactional
    public OrganizationMembership updateMemberRole(Long membershipId, OrganizationRole newRole) {
        logger.info("Updating membership {} to role {}", membershipId, newRole);

        OrganizationMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Membership not found: " + membershipId));

        membership.setRole(newRole);
        membership.setUpdatedAt(LocalDateTime.now());
        membership = membershipRepository.save(membership);

        logger.info("Updated membership {} to role {}", membershipId, newRole);
        return membership;
    }

    /**
     * Remove a member from an organization
     */
    @Transactional
    public void removeMember(Long membershipId) {
        logger.info("Removing membership {}", membershipId);

        OrganizationMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Membership not found: " + membershipId));

        membershipRepository.delete(membership);
        logger.info("Removed membership {}", membershipId);
    }

    /**
     * Upload logo to cloud storage (Azure, GCS, or S3)
     * @throws IOException if cloud storage upload fails
     */
    private void uploadToCloudStorage(String filename, byte[] fileBytes, String contentType, Long organizationId, String originalFilename) throws IOException {
        String logoPath = "org-logos/" + filename;

        if ("gcs".equalsIgnoreCase(storageProvider) && gcsStorage != null) {
            // Upload to Google Cloud Storage
            try {
                BlobId blobId = BlobId.of(gcsLogoBucket, logoPath);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                        .setContentType(contentType)
                        .setMetadata(java.util.Map.of(
                                "organizationId", organizationId.toString(),
                                "originalFilename", originalFilename != null ? originalFilename : filename
                        ))
                        .build();

                gcsStorage.create(blobInfo, fileBytes);
                logger.info("✓ Successfully uploaded logo to Google Cloud Storage: {} ({} bytes)", logoPath, fileBytes.length);
            } catch (Exception e) {
                logger.error("✗ Failed to upload logo to GCS: {}", e.getMessage(), e);
                throw new IOException("Failed to upload logo to Google Cloud Storage: " + e.getMessage(), e);
            }
        } else if ("s3".equalsIgnoreCase(storageProvider) && s3Client != null) {
            // Upload to AWS S3
            try {
                PutObjectRequest putRequest = PutObjectRequest.builder()
                        .bucket(s3LogoBucket)
                        .key(logoPath)
                        .contentType(contentType)
                        .metadata(java.util.Map.of(
                                "organizationId", organizationId.toString(),
                                "originalFilename", originalFilename != null ? originalFilename : filename
                        ))
                        .build();

                s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));
                logger.info("✓ Successfully uploaded logo to AWS S3: {} ({} bytes)", logoPath, fileBytes.length);
            } catch (Exception e) {
                logger.error("✗ Failed to upload logo to S3: {}", e.getMessage(), e);
                throw new IOException("Failed to upload logo to AWS S3: " + e.getMessage(), e);
            }
        } else if (azureContainerClient != null) {
            // Upload to Azure Blob Storage (default)
            try {
                BlobClient blobClient = azureContainerClient.getBlobClient(logoPath);

                // Delete existing blob to ensure clean slate
                if (blobClient.exists()) {
                    logger.info("Deleting existing blob before re-upload: {}", logoPath);
                    blobClient.delete();
                }

                // Simple upload with overwrite using BinaryData (prevents text encoding issues)
                logger.info("Uploading {} bytes to Azure: {}", fileBytes.length, logoPath);
                com.azure.core.util.BinaryData binaryData = com.azure.core.util.BinaryData.fromBytes(fileBytes);
                blobClient.upload(binaryData, true);

                // Set HTTP headers immediately after upload
                com.azure.storage.blob.models.BlobHttpHeaders headers = new com.azure.storage.blob.models.BlobHttpHeaders()
                        .setContentType(contentType);
                blobClient.setHttpHeaders(headers);

                // Set metadata
                blobClient.setMetadata(java.util.Map.of(
                        "organizationId", organizationId.toString(),
                        "originalFilename", originalFilename != null ? originalFilename : filename
                ));

                // Verify upload by reading back the blob size
                com.azure.storage.blob.models.BlobProperties props = blobClient.getProperties();
                logger.info("✓ Successfully uploaded logo to Azure Blob Storage: {} (uploaded: {} bytes, verified: {} bytes, content-type: {})",
                    logoPath, fileBytes.length, props.getBlobSize(), props.getContentType());

                if (props.getBlobSize() != fileBytes.length) {
                    String errorMsg = String.format("SIZE MISMATCH! Uploaded %d bytes but blob size is %d", fileBytes.length, props.getBlobSize());
                    logger.error("✗ {}", errorMsg);
                    throw new IOException(errorMsg);
                }
            } catch (IOException e) {
                // Re-throw IOException (already logged above)
                throw e;
            } catch (Exception e) {
                logger.error("✗ Failed to upload logo to Azure: {}", e.getMessage(), e);
                throw new IOException("Failed to upload logo to Azure Blob Storage: " + e.getMessage(), e);
            }
        } else {
            throw new IOException("Cloud storage is not properly configured. Container client is null.");
        }
    }

    /**
     * Delete logo from cloud storage (Azure, GCS, or S3)
     */
    private void deleteFromCloudStorage(String filename) {
        String logoPath = "org-logos/" + filename;

        if ("gcs".equalsIgnoreCase(storageProvider) && gcsStorage != null) {
            // Delete from Google Cloud Storage
            try {
                BlobId blobId = BlobId.of(gcsLogoBucket, logoPath);
                boolean deleted = gcsStorage.delete(blobId);
                if (deleted) {
                    logger.info("Deleted logo from Google Cloud Storage: {}", logoPath);
                }
            } catch (Exception e) {
                logger.error("Failed to delete logo from GCS: {}", filename, e);
            }
        } else if ("s3".equalsIgnoreCase(storageProvider) && s3Client != null) {
            // Delete from AWS S3
            try {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(s3LogoBucket)
                        .key(logoPath)
                        .build();

                s3Client.deleteObject(deleteRequest);
                logger.info("Deleted logo from AWS S3: {}", logoPath);
            } catch (Exception e) {
                logger.error("Failed to delete logo from S3: {}", filename, e);
            }
        } else if (azureContainerClient != null) {
            // Delete from Azure Blob Storage (default)
            try {
                BlobClient blobClient = azureContainerClient.getBlobClient(logoPath);
                if (blobClient.exists()) {
                    blobClient.delete();
                    logger.info("Deleted logo from Azure Blob Storage: {}", logoPath);
                }
            } catch (Exception e) {
                logger.error("Failed to delete logo from Azure: {}", filename, e);
            }
        }
    }
}

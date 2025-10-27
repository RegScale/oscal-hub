package gov.nist.oscal.tools.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AWS S3 Storage implementation of StorageService
 * Uses S3 buckets for OSCAL component builder files
 *
 * Activated when: storage.provider=s3 (for AWS deployments)
 */
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    @Value("${aws.s3.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.s3.bucket-build:oscal-tools-build-prod}")
    private String buildBucketName;

    @Value("${aws.s3.build-folder:build}")
    private String buildFolder;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private S3Client s3Client;
    private boolean useLocalStorage = false;
    private Path localBuildPath;

    @PostConstruct
    public void init() {
        logger.info("=================================================================");
        logger.info("Storage Provider: AWS S3 (storage.provider=s3)");
        logger.info("=================================================================");

        try {
            logger.info("Initializing S3 client for region: {}", awsRegion);

            // Create S3 client (uses IAM role credentials when running on EC2/EB)
            s3Client = S3Client.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            // Test S3 connection by checking if bucket exists
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(buildBucketName)
                    .build();

            s3Client.headBucket(headBucketRequest);
            logger.info("S3 bucket '{}' is accessible", buildBucketName);
            logger.info("AWS S3 storage initialized successfully");

        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                logger.warn("S3 bucket '{}' does not exist. Attempting to create...", buildBucketName);
                createBucket();
            } else if (e.statusCode() == 403) {
                logger.error("Access denied to S3 bucket '{}'. Check IAM permissions.", buildBucketName);
                logger.error("Required permissions: s3:GetBucketLocation, s3:ListBucket, s3:GetObject, s3:PutObject, s3:DeleteObject");
                fallbackToLocalStorage();
            } else {
                logger.error("Failed to connect to S3: {} (Status: {})", e.getMessage(), e.statusCode());
                fallbackToLocalStorage();
            }
        } catch (Exception e) {
            logger.error("Failed to initialize S3 client: {}", e.getMessage(), e);
            logger.info("Ensure AWS credentials are configured:");
            logger.info("  1. IAM role attached to EC2/Elastic Beanstalk instance, OR");
            logger.info("  2. AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY environment variables, OR");
            logger.info("  3. ~/.aws/credentials file configured");
            fallbackToLocalStorage();
        }
    }

    private void createBucket() {
        try {
            CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(buildBucketName)
                    .build();

            s3Client.createBucket(createBucketRequest);
            logger.info("Created S3 bucket: {}", buildBucketName);

            // Enable versioning
            PutBucketVersioningRequest versioningRequest = PutBucketVersioningRequest.builder()
                    .bucket(buildBucketName)
                    .versioningConfiguration(VersioningConfiguration.builder()
                            .status(BucketVersioningStatus.ENABLED)
                            .build())
                    .build();

            s3Client.putBucketVersioning(versioningRequest);
            logger.info("Enabled versioning for bucket: {}", buildBucketName);

        } catch (S3Exception e) {
            logger.error("Failed to create S3 bucket: {} (Status: {})", e.getMessage(), e.statusCode());
            fallbackToLocalStorage();
        }
    }

    private void fallbackToLocalStorage() {
        logger.warn("Falling back to local file storage");
        useLocalStorage = true;
        localBuildPath = Paths.get(uploadDir, buildFolder);
        try {
            Files.createDirectories(localBuildPath);
            logger.info("Local storage initialized at: {}", localBuildPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create local directory: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (s3Client != null) {
            s3Client.close();
            logger.info("S3 client closed");
        }
    }

    @Override
    public String uploadComponent(String username, String filename, String content, Map<String, String> metadata) {
        String key = buildPath(username, filename);

        if (useLocalStorage) {
            saveToLocalStorage(content, key);
            return key;
        }

        try {
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(buildBucketName)
                    .key(key)
                    .contentType("application/json")
                    .contentLength((long) contentBytes.length);

            if (metadata != null && !metadata.isEmpty()) {
                requestBuilder.metadata(metadata);
            }

            PutObjectRequest request = requestBuilder.build();

            s3Client.putObject(request, RequestBody.fromBytes(contentBytes));

            logger.info("Uploaded component to S3: {}/{}", buildBucketName, key);
            return key;

        } catch (S3Exception e) {
            logger.error("Failed to upload to S3: {} (Status: {})", e.getMessage(), e.statusCode());
            throw new RuntimeException("Failed to upload component", e);
        }
    }

    @Override
    public String downloadComponent(String key) {
        if (useLocalStorage) {
            return getFromLocalStorage(key);
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(buildBucketName)
                    .key(key)
                    .build();

            byte[] objectBytes = s3Client.getObjectAsBytes(getObjectRequest).asByteArray();

            logger.info("Downloaded component from S3: {}/{}", buildBucketName, key);
            return new String(objectBytes, StandardCharsets.UTF_8);

        } catch (NoSuchKeyException e) {
            logger.error("Component not found in S3: {}", key);
            throw new RuntimeException("Component not found: " + key);
        } catch (S3Exception e) {
            logger.error("Failed to download from S3: {} (Status: {})", e.getMessage(), e.statusCode());
            throw new RuntimeException("Failed to download component", e);
        }
    }

    @Override
    public List<String> listUserComponents(String username) {
        String prefix = buildFolder + "/" + username + "/";

        if (useLocalStorage) {
            return listFromLocalStorage(username);
        }

        List<String> componentPaths = new ArrayList<>();

        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(buildBucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            componentPaths = listResponse.contents().stream()
                    .map(S3Object::key)
                    .filter(key -> !key.endsWith("/"))  // Exclude "directory" markers
                    .collect(Collectors.toList());

            logger.info("Listed {} components for user: {}", componentPaths.size(), username);
            return componentPaths;

        } catch (S3Exception e) {
            logger.error("Failed to list components from S3: {} (Status: {})", e.getMessage(), e.statusCode());
            throw new RuntimeException("Failed to list user components", e);
        }
    }

    @Override
    public boolean deleteComponent(String key) {
        if (useLocalStorage) {
            return deleteFromLocalStorage(key);
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(buildBucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);

            logger.info("Deleted component from S3: {}/{}", buildBucketName, key);
            return true;

        } catch (S3Exception e) {
            logger.error("Failed to delete from S3: {} (Status: {})", e.getMessage(), e.statusCode());
            return false;
        }
    }

    @Override
    public boolean componentExists(String key) {
        if (useLocalStorage) {
            Path filePath = localBuildPath.resolve(key.replace(buildFolder + "/", ""));
            return Files.exists(filePath);
        }

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(buildBucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            logger.error("Failed to check if component exists: {} (Status: {})", e.getMessage(), e.statusCode());
            return false;
        }
    }

    @Override
    public long getFileSize(String key) {
        if (useLocalStorage) {
            try {
                Path filePath = localBuildPath.resolve(key.replace(buildFolder + "/", ""));
                return Files.size(filePath);
            } catch (IOException e) {
                logger.error("Failed to get file size from local storage: {}", e.getMessage(), e);
                return 0;
            }
        }

        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(buildBucketName)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headRequest);
            return response.contentLength();

        } catch (S3Exception e) {
            logger.error("Failed to get file size from S3: {} (Status: {})", e.getMessage(), e.statusCode());
            throw new RuntimeException("Failed to get file size", e);
        }
    }

    @Override
    public String buildPath(String username, String filename) {
        String sanitizedFileName = sanitizeFileName(filename);
        return String.format("%s/%s/%s", buildFolder, username, sanitizedFileName);
    }

    @Override
    public boolean isConfigured() {
        return s3Client != null || useLocalStorage;
    }

    @Override
    public String getStorageProvider() {
        return useLocalStorage ? "Local File System (S3 fallback)" : "AWS S3";
    }

    /**
     * Sanitize filename to remove problematic characters
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // Local storage fallback methods (same as Azure implementation)

    private void saveToLocalStorage(String content, String key) {
        try {
            String relativePath = key.replace(buildFolder + "/", "");
            Path filePath = localBuildPath.resolve(relativePath);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            logger.info("Saved component to local storage: {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to save component to local storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save component", e);
        }
    }

    private String getFromLocalStorage(String key) {
        try {
            String relativePath = key.replace(buildFolder + "/", "");
            Path filePath = localBuildPath.resolve(relativePath);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("Component not found: " + key);
            }
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read component from local storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read component", e);
        }
    }

    private List<String> listFromLocalStorage(String username) {
        List<String> componentPaths = new ArrayList<>();
        try {
            Path userPath = localBuildPath.resolve(username);
            if (Files.exists(userPath)) {
                Files.walk(userPath)
                        .filter(Files::isRegularFile)
                        .forEach(path -> {
                            String relativePath = localBuildPath.relativize(path).toString();
                            componentPaths.add(buildFolder + "/" + relativePath);
                        });
            }
            logger.info("Listed {} components for user from local storage: {}", componentPaths.size(), username);
            return componentPaths;
        } catch (IOException e) {
            logger.error("Failed to list user components from local storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to list user components", e);
        }
    }

    private boolean deleteFromLocalStorage(String key) {
        try {
            String relativePath = key.replace(buildFolder + "/", "");
            Path filePath = localBuildPath.resolve(relativePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Deleted component from local storage: {}", filePath.toAbsolutePath());
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error("Failed to delete component from local storage: {}", e.getMessage(), e);
            return false;
        }
    }
}

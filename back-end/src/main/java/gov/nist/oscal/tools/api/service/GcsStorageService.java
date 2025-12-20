package gov.nist.oscal.tools.api.service;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import gov.nist.oscal.tools.api.util.PathSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

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
import java.util.stream.StreamSupport;

/**
 * Google Cloud Storage implementation of StorageService
 * Uses GCS buckets for OSCAL component builder files
 *
 * Activated when: storage.provider=gcs (for Google Cloud deployments)
 */
@Service
@ConditionalOnProperty(name = "storage.provider", havingValue = "gcs")
public class GcsStorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(GcsStorageService.class);

    @Value("${gcp.project-id:}")
    private String projectId;

    @Value("${gcp.storage.bucket-build:oscal-tools-build-prod}")
    private String buildBucketName;

    @Value("${gcp.storage.build-folder:build}")
    private String buildFolder;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private Storage storage;
    private boolean useLocalStorage = false;
    private Path localBuildPath;

    @PostConstruct
    public void init() {
        logger.info("=================================================================");
        logger.info("Storage Provider: Google Cloud Storage (storage.provider=gcs)");
        logger.info("=================================================================");

        try {
            logger.info("Initializing GCS client for project: {}", projectId);

            // Create GCS client (uses Application Default Credentials when running on GCP)
            // ADC automatically uses:
            // 1. Cloud Run service account
            // 2. Compute Engine service account
            // 3. GOOGLE_APPLICATION_CREDENTIALS environment variable
            storage = StorageOptions.newBuilder()
                    .setProjectId(projectId)
                    .build()
                    .getService();

            // Test GCS connection by checking if bucket exists
            Bucket bucket = storage.get(buildBucketName);

            if (bucket == null) {
                logger.warn("GCS bucket '{}' does not exist. Attempting to create...", buildBucketName);
                createBucket();
            } else {
                logger.info("GCS bucket '{}' is accessible", buildBucketName);
                logger.info("Google Cloud Storage initialized successfully");
            }

        } catch (StorageException e) {
            if (e.getCode() == 403) {
                logger.error("Access denied to GCS bucket '{}'. Check service account permissions.", buildBucketName);
                logger.error("Required permissions: storage.buckets.get, storage.objects.create, storage.objects.delete, storage.objects.get, storage.objects.list");
                fallbackToLocalStorage();
            } else if (e.getCode() == 404) {
                logger.warn("GCS bucket '{}' not found. Attempting to create...", buildBucketName);
                createBucket();
            } else {
                logger.error("Failed to connect to GCS: {} (Code: {})", e.getMessage(), e.getCode());
                fallbackToLocalStorage();
            }
        } catch (Exception e) {
            logger.error("Failed to initialize GCS client: {}", e.getMessage(), e);
            logger.info("Ensure Google Cloud credentials are configured:");
            logger.info("  1. Running on Cloud Run with service account, OR");
            logger.info("  2. Running on Compute Engine with service account, OR");
            logger.info("  3. GOOGLE_APPLICATION_CREDENTIALS environment variable set, OR");
            logger.info("  4. gcloud auth application-default login executed");
            fallbackToLocalStorage();
        }
    }

    private void createBucket() {
        try {
            // Create bucket with best practices for production
            BucketInfo bucketInfo = BucketInfo.newBuilder(buildBucketName)
                    // Set location (single region for lower latency, multi-region for HA)
                    .setLocation("US") // Change to specific region like "us-central1" if needed
                    // Enable versioning for data protection
                    .setVersioningEnabled(true)
                    // Set storage class (STANDARD, NEARLINE, COLDLINE, ARCHIVE)
                    .setStorageClass(StorageClass.STANDARD)
                    // Uniform bucket-level access (recommended over ACLs)
                    .setIamConfiguration(
                            BucketInfo.IamConfiguration.newBuilder()
                                    .setIsUniformBucketLevelAccessEnabled(true)
                                    .build()
                    )
                    .build();

            Bucket bucket = storage.create(bucketInfo);
            logger.info("Created GCS bucket: {}", buildBucketName);
            logger.info("Bucket location: {}", bucket.getLocation());
            logger.info("Versioning enabled: {}", bucket.versioningEnabled());

        } catch (StorageException e) {
            logger.error("Failed to create GCS bucket: {} (Code: {})", e.getMessage(), e.getCode());
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
        if (storage != null) {
            try {
                storage.close();
                logger.info("GCS client closed");
            } catch (Exception e) {
                logger.warn("Error closing GCS client: {}", e.getMessage());
            }
        }
    }

    @Override
    public String uploadComponent(String username, String filename, String content, Map<String, String> metadata) {
        String blobName = buildPath(username, filename);

        if (useLocalStorage) {
            saveToLocalStorage(content, blobName);
            return blobName;
        }

        try {
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

            BlobId blobId = BlobId.of(buildBucketName, blobName);
            BlobInfo.Builder blobInfoBuilder = BlobInfo.newBuilder(blobId)
                    .setContentType("application/json");

            // Add custom metadata if provided
            if (metadata != null && !metadata.isEmpty()) {
                blobInfoBuilder.setMetadata(metadata);
            }

            BlobInfo blobInfo = blobInfoBuilder.build();

            // Upload with retry strategy
            storage.create(blobInfo, contentBytes);

            logger.info("Uploaded component to GCS: {}/{}", buildBucketName, blobName);
            return blobName;

        } catch (StorageException e) {
            logger.error("Failed to upload to GCS: {} (Code: {})", e.getMessage(), e.getCode());
            throw new RuntimeException("Failed to upload component", e);
        }
    }

    @Override
    public String downloadComponent(String blobName) {
        if (useLocalStorage) {
            return getFromLocalStorage(blobName);
        }

        try {
            BlobId blobId = BlobId.of(buildBucketName, blobName);
            Blob blob = storage.get(blobId);

            if (blob == null) {
                logger.error("Component not found in GCS: {}", blobName);
                throw new RuntimeException("Component not found: " + blobName);
            }

            byte[] content = blob.getContent();

            logger.info("Downloaded component from GCS: {}/{}", buildBucketName, blobName);
            return new String(content, StandardCharsets.UTF_8);

        } catch (StorageException e) {
            logger.error("Failed to download from GCS: {} (Code: {})", e.getMessage(), e.getCode());
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
            // List objects with prefix
            Page<Blob> blobs = storage.list(buildBucketName,
                    Storage.BlobListOption.prefix(prefix),
                    Storage.BlobListOption.currentDirectory());

            componentPaths = StreamSupport.stream(blobs.iterateAll().spliterator(), false)
                    .map(Blob::getName)
                    .filter(name -> !name.endsWith("/"))  // Exclude "directory" markers
                    .collect(Collectors.toList());

            logger.info("Listed {} components for user: {}", componentPaths.size(), username);
            return componentPaths;

        } catch (StorageException e) {
            logger.error("Failed to list components from GCS: {} (Code: {})", e.getMessage(), e.getCode());
            throw new RuntimeException("Failed to list user components", e);
        }
    }

    @Override
    public boolean deleteComponent(String blobName) {
        if (useLocalStorage) {
            return deleteFromLocalStorage(blobName);
        }

        try {
            BlobId blobId = BlobId.of(buildBucketName, blobName);
            boolean deleted = storage.delete(blobId);

            if (deleted) {
                logger.info("Deleted component from GCS: {}/{}", buildBucketName, blobName);
            } else {
                logger.warn("Component not found for deletion: {}", blobName);
            }

            return deleted;

        } catch (StorageException e) {
            logger.error("Failed to delete from GCS: {} (Code: {})", e.getMessage(), e.getCode());
            return false;
        }
    }

    @Override
    public boolean componentExists(String blobName) {
        if (useLocalStorage) {
            Path filePath = localBuildPath.resolve(blobName.replace(buildFolder + "/", ""));
            return Files.exists(filePath);
        }

        try {
            BlobId blobId = BlobId.of(buildBucketName, blobName);
            Blob blob = storage.get(blobId);
            return blob != null && blob.exists();

        } catch (StorageException e) {
            logger.error("Failed to check if component exists: {} (Code: {})", e.getMessage(), e.getCode());
            return false;
        }
    }

    @Override
    public long getFileSize(String blobName) {
        if (useLocalStorage) {
            try {
                Path filePath = localBuildPath.resolve(blobName.replace(buildFolder + "/", ""));
                return Files.size(filePath);
            } catch (IOException e) {
                logger.error("Failed to get file size from local storage: {}", e.getMessage(), e);
                return 0;
            }
        }

        try {
            BlobId blobId = BlobId.of(buildBucketName, blobName);
            Blob blob = storage.get(blobId);

            if (blob == null) {
                throw new RuntimeException("Component not found: " + blobName);
            }

            return blob.getSize();

        } catch (StorageException e) {
            logger.error("Failed to get file size from GCS: {} (Code: {})", e.getMessage(), e.getCode());
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
        return storage != null || useLocalStorage;
    }

    @Override
    public String getStorageProvider() {
        return useLocalStorage ? "Local File System (GCS fallback)" : "Google Cloud Storage";
    }

    /**
     * Sanitize filename to remove problematic characters
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // Local storage fallback methods

    private void saveToLocalStorage(String content, String key) {
        try {
            String relativePath = key.replace(buildFolder + "/", "");
            // Use PathSanitizer to prevent path traversal attacks
            Path filePath = PathSanitizer.safeResolve(localBuildPath, relativePath);
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
            // Use PathSanitizer to prevent path traversal attacks
            Path filePath = PathSanitizer.safeResolve(localBuildPath, relativePath);
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
            // Sanitize username to prevent path traversal
            String sanitizedUsername = PathSanitizer.sanitizeFilename(username);
            Path userPath = PathSanitizer.safeResolve(localBuildPath, sanitizedUsername);
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

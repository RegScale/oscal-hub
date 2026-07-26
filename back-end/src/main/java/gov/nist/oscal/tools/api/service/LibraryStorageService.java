package gov.nist.oscal.tools.api.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import gov.nist.oscal.tools.api.exception.LibraryFileNotFoundException;
import gov.nist.oscal.tools.api.util.PathSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Storage for library files (published catalogs, profiles, etc.).
 *
 * Provider selection follows {@code storage.provider}:
 * <ul>
 *   <li>{@code gcs} — Google Cloud Storage under the {@code library/} folder of the
 *       build bucket. This is the production (Cloud Run) configuration: Cloud Run's
 *       local filesystem is ephemeral, so local storage there silently loses files
 *       across revisions/instances (root cause of "Library file not found" 500s).</li>
 *   <li>{@code azure} (default) — Azure Blob Storage when a connection string is
 *       configured, otherwise local file storage (development).</li>
 * </ul>
 */
@Service
public class LibraryStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LibraryStorageService.class);

    @Value("${storage.provider:azure}")
    private String storageProvider;

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.library-container-name:oscal-library}")
    private String libraryContainerName;

    @Value("${gcp.project-id:}")
    private String gcpProjectId;

    @Value("${gcp.storage.bucket-build:oscal-tools-build-prod}")
    private String gcsBucketName;

    @Value("${gcp.storage.library-folder:library}")
    private String gcsLibraryFolder;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;
    private Storage gcsStorage;
    private boolean useGcs = false;
    private boolean useLocalStorage = false;
    private Path localLibraryPath;

    @PostConstruct
    public void init() {
        if ("gcs".equalsIgnoreCase(storageProvider)) {
            initGcs();
            return;
        }

        // Check if Azure Storage is configured
        if (connectionString == null || connectionString.trim().isEmpty()) {
            logger.warn("Azure Blob Storage connection string not configured. Using local file storage for library.");
            logger.info("To enable Azure Blob Storage:");
            logger.info("  1. For local development: Create .env file (copy .env.example and add your connection string)");
            logger.info("  2. For production: Set AZURE_STORAGE_CONNECTION_STRING environment variable");
            initLocalStorage();
            return;
        }

        try {
            logger.info("Initializing Azure Blob Storage client for library...");
            blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            containerClient = blobServiceClient.getBlobContainerClient(libraryContainerName);

            // Create container if it doesn't exist
            if (!containerClient.exists()) {
                containerClient.create();
                logger.info("Created library blob container: {}", libraryContainerName);
            } else {
                logger.info("Using existing library blob container: {}", libraryContainerName);
            }

            logger.info("Azure Blob Storage for library initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Azure Blob Storage for library: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize Azure Blob Storage for library", e);
        }
    }

    private void initGcs() {
        try {
            logger.info("Initializing GCS client for library storage (bucket: {}, folder: {})",
                    gcsBucketName, gcsLibraryFolder);
            // Uses Application Default Credentials (Cloud Run service account in prod)
            gcsStorage = StorageOptions.newBuilder()
                    .setProjectId(gcpProjectId)
                    .build()
                    .getService();
            useGcs = true;
            logger.info("GCS library storage initialized successfully");
        } catch (Exception e) {
            // Do NOT fall back to local storage on Cloud Run: files written locally are
            // silently lost when the instance is replaced, corrupting the library.
            logger.error("Failed to initialize GCS client for library storage: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize GCS library storage", e);
        }
    }

    private void initLocalStorage() {
        useLocalStorage = true;
        localLibraryPath = Paths.get(uploadDir, "library");
        try {
            Files.createDirectories(localLibraryPath);
            logger.info("Library local storage initialized at: {}", localLibraryPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to create library directory: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize library storage", e);
        }
    }

    private String gcsObjectName(String blobPath) {
        return gcsLibraryFolder + "/" + blobPath;
    }

    /**
     * Save a library file version to storage (GCS, Azure, or local)
     *
     * @param content File content
     * @param blobPath Path in storage (e.g., "itemId/versionId/filename.json")
     * @param metadata Metadata to attach to the file
     * @return True if successful
     */
    public boolean saveLibraryFile(String content, String blobPath, Map<String, String> metadata) {
        if (useGcs) {
            return saveToGcs(content, blobPath, metadata);
        }

        if (useLocalStorage) {
            return saveToLocalStorage(content, blobPath);
        }

        if (containerClient == null) {
            throw new RuntimeException("Storage is not configured");
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobPath);
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

            // Upload file
            blobClient.upload(new ByteArrayInputStream(contentBytes), contentBytes.length, true);

            // Set metadata if provided
            if (metadata != null && !metadata.isEmpty()) {
                blobClient.setMetadata(metadata);
            }

            logger.info("Saved library file to Azure Blob Storage: {}", blobPath);
            return true;
        } catch (Exception e) {
            logger.error("Failed to save library file to Azure Blob Storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save library file", e);
        }
    }

    private boolean saveToGcs(String content, String blobPath, Map<String, String> metadata) {
        try {
            BlobId blobId = BlobId.of(gcsBucketName, gcsObjectName(blobPath));
            BlobInfo.Builder builder = BlobInfo.newBuilder(blobId)
                    .setContentType("application/json");
            if (metadata != null && !metadata.isEmpty()) {
                builder.setMetadata(metadata);
            }
            gcsStorage.create(builder.build(), content.getBytes(StandardCharsets.UTF_8));
            logger.info("Saved library file to GCS: {}/{}", gcsBucketName, gcsObjectName(blobPath));
            return true;
        } catch (Exception e) {
            logger.error("Failed to save library file to GCS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save library file", e);
        }
    }

    private boolean saveToLocalStorage(String content, String blobPath) {
        try {
            // Use PathSanitizer to prevent path traversal attacks
            Path filePath = PathSanitizer.safeResolve(localLibraryPath, blobPath);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            logger.info("Saved library file to local storage: {}", filePath.toAbsolutePath());
            return true;
        } catch (IOException e) {
            logger.error("Failed to save library file to local storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save library file", e);
        }
    }

    /**
     * Get library file content from storage (GCS, Azure, or local)
     *
     * @param blobPath Path in storage
     * @return File content as string
     */
    public String getLibraryFileContent(String blobPath) {
        if (useGcs) {
            return getFromGcs(blobPath);
        }

        if (useLocalStorage) {
            return getFromLocalStorage(blobPath);
        }

        if (containerClient == null) {
            throw new RuntimeException("Storage is not configured");
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobPath);

            if (!blobClient.exists()) {
                throw new LibraryFileNotFoundException(blobPath);
            }

            BinaryData content = blobClient.downloadContent();
            return content.toString();
        } catch (LibraryFileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to get library file content: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get library file content", e);
        }
    }

    private String getFromGcs(String blobPath) {
        try {
            Blob blob = gcsStorage.get(BlobId.of(gcsBucketName, gcsObjectName(blobPath)));
            if (blob == null || !blob.exists()) {
                throw new LibraryFileNotFoundException(blobPath);
            }
            return new String(blob.getContent(), StandardCharsets.UTF_8);
        } catch (LibraryFileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to get library file from GCS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get library file content", e);
        }
    }

    private String getFromLocalStorage(String blobPath) {
        try {
            Path filePath = localLibraryPath.resolve(blobPath);
            if (!Files.exists(filePath)) {
                throw new LibraryFileNotFoundException(blobPath);
            }
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read library file from local storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read library file", e);
        }
    }

    /**
     * Delete a library file from storage (GCS, Azure, or local)
     *
     * @param blobPath Path in storage
     * @return True if successful
     */
    public boolean deleteLibraryFile(String blobPath) {
        if (useGcs) {
            try {
                boolean deleted = gcsStorage.delete(BlobId.of(gcsBucketName, gcsObjectName(blobPath)));
                if (deleted) {
                    logger.info("Deleted library file from GCS: {}/{}", gcsBucketName, gcsObjectName(blobPath));
                }
                return deleted;
            } catch (Exception e) {
                logger.error("Failed to delete library file from GCS: {}", e.getMessage(), e);
                return false;
            }
        }

        if (useLocalStorage) {
            try {
                Path filePath = PathSanitizer.safeResolve(localLibraryPath, blobPath);
                return Files.deleteIfExists(filePath);
            } catch (IOException e) {
                logger.error("Failed to delete library file from local storage: {}", e.getMessage(), e);
                return false;
            }
        }

        if (containerClient == null) {
            throw new RuntimeException("Azure Blob Storage is not configured. Please set AZURE_STORAGE_CONNECTION_STRING environment variable.");
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobPath);

            if (blobClient.exists()) {
                blobClient.delete();
                logger.info("Deleted library file from Azure Blob Storage: {}", blobPath);
                return true;
            }

            return false;
        } catch (Exception e) {
            logger.error("Failed to delete library file: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if a library file exists in storage (GCS, Azure, or local)
     *
     * @param blobPath Path in storage
     * @return True if file exists
     */
    public boolean fileExists(String blobPath) {
        if (useGcs) {
            try {
                Blob blob = gcsStorage.get(BlobId.of(gcsBucketName, gcsObjectName(blobPath)));
                return blob != null && blob.exists();
            } catch (Exception e) {
                logger.error("Failed to check if library file exists in GCS: {}", e.getMessage(), e);
                return false;
            }
        }

        if (useLocalStorage) {
            try {
                return Files.exists(PathSanitizer.safeResolve(localLibraryPath, blobPath));
            } catch (Exception e) {
                return false;
            }
        }

        if (containerClient == null) {
            return false;
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobPath);
            return blobClient.exists();
        } catch (Exception e) {
            logger.error("Failed to check if library file exists: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get file size in bytes
     *
     * @param blobPath Path in storage
     * @return File size in bytes
     */
    public long getFileSize(String blobPath) {
        if (useGcs) {
            try {
                Blob blob = gcsStorage.get(BlobId.of(gcsBucketName, gcsObjectName(blobPath)));
                if (blob == null) {
                    throw new LibraryFileNotFoundException(blobPath);
                }
                return blob.getSize();
            } catch (LibraryFileNotFoundException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Failed to get file size from GCS: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to get file size", e);
            }
        }

        if (useLocalStorage) {
            try {
                return Files.size(PathSanitizer.safeResolve(localLibraryPath, blobPath));
            } catch (IOException e) {
                logger.error("Failed to get file size from local storage: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to get file size", e);
            }
        }

        if (containerClient == null) {
            throw new RuntimeException("Azure Blob Storage is not configured.");
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobPath);
            return blobClient.getProperties().getBlobSize();
        } catch (Exception e) {
            logger.error("Failed to get file size: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get file size", e);
        }
    }

    /**
     * Build blob path for library file version
     * Format: {itemId}/{versionId}/{filename}
     *
     * @param itemId Library item ID
     * @param versionId Version ID
     * @param fileName Original filename
     * @return Blob path
     */
    public String buildBlobPath(String itemId, String versionId, String fileName) {
        String sanitizedFileName = sanitizeFileName(fileName);
        return String.format("%s/%s/%s", itemId, versionId, sanitizedFileName);
    }

    /**
     * Sanitize filename to remove problematic characters
     *
     * @param fileName Original filename
     * @return Sanitized filename
     */
    private String sanitizeFileName(String fileName) {
        // Remove or replace characters that might cause issues
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Check if storage is configured
     *
     * @return True if configured and ready
     */
    public boolean isConfigured() {
        return useGcs || containerClient != null || useLocalStorage;
    }
}

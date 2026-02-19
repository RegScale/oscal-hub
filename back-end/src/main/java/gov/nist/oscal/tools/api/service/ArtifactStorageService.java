package gov.nist.oscal.tools.api.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
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
 * Service for handling Azure Blob Storage operations for artifact files.
 * Uses a separate container from library files for better organization.
 */
@Service
public class ArtifactStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactStorageService.class);

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.artifact-container-name:oscal-artifacts}")
    private String artifactContainerName;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;
    private boolean useLocalStorage = false;
    private Path localArtifactPath;

    @PostConstruct
    public void init() {
        // Check if Azure Storage is configured
        if (connectionString == null || connectionString.trim().isEmpty()) {
            logger.warn("Azure Blob Storage connection string not configured. Using local file storage for artifacts.");

            // Use local file storage as fallback
            useLocalStorage = true;
            localArtifactPath = Paths.get(uploadDir, "artifacts");
            try {
                Files.createDirectories(localArtifactPath);
                logger.info("Artifact local storage initialized at: {}", localArtifactPath.toAbsolutePath());
            } catch (IOException e) {
                logger.error("Failed to create artifact directory: {}", e.getMessage(), e);
                throw new RuntimeException("Could not initialize artifact storage", e);
            }
            return;
        }

        try {
            logger.info("Initializing Azure Blob Storage client for artifacts...");
            blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            containerClient = blobServiceClient.getBlobContainerClient(artifactContainerName);

            // Create container if it doesn't exist
            if (!containerClient.exists()) {
                containerClient.create();
                logger.info("Created artifact blob container: {}", artifactContainerName);
            } else {
                logger.info("Using existing artifact blob container: {}", artifactContainerName);
            }

            logger.info("Azure Blob Storage for artifacts initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Azure Blob Storage for artifacts: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize Azure Blob Storage for artifacts", e);
        }
    }

    /**
     * Save an artifact file version to storage (Azure or local)
     *
     * @param content  File content
     * @param blobPath Path in storage (e.g., "artifactId/versionId/content.md")
     * @param metadata Metadata to attach to the file
     * @return True if successful
     */
    public boolean saveArtifactFile(String content, String blobPath, Map<String, String> metadata) {
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

            logger.info("Saved artifact file to Azure Blob Storage: {}", blobPath);
            return true;
        } catch (Exception e) {
            logger.error("Failed to save artifact file to Azure Blob Storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save artifact file", e);
        }
    }

    private boolean saveToLocalStorage(String content, String blobPath) {
        try {
            // Use PathSanitizer to prevent path traversal attacks
            Path filePath = PathSanitizer.safeResolve(localArtifactPath, blobPath);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            logger.info("Saved artifact file to local storage: {}", filePath.toAbsolutePath());
            return true;
        } catch (IOException e) {
            logger.error("Failed to save artifact file to local storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save artifact file", e);
        }
    }

    /**
     * Get artifact file content from storage (Azure or local)
     *
     * @param blobPath Path in storage
     * @return File content as string
     */
    public String getArtifactFileContent(String blobPath) {
        if (useLocalStorage) {
            return getFromLocalStorage(blobPath);
        }

        if (containerClient == null) {
            throw new RuntimeException("Storage is not configured");
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobPath);

            if (!blobClient.exists()) {
                throw new RuntimeException("Artifact file not found: " + blobPath);
            }

            BinaryData content = blobClient.downloadContent();
            return content.toString();
        } catch (Exception e) {
            logger.error("Failed to get artifact file content: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get artifact file content", e);
        }
    }

    private String getFromLocalStorage(String blobPath) {
        try {
            Path filePath = localArtifactPath.resolve(blobPath);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("Artifact file not found: " + blobPath);
            }
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read artifact file from local storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read artifact file", e);
        }
    }

    /**
     * Delete an artifact file from storage
     *
     * @param blobPath Path in storage
     * @return True if successful
     */
    public boolean deleteArtifactFile(String blobPath) {
        if (useLocalStorage) {
            return deleteFromLocalStorage(blobPath);
        }

        if (containerClient == null) {
            throw new RuntimeException("Azure Blob Storage is not configured.");
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobPath);

            if (blobClient.exists()) {
                blobClient.delete();
                logger.info("Deleted artifact file from Azure Blob Storage: {}", blobPath);
                return true;
            }

            return false;
        } catch (Exception e) {
            logger.error("Failed to delete artifact file: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean deleteFromLocalStorage(String blobPath) {
        try {
            Path filePath = localArtifactPath.resolve(blobPath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Deleted artifact file from local storage: {}", filePath.toAbsolutePath());
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error("Failed to delete artifact file from local storage: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if an artifact file exists in storage
     *
     * @param blobPath Path in storage
     * @return True if file exists
     */
    public boolean fileExists(String blobPath) {
        if (useLocalStorage) {
            return Files.exists(localArtifactPath.resolve(blobPath));
        }

        if (containerClient == null) {
            return false;
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobPath);
            return blobClient.exists();
        } catch (Exception e) {
            logger.error("Failed to check if artifact file exists: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Build blob path for artifact file version
     * Format: {artifactId}/{versionId}/{filename}
     *
     * @param artifactId Artifact ID
     * @param versionId  Version ID
     * @param fileName   Filename (typically "content.md")
     * @return Blob path
     */
    public String buildBlobPath(String artifactId, String versionId, String fileName) {
        String sanitizedFileName = sanitizeFileName(fileName);
        return String.format("%s/%s/%s", artifactId, versionId, sanitizedFileName);
    }

    /**
     * Sanitize filename to remove problematic characters
     *
     * @param fileName Original filename
     * @return Sanitized filename
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Check if storage is configured
     *
     * @return True if configured and ready
     */
    public boolean isConfigured() {
        return containerClient != null || useLocalStorage;
    }
}

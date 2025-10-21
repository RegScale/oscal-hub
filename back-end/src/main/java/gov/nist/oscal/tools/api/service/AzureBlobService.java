package gov.nist.oscal.tools.api.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for handling Azure Blob Storage operations for component definitions
 * Uses a dedicated build container for OSCAL component builder files
 */
@Service
public class AzureBlobService {

    private static final Logger logger = LoggerFactory.getLogger(AzureBlobService.class);

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.build-container-name:oscal-build-storage}")
    private String buildContainerName;

    @Value("${azure.storage.build-folder:build}")
    private String buildFolder;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;
    private boolean useLocalStorage = false;
    private Path localBuildPath;

    @PostConstruct
    public void init() {
        // Check if Azure Storage is configured
        if (connectionString == null || connectionString.trim().isEmpty()) {
            logger.warn("Azure Blob Storage connection string not configured. Using local file storage for component builder.");
            logger.info("To enable Azure Blob Storage:");
            logger.info("  1. For local development: Create .env file (copy .env.example and add your connection string)");
            logger.info("  2. For production: Set AZURE_STORAGE_CONNECTION_STRING environment variable");

            // Use local file storage as fallback
            useLocalStorage = true;
            localBuildPath = Paths.get(uploadDir, buildFolder);
            try {
                Files.createDirectories(localBuildPath);
                logger.info("Build local storage initialized at: {}", localBuildPath.toAbsolutePath());
            } catch (IOException e) {
                logger.error("Failed to create build directory: {}", e.getMessage(), e);
                throw new RuntimeException("Could not initialize build storage", e);
            }
            return;
        }

        try {
            logger.info("Initializing Azure Blob Storage client for component builder...");
            blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            containerClient = blobServiceClient.getBlobContainerClient(buildContainerName);

            // Create container if it doesn't exist
            if (!containerClient.exists()) {
                containerClient.create();
                logger.info("Created build blob container: {}", buildContainerName);
            } else {
                logger.info("Using existing build blob container: {}", buildContainerName);
            }

            logger.info("Azure Blob Storage for component builder initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Azure Blob Storage for component builder: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize Azure Blob Storage for component builder", e);
        }
    }

    /**
     * Upload a component definition to storage
     *
     * @param username User's username
     * @param filename Original filename
     * @param jsonContent Component definition JSON content
     * @param metadata Additional metadata to attach
     * @return The blob path where the file was stored
     */
    public String uploadComponent(String username, String filename, String jsonContent, Map<String, String> metadata) {
        String blobPath = buildBlobPath(username, filename);

        if (useLocalStorage) {
            saveToLocalStorage(jsonContent, blobPath);
            return blobPath;
        }

        if (containerClient == null) {
            throw new RuntimeException("Storage is not configured");
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobPath);
            byte[] contentBytes = jsonContent.getBytes(StandardCharsets.UTF_8);

            // Upload file (overwrite if exists)
            blobClient.upload(new ByteArrayInputStream(contentBytes), contentBytes.length, true);

            // Set metadata if provided
            if (metadata != null && !metadata.isEmpty()) {
                blobClient.setMetadata(metadata);
            }

            logger.info("Uploaded component to Azure Blob Storage: {}", blobPath);
            return blobPath;
        } catch (Exception e) {
            logger.error("Failed to upload component to Azure Blob Storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload component", e);
        }
    }

    /**
     * Download a component definition from storage
     *
     * @param blobPath Path in blob storage (e.g., "build/username/component.json")
     * @return Component JSON content
     */
    public String downloadComponent(String blobPath) {
        if (useLocalStorage) {
            return getFromLocalStorage(blobPath);
        }

        if (containerClient == null) {
            throw new RuntimeException("Storage is not configured");
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobPath);

            if (!blobClient.exists()) {
                throw new RuntimeException("Component not found: " + blobPath);
            }

            BinaryData content = blobClient.downloadContent();
            logger.info("Downloaded component from Azure Blob Storage: {}", blobPath);
            return content.toString();
        } catch (Exception e) {
            logger.error("Failed to download component: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download component", e);
        }
    }

    /**
     * List all components for a specific user
     *
     * @param username User's username
     * @return List of blob paths for user's components
     */
    public List<String> listUserComponents(String username) {
        String userPrefix = buildFolder + "/" + username + "/";
        List<String> componentPaths = new ArrayList<>();

        if (useLocalStorage) {
            return listFromLocalStorage(username);
        }

        if (containerClient == null) {
            throw new RuntimeException("Storage is not configured");
        }

        try {
            for (BlobItem blobItem : containerClient.listBlobsByHierarchy(userPrefix)) {
                if (!blobItem.isPrefix()) {
                    componentPaths.add(blobItem.getName());
                }
            }
            logger.info("Listed {} components for user: {}", componentPaths.size(), username);
            return componentPaths;
        } catch (Exception e) {
            logger.error("Failed to list user components: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to list user components", e);
        }
    }

    /**
     * Delete a component definition from storage
     *
     * @param blobPath Path in blob storage
     * @return True if successful
     */
    public boolean deleteComponent(String blobPath) {
        if (useLocalStorage) {
            return deleteFromLocalStorage(blobPath);
        }

        if (containerClient == null) {
            throw new RuntimeException("Storage is not configured");
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobPath);

            if (blobClient.exists()) {
                blobClient.delete();
                logger.info("Deleted component from Azure Blob Storage: {}", blobPath);
                return true;
            }

            logger.warn("Component not found for deletion: {}", blobPath);
            return false;
        } catch (Exception e) {
            logger.error("Failed to delete component: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if a component exists in storage
     *
     * @param blobPath Path in blob storage
     * @return True if component exists
     */
    public boolean componentExists(String blobPath) {
        if (useLocalStorage) {
            Path filePath = localBuildPath.resolve(blobPath.replace(buildFolder + "/", ""));
            return Files.exists(filePath);
        }

        if (containerClient == null) {
            return false;
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(blobPath);
            return blobClient.exists();
        } catch (Exception e) {
            logger.error("Failed to check if component exists: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get file size in bytes
     *
     * @param blobPath Path in blob storage
     * @return File size in bytes
     */
    public long getFileSize(String blobPath) {
        if (useLocalStorage) {
            try {
                Path filePath = localBuildPath.resolve(blobPath.replace(buildFolder + "/", ""));
                return Files.size(filePath);
            } catch (IOException e) {
                logger.error("Failed to get file size from local storage: {}", e.getMessage(), e);
                return 0;
            }
        }

        if (containerClient == null) {
            throw new RuntimeException("Storage is not configured");
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
     * Build blob path for component definition
     * Format: build/{username}/{filename}
     *
     * @param username User's username
     * @param filename Original filename
     * @return Blob path
     */
    public String buildBlobPath(String username, String filename) {
        String sanitizedFileName = sanitizeFileName(filename);
        return String.format("%s/%s/%s", buildFolder, username, sanitizedFileName);
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
        return containerClient != null || useLocalStorage;
    }

    // Local storage fallback methods

    private void saveToLocalStorage(String content, String blobPath) {
        try {
            // Remove the "build/" prefix from the path for local storage
            String relativePath = blobPath.replace(buildFolder + "/", "");
            Path filePath = localBuildPath.resolve(relativePath);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            logger.info("Saved component to local storage: {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to save component to local storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save component", e);
        }
    }

    private String getFromLocalStorage(String blobPath) {
        try {
            String relativePath = blobPath.replace(buildFolder + "/", "");
            Path filePath = localBuildPath.resolve(relativePath);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("Component not found: " + blobPath);
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

    private boolean deleteFromLocalStorage(String blobPath) {
        try {
            String relativePath = blobPath.replace(buildFolder + "/", "");
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

package gov.nist.oscal.tools.api.service;

import java.util.List;
import java.util.Map;

/**
 * Common interface for cloud storage operations.
 * Provides abstraction over different storage providers (Azure Blob Storage, AWS S3, etc.)
 *
 * Implementations should handle both cloud storage and local file storage as fallback.
 */
public interface StorageService {

    /**
     * Upload a component definition to storage
     *
     * @param username User's username (for organizing files)
     * @param filename Original filename
     * @param content Content to upload (JSON, XML, YAML, etc.)
     * @param metadata Additional metadata to attach to the file
     * @return The storage path/key where the file was stored
     * @throws RuntimeException if upload fails
     */
    String uploadComponent(String username, String filename, String content, Map<String, String> metadata);

    /**
     * Download a component definition from storage
     *
     * @param path Path/key in storage (e.g., "build/username/component.json")
     * @return Content of the file as a String
     * @throws RuntimeException if file not found or download fails
     */
    String downloadComponent(String path);

    /**
     * List all components for a specific user
     *
     * @param username User's username
     * @return List of storage paths for user's components
     * @throws RuntimeException if listing fails
     */
    List<String> listUserComponents(String username);

    /**
     * Delete a component definition from storage
     *
     * @param path Path/key in storage
     * @return True if deletion was successful, false otherwise
     */
    boolean deleteComponent(String path);

    /**
     * Check if a component exists in storage
     *
     * @param path Path/key in storage
     * @return True if component exists, false otherwise
     */
    boolean componentExists(String path);

    /**
     * Get file size in bytes
     *
     * @param path Path/key in storage
     * @return File size in bytes
     * @throws RuntimeException if file not found or operation fails
     */
    long getFileSize(String path);

    /**
     * Build storage path for a component definition
     * Format: {folder}/{username}/{filename}
     *
     * @param username User's username
     * @param filename Original filename
     * @return Storage path/key
     */
    String buildPath(String username, String filename);

    /**
     * Check if storage service is properly configured and ready to use
     *
     * @return True if storage is configured and operational, false otherwise
     */
    boolean isConfigured();

    /**
     * Get the storage provider type (for logging/debugging)
     *
     * @return Storage provider name (e.g., "Azure Blob Storage", "AWS S3", "Local File System")
     */
    String getStorageProvider();
}

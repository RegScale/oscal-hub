package gov.nist.oscal.tools.api.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import gov.nist.oscal.tools.api.model.OscalFormat;
import gov.nist.oscal.tools.api.model.OscalModelType;
import gov.nist.oscal.tools.api.model.SavedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private static final String LOCAL_STORAGE_DIR = System.getProperty("user.home") + "/.oscal-hub/files";

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.container-name:oscal-files}")
    private String containerName;

    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;
    private boolean useLocalStorage = false;

    @PostConstruct
    public void init() {
        // Check if Azure Storage is configured
        if (connectionString == null || connectionString.trim().isEmpty()) {
            logger.warn("Azure Blob Storage connection string not configured. Using local filesystem storage.");
            logger.info("Files will be stored in: {}", LOCAL_STORAGE_DIR);
            logger.info("To enable Azure Blob Storage:");
            logger.info("  1. For local development: Create .env file (copy .env.example and add your connection string)");
            logger.info("  2. For production: Set AZURE_STORAGE_CONNECTION_STRING environment variable");

            useLocalStorage = true;

            // Create local storage directory if it doesn't exist
            try {
                Path storagePath = Paths.get(LOCAL_STORAGE_DIR);
                if (!Files.exists(storagePath)) {
                    Files.createDirectories(storagePath);
                    logger.info("Created local storage directory: {}", LOCAL_STORAGE_DIR);
                }
            } catch (IOException e) {
                logger.error("Failed to create local storage directory: {}", e.getMessage(), e);
                throw new RuntimeException("Could not initialize local file storage", e);
            }
            return;
        }

        try {
            logger.info("Initializing Azure Blob Storage client...");
            blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            containerClient = blobServiceClient.getBlobContainerClient(containerName);

            // Create container if it doesn't exist
            if (!containerClient.exists()) {
                containerClient.create();
                logger.info("Created blob container: {}", containerName);
            } else {
                logger.info("Using existing blob container: {}", containerName);
            }

            logger.info("Azure Blob Storage initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Azure Blob Storage: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize Azure Blob Storage", e);
        }
    }

    /**
     * Save a file to Azure Blob Storage or local filesystem
     */
    public SavedFile saveFile(String content, String fileName, OscalModelType modelType, OscalFormat format, String username) {
        if (useLocalStorage) {
            return saveFileLocally(content, fileName, modelType, format, username);
        }

        if (containerClient == null) {
            throw new RuntimeException("Azure Blob Storage is not configured. Please set AZURE_STORAGE_CONNECTION_STRING environment variable.");
        }
        try {
            String fileId = UUID.randomUUID().toString();
            String sanitizedFileName = sanitizeFileName(fileName);
            String blobName = buildBlobPath(username, fileId, sanitizedFileName);

            // Upload to Azure Blob Storage
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            blobClient.upload(new ByteArrayInputStream(contentBytes), contentBytes.length, true);

            // Set metadata
            blobClient.setMetadata(java.util.Map.of(
                "username", username,
                "fileId", fileId,
                "originalFileName", fileName,
                "modelType", modelType != null ? modelType.toString() : "",
                "format", format.toString()
            ));

            SavedFile savedFile = new SavedFile();
            savedFile.setId(fileId);
            savedFile.setFileName(fileName);
            savedFile.setModelType(modelType);
            savedFile.setFormat(format);
            savedFile.setFileSize(contentBytes.length);
            savedFile.setUploadedAt(LocalDateTime.now());
            savedFile.setFilePath(blobName);
            savedFile.setUsername(username);

            logger.info("Saved file to Azure Blob Storage: {} (ID: {}) for user: {}", fileName, fileId, username);
            return savedFile;
        } catch (Exception e) {
            logger.error("Failed to save file to Azure Blob Storage: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save file", e);
        }
    }

    /**
     * Save a file to local filesystem
     */
    private SavedFile saveFileLocally(String content, String fileName, OscalModelType modelType, OscalFormat format, String username) {
        try {
            String fileId = UUID.randomUUID().toString();
            String sanitizedFileName = sanitizeFileName(fileName);
            String relativePath = buildBlobPath(username, fileId, sanitizedFileName);
            Path filePath = Paths.get(LOCAL_STORAGE_DIR, relativePath);

            // Create user directory if it doesn't exist
            Files.createDirectories(filePath.getParent());

            // Write file content
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            Files.write(filePath, contentBytes);

            // Write metadata file
            Path metadataPath = Paths.get(filePath.toString() + ".meta");
            String metadataContent = String.format(
                "username=%s\nfileId=%s\noriginalFileName=%s\nmodelType=%s\nformat=%s",
                username,
                fileId,
                fileName,
                modelType != null ? modelType.toString() : "",
                format.toString()
            );
            Files.write(metadataPath, metadataContent.getBytes(StandardCharsets.UTF_8));

            SavedFile savedFile = new SavedFile();
            savedFile.setId(fileId);
            savedFile.setFileName(fileName);
            savedFile.setModelType(modelType);
            savedFile.setFormat(format);
            savedFile.setFileSize(contentBytes.length);
            savedFile.setUploadedAt(LocalDateTime.now());
            savedFile.setFilePath(relativePath);
            savedFile.setUsername(username);

            logger.info("Saved file locally: {} (ID: {}) for user: {}", fileName, fileId, username);
            return savedFile;
        } catch (Exception e) {
            logger.error("Failed to save file locally: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save file", e);
        }
    }

    /**
     * Get a list of all saved files for a specific user
     */
    public List<SavedFile> listFiles(String username) {
        if (useLocalStorage) {
            return listFilesLocally(username);
        }

        if (containerClient == null) {
            logger.warn("Azure Blob Storage is not configured. Returning empty file list.");
            return new ArrayList<>();
        }
        try {
            String prefix = username + "/";
            List<SavedFile> files = new ArrayList<>();

            ListBlobsOptions options = new ListBlobsOptions().setPrefix(prefix);

            for (BlobItem blobItem : containerClient.listBlobs(options, null)) {
                try {
                    BlobClient blobClient = containerClient.getBlobClient(blobItem.getName());
                    SavedFile savedFile = blobItemToSavedFile(blobItem, blobClient, username);
                    if (savedFile != null) {
                        files.add(savedFile);
                    }
                } catch (Exception e) {
                    logger.warn("Error processing blob item: {}", blobItem.getName(), e);
                }
            }

            // Sort by upload date, most recent first
            return files.stream()
                    .sorted((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Failed to list files for user {}: {}", username, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get a list of all saved files from local filesystem for a specific user
     */
    private List<SavedFile> listFilesLocally(String username) {
        try {
            Path userDir = Paths.get(LOCAL_STORAGE_DIR, username);
            if (!Files.exists(userDir)) {
                return new ArrayList<>();
            }

            List<SavedFile> files = new ArrayList<>();
            try (Stream<Path> paths = Files.walk(userDir)) {
                files = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.toString().endsWith(".meta"))
                    .map(p -> localFileToSavedFile(p, username))
                    .filter(f -> f != null)
                    .sorted((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()))
                    .collect(Collectors.toList());
            }

            return files;
        } catch (Exception e) {
            logger.error("Failed to list local files for user {}: {}", username, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Convert local file to SavedFile
     */
    private SavedFile localFileToSavedFile(Path filePath, String username) {
        try {
            Path metadataPath = Paths.get(filePath.toString() + ".meta");
            if (!Files.exists(metadataPath)) {
                return null;
            }

            // Read metadata
            String metadataContent = new String(Files.readAllBytes(metadataPath), StandardCharsets.UTF_8);
            String[] metadataLines = metadataContent.split("\n");
            String fileId = null;
            String originalFileName = null;
            String modelTypeStr = null;
            String formatStr = null;

            for (String line : metadataLines) {
                if (line.startsWith("fileId=")) {
                    fileId = line.substring("fileId=".length());
                } else if (line.startsWith("originalFileName=")) {
                    originalFileName = line.substring("originalFileName=".length());
                } else if (line.startsWith("modelType=")) {
                    modelTypeStr = line.substring("modelType=".length());
                } else if (line.startsWith("format=")) {
                    formatStr = line.substring("format=".length());
                }
            }

            if (fileId == null || originalFileName == null) {
                return null;
            }

            OscalFormat format = OscalFormat.JSON;
            if (formatStr != null && !formatStr.isEmpty()) {
                try {
                    format = OscalFormat.valueOf(formatStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Use default
                }
            }

            OscalModelType modelType = null;
            if (modelTypeStr != null && !modelTypeStr.isEmpty()) {
                try {
                    modelType = OscalModelType.valueOf(modelTypeStr);
                } catch (IllegalArgumentException e) {
                    // Leave as null
                }
            }

            BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
            Path relativePath = Paths.get(LOCAL_STORAGE_DIR).relativize(filePath);

            SavedFile savedFile = new SavedFile();
            savedFile.setId(fileId);
            savedFile.setFileName(originalFileName);
            savedFile.setFormat(format);
            savedFile.setModelType(modelType);
            savedFile.setFileSize(attrs.size());
            savedFile.setUploadedAt(
                LocalDateTime.ofInstant(
                    attrs.lastModifiedTime().toInstant(),
                    ZoneId.systemDefault()
                )
            );
            savedFile.setFilePath(relativePath.toString());
            savedFile.setUsername(username);

            return savedFile;
        } catch (Exception e) {
            logger.error("Error converting local file to SavedFile: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get file content by ID for a specific user
     */
    public String getFileContent(String fileId, String username) {
        if (useLocalStorage) {
            return getFileContentLocally(fileId, username);
        }

        if (containerClient == null) {
            throw new RuntimeException("Azure Blob Storage is not configured. Please set AZURE_STORAGE_CONNECTION_STRING environment variable.");
        }
        try {
            String blobName = findBlobByFileId(fileId, username);
            if (blobName == null) {
                throw new RuntimeException("File not found: " + fileId);
            }

            BlobClient blobClient = containerClient.getBlobClient(blobName);
            BinaryData content = blobClient.downloadContent();
            return content.toString();
        } catch (Exception e) {
            logger.error("Failed to get file content for file {}: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Failed to get file content", e);
        }
    }

    /**
     * Get file content from local filesystem by ID for a specific user
     */
    private String getFileContentLocally(String fileId, String username) {
        try {
            Path filePath = findLocalFileByFileId(fileId, username);
            if (filePath == null) {
                throw new RuntimeException("File not found: " + fileId);
            }

            return new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Failed to get local file content for file {}: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Failed to get file content", e);
        }
    }

    /**
     * Find local file by file ID
     */
    private Path findLocalFileByFileId(String fileId, String username) throws IOException {
        Path userDir = Paths.get(LOCAL_STORAGE_DIR, username);
        if (!Files.exists(userDir)) {
            return null;
        }

        try (Stream<Path> paths = Files.walk(userDir)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(p -> !p.toString().endsWith(".meta"))
                .filter(p -> p.getFileName().toString().startsWith(fileId + "_"))
                .findFirst()
                .orElse(null);
        }
    }

    /**
     * Get saved file metadata by ID for a specific user
     */
    public SavedFile getFile(String fileId, String username) {
        if (useLocalStorage) {
            return getFileLocally(fileId, username);
        }

        if (containerClient == null) {
            throw new RuntimeException("Azure Blob Storage is not configured. Please set AZURE_STORAGE_CONNECTION_STRING environment variable.");
        }
        try {
            String blobName = findBlobByFileId(fileId, username);
            if (blobName == null) {
                throw new RuntimeException("File not found: " + fileId);
            }

            BlobClient blobClient = containerClient.getBlobClient(blobName);
            var properties = blobClient.getProperties();

            // Parse file information from blob name
            String[] parts = blobName.split("/", 2);
            if (parts.length < 2) {
                throw new RuntimeException("Invalid blob name format: " + blobName);
            }

            String filePart = parts[1];
            String[] fileNameParts = filePart.split("_", 2);
            if (fileNameParts.length < 2) {
                throw new RuntimeException("Invalid file name format: " + filePart);
            }

            String fileName = fileNameParts[1];

            // Get metadata
            var metadata = properties.getMetadata();
            OscalFormat format = detectFormat(fileName);
            OscalModelType modelType = null;

            if (metadata != null) {
                String formatStr = metadata.get("format");
                if (formatStr != null && !formatStr.isEmpty()) {
                    try {
                        format = OscalFormat.valueOf(formatStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // Use detected format
                    }
                }

                String modelTypeStr = metadata.get("modelType");
                if (modelTypeStr != null && !modelTypeStr.isEmpty()) {
                    try {
                        modelType = OscalModelType.valueOf(modelTypeStr);
                    } catch (IllegalArgumentException e) {
                        // Leave as null
                    }
                }
            }

            if (modelType == null) {
                modelType = detectModelType(fileName);
            }

            SavedFile savedFile = new SavedFile();
            savedFile.setId(fileId);
            savedFile.setFileName(fileName);
            savedFile.setFormat(format);
            savedFile.setModelType(modelType);
            savedFile.setFileSize(properties.getBlobSize());
            savedFile.setUploadedAt(
                LocalDateTime.ofInstant(
                    properties.getLastModified().toInstant(),
                    ZoneId.systemDefault()
                )
            );
            savedFile.setFilePath(blobName);
            savedFile.setUsername(username);

            return savedFile;
        } catch (Exception e) {
            logger.error("Failed to get file metadata for file {}: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Failed to get file metadata", e);
        }
    }

    /**
     * Get saved file metadata from local filesystem by ID for a specific user
     */
    private SavedFile getFileLocally(String fileId, String username) {
        try {
            Path filePath = findLocalFileByFileId(fileId, username);
            if (filePath == null) {
                throw new RuntimeException("File not found: " + fileId);
            }

            return localFileToSavedFile(filePath, username);
        } catch (Exception e) {
            logger.error("Failed to get local file metadata for file {}: {}", fileId, e.getMessage(), e);
            throw new RuntimeException("Failed to get file metadata", e);
        }
    }

    /**
     * Delete a file by ID for a specific user
     */
    public boolean deleteFile(String fileId, String username) {
        if (useLocalStorage) {
            return deleteFileLocally(fileId, username);
        }

        if (containerClient == null) {
            throw new RuntimeException("Azure Blob Storage is not configured. Please set AZURE_STORAGE_CONNECTION_STRING environment variable.");
        }
        try {
            String blobName = findBlobByFileId(fileId, username);
            if (blobName == null) {
                return false;
            }

            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.delete();

            logger.info("Deleted file with ID: {} for user: {}", fileId, username);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete file {}: {}", fileId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete a file from local filesystem by ID for a specific user
     */
    private boolean deleteFileLocally(String fileId, String username) {
        try {
            Path filePath = findLocalFileByFileId(fileId, username);
            if (filePath == null) {
                return false;
            }

            // Delete the file
            Files.delete(filePath);

            // Delete the metadata file
            Path metadataPath = Paths.get(filePath.toString() + ".meta");
            if (Files.exists(metadataPath)) {
                Files.delete(metadataPath);
            }

            logger.info("Deleted local file with ID: {} for user: {}", fileId, username);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete local file {}: {}", fileId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Find blob name by file ID and username
     */
    private String findBlobByFileId(String fileId, String username) {
        String prefix = username + "/";
        ListBlobsOptions options = new ListBlobsOptions().setPrefix(prefix);

        for (BlobItem blobItem : containerClient.listBlobs(options, null)) {
            if (blobItem.getName().contains(fileId + "_")) {
                return blobItem.getName();
            }
        }
        return null;
    }

    /**
     * Convert BlobItem to SavedFile
     */
    private SavedFile blobItemToSavedFile(BlobItem blobItem, BlobClient blobClient, String username) {
        try {
            String blobName = blobItem.getName();
            String[] parts = blobName.split("/", 2);

            if (parts.length < 2) {
                return null;
            }

            String filePart = parts[1];
            String[] fileNameParts = filePart.split("_", 2);

            if (fileNameParts.length < 2) {
                return null;
            }

            String fileId = fileNameParts[0];
            String originalFileName = fileNameParts[1];

            // Get metadata
            var metadata = blobClient.getProperties().getMetadata();
            OscalFormat format = detectFormat(originalFileName);
            OscalModelType modelType = null;

            if (metadata != null) {
                String formatStr = metadata.get("format");
                if (formatStr != null && !formatStr.isEmpty()) {
                    try {
                        format = OscalFormat.valueOf(formatStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // Use detected format
                    }
                }

                String modelTypeStr = metadata.get("modelType");
                if (modelTypeStr != null && !modelTypeStr.isEmpty()) {
                    try {
                        modelType = OscalModelType.valueOf(modelTypeStr);
                    } catch (IllegalArgumentException e) {
                        // Leave as null
                    }
                }
            }

            if (modelType == null) {
                modelType = detectModelType(originalFileName);
            }

            SavedFile savedFile = new SavedFile();
            savedFile.setId(fileId);
            savedFile.setFileName(originalFileName);
            savedFile.setFormat(format);
            savedFile.setModelType(modelType);
            savedFile.setFileSize(blobItem.getProperties().getContentLength());
            savedFile.setUploadedAt(
                LocalDateTime.ofInstant(
                    blobItem.getProperties().getLastModified().toInstant(),
                    ZoneId.systemDefault()
                )
            );
            savedFile.setFilePath(blobName);
            savedFile.setUsername(username);

            return savedFile;
        } catch (Exception e) {
            logger.error("Error converting blob item to SavedFile: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Build blob path: {username}/{fileId}_{filename}
     */
    private String buildBlobPath(String username, String fileId, String fileName) {
        return username + "/" + fileId + "_" + fileName;
    }

    private String sanitizeFileName(String fileName) {
        // Remove or replace characters that might cause issues
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private OscalFormat detectFormat(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".xml")) {
            return OscalFormat.XML;
        } else if (lowerFileName.endsWith(".json")) {
            return OscalFormat.JSON;
        } else if (lowerFileName.endsWith(".yaml") || lowerFileName.endsWith(".yml")) {
            return OscalFormat.YAML;
        }
        return OscalFormat.JSON; // default
    }

    private OscalModelType detectModelType(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.contains("catalog")) {
            return OscalModelType.CATALOG;
        } else if (lowerFileName.contains("profile")) {
            return OscalModelType.PROFILE;
        } else if (lowerFileName.contains("component")) {
            return OscalModelType.COMPONENT_DEFINITION;
        } else if (lowerFileName.contains("ssp") || lowerFileName.contains("system-security-plan")) {
            return OscalModelType.SYSTEM_SECURITY_PLAN;
        } else if (lowerFileName.contains("assessment-plan") || lowerFileName.contains("ap")) {
            return OscalModelType.ASSESSMENT_PLAN;
        } else if (lowerFileName.contains("assessment-results") || lowerFileName.contains("ar")) {
            return OscalModelType.ASSESSMENT_RESULTS;
        } else if (lowerFileName.contains("poam") || lowerFileName.contains("plan-of-action")) {
            return OscalModelType.PLAN_OF_ACTION_AND_MILESTONES;
        }
        return null; // Unknown
    }
}

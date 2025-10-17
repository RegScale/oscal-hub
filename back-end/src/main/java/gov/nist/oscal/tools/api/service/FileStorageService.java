package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.model.OscalFormat;
import gov.nist.oscal.tools.api.model.OscalModelType;
import gov.nist.oscal.tools.api.model.SavedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${oscal.storage.directory:uploads}")
    private String storageDirectory;

    private Path storagePath;

    @PostConstruct
    public void init() {
        try {
            storagePath = Paths.get(storageDirectory).toAbsolutePath().normalize();
            Files.createDirectories(storagePath);
            logger.info("File storage initialized at: {}", storagePath);
        } catch (IOException e) {
            logger.error("Failed to create storage directory: {}", e.getMessage());
            throw new RuntimeException("Could not initialize file storage", e);
        }
    }

    /**
     * Save a file to storage
     */
    public SavedFile saveFile(String content, String fileName, OscalModelType modelType, OscalFormat format, String username) throws IOException {
        String fileId = UUID.randomUUID().toString();
        String extension = getFileExtension(format);
        String sanitizedFileName = sanitizeFileName(fileName);
        String storedFileName = fileId + "_" + sanitizedFileName;

        // Create user-specific directory
        Path userPath = storagePath.resolve(username);
        Files.createDirectories(userPath);

        Path filePath = userPath.resolve(storedFileName);
        Files.writeString(filePath, content, StandardCharsets.UTF_8);

        SavedFile savedFile = new SavedFile();
        savedFile.setId(fileId);
        savedFile.setFileName(fileName);
        savedFile.setModelType(modelType);
        savedFile.setFormat(format);
        savedFile.setFileSize(content.getBytes(StandardCharsets.UTF_8).length);
        savedFile.setUploadedAt(LocalDateTime.now());
        savedFile.setFilePath(storedFileName);
        savedFile.setUsername(username);

        logger.info("Saved file: {} (ID: {}) for user: {}", fileName, fileId, username);
        return savedFile;
    }

    /**
     * Get a list of all saved files for a specific user
     */
    public List<SavedFile> listFiles(String username) throws IOException {
        Path userPath = storagePath.resolve(username);
        if (!Files.exists(userPath)) {
            return List.of(); // Return empty list if user directory doesn't exist
        }

        try (Stream<Path> paths = Files.list(userPath)) {
            return paths
                .filter(Files::isRegularFile)
                .map(path -> pathToSavedFile(path, username))
                .filter(savedFile -> savedFile != null)
                .sorted((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()))
                .collect(Collectors.toList());
        }
    }

    /**
     * Get file content by ID for a specific user
     */
    public String getFileContent(String fileId, String username) throws IOException {
        Path filePath = findFileById(fileId, username);
        if (filePath == null) {
            throw new IOException("File not found: " + fileId);
        }
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }

    /**
     * Get saved file metadata by ID for a specific user
     */
    public SavedFile getFile(String fileId, String username) throws IOException {
        Path filePath = findFileById(fileId, username);
        if (filePath == null) {
            throw new IOException("File not found: " + fileId);
        }
        return pathToSavedFile(filePath, username);
    }

    /**
     * Delete a file by ID for a specific user
     */
    public boolean deleteFile(String fileId, String username) throws IOException {
        Path filePath = findFileById(fileId, username);
        if (filePath == null) {
            return false;
        }
        Files.delete(filePath);
        logger.info("Deleted file with ID: {} for user: {}", fileId, username);
        return true;
    }

    private Path findFileById(String fileId, String username) throws IOException {
        Path userPath = storagePath.resolve(username);
        if (!Files.exists(userPath)) {
            return null;
        }

        try (Stream<Path> paths = Files.list(userPath)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().startsWith(fileId + "_"))
                .findFirst()
                .orElse(null);
        }
    }

    private SavedFile pathToSavedFile(Path path, String username) {
        try {
            String fileName = path.getFileName().toString();
            String[] parts = fileName.split("_", 2);
            if (parts.length < 2) {
                return null;
            }

            String fileId = parts[0];
            String originalFileName = parts[1];

            // Try to determine format and model type from file extension
            OscalFormat format = detectFormat(originalFileName);

            SavedFile savedFile = new SavedFile();
            savedFile.setId(fileId);
            savedFile.setFileName(originalFileName);
            savedFile.setFormat(format);
            savedFile.setFileSize(Files.size(path));
            savedFile.setUploadedAt(
                LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(path).toInstant(),
                    java.time.ZoneId.systemDefault()
                )
            );
            savedFile.setFilePath(fileName);
            savedFile.setUsername(username);

            // Model type detection could be improved by reading the file content
            // For now, we'll leave it null or try to infer from filename patterns
            savedFile.setModelType(detectModelType(originalFileName));

            return savedFile;
        } catch (IOException e) {
            logger.error("Error reading file metadata: {}", e.getMessage());
            return null;
        }
    }

    private String getFileExtension(OscalFormat format) {
        switch (format) {
            case XML:
                return ".xml";
            case JSON:
                return ".json";
            case YAML:
                return ".yaml";
            default:
                return ".txt";
        }
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

    private String sanitizeFileName(String fileName) {
        // Remove or replace characters that might cause issues
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

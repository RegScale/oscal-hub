package gov.nist.oscal.tools.api.controller;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import gov.nist.oscal.tools.api.model.OscalFormat;
import gov.nist.oscal.tools.api.model.OscalModelType;
import gov.nist.oscal.tools.api.model.SavedFile;
import gov.nist.oscal.tools.api.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Controller for serving uploaded files (logos, etc.) and managing saved OSCAL files
 * Supports local filesystem, Azure Blob Storage, Google Cloud Storage, and AWS S3
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private static final String UPLOAD_DIR = "uploads";

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${storage.provider:azure}")
    private String storageProvider;

    // Azure configuration
    @Value("${azure.storage.connection-string:}")
    private String azureConnectionString;

    @Value("${azure.storage.container-name:oscal-files}")
    private String azureContainerName;

    // GCS configuration
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
        logger.info("Initializing file serving (provider: {})...", storageProvider);

        if ("gcs".equalsIgnoreCase(storageProvider)) {
            initializeGcsStorage();
        } else if ("s3".equalsIgnoreCase(storageProvider)) {
            initializeS3Storage();
        } else {
            initializeAzureStorage();
        }
    }

    private void initializeAzureStorage() {
        if (azureConnectionString == null || azureConnectionString.trim().isEmpty()) {
            logger.info("Azure Blob Storage not configured, using local filesystem for file serving");
            return;
        }

        try {
            logger.info("Initializing Azure Blob Storage for file serving...");
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(azureConnectionString)
                    .buildClient();

            azureContainerClient = blobServiceClient.getBlobContainerClient(azureContainerName);
            useCloudStorage = true;
            logger.info("Azure Blob Storage initialized for file serving");
        } catch (Exception e) {
            logger.error("Failed to initialize Azure Blob Storage for file serving, falling back to local filesystem: {}", e.getMessage());
            useCloudStorage = false;
        }
    }

    private void initializeGcsStorage() {
        try {
            logger.info("Initializing Google Cloud Storage for file serving...");
            gcsStorage = StorageOptions.newBuilder()
                    .setProjectId(gcpProjectId)
                    .build()
                    .getService();

            useCloudStorage = true;
            logger.info("Google Cloud Storage initialized for file serving");
        } catch (Exception e) {
            logger.error("Failed to initialize GCS for file serving, falling back to local filesystem: {}", e.getMessage());
            useCloudStorage = false;
        }
    }

    private void initializeS3Storage() {
        try {
            logger.info("Initializing AWS S3 for file serving...");
            s3Client = S3Client.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();

            useCloudStorage = true;
            logger.info("AWS S3 initialized for file serving");
        } catch (Exception e) {
            logger.error("Failed to initialize S3 for file serving, falling back to local filesystem: {}", e.getMessage());
            useCloudStorage = false;
        }
    }

    @GetMapping("/org-logos/{filename:.+}")
    public ResponseEntity<Resource> serveOrgLogo(@PathVariable String filename) {
        logger.info("==> Serving logo request for: {}", filename);
        logger.info("    Storage provider: {}, useCloudStorage: {}", storageProvider, useCloudStorage);

        try {
            // Try cloud storage first if configured
            if (useCloudStorage) {
                logger.info("    Attempting to serve from cloud storage...");
                ResponseEntity<Resource> cloudResponse = serveFromCloud(filename);
                if (cloudResponse.getStatusCode().is2xxSuccessful()) {
                    logger.info("✓ Successfully served from cloud storage: {}", filename);
                    return cloudResponse;
                }
                // Cloud didn't have it, fall back to local
                logger.warn("✗ File not found in cloud storage, trying local filesystem: {}", filename);
            } else {
                logger.info("    Cloud storage not configured, using local filesystem");
            }

            // Try local filesystem
            logger.info("    Attempting to serve from local filesystem...");
            ResponseEntity<Resource> localResponse = serveFromLocal(filename);
            if (localResponse.getStatusCode().is2xxSuccessful()) {
                logger.info("✓ Successfully served from local filesystem: {}", filename);
            } else {
                logger.error("✗ File not found in local filesystem: {}", filename);
            }
            return localResponse;
        } catch (Exception e) {
            logger.error("✗ ERROR serving logo file: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<Resource> serveFromCloud(String filename) {
        String logoPath = "org-logos/" + filename;

        try {
            byte[] data = null;
            String contentType = determineContentType(filename);

            if ("gcs".equalsIgnoreCase(storageProvider) && gcsStorage != null) {
                // Serve from Google Cloud Storage
                BlobId blobId = BlobId.of(gcsLogoBucket, logoPath);
                Blob blob = gcsStorage.get(blobId);

                if (blob == null || !blob.exists()) {
                    logger.warn("Logo file not found in GCS: {}", logoPath);
                    return ResponseEntity.notFound().build();
                }

                data = blob.getContent();
                logger.debug("Serving logo from GCS: {}", logoPath);

            } else if ("s3".equalsIgnoreCase(storageProvider) && s3Client != null) {
                // Serve from AWS S3
                try {
                    GetObjectRequest getRequest = GetObjectRequest.builder()
                            .bucket(s3LogoBucket)
                            .key(logoPath)
                            .build();

                    data = s3Client.getObjectAsBytes(getRequest).asByteArray();
                    logger.debug("Serving logo from S3: {}", logoPath);

                } catch (NoSuchKeyException e) {
                    logger.warn("Logo file not found in S3: {}", logoPath);
                    return ResponseEntity.notFound().build();
                }

            } else if (azureContainerClient != null) {
                // Serve from Azure Blob Storage (default)
                logger.debug("Checking Azure Blob Storage for: {}", logoPath);
                BlobClient blobClient = azureContainerClient.getBlobClient(logoPath);

                if (!blobClient.exists()) {
                    logger.warn("✗ Logo file NOT FOUND in Azure Blob Storage: {}", logoPath);
                    logger.warn("   Container: {}, Path: {}", azureContainerName, logoPath);
                    return ResponseEntity.notFound().build();
                }

                // Get blob properties first
                com.azure.storage.blob.models.BlobProperties props = blobClient.getProperties();
                logger.info("✓ Found blob in Azure: {} (size: {} bytes, content-type: {})",
                    logoPath, props.getBlobSize(), props.getContentType());

                // Download content
                BinaryData content = blobClient.downloadContent();
                data = content.toBytes();

                logger.info("   Downloaded {} bytes from Azure for {}", data.length, logoPath);

                // Use content-type from blob properties if available
                if (props.getContentType() != null && !props.getContentType().isEmpty()) {
                    contentType = props.getContentType();
                    logger.debug("   Using Azure blob Content-Type: {}", contentType);
                }
            }

            if (data == null) {
                return ResponseEntity.notFound().build();
            }

            // Create resource from byte array
            ByteArrayResource resource = new ByteArrayResource(data);

            logger.info("Serving {} as {} ({} bytes)", filename, contentType, data.length);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(data.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .body(resource);

        } catch (Exception e) {
            logger.error("Error serving logo from cloud storage: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<Resource> serveFromLocal(String filename) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR, "org-logos", filename);

            if (!Files.exists(filePath)) {
                logger.warn("Logo file not found locally: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                logger.warn("Logo file not readable: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = determineContentType(filename);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error serving logo from local filesystem: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private String determineContentType(String filename) {
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".png")) {
            return "image/png";
        } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFilename.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerFilename.endsWith(".gif")) {
            return "image/gif";
        }
        return "application/octet-stream";
    }

    // ========================================
    // Saved Files Management Endpoints
    // ========================================

    /**
     * Get all saved files for the current user
     */
    @GetMapping
    public ResponseEntity<List<SavedFile>> getSavedFiles() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            List<SavedFile> files = fileStorageService.listFiles(username);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("Failed to get saved files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a saved file by ID
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<SavedFile> getSavedFile(@PathVariable String fileId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            SavedFile file = fileStorageService.getFile(fileId, username);
            return ResponseEntity.ok(file);
        } catch (Exception e) {
            logger.error("Failed to get saved file: {}", fileId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get file content by ID
     */
    @GetMapping("/{fileId}/content")
    public ResponseEntity<Map<String, String>> getFileContent(@PathVariable String fileId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            String content = fileStorageService.getFileContent(fileId, username);
            return ResponseEntity.ok(Map.of("content", content));
        } catch (Exception e) {
            logger.error("Failed to get file content: {}", fileId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Delete a saved file by ID
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteSavedFile(@PathVariable String fileId) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            boolean deleted = fileStorageService.deleteFile(fileId, username);
            if (deleted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            logger.error("Failed to delete saved file: {}", fileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Save a new file
     */
    @PostMapping
    public ResponseEntity<SavedFile> saveFile(@RequestBody SaveFileRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            SavedFile savedFile = fileStorageService.saveFile(
                request.getContent(),
                request.getFileName(),
                request.getModelType(),
                request.getFormat(),
                username
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(savedFile);
        } catch (Exception e) {
            logger.error("Failed to save file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Request DTO for saving files
     */
    public static class SaveFileRequest {
        private String content;
        private String fileName;
        private OscalModelType modelType;
        private OscalFormat format;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public OscalModelType getModelType() {
            return modelType;
        }

        public void setModelType(OscalModelType modelType) {
            this.modelType = modelType;
        }

        public OscalFormat getFormat() {
            return format;
        }

        public void setFormat(OscalFormat format) {
            this.format = format;
        }
    }
}

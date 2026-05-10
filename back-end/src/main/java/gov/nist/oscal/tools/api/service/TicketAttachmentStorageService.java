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
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Set;

/**
 * Service for handling storage of ticket attachment files.
 * Mirrors ArtifactStorageService but stores binary bytes instead of text,
 * and uses a dedicated Azure container for ticket data isolation.
 */
@Service
public class TicketAttachmentStorageService {

    private static final Logger logger = LoggerFactory.getLogger(TicketAttachmentStorageService.class);

    /** Maximum allowed attachment size: 10 MB. */
    public static final long MAX_BYTES = 10L * 1024 * 1024;

    /** Maximum number of attachments per request. */
    public static final int MAX_FILES_PER_REQUEST = 5;

    /** Allowed file extensions (lower-case, without leading dot). */
    public static final Set<String> ALLOWED_EXT = Set.of(
            "png", "jpg", "jpeg", "gif", "pdf", "txt", "log", "json", "xml", "yaml", "yml");

    @Value("${azure.storage.connection-string:}")
    private String connectionString;

    @Value("${azure.storage.ticket-container-name:oscal-tickets}")
    private String ticketContainerName;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private BlobServiceClient blobServiceClient;
    private BlobContainerClient containerClient;
    private boolean useLocalStorage = false;
    private Path localTicketPath;

    /** Record returned after a successful upload. */
    public record AttachmentUpload(
            String storagePath,
            long sizeBytes,
            String contentType,
            String originalFilename) {}

    /**
     * No-arg constructor to allow direct instantiation in unit tests
     * (Spring will also use this constructor when @Value fields are injected).
     */
    public TicketAttachmentStorageService() {
        // fields are injected by Spring via @Value after construction
    }

    @PostConstruct
    public void init() {
        if (connectionString == null || connectionString.trim().isEmpty()) {
            logger.warn("Azure Blob Storage connection string not configured. Using local file storage for ticket attachments.");
            useLocalStorage = true;
            localTicketPath = Paths.get(uploadDir, "tickets");
            try {
                Files.createDirectories(localTicketPath);
                logger.info("Ticket attachment local storage initialized at: {}", localTicketPath.toAbsolutePath());
            } catch (IOException e) {
                logger.error("Failed to create ticket attachment directory: {}", e.getMessage(), e);
                throw new RuntimeException("Could not initialize ticket attachment storage", e);
            }
            return;
        }

        try {
            logger.info("Initializing Azure Blob Storage client for ticket attachments...");
            blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            containerClient = blobServiceClient.getBlobContainerClient(ticketContainerName);

            if (!containerClient.exists()) {
                containerClient.create();
                logger.info("Created ticket attachment blob container: {}", ticketContainerName);
            } else {
                logger.info("Using existing ticket attachment blob container: {}", ticketContainerName);
            }

            logger.info("Azure Blob Storage for ticket attachments initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Azure Blob Storage for ticket attachments: {}", e.getMessage(), e);
            throw new RuntimeException("Could not initialize Azure Blob Storage for ticket attachments", e);
        }
    }

    /**
     * Validate an attachment before storing it.
     *
     * @param file the multipart file to validate
     * @throws IllegalArgumentException if the file is empty, exceeds the size limit, or has a disallowed extension
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }

        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException(
                    "File exceeds 10 MB limit: " + file.getOriginalFilename());
        }

        String ext = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext.toLowerCase())) {
            throw new IllegalArgumentException(
                    "File type ." + ext + " is not allowed");
        }
    }

    /**
     * Validate and upload a ticket attachment.
     *
     * @param ticketId the ticket this attachment belongs to
     * @param file     the multipart file to upload
     * @return an {@link AttachmentUpload} record with storage metadata
     * @throws IOException if the file cannot be read or written
     */
    public AttachmentUpload upload(Long ticketId, MultipartFile file) throws IOException {
        validate(file);

        String sanitized = sanitizeFilename(file.getOriginalFilename() != null
                ? file.getOriginalFilename() : "attachment");
        String blobPath = String.format("tickets/%d/%d-%s",
                ticketId, Instant.now().toEpochMilli(), sanitized);

        byte[] bytes = file.getBytes();

        if (useLocalStorage) {
            Path filePath = PathSanitizer.safeResolve(localTicketPath, blobPath);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, bytes);
            logger.info("Saved ticket attachment to local storage: {}", filePath.toAbsolutePath());
        } else {
            if (containerClient == null) {
                throw new RuntimeException("Storage is not configured");
            }
            BlobClient blobClient = containerClient.getBlobClient(blobPath);
            blobClient.upload(BinaryData.fromBytes(bytes), true);
            logger.info("Saved ticket attachment to Azure Blob Storage: {}", blobPath);
        }

        return new AttachmentUpload(blobPath, file.getSize(), file.getContentType(), file.getOriginalFilename());
    }

    /**
     * Download a ticket attachment by its storage path.
     *
     * @param storagePath the path returned by {@link #upload}
     * @return the raw bytes of the attachment
     * @throws IOException if the file cannot be read
     */
    public byte[] download(String storagePath) throws IOException {
        if (useLocalStorage) {
            Path filePath = localTicketPath.resolve(storagePath);
            if (!Files.exists(filePath)) {
                throw new IOException("Ticket attachment not found: " + storagePath);
            }
            return Files.readAllBytes(filePath);
        }

        if (containerClient == null) {
            throw new RuntimeException("Storage is not configured");
        }

        try {
            BlobClient blobClient = containerClient.getBlobClient(storagePath);
            if (!blobClient.exists()) {
                throw new IOException("Ticket attachment not found: " + storagePath);
            }
            return blobClient.downloadContent().toBytes();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to download ticket attachment: {}", e.getMessage(), e);
            throw new IOException("Failed to download ticket attachment: " + storagePath, e);
        }
    }

    /**
     * Check whether storage is configured and ready.
     *
     * @return true if either Azure or local storage has been initialised
     */
    public boolean isConfigured() {
        return containerClient != null || useLocalStorage;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}

package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.config.FileValidationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for validating file uploads and content
 *
 * Provides comprehensive validation including:
 * - File type whitelisting
 * - Magic number (file signature) validation
 * - Filename sanitization
 * - Size limit enforcement
 * - Base64 image validation
 *
 * Security features:
 * - Prevents malicious file uploads
 * - Prevents MIME type spoofing
 * - Prevents path traversal attacks
 * - Prevents denial-of-service via large files
 */
@Service
public class FileValidationService {

    private static final Logger logger = LoggerFactory.getLogger(FileValidationService.class);

    private final FileValidationConfig config;

    @Autowired
    public FileValidationService(FileValidationConfig config) {
        this.config = config;
    }

    /**
     * Validate a filename for security issues
     *
     * @param filename The filename to validate
     * @return Sanitized filename
     * @throws IllegalArgumentException if filename contains forbidden characters
     */
    public String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        String sanitized = filename.trim();

        // Check for forbidden characters
        for (String forbidden : config.getForbiddenFilenameCharacters()) {
            if (sanitized.contains(forbidden)) {
                logger.warn("Filename contains forbidden character '{}': {}", forbidden, filename);
                throw new IllegalArgumentException("Filename contains forbidden characters: " + forbidden);
            }
        }

        // Remove leading/trailing dots (hidden files on Unix, special files on Windows)
        sanitized = sanitized.replaceAll("^\\.+|\\.+$", "");

        // Limit filename length
        if (sanitized.length() > 255) {
            logger.warn("Filename too long ({}): {}", sanitized.length(), filename);
            sanitized = sanitized.substring(0, 255);
        }

        // Ensure filename is not empty after sanitization
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Filename is invalid after sanitization");
        }

        return sanitized;
    }

    /**
     * Validate file extension against whitelist
     *
     * @param filename The filename to check
     * @throws IllegalArgumentException if extension is not allowed
     */
    public void validateFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("File must have an extension");
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        if (!config.isExtensionAllowed(extension)) {
            logger.warn("Forbidden file extension '{}' in filename: {}", extension, filename);
            throw new IllegalArgumentException(
                "File extension '." + extension + "' is not allowed. " +
                "Allowed extensions: " + config.getAllowedFileExtensions()
            );
        }
    }

    /**
     * Validate file size
     *
     * @param content The file content (text or Base64)
     * @param maxSize Maximum allowed size in bytes
     * @throws IllegalArgumentException if file is too large
     */
    public void validateFileSize(String content, long maxSize) {
        if (content == null) {
            throw new IllegalArgumentException("File content cannot be null");
        }

        long size = content.getBytes().length;

        if (size > maxSize) {
            logger.warn("File size ({} bytes) exceeds maximum ({} bytes)", size, maxSize);
            throw new IllegalArgumentException(
                String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)",
                    size, maxSize)
            );
        }
    }

    /**
     * Validate Base64-encoded image (logo)
     *
     * @param dataUrl The data URL (data:image/png;base64,...)
     * @throws IllegalArgumentException if image is invalid
     */
    public void validateBase64Logo(String dataUrl) {
        if (dataUrl == null || dataUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Logo data cannot be empty");
        }

        // Validate data URL format
        if (!dataUrl.startsWith("data:image/")) {
            throw new IllegalArgumentException("Logo must be a data URL starting with 'data:image/'");
        }

        // Extract MIME type
        Pattern pattern = Pattern.compile("data:(image/[^;]+);base64,(.+)");
        Matcher matcher = pattern.matcher(dataUrl);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid data URL format. Expected: data:image/TYPE;base64,DATA");
        }

        String mimeType = matcher.group(1);
        String base64Data = matcher.group(2);

        // Validate MIME type against whitelist
        if (!config.isImageTypeAllowed(mimeType)) {
            logger.warn("Forbidden image type: {}", mimeType);
            throw new IllegalArgumentException(
                "Image type '" + mimeType + "' is not allowed. " +
                "Allowed types: " + config.getAllowedImageTypes()
            );
        }

        // Validate size
        validateFileSize(dataUrl, config.getMaxLogoSize());

        // Decode Base64
        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(base64Data);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid Base64 data in logo");
            throw new IllegalArgumentException("Logo contains invalid Base64 data", e);
        }

        // Validate magic number (file signature)
        if (config.isEnableMagicNumberValidation()) {
            validateImageMagicNumber(imageBytes, mimeType);
        }
    }

    /**
     * Validate image magic number (file signature)
     *
     * Prevents MIME type spoofing by checking the actual file content
     *
     * @param imageBytes The image bytes
     * @param expectedMimeType The expected MIME type
     * @throws IllegalArgumentException if magic number doesn't match
     */
    public void validateImageMagicNumber(byte[] imageBytes, String expectedMimeType) {
        if (imageBytes == null || imageBytes.length < 4) {
            throw new IllegalArgumentException("Image data is too small to validate");
        }

        // Get expected magic number for MIME type
        String expectedMagicNumber = config.getMagicNumber(expectedMimeType);

        if (expectedMagicNumber == null) {
            // SVG or unsupported type - do text-based validation for SVG
            if (expectedMimeType.equals("image/svg+xml")) {
                validateSvgContent(imageBytes);
            } else {
                logger.warn("No magic number defined for MIME type: {}", expectedMimeType);
            }
            return;
        }

        // Convert first few bytes to hex string
        StringBuilder hexString = new StringBuilder();
        int magicNumberLength = expectedMagicNumber.length() / 2;
        for (int i = 0; i < Math.min(magicNumberLength, imageBytes.length); i++) {
            hexString.append(String.format("%02X", imageBytes[i]));
        }

        // Check if actual magic number matches expected
        String actualMagicNumber = hexString.toString();
        if (!actualMagicNumber.startsWith(expectedMagicNumber)) {
            logger.warn("Magic number mismatch. Expected: {}, Actual: {}, MIME: {}",
                expectedMagicNumber, actualMagicNumber, expectedMimeType);
            throw new IllegalArgumentException(
                "File signature does not match declared type '" + expectedMimeType + "'. " +
                "Possible file type spoofing detected."
            );
        }

        logger.debug("Magic number validated for {}: {}", expectedMimeType, actualMagicNumber);
    }

    /**
     * Validate SVG content for security issues
     *
     * SVG files can contain JavaScript and should be validated carefully
     *
     * @param svgBytes The SVG file bytes
     * @throws IllegalArgumentException if SVG contains forbidden content
     */
    private void validateSvgContent(byte[] svgBytes) {
        String svgContent = new String(svgBytes);

        // Check for SVG start tag
        if (!svgContent.contains("<svg") && !svgContent.contains("<?xml")) {
            throw new IllegalArgumentException("File does not appear to be a valid SVG");
        }

        // Check for dangerous content (JavaScript, external resources)
        String[] forbiddenPatterns = {
            "<script",           // JavaScript
            "javascript:",        // JavaScript URLs
            "on[a-z]+=",         // Event handlers (onclick, onload, etc.)
            "<iframe",           // Embedded frames
            "<embed",            // Embedded content
            "<object"            // Embedded objects
        };

        for (String pattern : forbiddenPatterns) {
            if (svgContent.toLowerCase().matches(".*" + pattern + ".*")) {
                logger.warn("SVG contains forbidden pattern: {}", pattern);
                throw new IllegalArgumentException(
                    "SVG file contains forbidden content: " + pattern + ". " +
                    "SVG files with scripts or embedded content are not allowed for security reasons."
                );
            }
        }
    }

    /**
     * Validate OSCAL file content
     *
     * @param content The file content
     * @param filename The filename
     * @throws IllegalArgumentException if content is invalid
     */
    public void validateOscalFile(String content, String filename) {
        // Sanitize filename
        String sanitizedFilename = sanitizeFilename(filename);

        // Validate extension
        validateFileExtension(sanitizedFilename);

        // Validate size
        validateFileSize(content, config.getMaxFileSize());

        // Validate content is not empty
        if (content.trim().isEmpty()) {
            throw new IllegalArgumentException("File content cannot be empty");
        }

        // Basic format validation based on extension
        String extension = sanitizedFilename.substring(sanitizedFilename.lastIndexOf('.') + 1).toLowerCase();
        validateContentFormat(content, extension);
    }

    /**
     * Validate that content matches expected format
     *
     * @param content The file content
     * @param extension The file extension
     * @throws IllegalArgumentException if format doesn't match
     */
    private void validateContentFormat(String content, String extension) {
        String trimmedContent = content.trim();

        switch (extension) {
            case "json":
                if (!trimmedContent.startsWith("{") && !trimmedContent.startsWith("[")) {
                    throw new IllegalArgumentException("File extension is .json but content is not JSON format");
                }
                break;
            case "xml":
                if (!trimmedContent.startsWith("<") || !trimmedContent.startsWith("<?xml")) {
                    throw new IllegalArgumentException("File extension is .xml but content is not XML format");
                }
                break;
            case "yaml":
            case "yml":
                // YAML is harder to validate structurally, just check it's not JSON/XML
                if (trimmedContent.startsWith("<") || trimmedContent.startsWith("{") || trimmedContent.startsWith("[")) {
                    logger.warn("File extension is .yaml but content appears to be JSON/XML");
                }
                break;
        }
    }

    /**
     * Integration point for virus scanning (if enabled)
     *
     * @param fileBytes The file bytes to scan
     * @param filename The filename
     * @throws IllegalArgumentException if virus detected
     */
    public void scanForViruses(byte[] fileBytes, String filename) {
        if (!config.isEnableVirusScanning()) {
            logger.debug("Virus scanning is disabled");
            return;
        }

        logger.info("Virus scanning requested for file: {} (size: {} bytes)", filename, fileBytes.length);

        // Integration point for external virus scanning service
        // Could integrate with ClamAV, VirusTotal API, or other scanning service
        String scanUrl = config.getVirusScanningUrl();

        if (scanUrl == null || scanUrl.isEmpty()) {
            logger.warn("Virus scanning is enabled but no scan URL configured");
            return;
        }

        // TODO: Implement actual virus scanning integration
        // For now, just log that we would scan
        logger.info("Would scan file at URL: {}", scanUrl);
    }
}

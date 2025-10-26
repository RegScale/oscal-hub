package gov.nist.oscal.tools.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration for file upload validation
 *
 * Defines security constraints for file uploads including:
 * - Allowed file types and MIME types
 * - Maximum file sizes
 * - Filename validation rules
 * - Base64 image size limits
 *
 * Security rationale:
 * - Whitelist approach prevents malicious file uploads
 * - Magic number validation prevents MIME type spoofing
 * - Size limits prevent denial-of-service attacks
 * - Filename sanitization prevents path traversal
 */
@Configuration
@ConfigurationProperties(prefix = "file.validation")
public class FileValidationConfig {

    /**
     * Maximum file size for OSCAL documents (default: 10MB)
     */
    private long maxFileSize = 10 * 1024 * 1024; // 10 MB

    /**
     * Maximum size for Base64-encoded logos (default: 2MB)
     * Base64 encoding increases size by ~33%, so 2MB encoded ≈ 1.5MB original
     */
    private long maxLogoSize = 2 * 1024 * 1024; // 2 MB

    /**
     * Allowed file extensions for OSCAL documents
     */
    private Set<String> allowedFileExtensions = new HashSet<>(Arrays.asList(
        "json", "xml", "yaml", "yml"
    ));

    /**
     * Allowed MIME types for uploaded files
     */
    private Set<String> allowedMimeTypes = new HashSet<>(Arrays.asList(
        "application/json",
        "application/xml",
        "text/xml",
        "application/x-yaml",
        "text/yaml",
        "text/plain"
    ));

    /**
     * Allowed image MIME types for logos
     */
    private Set<String> allowedImageTypes = new HashSet<>(Arrays.asList(
        "image/png",
        "image/jpeg",
        "image/jpg",
        "image/gif",
        "image/svg+xml"
    ));

    /**
     * Magic numbers (file signatures) for image validation
     * Format: MIME type -> byte array of magic number
     */
    private static final String[][] IMAGE_MAGIC_NUMBERS = {
        // PNG: 89 50 4E 47
        {"image/png", "89504E47"},
        // JPEG: FF D8 FF
        {"image/jpeg", "FFD8FF"},
        {"image/jpg", "FFD8FF"},
        // GIF: 47 49 46 38
        {"image/gif", "47494638"},
        // SVG: 3C 73 76 67 or 3C 3F 78 6D 6C (<?xml)
        {"image/svg+xml", "3C737667"},
        {"image/svg+xml", "3C3F786D6C"}
    };

    /**
     * Characters forbidden in filenames (path traversal prevention)
     */
    private Set<String> forbiddenFilenameCharacters = new HashSet<>(Arrays.asList(
        "..", "/", "\\", ":", "*", "?", "\"", "<", ">", "|", "\0"
    ));

    /**
     * Enable magic number validation (recommended for production)
     */
    private boolean enableMagicNumberValidation = true;

    /**
     * Enable virus scanning integration (optional, requires external service)
     */
    private boolean enableVirusScanning = false;

    /**
     * Virus scanning service URL (if enabled)
     */
    private String virusScanningUrl;

    // Getters and Setters

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public long getMaxLogoSize() {
        return maxLogoSize;
    }

    public void setMaxLogoSize(long maxLogoSize) {
        this.maxLogoSize = maxLogoSize;
    }

    public Set<String> getAllowedFileExtensions() {
        return allowedFileExtensions;
    }

    public void setAllowedFileExtensions(Set<String> allowedFileExtensions) {
        this.allowedFileExtensions = allowedFileExtensions;
    }

    public Set<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }

    public void setAllowedMimeTypes(Set<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
    }

    public Set<String> getAllowedImageTypes() {
        return allowedImageTypes;
    }

    public void setAllowedImageTypes(Set<String> allowedImageTypes) {
        this.allowedImageTypes = allowedImageTypes;
    }

    public Set<String> getForbiddenFilenameCharacters() {
        return forbiddenFilenameCharacters;
    }

    public void setForbiddenFilenameCharacters(Set<String> forbiddenFilenameCharacters) {
        this.forbiddenFilenameCharacters = forbiddenFilenameCharacters;
    }

    public boolean isEnableMagicNumberValidation() {
        return enableMagicNumberValidation;
    }

    public void setEnableMagicNumberValidation(boolean enableMagicNumberValidation) {
        this.enableMagicNumberValidation = enableMagicNumberValidation;
    }

    public boolean isEnableVirusScanning() {
        return enableVirusScanning;
    }

    public void setEnableVirusScanning(boolean enableVirusScanning) {
        this.enableVirusScanning = enableVirusScanning;
    }

    public String getVirusScanningUrl() {
        return virusScanningUrl;
    }

    public void setVirusScanningUrl(String virusScanningUrl) {
        this.virusScanningUrl = virusScanningUrl;
    }

    public String[][] getImageMagicNumbers() {
        return IMAGE_MAGIC_NUMBERS;
    }

    /**
     * Get the magic number (file signature) for a given MIME type
     * @param mimeType The MIME type to look up
     * @return The magic number as a hex string, or null if not found
     */
    public String getMagicNumber(String mimeType) {
        for (String[] entry : IMAGE_MAGIC_NUMBERS) {
            if (entry[0].equalsIgnoreCase(mimeType)) {
                return entry[1];
            }
        }
        return null;
    }

    /**
     * Check if a file extension is allowed
     * @param extension The file extension (without dot)
     * @return true if allowed, false otherwise
     */
    public boolean isExtensionAllowed(String extension) {
        if (extension == null) {
            return false;
        }
        return allowedFileExtensions.contains(extension.toLowerCase());
    }

    /**
     * Check if a MIME type is allowed for documents
     * @param mimeType The MIME type
     * @return true if allowed, false otherwise
     */
    public boolean isMimeTypeAllowed(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return allowedMimeTypes.contains(mimeType.toLowerCase());
    }

    /**
     * Check if an image MIME type is allowed
     * @param mimeType The image MIME type
     * @return true if allowed, false otherwise
     */
    public boolean isImageTypeAllowed(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return allowedImageTypes.contains(mimeType.toLowerCase());
    }
}

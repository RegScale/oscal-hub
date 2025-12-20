package gov.nist.oscal.tools.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for sanitizing file paths to prevent path traversal attacks.
 *
 * This class provides methods to safely resolve and validate file paths,
 * ensuring that resolved paths remain within their intended base directories.
 */
public final class PathSanitizer {

    private static final Logger logger = LoggerFactory.getLogger(PathSanitizer.class);

    private PathSanitizer() {
        // Utility class - prevent instantiation
    }

    /**
     * Safely resolve a filename within a base directory, preventing path traversal attacks.
     *
     * @param baseDir The base directory that the resolved path must stay within
     * @param filename The filename or relative path to resolve
     * @return The safely resolved path
     * @throws IllegalArgumentException if the resolved path would escape the base directory
     */
    public static Path safeResolve(Path baseDir, String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Check for path traversal patterns in the input string (platform-independent)
        // This catches backslash-based traversal on Linux where \ is not a path separator
        if (!isPathSafe(filename)) {
            logger.warn("Path traversal pattern detected in input: {}", filename);
            throw new IllegalArgumentException("Invalid path: path traversal detected");
        }

        // Normalize the base directory
        Path normalizedBase = baseDir.toAbsolutePath().normalize();

        // Resolve and normalize the target path
        Path resolvedPath = normalizedBase.resolve(filename).normalize();

        // Verify the resolved path is within the base directory
        if (!resolvedPath.startsWith(normalizedBase)) {
            logger.warn("Path traversal attempt detected: base={}, filename={}, resolved={}",
                    normalizedBase, filename, resolvedPath);
            throw new IllegalArgumentException("Invalid path: path traversal detected");
        }

        return resolvedPath;
    }

    /**
     * Safely resolve a filename within a base directory specified as a string.
     *
     * @param baseDir The base directory path as a string
     * @param filename The filename or relative path to resolve
     * @return The safely resolved path
     * @throws IllegalArgumentException if the resolved path would escape the base directory
     */
    public static Path safeResolve(String baseDir, String filename) {
        if (baseDir == null || baseDir.trim().isEmpty()) {
            throw new IllegalArgumentException("Base directory cannot be null or empty");
        }
        return safeResolve(Paths.get(baseDir), filename);
    }

    /**
     * Safely resolve multiple path components within a base directory.
     *
     * @param baseDir The base directory that the resolved path must stay within
     * @param pathComponents The path components to resolve
     * @return The safely resolved path
     * @throws IllegalArgumentException if the resolved path would escape the base directory
     */
    public static Path safeResolve(Path baseDir, String... pathComponents) {
        if (pathComponents == null || pathComponents.length == 0) {
            throw new IllegalArgumentException("Path components cannot be null or empty");
        }

        // Normalize the base directory
        Path normalizedBase = baseDir.toAbsolutePath().normalize();

        // Build the path by resolving each component
        Path resolvedPath = normalizedBase;
        for (String component : pathComponents) {
            if (component == null || component.trim().isEmpty()) {
                throw new IllegalArgumentException("Path component cannot be null or empty");
            }
            // Check for path traversal patterns in each component (platform-independent)
            if (!isPathSafe(component)) {
                logger.warn("Path traversal pattern detected in component: {}", component);
                throw new IllegalArgumentException("Invalid path: path traversal detected");
            }
            resolvedPath = resolvedPath.resolve(component);
        }

        // Normalize and verify
        resolvedPath = resolvedPath.normalize();

        if (!resolvedPath.startsWith(normalizedBase)) {
            logger.warn("Path traversal attempt detected: base={}, components={}, resolved={}",
                    normalizedBase, String.join("/", pathComponents), resolvedPath);
            throw new IllegalArgumentException("Invalid path: path traversal detected");
        }

        return resolvedPath;
    }

    /**
     * Sanitize a filename by removing path separators and traversal sequences.
     * This extracts only the filename portion from any path.
     *
     * @param filename The filename to sanitize
     * @return The sanitized filename (base name only)
     * @throws IllegalArgumentException if the filename is null, empty, or results in an empty string
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // First, remove characters that are illegal in file paths on Windows/Unix
        // This must be done BEFORE calling Paths.get() to avoid InvalidPathException
        // Illegal chars: < > : " | ? * and null/control characters
        String sanitized = filename.replaceAll("[<>:\"|?*\\x00-\\x1F]", "_");

        // Get only the filename portion (removes any directory components)
        // Use forward slash normalization for cross-platform compatibility
        sanitized = sanitized.replace("\\", "/");
        int lastSlash = sanitized.lastIndexOf('/');
        String baseName = (lastSlash >= 0) ? sanitized.substring(lastSlash + 1) : sanitized;

        // Remove any remaining traversal patterns
        baseName = baseName.replace("..", "").replace("~", "");

        // Remove or replace remaining non-alphanumeric characters (except . _ -)
        baseName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Collapse multiple underscores
        baseName = baseName.replaceAll("_+", "_");

        // Remove leading/trailing underscores
        baseName = baseName.replaceAll("^_+|_+$", "");

        // Ensure we have a valid filename
        if (baseName.isEmpty()) {
            throw new IllegalArgumentException("Filename is invalid after sanitization");
        }

        return baseName;
    }

    /**
     * Validate that a path does not contain traversal sequences.
     *
     * @param path The path string to validate
     * @return true if the path is safe, false if it contains traversal sequences
     */
    public static boolean isPathSafe(String path) {
        if (path == null) {
            return false;
        }

        // Check for common traversal patterns
        String normalizedPath = path.replace("\\", "/");
        return !normalizedPath.contains("../")
                && !normalizedPath.contains("..\\")
                && !normalizedPath.startsWith("..")
                && !normalizedPath.contains("/..")
                && !normalizedPath.contains("\\..");
    }
}

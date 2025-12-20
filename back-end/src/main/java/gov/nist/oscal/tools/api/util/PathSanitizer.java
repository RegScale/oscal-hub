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
        // Using character-by-character processing to avoid ReDoS vulnerabilities
        StringBuilder sanitized = new StringBuilder(filename.length());
        for (int i = 0; i < filename.length(); i++) {
            char c = filename.charAt(i);
            // Replace illegal characters with underscore
            if (c == '<' || c == '>' || c == ':' || c == '"' || c == '|'
                    || c == '?' || c == '*' || c < 0x20) {
                sanitized.append('_');
            } else {
                sanitized.append(c);
            }
        }

        // Get only the filename portion (removes any directory components)
        // Use forward slash normalization for cross-platform compatibility
        String sanitizedStr = sanitized.toString().replace("\\", "/");
        int lastSlash = sanitizedStr.lastIndexOf('/');
        String baseName = (lastSlash >= 0) ? sanitizedStr.substring(lastSlash + 1) : sanitizedStr;

        // Remove any remaining traversal patterns (simple string replacement, no regex)
        baseName = baseName.replace("..", "").replace("~", "");

        // Replace any remaining non-alphanumeric characters (except . _ -)
        // Using character-by-character processing to avoid ReDoS
        StringBuilder cleaned = new StringBuilder(baseName.length());
        for (int i = 0; i < baseName.length(); i++) {
            char c = baseName.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-') {
                cleaned.append(c);
            } else {
                cleaned.append('_');
            }
        }
        baseName = cleaned.toString();

        // Collapse multiple underscores and strip leading/trailing underscores
        // Using loop-based approach to avoid ReDoS from regex like "^_+|_+$"
        baseName = collapseAndStripUnderscores(baseName);

        // Ensure we have a valid filename
        if (baseName.isEmpty()) {
            throw new IllegalArgumentException("Filename is invalid after sanitization");
        }

        return baseName;
    }

    /**
     * Collapse multiple consecutive underscores into one and strip leading/trailing underscores.
     * Uses loop-based approach to avoid ReDoS vulnerabilities from regex patterns like "^_+|_+$".
     *
     * @param input The string to process
     * @return The processed string with collapsed and stripped underscores
     */
    private static String collapseAndStripUnderscores(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Collapse multiple underscores into single underscore
        StringBuilder collapsed = new StringBuilder(input.length());
        boolean lastWasUnderscore = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '_') {
                if (!lastWasUnderscore) {
                    collapsed.append(c);
                    lastWasUnderscore = true;
                }
                // Skip consecutive underscores
            } else {
                collapsed.append(c);
                lastWasUnderscore = false;
            }
        }

        // Strip leading underscores
        int start = 0;
        while (start < collapsed.length() && collapsed.charAt(start) == '_') {
            start++;
        }

        // Strip trailing underscores
        int end = collapsed.length();
        while (end > start && collapsed.charAt(end - 1) == '_') {
            end--;
        }

        return collapsed.substring(start, end);
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

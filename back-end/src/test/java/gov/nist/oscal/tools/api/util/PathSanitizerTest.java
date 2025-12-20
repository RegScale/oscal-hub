package gov.nist.oscal.tools.api.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PathSanitizerTest {

    @TempDir
    Path tempDir;

    // ===================
    // safeResolve(Path, String) tests
    // ===================

    @Test
    void safeResolve_validFilename_resolvesCorrectly() {
        Path result = PathSanitizer.safeResolve(tempDir, "test.txt");

        assertEquals(tempDir.resolve("test.txt").normalize(), result);
        assertTrue(result.startsWith(tempDir));
    }

    @Test
    void safeResolve_validSubdirectory_resolvesCorrectly() {
        Path result = PathSanitizer.safeResolve(tempDir, "subdir/test.txt");

        assertEquals(tempDir.resolve("subdir/test.txt").normalize(), result);
        assertTrue(result.startsWith(tempDir));
    }

    @Test
    void safeResolve_pathTraversalAttempt_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve(tempDir, "../../../etc/passwd")
        );

        assertEquals("Invalid path: path traversal detected", exception.getMessage());
    }

    @Test
    void safeResolve_pathTraversalWithBackslash_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve(tempDir, "..\\..\\..\\etc\\passwd")
        );

        assertEquals("Invalid path: path traversal detected", exception.getMessage());
    }

    @Test
    void safeResolve_complexPathTraversal_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve(tempDir, "valid/../../secret/file.txt")
        );

        assertEquals("Invalid path: path traversal detected", exception.getMessage());
    }

    @Test
    void safeResolve_absolutePath_handledCorrectly() {
        // Absolute paths that try to escape should be caught
        String absolutePath = "/etc/passwd";
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve(tempDir, absolutePath)
        );

        assertEquals("Invalid path: path traversal detected", exception.getMessage());
    }

    @Test
    void safeResolve_nullFilename_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve(tempDir, (String) null)
        );

        assertEquals("Filename cannot be null or empty", exception.getMessage());
    }

    @Test
    void safeResolve_emptyFilename_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve(tempDir, "")
        );

        assertEquals("Filename cannot be null or empty", exception.getMessage());
    }

    @Test
    void safeResolve_whitespaceFilename_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve(tempDir, "   ")
        );

        assertEquals("Filename cannot be null or empty", exception.getMessage());
    }

    // ===================
    // safeResolve(String, String) tests
    // ===================

    @Test
    void safeResolveString_validFilename_resolvesCorrectly() {
        Path result = PathSanitizer.safeResolve(tempDir.toString(), "test.txt");

        assertEquals(tempDir.resolve("test.txt").normalize(), result);
    }

    @Test
    void safeResolveString_nullBaseDir_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve((String) null, "test.txt")
        );

        assertEquals("Base directory cannot be null or empty", exception.getMessage());
    }

    @Test
    void safeResolveString_emptyBaseDir_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve("", "test.txt")
        );

        assertEquals("Base directory cannot be null or empty", exception.getMessage());
    }

    // ===================
    // safeResolve(Path, String...) tests
    // ===================

    @Test
    void safeResolveVarargs_multipleComponents_resolvesCorrectly() {
        Path result = PathSanitizer.safeResolve(tempDir, "subdir", "another", "file.txt");

        assertEquals(tempDir.resolve("subdir/another/file.txt").normalize(), result);
        assertTrue(result.startsWith(tempDir));
    }

    @Test
    void safeResolveVarargs_traversalInMiddle_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve(tempDir, "subdir", "..", "..", "secret.txt")
        );

        assertEquals("Invalid path: path traversal detected", exception.getMessage());
    }

    @Test
    void safeResolveVarargs_nullArray_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve(tempDir, (String[]) null)
        );

        assertEquals("Path components cannot be null or empty", exception.getMessage());
    }

    @Test
    void safeResolveVarargs_emptyArray_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve(tempDir, new String[]{})
        );

        assertEquals("Path components cannot be null or empty", exception.getMessage());
    }

    @Test
    void safeResolveVarargs_nullComponent_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve(tempDir, "valid", null, "file.txt")
        );

        assertEquals("Path component cannot be null or empty", exception.getMessage());
    }

    // ===================
    // sanitizeFilename tests
    // ===================

    @Test
    void sanitizeFilename_validFilename_returnsUnchanged() {
        String result = PathSanitizer.sanitizeFilename("test-file_123.txt");

        assertEquals("test-file_123.txt", result);
    }

    @Test
    void sanitizeFilename_pathTraversalAttempt_extractsFilename() {
        String result = PathSanitizer.sanitizeFilename("../../../etc/passwd");

        assertEquals("passwd", result);
    }

    @Test
    void sanitizeFilename_windowsPath_extractsFilename() {
        String result = PathSanitizer.sanitizeFilename("C:\\Users\\Admin\\file.txt");

        assertEquals("file.txt", result);
    }

    @Test
    void sanitizeFilename_specialCharacters_sanitized() {
        String result = PathSanitizer.sanitizeFilename("file<with>special:chars.txt");

        // Special chars are replaced with _, then collapsed and trimmed
        assertEquals("file_with_special_chars.txt", result);
    }

    @Test
    void sanitizeFilename_nullFilename_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.sanitizeFilename(null)
        );

        assertEquals("Filename cannot be null or empty", exception.getMessage());
    }

    @Test
    void sanitizeFilename_emptyFilename_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.sanitizeFilename("")
        );

        assertEquals("Filename cannot be null or empty", exception.getMessage());
    }

    @Test
    void sanitizeFilename_doubleDotOnly_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.sanitizeFilename("..")
        );

        assertEquals("Filename is invalid after sanitization", exception.getMessage());
    }

    // ===================
    // isPathSafe tests
    // ===================

    @Test
    void isPathSafe_validPath_returnsTrue() {
        assertTrue(PathSanitizer.isPathSafe("subdir/file.txt"));
        assertTrue(PathSanitizer.isPathSafe("file.txt"));
        assertTrue(PathSanitizer.isPathSafe("dir1/dir2/file.txt"));
    }

    @Test
    void isPathSafe_pathTraversal_returnsFalse() {
        assertFalse(PathSanitizer.isPathSafe("../secret.txt"));
        assertFalse(PathSanitizer.isPathSafe("subdir/../../../etc/passwd"));
        assertFalse(PathSanitizer.isPathSafe(".."));
    }

    @Test
    void isPathSafe_windowsTraversal_returnsFalse() {
        assertFalse(PathSanitizer.isPathSafe("..\\secret.txt"));
        assertFalse(PathSanitizer.isPathSafe("subdir\\..\\..\\secret.txt"));
    }

    @Test
    void isPathSafe_nullPath_returnsFalse() {
        assertFalse(PathSanitizer.isPathSafe(null));
    }

    // ===================
    // Real-world attack scenarios
    // ===================

    @Test
    void attackScenario_encodedTraversal_blocked() {
        // URL-encoded path traversal (decoded by the time it reaches here)
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve(tempDir, "..%2F..%2Fetc%2Fpasswd".replace("%2F", "/"))
        );

        assertEquals("Invalid path: path traversal detected", exception.getMessage());
    }

    @Test
    void attackScenario_nullByteInjection_handled() {
        // Null byte injection attempt - null bytes are replaced with underscore
        // then the path traversal is extracted to just the filename
        String maliciousInput = "file.txt\0../../../etc/passwd";
        String result = PathSanitizer.sanitizeFilename(maliciousInput);
        // Null byte is replaced, then ".." is removed, leaving just "passwd"
        assertEquals("passwd", result);
    }

    @Test
    void attackScenario_unicodeTraversal_blocked() {
        // Unicode variations of path separators
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PathSanitizer.safeResolve(tempDir, "..／..／etc／passwd".replace("／", "/"))
        );

        assertEquals("Invalid path: path traversal detected", exception.getMessage());
    }
}

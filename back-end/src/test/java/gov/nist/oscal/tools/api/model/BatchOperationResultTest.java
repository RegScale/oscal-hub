package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BatchOperationResultTest {

    // ========== BatchOperationResult Tests ==========

    @Test
    void testTwoArgsConstructor() {
        String operationId = "batch-123";
        int totalFiles = 5;

        BatchOperationResult result = new BatchOperationResult(operationId, totalFiles);

        assertNotNull(result);
        assertTrue(result.isSuccess()); // default true in this constructor
        assertEquals(operationId, result.getOperationId());
        assertEquals(totalFiles, result.getTotalFiles());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertNull(result.getResults());
        assertEquals(0, result.getTotalDurationMs());
    }

    @Test
    void testSevenArgsConstructor() {
        boolean success = true;
        String operationId = "batch-456";
        int totalFiles = 10;
        int successCount = 8;
        int failureCount = 2;
        List<BatchOperationResult.FileResult> results = new ArrayList<>();
        long totalDurationMs = 5000L;

        BatchOperationResult result = new BatchOperationResult(
                success, operationId, totalFiles, successCount, failureCount, results, totalDurationMs
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(operationId, result.getOperationId());
        assertEquals(totalFiles, result.getTotalFiles());
        assertEquals(successCount, result.getSuccessCount());
        assertEquals(failureCount, result.getFailureCount());
        assertEquals(results, result.getResults());
        assertEquals(totalDurationMs, result.getTotalDurationMs());
    }

    @Test
    void testSetSuccess() {
        BatchOperationResult result = new BatchOperationResult("op-1", 5);
        assertTrue(result.isSuccess());

        result.setSuccess(false);
        assertFalse(result.isSuccess());
    }

    @Test
    void testSetOperationId() {
        BatchOperationResult result = new BatchOperationResult("op-1", 5);
        assertEquals("op-1", result.getOperationId());

        String newOperationId = "op-999";
        result.setOperationId(newOperationId);
        assertEquals(newOperationId, result.getOperationId());
    }

    @Test
    void testSetTotalFiles() {
        BatchOperationResult result = new BatchOperationResult("op-1", 5);
        assertEquals(5, result.getTotalFiles());

        result.setTotalFiles(100);
        assertEquals(100, result.getTotalFiles());
    }

    @Test
    void testSetSuccessCount() {
        BatchOperationResult result = new BatchOperationResult("op-1", 10);
        assertEquals(0, result.getSuccessCount());

        result.setSuccessCount(7);
        assertEquals(7, result.getSuccessCount());
    }

    @Test
    void testSetFailureCount() {
        BatchOperationResult result = new BatchOperationResult("op-1", 10);
        assertEquals(0, result.getFailureCount());

        result.setFailureCount(3);
        assertEquals(3, result.getFailureCount());
    }

    @Test
    void testSetResults() {
        BatchOperationResult result = new BatchOperationResult("op-1", 5);
        assertNull(result.getResults());

        List<BatchOperationResult.FileResult> fileResults = Arrays.asList(
                new BatchOperationResult.FileResult("file1.json", true, null, null, 100L),
                new BatchOperationResult.FileResult("file2.json", false, "Error", null, 200L)
        );

        result.setResults(fileResults);
        assertEquals(fileResults, result.getResults());
        assertEquals(2, result.getResults().size());
    }

    @Test
    void testSetTotalDurationMs() {
        BatchOperationResult result = new BatchOperationResult("op-1", 5);
        assertEquals(0, result.getTotalDurationMs());

        result.setTotalDurationMs(12345L);
        assertEquals(12345L, result.getTotalDurationMs());
    }

    @Test
    void testAllSuccessScenario() {
        List<BatchOperationResult.FileResult> fileResults = Arrays.asList(
                new BatchOperationResult.FileResult("file1.xml", true, null, new Object(), 100L),
                new BatchOperationResult.FileResult("file2.json", true, null, new Object(), 150L),
                new BatchOperationResult.FileResult("file3.yaml", true, null, new Object(), 200L)
        );

        BatchOperationResult result = new BatchOperationResult(
                true, "batch-all-success", 3, 3, 0, fileResults, 450L
        );

        assertTrue(result.isSuccess());
        assertEquals(3, result.getTotalFiles());
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(450L, result.getTotalDurationMs());
    }

    @Test
    void testAllFailureScenario() {
        List<BatchOperationResult.FileResult> fileResults = Arrays.asList(
                new BatchOperationResult.FileResult("bad1.xml", false, "Parse error", null, 50L),
                new BatchOperationResult.FileResult("bad2.json", false, "Invalid schema", null, 75L)
        );

        BatchOperationResult result = new BatchOperationResult(
                false, "batch-all-fail", 2, 0, 2, fileResults, 125L
        );

        assertFalse(result.isSuccess());
        assertEquals(2, result.getTotalFiles());
        assertEquals(0, result.getSuccessCount());
        assertEquals(2, result.getFailureCount());
        assertEquals(125L, result.getTotalDurationMs());
    }

    @Test
    void testPartialSuccessScenario() {
        List<BatchOperationResult.FileResult> fileResults = Arrays.asList(
                new BatchOperationResult.FileResult("good.json", true, null, new Object(), 100L),
                new BatchOperationResult.FileResult("bad.json", false, "Validation failed", null, 50L),
                new BatchOperationResult.FileResult("good2.xml", true, null, new Object(), 150L)
        );

        BatchOperationResult result = new BatchOperationResult(
                false, "batch-partial", 3, 2, 1, fileResults, 300L
        );

        assertFalse(result.isSuccess()); // overall success is false if any failed
        assertEquals(3, result.getTotalFiles());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(3, result.getResults().size());
    }

    @Test
    void testEmptyResultsList() {
        List<BatchOperationResult.FileResult> emptyResults = new ArrayList<>();

        BatchOperationResult result = new BatchOperationResult(
                true, "batch-empty", 0, 0, 0, emptyResults, 0L
        );

        assertTrue(result.isSuccess());
        assertEquals(0, result.getTotalFiles());
        assertNotNull(result.getResults());
        assertTrue(result.getResults().isEmpty());
    }

    @Test
    void testLargeFileCount() {
        BatchOperationResult result = new BatchOperationResult("batch-large", 10000);

        result.setSuccessCount(9500);
        result.setFailureCount(500);
        result.setTotalDurationMs(300000L);

        assertEquals(10000, result.getTotalFiles());
        assertEquals(9500, result.getSuccessCount());
        assertEquals(500, result.getFailureCount());
    }

    @Test
    void testZeroDuration() {
        BatchOperationResult result = new BatchOperationResult(
                true, "batch-instant", 1, 1, 0, new ArrayList<>(), 0L
        );

        assertEquals(0L, result.getTotalDurationMs());
    }

    @Test
    void testNullOperationId() {
        BatchOperationResult result = new BatchOperationResult(null, 5);
        assertNull(result.getOperationId());

        result.setOperationId("new-op-id");
        assertEquals("new-op-id", result.getOperationId());

        result.setOperationId(null);
        assertNull(result.getOperationId());
    }

    @Test
    void testNullResultsList() {
        BatchOperationResult result = new BatchOperationResult(
                true, "batch-null-results", 5, 5, 0, null, 1000L
        );

        assertNull(result.getResults());
    }

    // ========== FileResult Tests ==========

    @Test
    void testFileResultNoArgsConstructor() {
        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult();

        assertNotNull(fileResult);
        assertNull(fileResult.getFilename());
        assertFalse(fileResult.isSuccess()); // primitive boolean defaults to false
        assertNull(fileResult.getError());
        assertNull(fileResult.getResult());
        assertEquals(0L, fileResult.getDurationMs());
    }

    @Test
    void testFileResultFiveArgsConstructor() {
        String filename = "test.json";
        boolean success = true;
        String error = null;
        Object result = new ValidationResult();
        long durationMs = 250L;

        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult(
                filename, success, error, result, durationMs
        );

        assertNotNull(fileResult);
        assertEquals(filename, fileResult.getFilename());
        assertTrue(fileResult.isSuccess());
        assertNull(fileResult.getError());
        assertEquals(result, fileResult.getResult());
        assertEquals(durationMs, fileResult.getDurationMs());
    }

    @Test
    void testFileResultSetFilename() {
        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult();
        assertNull(fileResult.getFilename());

        String filename = "document.xml";
        fileResult.setFilename(filename);
        assertEquals(filename, fileResult.getFilename());
    }

    @Test
    void testFileResultSetSuccess() {
        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult();
        assertFalse(fileResult.isSuccess());

        fileResult.setSuccess(true);
        assertTrue(fileResult.isSuccess());

        fileResult.setSuccess(false);
        assertFalse(fileResult.isSuccess());
    }

    @Test
    void testFileResultSetError() {
        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult();
        assertNull(fileResult.getError());

        String error = "File not found";
        fileResult.setError(error);
        assertEquals(error, fileResult.getError());
    }

    @Test
    void testFileResultSetResult() {
        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult();
        assertNull(fileResult.getResult());

        Object result = new ConversionResult(true, "converted content",
                OscalFormat.JSON, OscalFormat.XML);
        fileResult.setResult(result);
        assertEquals(result, fileResult.getResult());
    }

    @Test
    void testFileResultSetDurationMs() {
        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult();
        assertEquals(0L, fileResult.getDurationMs());

        fileResult.setDurationMs(500L);
        assertEquals(500L, fileResult.getDurationMs());
    }

    @Test
    void testFileResultSuccessCase() {
        ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(true);

        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult(
                "valid-catalog.json", true, null, validationResult, 150L
        );

        assertTrue(fileResult.isSuccess());
        assertNull(fileResult.getError());
        assertNotNull(fileResult.getResult());
        assertEquals(150L, fileResult.getDurationMs());
    }

    @Test
    void testFileResultFailureCase() {
        String errorMessage = "Schema validation failed: missing required field 'metadata'";

        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult(
                "invalid-ssp.xml", false, errorMessage, null, 75L
        );

        assertFalse(fileResult.isSuccess());
        assertEquals(errorMessage, fileResult.getError());
        assertNull(fileResult.getResult());
    }

    @Test
    void testFileResultWithLongFilename() {
        String longFilename = "/very/long/path/to/some/directory/structure/deeply/nested/file-with-very-long-name.json";

        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult();
        fileResult.setFilename(longFilename);

        assertEquals(longFilename, fileResult.getFilename());
    }

    @Test
    void testFileResultWithVariousFileExtensions() {
        String[] filenames = {"test.json", "test.xml", "test.yaml", "test.yml"};

        for (String filename : filenames) {
            BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult();
            fileResult.setFilename(filename);
            assertEquals(filename, fileResult.getFilename());
        }
    }

    @Test
    void testFileResultNullFilename() {
        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult(
                null, true, null, new Object(), 100L
        );

        assertNull(fileResult.getFilename());
    }

    @Test
    void testFileResultNullError() {
        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult(
                "file.json", true, null, new Object(), 100L
        );

        assertNull(fileResult.getError());
    }

    @Test
    void testFileResultNullResult() {
        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult(
                "file.json", false, "Error occurred", null, 50L
        );

        assertNull(fileResult.getResult());
    }

    @Test
    void testFileResultZeroDuration() {
        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult(
                "instant.json", true, null, new Object(), 0L
        );

        assertEquals(0L, fileResult.getDurationMs());
    }

    @Test
    void testFileResultLongDuration() {
        long longDuration = 999999L;
        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult(
                "slow.xml", true, null, new Object(), longDuration
        );

        assertEquals(longDuration, fileResult.getDurationMs());
    }

    @Test
    void testFileResultMultilineError() {
        String multilineError = "Multiple validation errors:\n" +
                "- Line 15: Missing required element 'title'\n" +
                "- Line 42: Invalid date format\n" +
                "- Line 67: Unknown property 'custom-field'";

        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult();
        fileResult.setSuccess(false);
        fileResult.setError(multilineError);

        assertFalse(fileResult.isSuccess());
        assertTrue(fileResult.getError().contains("Multiple validation errors"));
        assertTrue(fileResult.getError().contains("Line 15"));
    }

    @Test
    void testFileResultWithValidationResult() {
        ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(false);

        List<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError("Invalid format", "ERROR"));
        validationResult.setErrors(errors);

        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult(
                "test.json", false, "Validation failed", validationResult, 200L
        );

        assertFalse(fileResult.isSuccess());
        assertNotNull(fileResult.getResult());
        assertTrue(fileResult.getResult() instanceof ValidationResult);

        ValidationResult storedResult = (ValidationResult) fileResult.getResult();
        assertFalse(storedResult.isValid());
    }

    @Test
    void testFileResultWithConversionResult() {
        ConversionResult conversionResult = new ConversionResult(true, "converted content",
                OscalFormat.XML, OscalFormat.JSON);

        BatchOperationResult.FileResult fileResult = new BatchOperationResult.FileResult(
                "convert.xml", true, null, conversionResult, 300L
        );

        assertTrue(fileResult.isSuccess());
        assertNotNull(fileResult.getResult());
        assertTrue(fileResult.getResult() instanceof ConversionResult);

        ConversionResult storedResult = (ConversionResult) fileResult.getResult();
        assertTrue(storedResult.isSuccess());
        assertEquals("converted content", storedResult.getContent());
    }

    @Test
    void testIntegrationBatchWithMultipleFileResults() {
        // Create multiple file results
        ValidationResult validResult = new ValidationResult();
        validResult.setValid(true);

        ConversionResult conversionResult = new ConversionResult(true, "converted content",
                OscalFormat.JSON, OscalFormat.XML);

        List<BatchOperationResult.FileResult> fileResults = Arrays.asList(
                new BatchOperationResult.FileResult("file1.json", true, null, validResult, 100L),
                new BatchOperationResult.FileResult("file2.xml", true, null, conversionResult, 150L),
                new BatchOperationResult.FileResult("file3.yaml", false, "Parse error", null, 50L)
        );

        // Create batch operation result
        BatchOperationResult batchResult = new BatchOperationResult(
                false, "batch-mixed", 3, 2, 1, fileResults, 300L
        );

        // Verify batch result
        assertEquals(3, batchResult.getTotalFiles());
        assertEquals(2, batchResult.getSuccessCount());
        assertEquals(1, batchResult.getFailureCount());
        assertEquals(3, batchResult.getResults().size());

        // Verify individual file results
        assertTrue(batchResult.getResults().get(0).isSuccess());
        assertTrue(batchResult.getResults().get(1).isSuccess());
        assertFalse(batchResult.getResults().get(2).isSuccess());

        // Verify results are correct type
        assertNotNull(batchResult.getResults().get(0).getResult());
        assertNotNull(batchResult.getResults().get(1).getResult());
        assertNull(batchResult.getResults().get(2).getResult());
    }
}

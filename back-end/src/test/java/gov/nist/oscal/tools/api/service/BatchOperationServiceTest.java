package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.OperationHistory;
import gov.nist.oscal.tools.api.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BatchOperationServiceTest {

    @Mock
    private ValidationService validationService;

    @Mock
    private ConversionService conversionService;

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private BatchOperationService batchOperationService;

    private BatchOperationRequest validationRequest;
    private BatchOperationRequest conversionRequest;
    private List<BatchOperationRequest.FileContent> testFiles;

    @BeforeEach
    void setUp() {
        // Create test files
        testFiles = new ArrayList<>();

        BatchOperationRequest.FileContent file1 = new BatchOperationRequest.FileContent();
        file1.setFilename("test1.json");
        file1.setContent("{\"catalog\": {\"uuid\": \"test-1\"}}");
        file1.setFormat(OscalFormat.JSON);
        testFiles.add(file1);

        BatchOperationRequest.FileContent file2 = new BatchOperationRequest.FileContent();
        file2.setFilename("test2.xml");
        file2.setContent("<catalog><uuid>test-2</uuid></catalog>");
        file2.setFormat(OscalFormat.XML);
        testFiles.add(file2);

        // Create validation request
        validationRequest = new BatchOperationRequest();
        validationRequest.setOperationType(BatchOperationRequest.BatchOperationType.VALIDATE);
        validationRequest.setModelType(OscalModelType.CATALOG);
        validationRequest.setFiles(testFiles);

        // Create conversion request
        conversionRequest = new BatchOperationRequest();
        conversionRequest.setOperationType(BatchOperationRequest.BatchOperationType.CONVERT);
        conversionRequest.setModelType(OscalModelType.CATALOG);
        conversionRequest.setFromFormat(OscalFormat.JSON);
        conversionRequest.setToFormat(OscalFormat.XML);
        conversionRequest.setFiles(testFiles);
    }

    // ========== Batch Validation Tests ==========

    @Test
    void testProcessBatch_validation_success() throws InterruptedException {
        // Mock successful validation results
        ValidationResult valResult1 = new ValidationResult();
        valResult1.setValid(true);
        valResult1.setErrors(Collections.emptyList());

        ValidationResult valResult2 = new ValidationResult();
        valResult2.setValid(true);
        valResult2.setErrors(Collections.emptyList());

        when(validationService.validate(any(ValidationRequest.class), eq("testuser")))
                .thenReturn(valResult1, valResult2);

        BatchOperationResult result = batchOperationService.processBatch(validationRequest, "testuser");

        assertNotNull(result);
        assertNotNull(result.getOperationId());
        assertEquals(2, result.getTotalFiles());
        assertNull(result.getResults()); // Initially no results

        // Wait for async processing
        Thread.sleep(1000);

        // Get completed result
        BatchOperationResult completedResult = batchOperationService.getBatchResult(result.getOperationId());
        assertNotNull(completedResult);
        assertNotNull(completedResult.getResults()); // Completed now has results
        assertEquals(2, completedResult.getSuccessCount());
        assertEquals(0, completedResult.getFailureCount());
        assertEquals(2, completedResult.getResults().size());

        // Verify history was saved
        verify(historyService, atLeast(2)).saveOperation(any(OperationHistory.class));
    }

    @Test
    void testProcessBatch_validation_withFailures() throws InterruptedException {
        // Mock validation results - one success, one failure
        ValidationResult valResult1 = new ValidationResult();
        valResult1.setValid(true);
        valResult1.setErrors(Collections.emptyList());

        ValidationResult valResult2 = new ValidationResult();
        valResult2.setValid(false);
        ValidationError error = new ValidationError();
        error.setMessage("Validation failed");
        error.setSeverity("ERROR");
        valResult2.setErrors(Collections.singletonList(error));

        when(validationService.validate(any(ValidationRequest.class), eq("testuser")))
                .thenReturn(valResult1, valResult2);

        BatchOperationResult result = batchOperationService.processBatch(validationRequest, "testuser");

        assertNotNull(result);
        assertNull(result.getResults());

        // Wait for async processing
        Thread.sleep(1000);

        BatchOperationResult completedResult = batchOperationService.getBatchResult(result.getOperationId());
        assertNotNull(completedResult);
        assertNotNull(completedResult.getResults());
        assertEquals(1, completedResult.getSuccessCount());
        assertEquals(1, completedResult.getFailureCount());

        // Verify failure file has error message
        BatchOperationResult.FileResult failedFile = completedResult.getResults().stream()
                .filter(f -> !f.isSuccess())
                .findFirst()
                .orElse(null);
        assertNotNull(failedFile);
        assertEquals("Validation failed", failedFile.getError());
    }

    @Test
    void testProcessBatch_validation_exception() throws InterruptedException {
        // Mock validation service throwing exception
        when(validationService.validate(any(ValidationRequest.class), eq("testuser")))
                .thenThrow(new RuntimeException("Validation service error"));

        BatchOperationResult result = batchOperationService.processBatch(validationRequest, "testuser");

        assertNotNull(result);

        // Wait for async processing
        Thread.sleep(1000);

        BatchOperationResult completedResult = batchOperationService.getBatchResult(result.getOperationId());
        assertNotNull(completedResult);
        assertNotNull(completedResult.getResults());
        assertEquals(0, completedResult.getSuccessCount());
        assertEquals(2, completedResult.getFailureCount());

        // All files should have error messages
        completedResult.getResults().forEach(fileResult -> {
            assertFalse(fileResult.isSuccess());
            assertNotNull(fileResult.getError());
            assertTrue(fileResult.getError().contains("Processing error"));
        });
    }

    // ========== Batch Conversion Tests ==========

    @Test
    void testProcessBatch_conversion_success() throws InterruptedException {
        // Mock successful conversion results
        ConversionResult convResult1 = new ConversionResult(true, "<converted>content1</converted>", OscalFormat.JSON, OscalFormat.XML);
        ConversionResult convResult2 = new ConversionResult(true, "<converted>content2</converted>", OscalFormat.JSON, OscalFormat.XML);

        when(conversionService.convert(any(ConversionRequest.class), eq("testuser")))
                .thenReturn(convResult1, convResult2);

        BatchOperationResult result = batchOperationService.processBatch(conversionRequest, "testuser");

        assertNotNull(result);
        assertNull(result.getResults());

        // Wait for async processing
        Thread.sleep(1000);

        BatchOperationResult completedResult = batchOperationService.getBatchResult(result.getOperationId());
        assertNotNull(completedResult);
        assertNotNull(completedResult.getResults());
        assertEquals(2, completedResult.getSuccessCount());
        assertEquals(0, completedResult.getFailureCount());
    }

    @Test
    void testProcessBatch_conversion_withFailures() throws InterruptedException {
        // Mock conversion results - one success, one failure
        ConversionResult convResult1 = new ConversionResult(true, "<converted>content1</converted>", OscalFormat.JSON, OscalFormat.XML);
        ConversionResult convResult2 = new ConversionResult(false, "Conversion error: Invalid format", OscalFormat.JSON, OscalFormat.XML, true);

        when(conversionService.convert(any(ConversionRequest.class), eq("testuser")))
                .thenReturn(convResult1, convResult2);

        BatchOperationResult result = batchOperationService.processBatch(conversionRequest, "testuser");

        assertNotNull(result);

        // Wait for async processing
        Thread.sleep(1000);

        BatchOperationResult completedResult = batchOperationService.getBatchResult(result.getOperationId());
        assertNotNull(completedResult);
        assertNotNull(completedResult.getResults());
        assertEquals(1, completedResult.getSuccessCount());
        assertEquals(1, completedResult.getFailureCount());

        // Verify failure file has error message
        BatchOperationResult.FileResult failedFile = completedResult.getResults().stream()
                .filter(f -> !f.isSuccess())
                .findFirst()
                .orElse(null);
        assertNotNull(failedFile);
        assertEquals("Conversion error: Invalid format", failedFile.getError());
    }

    @Test
    void testProcessBatch_conversion_exception() throws InterruptedException {
        when(conversionService.convert(any(ConversionRequest.class), eq("testuser")))
                .thenThrow(new RuntimeException("Conversion service error"));

        BatchOperationResult result = batchOperationService.processBatch(conversionRequest, "testuser");

        assertNotNull(result);

        // Wait for async processing
        Thread.sleep(1000);

        BatchOperationResult completedResult = batchOperationService.getBatchResult(result.getOperationId());
        assertNotNull(completedResult);
        assertEquals(0, completedResult.getSuccessCount());
        assertEquals(2, completedResult.getFailureCount());
    }

    // ========== Result Retrieval Tests ==========

    @Test
    void testGetBatchResult_success() throws InterruptedException {
        ValidationResult valResult = new ValidationResult();
        valResult.setValid(true);
        when(validationService.validate(any(ValidationRequest.class), eq("testuser")))
                .thenReturn(valResult);

        BatchOperationResult initialResult = batchOperationService.processBatch(validationRequest, "testuser");
        String operationId = initialResult.getOperationId();

        // Get result immediately
        BatchOperationResult retrievedResult = batchOperationService.getBatchResult(operationId);
        assertNotNull(retrievedResult);
        assertEquals(operationId, retrievedResult.getOperationId());
        assertNull(retrievedResult.getResults());

        // Wait and get completed result
        Thread.sleep(1000);
        BatchOperationResult completedResult = batchOperationService.getBatchResult(operationId);
        assertNotNull(completedResult);
        assertNotNull(completedResult.getResults());
    }

    @Test
    void testGetBatchResult_notFound() {
        BatchOperationResult result = batchOperationService.getBatchResult("non-existent-id");
        assertNull(result);
    }

    @Test
    void testGetAllBatchResults() throws InterruptedException {
        ValidationResult valResult = new ValidationResult();
        valResult.setValid(true);
        when(validationService.validate(any(ValidationRequest.class), eq("testuser")))
                .thenReturn(valResult);

        // Process multiple batches
        batchOperationService.processBatch(validationRequest, "testuser");
        batchOperationService.processBatch(validationRequest, "testuser");

        Thread.sleep(500); // Brief wait

        List<BatchOperationResult> allResults = batchOperationService.getAllBatchResults();
        assertNotNull(allResults);
        assertEquals(2, allResults.size());
    }

    @Test
    void testClearOldResults() throws InterruptedException {
        ValidationResult valResult = new ValidationResult();
        valResult.setValid(true);
        when(validationService.validate(any(ValidationRequest.class), eq("testuser")))
                .thenReturn(valResult);

        batchOperationService.processBatch(validationRequest, "testuser");

        Thread.sleep(500);

        List<BatchOperationResult> beforeClear = batchOperationService.getAllBatchResults();
        assertEquals(1, beforeClear.size());

        batchOperationService.clearOldResults();

        List<BatchOperationResult> afterClear = batchOperationService.getAllBatchResults();
        assertTrue(afterClear.isEmpty());
    }

    // ========== History Saving Tests ==========

    @Test
    void testHistorySaving_batchSummary() throws InterruptedException {
        ValidationResult valResult = new ValidationResult();
        valResult.setValid(true);
        when(validationService.validate(any(ValidationRequest.class), eq("testuser")))
                .thenReturn(valResult);

        batchOperationService.processBatch(validationRequest, "testuser");

        // Wait for async processing
        Thread.sleep(1000);

        // Verify batch summary was saved to history
        ArgumentCaptor<OperationHistory> historyCaptor = ArgumentCaptor.forClass(OperationHistory.class);
        verify(historyService, atLeast(1)).saveOperation(historyCaptor.capture());

        // Find the batch summary (not individual files)
        List<OperationHistory> savedHistories = historyCaptor.getAllValues();
        OperationHistory batchSummary = savedHistories.stream()
                .filter(h -> h.getFileName() != null && h.getFileName().contains("Batch"))
                .findFirst()
                .orElse(null);

        assertNotNull(batchSummary);
        assertEquals("BATCH_VALIDATE", batchSummary.getOperationType());
        assertTrue(batchSummary.getSuccess());
        assertTrue(batchSummary.getDetails().contains("success"));
    }

    @Test
    void testHistorySaving_individualFiles() throws InterruptedException {
        ValidationResult valResult = new ValidationResult();
        valResult.setValid(true);
        when(validationService.validate(any(ValidationRequest.class), eq("testuser")))
                .thenReturn(valResult);

        batchOperationService.processBatch(validationRequest, "testuser");

        // Wait for async processing
        Thread.sleep(1000);

        // Verify individual file histories were saved
        ArgumentCaptor<OperationHistory> historyCaptor = ArgumentCaptor.forClass(OperationHistory.class);
        verify(historyService, atLeast(2)).saveOperation(historyCaptor.capture());

        List<OperationHistory> savedHistories = historyCaptor.getAllValues();

        // Should have at least 2 individual file histories
        long fileHistoryCount = savedHistories.stream()
                .filter(h -> h.getFileName() != null && !h.getFileName().contains("Batch"))
                .count();
        assertTrue(fileHistoryCount >= 2);
    }

    @Test
    void testHistorySaving_failsGracefully() throws InterruptedException {
        // Mock history service to throw exception
        doThrow(new RuntimeException("History save failed"))
                .when(historyService).saveOperation(any(OperationHistory.class));

        ValidationResult valResult = new ValidationResult();
        valResult.setValid(true);
        when(validationService.validate(any(ValidationRequest.class), eq("testuser")))
                .thenReturn(valResult);

        // Should not throw exception even if history saving fails
        BatchOperationResult result = batchOperationService.processBatch(validationRequest, "testuser");

        assertNotNull(result);

        // Wait for async processing
        Thread.sleep(1000);

        // Batch should still complete successfully
        BatchOperationResult completedResult = batchOperationService.getBatchResult(result.getOperationId());
        assertNotNull(completedResult);
        assertNotNull(completedResult.getResults());
        assertEquals(2, completedResult.getSuccessCount());
    }

    // ========== Edge Cases ==========

    @Test
    void testProcessBatch_emptyFileList() throws InterruptedException {
        validationRequest.setFiles(Collections.emptyList());

        BatchOperationResult result = batchOperationService.processBatch(validationRequest, "testuser");

        assertNotNull(result);
        assertEquals(0, result.getTotalFiles());

        Thread.sleep(500);

        BatchOperationResult completedResult = batchOperationService.getBatchResult(result.getOperationId());
        assertNotNull(completedResult);
        assertNotNull(completedResult.getResults());
        assertEquals(0, completedResult.getSuccessCount());
        assertEquals(0, completedResult.getFailureCount());
        assertTrue(completedResult.getResults().isEmpty());
    }

    @Test
    void testProcessBatch_singleFile() throws InterruptedException {
        validationRequest.setFiles(Collections.singletonList(testFiles.get(0)));

        ValidationResult valResult = new ValidationResult();
        valResult.setValid(true);
        when(validationService.validate(any(ValidationRequest.class), eq("testuser")))
                .thenReturn(valResult);

        BatchOperationResult result = batchOperationService.processBatch(validationRequest, "testuser");

        assertNotNull(result);
        assertEquals(1, result.getTotalFiles());

        Thread.sleep(1000);

        BatchOperationResult completedResult = batchOperationService.getBatchResult(result.getOperationId());
        assertNotNull(completedResult);
        assertEquals(1, completedResult.getSuccessCount());
        assertEquals(0, completedResult.getFailureCount());
    }

    @Test
    void testProcessBatch_durationTracking() throws InterruptedException {
        ValidationResult valResult = new ValidationResult();
        valResult.setValid(true);
        when(validationService.validate(any(ValidationRequest.class), eq("testuser")))
                .thenReturn(valResult);

        BatchOperationResult result = batchOperationService.processBatch(validationRequest, "testuser");

        Thread.sleep(1000);

        BatchOperationResult completedResult = batchOperationService.getBatchResult(result.getOperationId());
        assertNotNull(completedResult);
        assertTrue(completedResult.getTotalDurationMs() >= 0); // Duration may be 0 for fast operations

        // Each file should have duration tracked
        completedResult.getResults().forEach(fileResult -> {
            assertTrue(fileResult.getDurationMs() >= 0);
        });
    }
}

package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.OperationHistory;
import gov.nist.oscal.tools.api.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BatchOperationService {

    private final ValidationService validationService;
    private final ConversionService conversionService;
    private final HistoryService historyService;

    // In-memory storage for batch results (in production, use a database or cache)
    private final ConcurrentHashMap<String, BatchOperationResult> batchResults = new ConcurrentHashMap<>();

    @Autowired
    public BatchOperationService(ValidationService validationService, ConversionService conversionService, HistoryService historyService) {
        this.validationService = validationService;
        this.conversionService = conversionService;
        this.historyService = historyService;
    }

    /**
     * Process a batch operation asynchronously
     */
    public BatchOperationResult processBatch(BatchOperationRequest request, String username) {
        // Generate unique operation ID
        String operationId = UUID.randomUUID().toString();

        // Create initial result
        BatchOperationResult initialResult = new BatchOperationResult(operationId, request.getFiles().size());
        batchResults.put(operationId, initialResult);

        // Process files asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                List<BatchOperationResult.FileResult> fileResults = processFilesSequentially(request, operationId, username);

                // Calculate totals
                int successCount = (int) fileResults.stream().filter(BatchOperationResult.FileResult::isSuccess).count();
                int failureCount = fileResults.size() - successCount;
                long totalDuration = fileResults.stream().mapToLong(BatchOperationResult.FileResult::getDurationMs).sum();

                // Update result
                BatchOperationResult completedResult = new BatchOperationResult(
                        true,
                        operationId,
                        request.getFiles().size(),
                        successCount,
                        failureCount,
                        fileResults,
                        totalDuration
                );

                batchResults.put(operationId, completedResult);

                // Save batch summary to history
                saveBatchSummaryToHistory(request, completedResult);

            } catch (Exception e) {
                // Handle error
                BatchOperationResult errorResult = new BatchOperationResult(
                        false,
                        operationId,
                        request.getFiles().size(),
                        0,
                        request.getFiles().size(),
                        new ArrayList<>(),
                        0
                );
                batchResults.put(operationId, errorResult);
            }
        });

        return initialResult;
    }

    /**
     * Process files sequentially to avoid overloading the system
     */
    private List<BatchOperationResult.FileResult> processFilesSequentially(BatchOperationRequest request, String batchOperationId, String username) {
        List<BatchOperationResult.FileResult> results = new ArrayList<>();

        for (BatchOperationRequest.FileContent file : request.getFiles()) {
            long startTime = System.currentTimeMillis();

            try {
                Object result;
                boolean success;
                String error = null;

                if (request.getOperationType() == BatchOperationRequest.BatchOperationType.VALIDATE) {
                    // Validation operation
                    ValidationRequest valRequest = new ValidationRequest();
                    valRequest.setContent(file.getContent());
                    valRequest.setModelType(request.getModelType());
                    valRequest.setFormat(file.getFormat());

                    ValidationResult valResult = validationService.validate(valRequest, username);
                    result = valResult;
                    success = valResult.isValid();
                    if (!success && !valResult.getErrors().isEmpty()) {
                        error = valResult.getErrors().get(0).getMessage();
                    }

                } else if (request.getOperationType() == BatchOperationRequest.BatchOperationType.CONVERT) {
                    // Conversion operation
                    ConversionRequest convRequest = new ConversionRequest();
                    convRequest.setContent(file.getContent());
                    convRequest.setModelType(request.getModelType());
                    convRequest.setFromFormat(request.getFromFormat());
                    convRequest.setToFormat(request.getToFormat());

                    ConversionResult convResult = conversionService.convert(convRequest, username);
                    result = convResult;
                    success = convResult.isSuccess();
                    error = convResult.getError();

                } else {
                    throw new IllegalArgumentException("Unsupported operation type: " + request.getOperationType());
                }

                long duration = System.currentTimeMillis() - startTime;
                results.add(new BatchOperationResult.FileResult(file.getFilename(), success, error, result, duration));

                // Save individual file to history
                saveFileToHistory(request, file, success, error, duration, batchOperationId);

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                results.add(new BatchOperationResult.FileResult(
                        file.getFilename(),
                        false,
                        "Processing error: " + e.getMessage(),
                        null,
                        duration
                ));

                // Save failed file to history
                saveFileToHistory(request, file, false, e.getMessage(), duration, batchOperationId);
            }
        }

        return results;
    }

    private void saveFileToHistory(BatchOperationRequest request, BatchOperationRequest.FileContent file,
                                   boolean success, String error, long durationMs, String batchOperationId) {
        try {
            OperationHistory history = new OperationHistory();
            history.setOperationType("BATCH_" + request.getOperationType().name());
            history.setFileName(file.getFilename());
            history.setSuccess(success);
            history.setModelType(request.getModelType().getValue());
            history.setFormat(file.getFormat().toString());
            history.setDurationMs(durationMs);
            history.setBatchOperationId(batchOperationId);

            String details = success ? "Processing successful" : "Processing failed: " + error;
            history.setDetails(details);

            historyService.saveOperation(history);
        } catch (Exception e) {
            // Don't fail batch operation if history save fails
        }
    }

    private void saveBatchSummaryToHistory(BatchOperationRequest request, BatchOperationResult result) {
        try {
            OperationHistory history = new OperationHistory();
            history.setOperationType("BATCH_" + request.getOperationType().name());
            history.setFileName(String.format("Batch (%d files)", result.getTotalFiles()));
            history.setSuccess(result.getFailureCount() == 0);
            history.setModelType(request.getModelType().getValue());
            history.setDurationMs(result.getTotalDurationMs());
            history.setFileCount(result.getTotalFiles());
            history.setBatchOperationId(result.getOperationId());

            String details = String.format("Batch completed: %d success, %d failed",
                    result.getSuccessCount(), result.getFailureCount());
            history.setDetails(details);

            historyService.saveOperation(history);
        } catch (Exception e) {
            // Don't fail batch operation if history save fails
        }
    }

    /**
     * Get batch operation result by ID
     */
    public BatchOperationResult getBatchResult(String operationId) {
        return batchResults.get(operationId);
    }

    /**
     * Get all batch results (for history feature)
     */
    public List<BatchOperationResult> getAllBatchResults() {
        return new ArrayList<>(batchResults.values());
    }

    /**
     * Clear batch results (cleanup old operations)
     */
    public void clearOldResults() {
        // In production, implement TTL-based cleanup
        batchResults.clear();
    }
}

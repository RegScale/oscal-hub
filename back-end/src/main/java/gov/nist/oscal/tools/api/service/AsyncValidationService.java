package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Async validation service for handling long-running OSCAL operations.
 *
 * This service provides non-blocking validation that runs in a separate thread pool,
 * allowing the main request thread to return immediately with an operation ID.
 * Clients can poll for results or use WebSockets for real-time updates.
 *
 * Benefits:
 * - Prevents request timeouts for large documents
 * - Improves API responsiveness
 * - Allows batch processing of multiple documents
 * - Enables progress tracking for long operations
 */
@Service
public class AsyncValidationService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncValidationService.class);

    private final ValidationService validationService;
    private final ConversionService conversionService;
    private final WebSocketNotificationService notificationService;

    /**
     * In-memory storage for async operation results.
     * In production, consider using Redis or a database for persistence.
     */
    private final Map<String, AsyncOperationResult<?>> operationResults = new ConcurrentHashMap<>();

    /**
     * TTL for operation results (30 minutes)
     */
    private static final long RESULT_TTL_MS = TimeUnit.MINUTES.toMillis(30);

    @Autowired
    public AsyncValidationService(ValidationService validationService,
                                   ConversionService conversionService,
                                   WebSocketNotificationService notificationService) {
        this.validationService = validationService;
        this.conversionService = conversionService;
        this.notificationService = notificationService;
    }

    /**
     * Start an async validation operation.
     * Returns immediately with an operation ID.
     *
     * @param request The validation request
     * @param username The user performing the operation
     * @return Operation ID for tracking
     */
    public String startAsyncValidation(ValidationRequest request, String username) {
        String operationId = generateOperationId();

        // Initialize the result as PENDING
        operationResults.put(operationId, new AsyncOperationResult<>(
            operationId,
            AsyncOperationStatus.PENDING,
            null,
            null,
            System.currentTimeMillis()
        ));

        // Start the async operation
        executeValidationAsync(operationId, request, username);

        return operationId;
    }

    /**
     * Execute validation asynchronously using the OSCAL task executor.
     */
    @Async("oscalTaskExecutor")
    protected void executeValidationAsync(String operationId, ValidationRequest request, String username) {
        try {
            // Update status to IN_PROGRESS
            updateOperationStatus(operationId, AsyncOperationStatus.IN_PROGRESS, null, null);
            notificationService.sendAsyncStatus(operationId, "IN_PROGRESS", null, null);

            logger.info("Starting async validation for operation {}", operationId);
            long startTime = System.currentTimeMillis();

            // Perform the actual validation
            ValidationResult result = validationService.validate(request, username);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Async validation {} completed in {}ms, valid: {}",
                operationId, duration, result.isValid());

            // Update with the result
            updateOperationStatus(operationId, AsyncOperationStatus.COMPLETED, result, null);

            // Send WebSocket notification
            notificationService.sendValidationResult(operationId, result.isValid(),
                result.getErrors() != null ? result.getErrors().size() : 0);

        } catch (Exception e) {
            logger.error("Async validation {} failed: {}", operationId, e.getMessage());
            updateOperationStatus(operationId, AsyncOperationStatus.FAILED, null, e.getMessage());
            notificationService.sendAsyncStatus(operationId, "FAILED", null, e.getMessage());
        }
    }

    /**
     * Start an async conversion operation.
     * Returns immediately with an operation ID.
     *
     * @param request The conversion request
     * @param username The user performing the operation
     * @return Operation ID for tracking
     */
    public String startAsyncConversion(ConversionRequest request, String username) {
        String operationId = generateOperationId();

        // Initialize the result as PENDING
        operationResults.put(operationId, new AsyncOperationResult<>(
            operationId,
            AsyncOperationStatus.PENDING,
            null,
            null,
            System.currentTimeMillis()
        ));

        // Start the async operation
        executeConversionAsync(operationId, request, username);

        return operationId;
    }

    /**
     * Execute conversion asynchronously using the OSCAL task executor.
     */
    @Async("oscalTaskExecutor")
    protected void executeConversionAsync(String operationId, ConversionRequest request, String username) {
        try {
            // Update status to IN_PROGRESS
            updateOperationStatus(operationId, AsyncOperationStatus.IN_PROGRESS, null, null);
            notificationService.sendAsyncStatus(operationId, "IN_PROGRESS", null, null);

            logger.info("Starting async conversion for operation {}", operationId);
            long startTime = System.currentTimeMillis();

            // Perform the actual conversion
            ConversionResult result = conversionService.convert(request, username);

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Async conversion {} completed in {}ms, success: {}",
                operationId, duration, result.isSuccess());

            // Update with the result
            updateOperationStatus(operationId, AsyncOperationStatus.COMPLETED, result, null);

            // Send WebSocket notification
            notificationService.sendAsyncStatus(operationId, "COMPLETED", result, null);

        } catch (Exception e) {
            logger.error("Async conversion {} failed: {}", operationId, e.getMessage());
            updateOperationStatus(operationId, AsyncOperationStatus.FAILED, null, e.getMessage());
            notificationService.sendAsyncStatus(operationId, "FAILED", null, e.getMessage());
        }
    }

    /**
     * Get the result of an async operation.
     *
     * @param operationId The operation ID
     * @return The operation result, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> AsyncOperationResult<T> getOperationResult(String operationId) {
        AsyncOperationResult<?> result = operationResults.get(operationId);
        if (result == null) {
            return null;
        }

        // Check if result has expired
        if (System.currentTimeMillis() - result.getCreatedAt() > RESULT_TTL_MS) {
            operationResults.remove(operationId);
            return null;
        }

        return (AsyncOperationResult<T>) result;
    }

    /**
     * Clean up expired operation results.
     * This should be called periodically (e.g., by a scheduled task).
     */
    public void cleanupExpiredResults() {
        long now = System.currentTimeMillis();
        operationResults.entrySet().removeIf(entry ->
            now - entry.getValue().getCreatedAt() > RESULT_TTL_MS
        );
    }

    /**
     * Get the count of pending operations.
     */
    public long getPendingOperationCount() {
        return operationResults.values().stream()
            .filter(r -> r.getStatus() == AsyncOperationStatus.PENDING ||
                         r.getStatus() == AsyncOperationStatus.IN_PROGRESS)
            .count();
    }

    private String generateOperationId() {
        return "async-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @SuppressWarnings("unchecked")
    private <T> void updateOperationStatus(String operationId, AsyncOperationStatus status,
                                            T result, String error) {
        AsyncOperationResult<?> existing = operationResults.get(operationId);
        if (existing != null) {
            operationResults.put(operationId, new AsyncOperationResult<>(
                operationId,
                status,
                result,
                error,
                existing.getCreatedAt()
            ));
        }
    }

    /**
     * Status enum for async operations.
     */
    public enum AsyncOperationStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    /**
     * Result wrapper for async operations.
     */
    public static class AsyncOperationResult<T> {
        private final String operationId;
        private final AsyncOperationStatus status;
        private final T result;
        private final String error;
        private final long createdAt;

        public AsyncOperationResult(String operationId, AsyncOperationStatus status,
                                     T result, String error, long createdAt) {
            this.operationId = operationId;
            this.status = status;
            this.result = result;
            this.error = error;
            this.createdAt = createdAt;
        }

        public String getOperationId() { return operationId; }
        public AsyncOperationStatus getStatus() { return status; }
        public T getResult() { return result; }
        public String getError() { return error; }
        public long getCreatedAt() { return createdAt; }

        public boolean isComplete() {
            return status == AsyncOperationStatus.COMPLETED || status == AsyncOperationStatus.FAILED;
        }
    }
}

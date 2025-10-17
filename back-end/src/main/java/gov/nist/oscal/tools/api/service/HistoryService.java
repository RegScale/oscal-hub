package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.OperationHistory;
import gov.nist.oscal.tools.api.repository.HistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class HistoryService {

    private final HistoryRepository historyRepository;

    @Autowired
    public HistoryService(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    /**
     * Save a new operation to history
     */
    public OperationHistory saveOperation(OperationHistory operation) {
        if (operation.getTimestamp() == null) {
            operation.setTimestamp(LocalDateTime.now());
        }
        return historyRepository.save(operation);
    }

    /**
     * Get all operations (paginated)
     */
    public Page<OperationHistory> getAllOperations(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return historyRepository.findAllByOrderByTimestampDesc(pageable);
    }

    /**
     * Get operations by type (paginated)
     */
    public Page<OperationHistory> getOperationsByType(String operationType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return historyRepository.findByOperationTypeOrderByTimestampDesc(operationType, pageable);
    }

    /**
     * Get operations by success status (paginated)
     */
    public Page<OperationHistory> getOperationsByStatus(Boolean success, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return historyRepository.findBySuccessOrderByTimestampDesc(success, pageable);
    }

    /**
     * Search operations by filename
     */
    public Page<OperationHistory> searchByFileName(String filename, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return historyRepository.searchByFileName(filename, pageable);
    }

    /**
     * Get operations within date range
     */
    public Page<OperationHistory> getOperationsByDateRange(
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size);
        return historyRepository.findByTimestampBetweenOrderByTimestampDesc(startDate, endDate, pageable);
    }

    /**
     * Get recent operations (last 10)
     */
    public List<OperationHistory> getRecentOperations() {
        return historyRepository.findTop10ByOrderByTimestampDesc();
    }

    /**
     * Get a specific operation by ID
     */
    public Optional<OperationHistory> getOperationById(Long id) {
        return historyRepository.findById(id);
    }

    /**
     * Get all operations in a batch
     */
    public List<OperationHistory> getBatchOperations(String batchOperationId) {
        return historyRepository.findByBatchOperationIdOrderByTimestampAsc(batchOperationId);
    }

    /**
     * Delete an operation
     */
    @Transactional
    public void deleteOperation(Long id) {
        historyRepository.deleteById(id);
    }

    /**
     * Get operation statistics
     */
    public OperationStats getStatistics() {
        long totalOperations = historyRepository.count();
        long successfulOperations = historyRepository.countBySuccess(true);
        long failedOperations = historyRepository.countBySuccess(false);

        long validateCount = historyRepository.countByOperationType("VALIDATE");
        long convertCount = historyRepository.countByOperationType("CONVERT");
        long resolveCount = historyRepository.countByOperationType("RESOLVE");
        long batchCount = historyRepository.countByOperationType("BATCH_VALIDATE") +
                         historyRepository.countByOperationType("BATCH_CONVERT");

        return new OperationStats(
            totalOperations,
            successfulOperations,
            failedOperations,
            validateCount,
            convertCount,
            resolveCount,
            batchCount
        );
    }

    /**
     * Clean up old operations (runs daily at 2 AM)
     * Deletes operations older than 30 days
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldOperations() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        historyRepository.deleteByTimestampBefore(cutoffDate);
    }

    /**
     * Statistics DTO
     */
    public static class OperationStats {
        private final long totalOperations;
        private final long successfulOperations;
        private final long failedOperations;
        private final long validateCount;
        private final long convertCount;
        private final long resolveCount;
        private final long batchCount;

        public OperationStats(long totalOperations, long successfulOperations, long failedOperations,
                            long validateCount, long convertCount, long resolveCount, long batchCount) {
            this.totalOperations = totalOperations;
            this.successfulOperations = successfulOperations;
            this.failedOperations = failedOperations;
            this.validateCount = validateCount;
            this.convertCount = convertCount;
            this.resolveCount = resolveCount;
            this.batchCount = batchCount;
        }

        // Getters
        public long getTotalOperations() { return totalOperations; }
        public long getSuccessfulOperations() { return successfulOperations; }
        public long getFailedOperations() { return failedOperations; }
        public long getValidateCount() { return validateCount; }
        public long getConvertCount() { return convertCount; }
        public long getResolveCount() { return resolveCount; }
        public long getBatchCount() { return batchCount; }

        public double getSuccessRate() {
            return totalOperations > 0 ? (double) successfulOperations / totalOperations * 100 : 0;
        }
    }
}

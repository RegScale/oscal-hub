package gov.nist.oscal.tools.api.repository;

import gov.nist.oscal.tools.api.entity.OperationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoryRepository extends JpaRepository<OperationHistory, Long> {

    // Find recent operations (paginated)
    Page<OperationHistory> findAllByOrderByTimestampDesc(Pageable pageable);

    // Find by operation type
    Page<OperationHistory> findByOperationTypeOrderByTimestampDesc(String operationType, Pageable pageable);

    // Find by success status
    Page<OperationHistory> findBySuccessOrderByTimestampDesc(Boolean success, Pageable pageable);

    // Find by batch operation ID (to get all files in a batch)
    List<OperationHistory> findByBatchOperationIdOrderByTimestampAsc(String batchOperationId);

    // Search by filename (partial match)
    @Query("SELECT h FROM OperationHistory h WHERE LOWER(h.fileName) LIKE LOWER(CONCAT('%', :filename, '%')) ORDER BY h.timestamp DESC")
    Page<OperationHistory> searchByFileName(@Param("filename") String filename, Pageable pageable);

    // Find operations within a date range
    Page<OperationHistory> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    // Get recent operations (last N entries)
    List<OperationHistory> findTop10ByOrderByTimestampDesc();

    // Count operations by type
    Long countByOperationType(String operationType);

    // Count successful operations
    Long countBySuccess(Boolean success);

    // Delete old operations (older than X days)
    void deleteByTimestampBefore(LocalDateTime cutoffDate);

    // Find operations by user
    Page<OperationHistory> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    // Find recent operations for a user (last N entries)
    List<OperationHistory> findTop10ByUserIdOrderByTimestampDesc(Long userId);

    // Find operations by user and type
    Page<OperationHistory> findByUserIdAndOperationTypeOrderByTimestampDesc(Long userId, String operationType, Pageable pageable);
}

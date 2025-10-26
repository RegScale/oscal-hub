package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.OperationHistory;
import gov.nist.oscal.tools.api.repository.HistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for HistoryService.
 * Tests all CRUD operations, pagination, statistics, and scheduled cleanup.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HistoryServiceTest {

    @Mock
    private HistoryRepository historyRepository;

    @InjectMocks
    private HistoryService historyService;

    private OperationHistory validationOp;
    private OperationHistory conversionOp;
    private OperationHistory batchOp;

    @BeforeEach
    void setUp() {
        // Create test operation histories
        validationOp = new OperationHistory();
        validationOp.setId(1L);
        validationOp.setOperationType("VALIDATE");
        validationOp.setFileName("catalog.json");
        validationOp.setSuccess(true);
        validationOp.setTimestamp(LocalDateTime.now().minusHours(1));
        validationOp.setDurationMs(150L);

        conversionOp = new OperationHistory();
        conversionOp.setId(2L);
        conversionOp.setOperationType("CONVERT");
        conversionOp.setFileName("profile.xml");
        conversionOp.setSuccess(false);
        conversionOp.setTimestamp(LocalDateTime.now().minusHours(2));
        conversionOp.setDetails("Conversion failed: Invalid format");
        conversionOp.setDurationMs(500L);

        batchOp = new OperationHistory();
        batchOp.setId(3L);
        batchOp.setOperationType("BATCH_VALIDATE");
        batchOp.setFileName("batch-operation");
        batchOp.setSuccess(true);
        batchOp.setTimestamp(LocalDateTime.now().minusDays(1));
        batchOp.setBatchOperationId("batch-123");
        batchOp.setDurationMs(3000L);
    }

    // ==================== Save Operation Tests ====================

    @Test
    void testSaveOperation_withTimestamp_success() {
        when(historyRepository.save(any(OperationHistory.class))).thenReturn(validationOp);

        OperationHistory result = historyService.saveOperation(validationOp);

        assertNotNull(result);
        assertEquals(validationOp.getId(), result.getId());
        assertEquals(validationOp.getOperationType(), result.getOperationType());
        verify(historyRepository).save(validationOp);
    }

    @Test
    void testSaveOperation_withoutTimestamp_setsTimestamp() {
        OperationHistory opWithoutTimestamp = new OperationHistory();
        opWithoutTimestamp.setOperationType("VALIDATE");
        opWithoutTimestamp.setFileName("test.json");
        opWithoutTimestamp.setSuccess(true);

        when(historyRepository.save(any(OperationHistory.class))).thenReturn(opWithoutTimestamp);

        ArgumentCaptor<OperationHistory> captor = ArgumentCaptor.forClass(OperationHistory.class);

        historyService.saveOperation(opWithoutTimestamp);

        verify(historyRepository).save(captor.capture());
        OperationHistory saved = captor.getValue();
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void testSaveOperation_nullTimestamp_setsCurrentTime() {
        validationOp.setTimestamp(null);

        when(historyRepository.save(any(OperationHistory.class))).thenReturn(validationOp);

        LocalDateTime beforeSave = LocalDateTime.now().minusSeconds(1);
        historyService.saveOperation(validationOp);
        LocalDateTime afterSave = LocalDateTime.now().plusSeconds(1);

        assertNotNull(validationOp.getTimestamp());
        assertTrue(validationOp.getTimestamp().isAfter(beforeSave));
        assertTrue(validationOp.getTimestamp().isBefore(afterSave));
    }

    // ==================== Get All Operations Tests ====================

    @Test
    void testGetAllOperations_returnsPagedResults() {
        List<OperationHistory> operations = Arrays.asList(validationOp, conversionOp, batchOp);
        Page<OperationHistory> page = new PageImpl<>(operations, PageRequest.of(0, 10), operations.size());

        when(historyRepository.findAllByOrderByTimestampDesc(any(Pageable.class))).thenReturn(page);

        Page<OperationHistory> result = historyService.getAllOperations(0, 10);

        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(operations, result.getContent());
        verify(historyRepository).findAllByOrderByTimestampDesc(any(Pageable.class));
    }

    @Test
    void testGetAllOperations_emptyResults() {
        Page<OperationHistory> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);

        when(historyRepository.findAllByOrderByTimestampDesc(any(Pageable.class))).thenReturn(emptyPage);

        Page<OperationHistory> result = historyService.getAllOperations(0, 10);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void testGetAllOperations_differentPageSizes() {
        Page<OperationHistory> page = new PageImpl<>(Arrays.asList(validationOp), PageRequest.of(0, 5), 1);

        when(historyRepository.findAllByOrderByTimestampDesc(any(Pageable.class))).thenReturn(page);

        Page<OperationHistory> result = historyService.getAllOperations(0, 5);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(historyRepository).findAllByOrderByTimestampDesc(pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(5, pageableCaptor.getValue().getPageSize());
    }

    // ==================== Get Operations By Type Tests ====================

    @Test
    void testGetOperationsByType_returnsFilteredResults() {
        Page<OperationHistory> page = new PageImpl<>(Arrays.asList(validationOp), PageRequest.of(0, 10), 1);

        when(historyRepository.findByOperationTypeOrderByTimestampDesc(eq("VALIDATE"), any(Pageable.class)))
                .thenReturn(page);

        Page<OperationHistory> result = historyService.getOperationsByType("VALIDATE", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("VALIDATE", result.getContent().get(0).getOperationType());
        verify(historyRepository).findByOperationTypeOrderByTimestampDesc(eq("VALIDATE"), any(Pageable.class));
    }

    @Test
    void testGetOperationsByType_noMatchingType() {
        Page<OperationHistory> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);

        when(historyRepository.findByOperationTypeOrderByTimestampDesc(eq("NONEXISTENT"), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<OperationHistory> result = historyService.getOperationsByType("NONEXISTENT", 0, 10);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }

    // ==================== Get Operations By Status Tests ====================

    @Test
    void testGetOperationsByStatus_successfulOperations() {
        Page<OperationHistory> page = new PageImpl<>(Arrays.asList(validationOp, batchOp), PageRequest.of(0, 10), 2);

        when(historyRepository.findBySuccessOrderByTimestampDesc(eq(true), any(Pageable.class)))
                .thenReturn(page);

        Page<OperationHistory> result = historyService.getOperationsByStatus(true, 0, 10);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().allMatch(OperationHistory::getSuccess));
    }

    @Test
    void testGetOperationsByStatus_failedOperations() {
        Page<OperationHistory> page = new PageImpl<>(Arrays.asList(conversionOp), PageRequest.of(0, 10), 1);

        when(historyRepository.findBySuccessOrderByTimestampDesc(eq(false), any(Pageable.class)))
                .thenReturn(page);

        Page<OperationHistory> result = historyService.getOperationsByStatus(false, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertFalse(result.getContent().get(0).getSuccess());
    }

    // ==================== Search By FileName Tests ====================

    @Test
    void testSearchByFileName_returnsMatchingResults() {
        Page<OperationHistory> page = new PageImpl<>(Arrays.asList(validationOp), PageRequest.of(0, 10), 1);

        when(historyRepository.searchByFileName(eq("catalog"), any(Pageable.class)))
                .thenReturn(page);

        Page<OperationHistory> result = historyService.searchByFileName("catalog", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).getFileName().contains("catalog"));
    }

    @Test
    void testSearchByFileName_noMatches() {
        Page<OperationHistory> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);

        when(historyRepository.searchByFileName(eq("nonexistent"), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<OperationHistory> result = historyService.searchByFileName("nonexistent", 0, 10);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    // ==================== Get Operations By Date Range Tests ====================

    @Test
    void testGetOperationsByDateRange_returnsResultsInRange() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();

        Page<OperationHistory> page = new PageImpl<>(
                Arrays.asList(validationOp, conversionOp, batchOp),
                PageRequest.of(0, 10),
                3
        );

        when(historyRepository.findByTimestampBetweenOrderByTimestampDesc(
                eq(startDate), eq(endDate), any(Pageable.class)))
                .thenReturn(page);

        Page<OperationHistory> result = historyService.getOperationsByDateRange(startDate, endDate, 0, 10);

        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        verify(historyRepository).findByTimestampBetweenOrderByTimestampDesc(eq(startDate), eq(endDate), any(Pageable.class));
    }

    @Test
    void testGetOperationsByDateRange_noResultsInRange() {
        LocalDateTime startDate = LocalDateTime.now().minusYears(2);
        LocalDateTime endDate = LocalDateTime.now().minusYears(1);

        Page<OperationHistory> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);

        when(historyRepository.findByTimestampBetweenOrderByTimestampDesc(
                eq(startDate), eq(endDate), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<OperationHistory> result = historyService.getOperationsByDateRange(startDate, endDate, 0, 10);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    // ==================== Get Recent Operations Tests ====================

    @Test
    void testGetRecentOperations_returnsLast10() {
        List<OperationHistory> recentOps = Arrays.asList(validationOp, conversionOp, batchOp);

        when(historyRepository.findTop10ByOrderByTimestampDesc()).thenReturn(recentOps);

        List<OperationHistory> result = historyService.getRecentOperations();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(recentOps, result);
        verify(historyRepository).findTop10ByOrderByTimestampDesc();
    }

    @Test
    void testGetRecentOperations_emptyList() {
        when(historyRepository.findTop10ByOrderByTimestampDesc()).thenReturn(Arrays.asList());

        List<OperationHistory> result = historyService.getRecentOperations();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Get Operation By ID Tests ====================

    @Test
    void testGetOperationById_found() {
        when(historyRepository.findById(1L)).thenReturn(Optional.of(validationOp));

        Optional<OperationHistory> result = historyService.getOperationById(1L);

        assertTrue(result.isPresent());
        assertEquals(validationOp.getId(), result.get().getId());
        verify(historyRepository).findById(1L);
    }

    @Test
    void testGetOperationById_notFound() {
        when(historyRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<OperationHistory> result = historyService.getOperationById(999L);

        assertFalse(result.isPresent());
        verify(historyRepository).findById(999L);
    }

    // ==================== Get Batch Operations Tests ====================

    @Test
    void testGetBatchOperations_returnsBatchOperations() {
        OperationHistory batchOp1 = new OperationHistory();
        batchOp1.setId(10L);
        batchOp1.setBatchOperationId("batch-123");

        OperationHistory batchOp2 = new OperationHistory();
        batchOp2.setId(11L);
        batchOp2.setBatchOperationId("batch-123");

        List<OperationHistory> batchOps = Arrays.asList(batchOp1, batchOp2);

        when(historyRepository.findByBatchOperationIdOrderByTimestampAsc("batch-123"))
                .thenReturn(batchOps);

        List<OperationHistory> result = historyService.getBatchOperations("batch-123");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(op -> "batch-123".equals(op.getBatchOperationId())));
    }

    @Test
    void testGetBatchOperations_noBatchFound() {
        when(historyRepository.findByBatchOperationIdOrderByTimestampAsc("nonexistent"))
                .thenReturn(Arrays.asList());

        List<OperationHistory> result = historyService.getBatchOperations("nonexistent");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== Delete Operation Tests ====================

    @Test
    void testDeleteOperation_success() {
        doNothing().when(historyRepository).deleteById(1L);

        historyService.deleteOperation(1L);

        verify(historyRepository).deleteById(1L);
    }

    @Test
    void testDeleteOperation_nonExistentId() {
        doNothing().when(historyRepository).deleteById(999L);

        historyService.deleteOperation(999L);

        verify(historyRepository).deleteById(999L);
    }

    // ==================== Get Statistics Tests ====================

    @Test
    void testGetStatistics_returnsCorrectStats() {
        when(historyRepository.count()).thenReturn(100L);
        when(historyRepository.countBySuccess(true)).thenReturn(85L);
        when(historyRepository.countBySuccess(false)).thenReturn(15L);
        when(historyRepository.countByOperationType("VALIDATE")).thenReturn(40L);
        when(historyRepository.countByOperationType("CONVERT")).thenReturn(30L);
        when(historyRepository.countByOperationType("RESOLVE")).thenReturn(20L);
        when(historyRepository.countByOperationType("BATCH_VALIDATE")).thenReturn(5L);
        when(historyRepository.countByOperationType("BATCH_CONVERT")).thenReturn(5L);

        HistoryService.OperationStats stats = historyService.getStatistics();

        assertNotNull(stats);
        assertEquals(100L, stats.getTotalOperations());
        assertEquals(85L, stats.getSuccessfulOperations());
        assertEquals(15L, stats.getFailedOperations());
        assertEquals(40L, stats.getValidateCount());
        assertEquals(30L, stats.getConvertCount());
        assertEquals(20L, stats.getResolveCount());
        assertEquals(10L, stats.getBatchCount()); // 5 + 5
        assertEquals(85.0, stats.getSuccessRate(), 0.01);
    }

    @Test
    void testGetStatistics_noOperations() {
        when(historyRepository.count()).thenReturn(0L);
        when(historyRepository.countBySuccess(true)).thenReturn(0L);
        when(historyRepository.countBySuccess(false)).thenReturn(0L);
        when(historyRepository.countByOperationType(anyString())).thenReturn(0L);

        HistoryService.OperationStats stats = historyService.getStatistics();

        assertNotNull(stats);
        assertEquals(0L, stats.getTotalOperations());
        assertEquals(0.0, stats.getSuccessRate(), 0.01);
    }

    @Test
    void testGetStatistics_allOperationsFailed() {
        when(historyRepository.count()).thenReturn(50L);
        when(historyRepository.countBySuccess(true)).thenReturn(0L);
        when(historyRepository.countBySuccess(false)).thenReturn(50L);
        when(historyRepository.countByOperationType(anyString())).thenReturn(0L);

        HistoryService.OperationStats stats = historyService.getStatistics();

        assertNotNull(stats);
        assertEquals(50L, stats.getTotalOperations());
        assertEquals(0L, stats.getSuccessfulOperations());
        assertEquals(50L, stats.getFailedOperations());
        assertEquals(0.0, stats.getSuccessRate(), 0.01);
    }

    // ==================== Cleanup Old Operations Tests ====================

    @Test
    void testCleanupOldOperations_deletesOldRecords() {
        doNothing().when(historyRepository).deleteByTimestampBefore(any(LocalDateTime.class));

        historyService.cleanupOldOperations();

        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(historyRepository).deleteByTimestampBefore(dateCaptor.capture());

        LocalDateTime cutoffDate = dateCaptor.getValue();
        LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(30);

        // Allow 1-second tolerance for test execution time
        assertTrue(cutoffDate.isAfter(expectedCutoff.minusSeconds(1)));
        assertTrue(cutoffDate.isBefore(expectedCutoff.plusSeconds(1)));
    }

    // ==================== OperationStats Tests ====================

    @Test
    void testOperationStats_gettersWork() {
        HistoryService.OperationStats stats = new HistoryService.OperationStats(
                100, 80, 20, 40, 30, 20, 10
        );

        assertEquals(100, stats.getTotalOperations());
        assertEquals(80, stats.getSuccessfulOperations());
        assertEquals(20, stats.getFailedOperations());
        assertEquals(40, stats.getValidateCount());
        assertEquals(30, stats.getConvertCount());
        assertEquals(20, stats.getResolveCount());
        assertEquals(10, stats.getBatchCount());
    }

    @Test
    void testOperationStats_successRateCalculation() {
        HistoryService.OperationStats stats = new HistoryService.OperationStats(
                200, 150, 50, 0, 0, 0, 0
        );

        assertEquals(75.0, stats.getSuccessRate(), 0.01);
    }

    @Test
    void testOperationStats_successRateWithZeroOperations() {
        HistoryService.OperationStats stats = new HistoryService.OperationStats(
                0, 0, 0, 0, 0, 0, 0
        );

        assertEquals(0.0, stats.getSuccessRate(), 0.01);
    }

    @Test
    void testOperationStats_successRatePerfectScore() {
        HistoryService.OperationStats stats = new HistoryService.OperationStats(
                100, 100, 0, 0, 0, 0, 0
        );

        assertEquals(100.0, stats.getSuccessRate(), 0.01);
    }
}

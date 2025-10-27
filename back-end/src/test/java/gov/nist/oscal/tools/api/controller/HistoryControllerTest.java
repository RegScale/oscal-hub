package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.OperationHistory;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.service.HistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HistoryController.class)
class HistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HistoryService historyService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    @Test
    @WithMockUser(username = "testuser")
    void testGetAllOperations_success_returnsPagedResults() throws Exception {
        // Arrange
        OperationHistory op1 = new OperationHistory();
        op1.setId(1L);
        op1.setOperationType("VALIDATE");
        op1.setFileName("test.xml");
        op1.setSuccess(true);

        OperationHistory op2 = new OperationHistory();
        op2.setId(2L);
        op2.setOperationType("CONVERT");
        op2.setFileName("test2.xml");
        op2.setSuccess(true);

        Page<OperationHistory> page = new PageImpl<>(Arrays.asList(op1, op2));

        when(historyService.getAllOperations(0, 20)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/history")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].operationType").value("VALIDATE"))
                .andExpect(jsonPath("$.content[1].operationType").value("CONVERT"));

        verify(historyService, times(1)).getAllOperations(0, 20);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetAllOperations_defaultPagination_usesDefaults() throws Exception {
        // Arrange
        Page<OperationHistory> emptyPage = new PageImpl<>(Arrays.asList());

        when(historyService.getAllOperations(0, 20)).thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get("/api/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(historyService, times(1)).getAllOperations(0, 20);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRecentOperations_success_returnsOperations() throws Exception {
        // Arrange
        OperationHistory op1 = new OperationHistory();
        op1.setId(1L);
        op1.setOperationType("VALIDATE");
        op1.setFileName("recent.xml");
        op1.setSuccess(true);

        List<OperationHistory> operations = Arrays.asList(op1);

        when(historyService.getRecentOperations()).thenReturn(operations);

        // Act & Assert
        mockMvc.perform(get("/api/history/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fileName").value("recent.xml"));

        verify(historyService, times(1)).getRecentOperations();
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetOperationById_found_returnsOperation() throws Exception {
        // Arrange
        OperationHistory operation = new OperationHistory();
        operation.setId(1L);
        operation.setOperationType("VALIDATE");
        operation.setFileName("test.xml");
        operation.setSuccess(true);

        when(historyService.getOperationById(1L)).thenReturn(Optional.of(operation));

        // Act & Assert
        mockMvc.perform(get("/api/history/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileName").value("test.xml"));

        verify(historyService, times(1)).getOperationById(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetOperationById_notFound_returns404() throws Exception {
        // Arrange
        when(historyService.getOperationById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/history/999"))
                .andExpect(status().isNotFound());

        verify(historyService, times(1)).getOperationById(999L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetOperationsByType_success_returnsFilteredOperations() throws Exception {
        // Arrange
        OperationHistory op1 = new OperationHistory();
        op1.setId(1L);
        op1.setOperationType("VALIDATE");
        op1.setFileName("test1.xml");

        Page<OperationHistory> page = new PageImpl<>(Arrays.asList(op1));

        when(historyService.getOperationsByType("VALIDATE", 0, 20)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/history/type/VALIDATE")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].operationType").value("VALIDATE"));

        verify(historyService, times(1)).getOperationsByType("VALIDATE", 0, 20);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetOperationsByStatus_success_returnsFilteredOperations() throws Exception {
        // Arrange
        OperationHistory op1 = new OperationHistory();
        op1.setId(1L);
        op1.setSuccess(true);

        Page<OperationHistory> page = new PageImpl<>(Arrays.asList(op1));

        when(historyService.getOperationsByStatus(true, 0, 20)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/history/status/true")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].success").value(true));

        verify(historyService, times(1)).getOperationsByStatus(true, 0, 20);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSearchByFileName_success_returnsMatchingOperations() throws Exception {
        // Arrange
        OperationHistory op1 = new OperationHistory();
        op1.setId(1L);
        op1.setFileName("test-file.xml");

        Page<OperationHistory> page = new PageImpl<>(Arrays.asList(op1));

        when(historyService.searchByFileName("test", 0, 20)).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/history/search")
                .param("filename", "test")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fileName").value("test-file.xml"));

        verify(historyService, times(1)).searchByFileName("test", 0, 20);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetOperationsByDateRange_success_returnsOperations() throws Exception {
        // Arrange
        OperationHistory op1 = new OperationHistory();
        op1.setId(1L);
        op1.setTimestamp(LocalDateTime.of(2024, 1, 15, 10, 0));

        Page<OperationHistory> page = new PageImpl<>(Arrays.asList(op1));

        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 31, 23, 59);

        when(historyService.getOperationsByDateRange(any(LocalDateTime.class), any(LocalDateTime.class), eq(0), eq(20)))
                .thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/history/daterange")
                .param("startDate", "2024-01-01T00:00:00")
                .param("endDate", "2024-01-31T23:59:00")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(historyService, times(1)).getOperationsByDateRange(any(LocalDateTime.class), any(LocalDateTime.class), eq(0), eq(20));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetBatchOperations_success_returnsOperations() throws Exception {
        // Arrange
        OperationHistory op1 = new OperationHistory();
        op1.setId(1L);
        op1.setBatchOperationId("batch-123");

        OperationHistory op2 = new OperationHistory();
        op2.setId(2L);
        op2.setBatchOperationId("batch-123");

        List<OperationHistory> operations = Arrays.asList(op1, op2);

        when(historyService.getBatchOperations("batch-123")).thenReturn(operations);

        // Act & Assert
        mockMvc.perform(get("/api/history/batch/batch-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].batchOperationId").value("batch-123"))
                .andExpect(jsonPath("$[1].batchOperationId").value("batch-123"));

        verify(historyService, times(1)).getBatchOperations("batch-123");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetStatistics_success_returnsStats() throws Exception {
        // Arrange
        HistoryService.OperationStats stats = new HistoryService.OperationStats(
                100L,  // totalOperations
                85L,   // successfulOperations
                15L,   // failedOperations
                50L,   // validateCount
                30L,   // convertCount
                15L,   // resolveCount
                5L     // batchCount
        );

        when(historyService.getStatistics()).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/history/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOperations").value(100))
                .andExpect(jsonPath("$.successfulOperations").value(85))
                .andExpect(jsonPath("$.failedOperations").value(15));

        verify(historyService, times(1)).getStatistics();
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteOperation_found_deletesSuccessfully() throws Exception {
        // Arrange
        OperationHistory operation = new OperationHistory();
        operation.setId(1L);

        when(historyService.getOperationById(1L)).thenReturn(Optional.of(operation));
        doNothing().when(historyService).deleteOperation(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/history/1")
                .with(csrf()))
                .andExpect(status().isOk());

        verify(historyService, times(1)).getOperationById(1L);
        verify(historyService, times(1)).deleteOperation(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteOperation_notFound_returns404() throws Exception {
        // Arrange
        when(historyService.getOperationById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(delete("/api/history/999")
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(historyService, times(1)).getOperationById(999L);
        verify(historyService, never()).deleteOperation(anyLong());
    }

    @Test
    void testGetAllOperations_unauthenticated_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/history"))
                .andExpect(status().isUnauthorized());

        verify(historyService, never()).getAllOperations(anyInt(), anyInt());
    }

    @Test
    void testDeleteOperation_unauthenticated_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/history/1")
                .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(historyService, never()).deleteOperation(anyLong());
    }
}

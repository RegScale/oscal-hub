package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConditionOfApproval;
import gov.nist.oscal.tools.api.model.ConditionOfApprovalRequest;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.ConditionOfApprovalService;
import gov.nist.oscal.tools.api.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConditionOfApprovalController.class)
class ConditionOfApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConditionOfApprovalService conditionService;

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

    private Authorization createMockAuthorization(Long id) {
        Authorization auth = new Authorization();
        auth.setId(id);
        return auth;
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateCondition_success_returnsCreatedCondition() throws Exception {
        // Arrange
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        request.setAuthorizationId(1L);
        request.setCondition("Test condition");
        request.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        request.setDueDate("2024-12-31");

        Authorization auth = createMockAuthorization(1L);
        ConditionOfApproval condition = new ConditionOfApproval();
        condition.setId(1L);
        condition.setAuthorization(auth);
        condition.setCondition("Test condition");
        condition.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        condition.setDueDate(LocalDate.of(2024, 12, 31));

        when(conditionService.createCondition(eq(1L), eq("Test condition"),
                eq(ConditionOfApproval.ConditionType.MANDATORY), any(LocalDate.class)))
                .thenReturn(condition);

        // Act & Assert
        mockMvc.perform(post("/api/conditions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.condition").value("Test condition"));

        verify(conditionService, times(1)).createCondition(eq(1L), eq("Test condition"),
                eq(ConditionOfApproval.ConditionType.MANDATORY), any(LocalDate.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateCondition_noDueDate_success() throws Exception {
        // Arrange
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        request.setAuthorizationId(1L);
        request.setCondition("Test condition");
        request.setConditionType(ConditionOfApproval.ConditionType.RECOMMENDED);
        request.setDueDate(null);

        Authorization auth = createMockAuthorization(1L);
        ConditionOfApproval condition = new ConditionOfApproval();
        condition.setId(1L);
        condition.setAuthorization(auth);
        condition.setCondition("Test condition");
        condition.setConditionType(ConditionOfApproval.ConditionType.RECOMMENDED);

        when(conditionService.createCondition(eq(1L), eq("Test condition"),
                eq(ConditionOfApproval.ConditionType.RECOMMENDED), isNull()))
                .thenReturn(condition);

        // Act & Assert
        mockMvc.perform(post("/api/conditions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(conditionService, times(1)).createCondition(eq(1L), eq("Test condition"),
                eq(ConditionOfApproval.ConditionType.RECOMMENDED), isNull());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateCondition_serviceException_returns400() throws Exception {
        // Arrange
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        request.setAuthorizationId(1L);
        request.setCondition("Test condition");
        request.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        request.setDueDate("2024-12-31");

        when(conditionService.createCondition(anyLong(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(post("/api/conditions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateCondition_success_returnsUpdatedCondition() throws Exception {
        // Arrange
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        request.setCondition("Updated condition");
        request.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        request.setDueDate("2025-01-31");

        Authorization auth = createMockAuthorization(1L);
        ConditionOfApproval condition = new ConditionOfApproval();
        condition.setId(1L);
        condition.setAuthorization(auth);
        condition.setCondition("Updated condition");
        condition.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);
        condition.setDueDate(LocalDate.of(2025, 1, 31));

        when(conditionService.updateCondition(eq(1L), eq("Updated condition"),
                eq(ConditionOfApproval.ConditionType.MANDATORY), any(LocalDate.class)))
                .thenReturn(condition);

        // Act & Assert
        mockMvc.perform(put("/api/conditions/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condition").value("Updated condition"));

        verify(conditionService, times(1)).updateCondition(eq(1L), eq("Updated condition"),
                eq(ConditionOfApproval.ConditionType.MANDATORY), any(LocalDate.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateCondition_notFound_returns404() throws Exception {
        // Arrange
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        request.setCondition("Updated condition");
        request.setConditionType(ConditionOfApproval.ConditionType.RECOMMENDED);

        when(conditionService.updateCondition(anyLong(), anyString(), any(), any()))
                .thenThrow(new RuntimeException("Not found"));

        // Act & Assert
        mockMvc.perform(put("/api/conditions/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetCondition_success_returnsCondition() throws Exception {
        // Arrange
        Authorization auth = createMockAuthorization(1L);
        ConditionOfApproval condition = new ConditionOfApproval();
        condition.setId(1L);
        condition.setAuthorization(auth);
        condition.setCondition("Test condition");
        condition.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);

        when(conditionService.getCondition(1L)).thenReturn(condition);

        // Act & Assert
        mockMvc.perform(get("/api/conditions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.condition").value("Test condition"));

        verify(conditionService, times(1)).getCondition(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetCondition_notFound_returns404() throws Exception {
        // Arrange
        when(conditionService.getCondition(999L)).thenThrow(new RuntimeException("Not found"));

        // Act & Assert
        mockMvc.perform(get("/api/conditions/999"))
                .andExpect(status().isNotFound());

        verify(conditionService, times(1)).getCondition(999L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetConditionsByAuthorization_success_returnsConditions() throws Exception {
        // Arrange
        Authorization auth = createMockAuthorization(1L);

        ConditionOfApproval condition1 = new ConditionOfApproval();
        condition1.setId(1L);
        condition1.setAuthorization(auth);
        condition1.setCondition("Condition 1");

        ConditionOfApproval condition2 = new ConditionOfApproval();
        condition2.setId(2L);
        condition2.setAuthorization(auth);
        condition2.setCondition("Condition 2");

        List<ConditionOfApproval> conditions = Arrays.asList(condition1, condition2);

        when(conditionService.getConditionsByAuthorization(1L)).thenReturn(conditions);

        // Act & Assert
        mockMvc.perform(get("/api/conditions/authorization/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].condition").value("Condition 1"))
                .andExpect(jsonPath("$[1].condition").value("Condition 2"));

        verify(conditionService, times(1)).getConditionsByAuthorization(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetConditionsByAuthorization_serviceException_returns500() throws Exception {
        // Arrange
        when(conditionService.getConditionsByAuthorization(anyLong()))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(get("/api/conditions/authorization/1"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetConditionsByAuthorizationAndType_success_returnsConditions() throws Exception {
        // Arrange
        Authorization auth = createMockAuthorization(1L);
        ConditionOfApproval condition1 = new ConditionOfApproval();
        condition1.setId(1L);
        condition1.setAuthorization(auth);
        condition1.setCondition("Monitoring condition");
        condition1.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);

        List<ConditionOfApproval> conditions = Arrays.asList(condition1);

        when(conditionService.getConditionsByAuthorizationAndType(
                eq(1L), eq(ConditionOfApproval.ConditionType.MANDATORY)))
                .thenReturn(conditions);

        // Act & Assert
        mockMvc.perform(get("/api/conditions/authorization/1/type/MANDATORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].condition").value("Monitoring condition"));

        verify(conditionService, times(1)).getConditionsByAuthorizationAndType(
                eq(1L), eq(ConditionOfApproval.ConditionType.MANDATORY));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetConditionsByAuthorizationAndType_serviceException_returns500() throws Exception {
        // Arrange
        when(conditionService.getConditionsByAuthorizationAndType(anyLong(), any()))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(get("/api/conditions/authorization/1/type/MANDATORY"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteCondition_success_returns200() throws Exception {
        // Arrange
        doNothing().when(conditionService).deleteCondition(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/conditions/1")
                .with(csrf()))
                .andExpect(status().isOk());

        verify(conditionService, times(1)).deleteCondition(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteCondition_notFound_returns404() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Not found")).when(conditionService).deleteCondition(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/conditions/999")
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(conditionService, times(1)).deleteCondition(999L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteConditionsByAuthorization_success_returns200() throws Exception {
        // Arrange
        doNothing().when(conditionService).deleteConditionsByAuthorization(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/conditions/authorization/1")
                .with(csrf()))
                .andExpect(status().isOk());

        verify(conditionService, times(1)).deleteConditionsByAuthorization(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteConditionsByAuthorization_serviceException_returns500() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Service error"))
                .when(conditionService).deleteConditionsByAuthorization(anyLong());

        // Act & Assert
        mockMvc.perform(delete("/api/conditions/authorization/1")
                .with(csrf()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testCreateCondition_unauthenticated_returns401() throws Exception {
        // Arrange
        ConditionOfApprovalRequest request = new ConditionOfApprovalRequest();
        request.setAuthorizationId(1L);
        request.setCondition("Test condition");
        request.setConditionType(ConditionOfApproval.ConditionType.MANDATORY);

        // Act & Assert
        mockMvc.perform(post("/api/conditions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(conditionService, never()).createCondition(anyLong(), anyString(), any(), any());
    }

    @Test
    void testDeleteCondition_unauthenticated_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/conditions/1")
                .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(conditionService, never()).deleteCondition(anyLong());
    }
}

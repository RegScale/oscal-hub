package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.model.CustomRuleRequest;
import gov.nist.oscal.tools.api.model.CustomRuleResponse;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.service.CustomRulesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomRulesController.class)
class CustomRulesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomRulesService customRulesService;

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
    void testGetAllCustomRules_success_returnsRules() throws Exception {
        // Arrange
        CustomRuleResponse rule1 = new CustomRuleResponse();
        rule1.setId(1L);
        rule1.setRuleId("custom-rule-1");

        CustomRuleResponse rule2 = new CustomRuleResponse();
        rule2.setId(2L);
        rule2.setRuleId("custom-rule-2");

        List<CustomRuleResponse> rules = Arrays.asList(rule1, rule2);

        when(customRulesService.getAllCustomRules()).thenReturn(rules);

        // Act & Assert
        mockMvc.perform(get("/api/rules/custom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ruleId").value("custom-rule-1"))
                .andExpect(jsonPath("$[1].ruleId").value("custom-rule-2"));

        verify(customRulesService, times(1)).getAllCustomRules();
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetCustomRuleById_found_returnsRule() throws Exception {
        // Arrange
        CustomRuleResponse rule = new CustomRuleResponse();
        rule.setId(1L);
        rule.setRuleId("custom-rule-1");

        when(customRulesService.getCustomRuleById(1L)).thenReturn(Optional.of(rule));

        // Act & Assert
        mockMvc.perform(get("/api/rules/custom/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ruleId").value("custom-rule-1"));

        verify(customRulesService, times(1)).getCustomRuleById(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetCustomRuleById_notFound_returns404() throws Exception {
        // Arrange
        when(customRulesService.getCustomRuleById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/rules/custom/999"))
                .andExpect(status().isNotFound());

        verify(customRulesService, times(1)).getCustomRuleById(999L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetCustomRuleByRuleId_found_returnsRule() throws Exception {
        // Arrange
        CustomRuleResponse rule = new CustomRuleResponse();
        rule.setId(1L);
        rule.setRuleId("custom-rule-1");

        when(customRulesService.getCustomRuleByRuleId("custom-rule-1")).thenReturn(Optional.of(rule));

        // Act & Assert
        mockMvc.perform(get("/api/rules/custom/rule/custom-rule-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId").value("custom-rule-1"));

        verify(customRulesService, times(1)).getCustomRuleByRuleId("custom-rule-1");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetCustomRuleByRuleId_notFound_returns404() throws Exception {
        // Arrange
        when(customRulesService.getCustomRuleByRuleId("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/rules/custom/rule/nonexistent"))
                .andExpect(status().isNotFound());

        verify(customRulesService, times(1)).getCustomRuleByRuleId("nonexistent");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetEnabledCustomRules_success_returnsEnabledRules() throws Exception {
        // Arrange
        CustomRuleResponse rule1 = new CustomRuleResponse();
        rule1.setId(1L);
        rule1.setRuleId("enabled-rule-1");
        rule1.setEnabled(true);

        List<CustomRuleResponse> rules = Arrays.asList(rule1);

        when(customRulesService.getEnabledCustomRules()).thenReturn(rules);

        // Act & Assert
        mockMvc.perform(get("/api/rules/custom/enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].enabled").value(true));

        verify(customRulesService, times(1)).getEnabledCustomRules();
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetCustomRulesByCategory_success_returnsRules() throws Exception {
        // Arrange
        CustomRuleResponse rule1 = new CustomRuleResponse();
        rule1.setId(1L);
        rule1.setCategory("Metadata");

        List<CustomRuleResponse> rules = Arrays.asList(rule1);

        when(customRulesService.getCustomRulesByCategory("Metadata")).thenReturn(rules);

        // Act & Assert
        mockMvc.perform(get("/api/rules/custom/category/Metadata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("Metadata"));

        verify(customRulesService, times(1)).getCustomRulesByCategory("Metadata");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetCustomRulesByModelType_success_returnsRules() throws Exception {
        // Arrange
        CustomRuleResponse rule1 = new CustomRuleResponse();
        rule1.setId(1L);
        rule1.setApplicableModelTypes(Arrays.asList("catalog"));

        List<CustomRuleResponse> rules = Arrays.asList(rule1);

        when(customRulesService.getCustomRulesForModelType("catalog")).thenReturn(rules);

        // Act & Assert
        mockMvc.perform(get("/api/rules/custom/model/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].applicableModelTypes[0]").value("catalog"));

        verify(customRulesService, times(1)).getCustomRulesForModelType("catalog");
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateCustomRule_success_returnsCreatedRule() throws Exception {
        // Arrange
        CustomRuleRequest request = new CustomRuleRequest();
        request.setRuleId("new-rule");
        request.setName("New Rule");
        request.setDescription("A new custom rule");
        request.setRuleType("pattern-match");
        request.setSeverity("error");
        request.setEnabled(true);

        CustomRuleResponse response = new CustomRuleResponse();
        response.setId(1L);
        response.setRuleId("new-rule");

        when(customRulesService.createCustomRule(any(CustomRuleRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/rules/custom")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ruleId").value("new-rule"));

        verify(customRulesService, times(1)).createCustomRule(any(CustomRuleRequest.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testCreateCustomRule_invalidRequest_returns400() throws Exception {
        // Arrange
        CustomRuleRequest request = new CustomRuleRequest();
        request.setRuleId("duplicate-rule");
        request.setName("Duplicate Rule");
        request.setRuleType("pattern-match");
        request.setSeverity("error");
        request.setEnabled(true);

        when(customRulesService.createCustomRule(any(CustomRuleRequest.class)))
                .thenThrow(new IllegalArgumentException("Rule ID already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/rules/custom")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Rule ID already exists"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateCustomRule_success_returnsUpdatedRule() throws Exception {
        // Arrange
        CustomRuleRequest request = new CustomRuleRequest();
        request.setRuleId("updated-rule");
        request.setName("Updated Rule");
        request.setRuleType("pattern-match");
        request.setSeverity("warning");
        request.setEnabled(true);

        CustomRuleResponse response = new CustomRuleResponse();
        response.setId(1L);
        response.setRuleId("updated-rule");

        when(customRulesService.updateCustomRule(eq(1L), any(CustomRuleRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/rules/custom/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleId").value("updated-rule"));

        verify(customRulesService, times(1)).updateCustomRule(eq(1L), any(CustomRuleRequest.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testUpdateCustomRule_notFound_returns400() throws Exception {
        // Arrange
        CustomRuleRequest request = new CustomRuleRequest();
        request.setRuleId("updated-rule");
        request.setName("Updated Rule");
        request.setRuleType("pattern-match");
        request.setSeverity("warning");
        request.setEnabled(true);

        when(customRulesService.updateCustomRule(eq(999L), any(CustomRuleRequest.class)))
                .thenThrow(new IllegalArgumentException("Rule not found"));

        // Act & Assert
        mockMvc.perform(put("/api/rules/custom/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Rule not found"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteCustomRule_success_returns204() throws Exception {
        // Arrange
        doNothing().when(customRulesService).deleteCustomRule(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/rules/custom/1")
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(customRulesService, times(1)).deleteCustomRule(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeleteCustomRule_notFound_returns400() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Rule not found"))
                .when(customRulesService).deleteCustomRule(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/rules/custom/999")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Rule not found"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testToggleRuleEnabled_success_returnsToggledRule() throws Exception {
        // Arrange
        CustomRuleResponse response = new CustomRuleResponse();
        response.setId(1L);
        response.setRuleId("toggle-rule");
        response.setEnabled(false);

        when(customRulesService.toggleRuleEnabled(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(patch("/api/rules/custom/1/toggle")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        verify(customRulesService, times(1)).toggleRuleEnabled(1L);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testToggleRuleEnabled_notFound_returns400() throws Exception {
        // Arrange
        when(customRulesService.toggleRuleEnabled(999L))
                .thenThrow(new IllegalArgumentException("Rule not found"));

        // Act & Assert
        mockMvc.perform(patch("/api/rules/custom/999/toggle")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Rule not found"));
    }

    @Test
    void testGetAllCustomRules_unauthenticated_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/rules/custom"))
                .andExpect(status().isUnauthorized());

        verify(customRulesService, never()).getAllCustomRules();
    }

    @Test
    void testCreateCustomRule_unauthenticated_returns401() throws Exception {
        // Arrange
        CustomRuleRequest request = new CustomRuleRequest();
        request.setRuleId("new-rule");

        // Act & Assert
        mockMvc.perform(post("/api/rules/custom")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(customRulesService, never()).createCustomRule(any());
    }
}

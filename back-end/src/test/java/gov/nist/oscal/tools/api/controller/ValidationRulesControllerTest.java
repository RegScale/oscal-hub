package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.ValidationRulesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ValidationRulesController.class)
class ValidationRulesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ValidationRulesService validationRulesService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "testuser")
    void testGetAllRules_success_returnsRules() throws Exception {
        // Arrange
        ValidationRulesResponse response = new ValidationRulesResponse();
        response.setTotalRules(50);
        response.setBuiltInRules(45);
        response.setCustomRules(5);

        when(validationRulesService.getAllRules()).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRules").value(50))
                .andExpect(jsonPath("$.builtInRules").value(45))
                .andExpect(jsonPath("$.customRules").value(5));

        verify(validationRulesService, times(1)).getAllRules();
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRulesForModelType_success_returnsRules() throws Exception {
        // Arrange
        ValidationRulesResponse response = new ValidationRulesResponse();
        response.setTotalRules(20);

        when(validationRulesService.getRulesForModelType(OscalModelType.CATALOG))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/rules/model/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRules").value(20));

        verify(validationRulesService, times(1)).getRulesForModelType(OscalModelType.CATALOG);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetRulesForModelType_invalidType_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/rules/model/invalid-type"))
                .andExpect(status().isBadRequest());

        verify(validationRulesService, never()).getRulesForModelType(any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetCategories_success_returnsCategories() throws Exception {
        // Arrange
        ValidationRuleCategory category1 = new ValidationRuleCategory();
        category1.setName("Metadata");
        category1.setRuleCount(10);

        ValidationRuleCategory category2 = new ValidationRuleCategory();
        category2.setName("Security Controls");
        category2.setRuleCount(15);

        when(validationRulesService.getCategories())
                .thenReturn(Arrays.asList(category1, category2));

        // Act & Assert
        mockMvc.perform(get("/api/rules/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Metadata"))
                .andExpect(jsonPath("$[1].name").value("Security Controls"));

        verify(validationRulesService, times(1)).getCategories();
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetStats_success_returnsStats() throws Exception {
        // Arrange
        ValidationRulesResponse allRules = new ValidationRulesResponse();
        allRules.setTotalRules(50);
        allRules.setBuiltInRules(45);
        allRules.setCustomRules(5);

        Map<String, Integer> rulesByModelType = new HashMap<>();
        rulesByModelType.put("catalog", 20);
        rulesByModelType.put("profile", 15);
        allRules.setRulesByModelType(rulesByModelType);

        Map<String, Integer> rulesByCategory = new HashMap<>();
        rulesByCategory.put("Metadata", 10);
        allRules.setRulesByCategory(rulesByCategory);

        when(validationRulesService.getAllRules()).thenReturn(allRules);

        // Act & Assert
        mockMvc.perform(get("/api/rules/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRules").value(50))
                .andExpect(jsonPath("$.builtInRules").value(45))
                .andExpect(jsonPath("$.customRules").value(5));

        verify(validationRulesService, times(1)).getAllRules();
    }
}

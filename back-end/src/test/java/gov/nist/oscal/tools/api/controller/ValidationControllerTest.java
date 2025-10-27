/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.model.*;
import gov.nist.oscal.tools.api.model.BatchOperationRequest.BatchOperationType;
import gov.nist.oscal.tools.api.model.BatchOperationRequest.FileContent;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.BatchOperationService;
import gov.nist.oscal.tools.api.service.ConversionService;
import gov.nist.oscal.tools.api.service.ProfileResolutionService;
import gov.nist.oscal.tools.api.service.RateLimitService;
import gov.nist.oscal.tools.api.service.ValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ValidationController.class)
class ValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ValidationService validationService;

    @MockBean
    private ConversionService conversionService;

    @MockBean
    private ProfileResolutionService profileResolutionService;

    @MockBean
    private BatchOperationService batchOperationService;

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

    // ========== VALIDATE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testValidate_validCatalog_returnsSuccess() throws Exception {
        // Given
        ValidationRequest request = new ValidationRequest();
        request.setContent("<?xml version=\"1.0\"?><catalog xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\"></catalog>");
        request.setFormat(OscalFormat.XML);
        request.setModelType(OscalModelType.CATALOG);

        ValidationResult result = new ValidationResult();
        result.setValid(true);
        result.setModelType(OscalModelType.CATALOG);
        result.setFormat(OscalFormat.XML);

        when(validationService.validate(any(ValidationRequest.class), eq("testuser")))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/validate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.modelType").value("catalog"))
                .andExpect(jsonPath("$.format").value("XML"));

        verify(validationService).validate(any(ValidationRequest.class), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testValidate_invalidDocument_returnsInvalid() throws Exception {
        // Given
        ValidationRequest request = new ValidationRequest();
        request.setContent("invalid xml");
        request.setFormat(OscalFormat.XML);
        request.setModelType(OscalModelType.CATALOG);

        ValidationResult result = new ValidationResult();
        result.setValid(false);
        ValidationError error = new ValidationError("Invalid XML syntax", "ERROR");
        result.setErrors(List.of(error));

        when(validationService.validate(any(ValidationRequest.class), eq("testuser")))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/validate")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.errors[0].message").value("Invalid XML syntax"));

        verify(validationService).validate(any(ValidationRequest.class), eq("testuser"));
    }

    // ========== CONVERT ENDPOINT TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testConvert_xmlToJson_returnsConverted() throws Exception {
        // Given
        ConversionRequest request = new ConversionRequest();
        request.setContent("<?xml version=\"1.0\"?><catalog xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\"></catalog>");
        request.setFromFormat(OscalFormat.XML);
        request.setToFormat(OscalFormat.JSON);
        request.setModelType(OscalModelType.CATALOG);

        ConversionResult result = new ConversionResult(true, "{\"catalog\": {}}", OscalFormat.XML, OscalFormat.JSON);

        when(conversionService.convert(any(ConversionRequest.class), eq("testuser")))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/convert")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content").value("{\"catalog\": {}}"))
                .andExpect(jsonPath("$.fromFormat").value("XML"))
                .andExpect(jsonPath("$.toFormat").value("JSON"));

        verify(conversionService).convert(any(ConversionRequest.class), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testConvert_invalidContent_returnsFailure() throws Exception {
        // Given
        ConversionRequest request = new ConversionRequest();
        request.setContent("invalid content");
        request.setFromFormat(OscalFormat.XML);
        request.setToFormat(OscalFormat.JSON);
        request.setModelType(OscalModelType.CATALOG);

        ConversionResult result = new ConversionResult(false, "Failed to parse XML", OscalFormat.XML, OscalFormat.JSON, true);

        when(conversionService.convert(any(ConversionRequest.class), eq("testuser")))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/convert")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Failed to parse XML"));

        verify(conversionService).convert(any(ConversionRequest.class), eq("testuser"));
    }

    // ========== RESOLVE PROFILE ENDPOINT TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testResolveProfile_validProfile_returnsResult() throws Exception {
        // Given
        ProfileResolutionRequest request = new ProfileResolutionRequest();
        request.setProfileContent("<?xml version=\"1.0\"?><profile xmlns=\"http://csrc.nist.gov/ns/oscal/1.0\"></profile>");
        request.setFormat(OscalFormat.XML);

        ProfileResolutionResult result = new ProfileResolutionResult(false, "Not yet implemented");

        when(profileResolutionService.resolveProfile(any(ProfileResolutionRequest.class), eq("testuser")))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/profile/resolve")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        verify(profileResolutionService).resolveProfile(any(ProfileResolutionRequest.class), eq("testuser"));
    }

    // ========== BATCH OPERATION ENDPOINT TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testProcessBatch_validRequest_returnsOperationId() throws Exception {
        // Given
        BatchOperationRequest request = new BatchOperationRequest();
        request.setOperationType(BatchOperationType.VALIDATE);
        request.setModelType(OscalModelType.CATALOG);

        List<FileContent> files = new ArrayList<>();
        FileContent file1 = new FileContent();
        file1.setFilename("test-catalog.xml");
        file1.setContent("content1");
        file1.setFormat(OscalFormat.XML);
        files.add(file1);
        request.setFiles(files);

        BatchOperationResult result = new BatchOperationResult("op-12345", 1);

        when(batchOperationService.processBatch(any(BatchOperationRequest.class), eq("testuser")))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(post("/api/batch")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationId").value("op-12345"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalFiles").value(1));

        verify(batchOperationService).processBatch(any(BatchOperationRequest.class), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetBatchResult_existingOperation_returnsResult() throws Exception {
        // Given
        String operationId = "op-12345";

        BatchOperationResult result = new BatchOperationResult(
                true,
                operationId,
                5,
                5,
                0,
                new ArrayList<>(),
                1000L
        );

        when(batchOperationService.getBatchResult(operationId))
                .thenReturn(result);

        // When & Then
        mockMvc.perform(get("/api/batch/{operationId}", operationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationId").value(operationId))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalFiles").value(5))
                .andExpect(jsonPath("$.successCount").value(5));

        verify(batchOperationService).getBatchResult(operationId);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetBatchResult_nonExistentOperation_returns404() throws Exception {
        // Given
        String operationId = "non-existent";

        when(batchOperationService.getBatchResult(operationId))
                .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/batch/{operationId}", operationId))
                .andExpect(status().isNotFound());

        verify(batchOperationService).getBatchResult(operationId);
    }

    // ========== HEALTH ENDPOINT TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testHealth_returnsOk() throws Exception {
        // When & Then - Note: health endpoint is public in production, but we use @WithMockUser here
        // because the @WebMvcTest context doesn't fully load the SecurityConfig permitAll rules
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OSCAL CLI API is running"));
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    @WithMockUser(username = "testuser")
    void testValidate_allOscalModelTypes_handlesCorrectly() throws Exception {
        // Test each model type
        OscalModelType[] modelTypes = {
            OscalModelType.CATALOG,
            OscalModelType.PROFILE,
            OscalModelType.COMPONENT_DEFINITION,
            OscalModelType.SYSTEM_SECURITY_PLAN,
            OscalModelType.ASSESSMENT_PLAN,
            OscalModelType.ASSESSMENT_RESULTS,
            OscalModelType.PLAN_OF_ACTION_AND_MILESTONES
        };

        for (OscalModelType modelType : modelTypes) {
            ValidationRequest request = new ValidationRequest();
            request.setContent("<test></test>");
            request.setFormat(OscalFormat.XML);
            request.setModelType(modelType);

            ValidationResult result = new ValidationResult();
            result.setValid(true);
            result.setModelType(modelType);

            when(validationService.validate(any(ValidationRequest.class), eq("testuser")))
                    .thenReturn(result);

            mockMvc.perform(post("/api/validate")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.modelType").value(modelType.getValue()));
        }

        verify(validationService, times(modelTypes.length))
                .validate(any(ValidationRequest.class), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testConvert_allFormats_handlesCorrectly() throws Exception {
        // Test all format combinations
        OscalFormat[] formats = {OscalFormat.XML, OscalFormat.JSON, OscalFormat.YAML};

        for (OscalFormat inputFormat : formats) {
            for (OscalFormat outputFormat : formats) {
                if (inputFormat.equals(outputFormat)) continue;

                ConversionRequest request = new ConversionRequest();
                request.setContent("content");
                request.setFromFormat(inputFormat);
                request.setToFormat(outputFormat);
                request.setModelType(OscalModelType.CATALOG);

                ConversionResult result = new ConversionResult(true, "converted content", inputFormat, outputFormat);

                when(conversionService.convert(any(ConversionRequest.class), eq("testuser")))
                        .thenReturn(result);

                mockMvc.perform(post("/api/convert")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true));
            }
        }

        // 3 input formats × 2 output formats each (excluding same format) = 6 conversions
        verify(conversionService, times(6))
                .convert(any(ConversionRequest.class), eq("testuser"));
    }
}

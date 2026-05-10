package gov.nist.oscal.tools.api.service.ai.tools;

import gov.nist.oscal.tools.api.model.OscalFormat;
import gov.nist.oscal.tools.api.model.ProfileResolutionRequest;
import gov.nist.oscal.tools.api.model.ProfileResolutionResult;
import gov.nist.oscal.tools.api.service.ProfileResolutionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResolveProfileToolTest {

    @Test
    void schema_advertisesAllowedFormats() {
        ResolveProfileTool tool = new ResolveProfileTool(mock(ProfileResolutionService.class));
        assertThat(tool.name()).isEqualTo("resolve_profile");
        assertThat(tool.inputSchemaJson())
                .contains("profileContent")
                .contains("\"JSON\"")
                .contains("\"XML\"")
                .contains("\"YAML\"");
    }

    @Test
    void successfulResolution_returnsResolvedCatalogInContent() {
        ProfileResolutionService svc = mock(ProfileResolutionService.class);
        when(svc.resolveProfile(any(), anyString()))
                .thenReturn(new ProfileResolutionResult(true, "<catalog id=\"ok\"/>", 42));

        ResolveProfileTool tool = new ResolveProfileTool(svc);
        ToolResult r = tool.invoke(new ToolCall("resolve_profile",
                "{\"profileContent\":\"<profile/>\",\"format\":\"XML\"}"));

        assertThat(r.ok()).isTrue();
        assertThat(r.contentJson()).contains("resolvedCatalog");
        assertThat(r.contentJson()).contains("ok");
    }

    @Test
    void successfulResolution_passesFormatThroughToService() {
        ProfileResolutionService svc = mock(ProfileResolutionService.class);
        when(svc.resolveProfile(any(), anyString()))
                .thenReturn(new ProfileResolutionResult(true, "{}", 0));

        ResolveProfileTool tool = new ResolveProfileTool(svc);
        tool.invoke(new ToolCall("resolve_profile",
                "{\"profileContent\":\"{\\\"profile\\\":{}}\",\"format\":\"JSON\"}"));

        ArgumentCaptor<ProfileResolutionRequest> cap =
                ArgumentCaptor.forClass(ProfileResolutionRequest.class);
        verify(svc).resolveProfile(cap.capture(), anyString());
        assertThat(cap.getValue().getFormat()).isEqualTo(OscalFormat.JSON);
        assertThat(cap.getValue().getProfileContent()).contains("profile");
    }

    @Test
    void failedResolution_surfacedAsErrorWithUpstreamMessage() {
        ProfileResolutionService svc = mock(ProfileResolutionService.class);
        when(svc.resolveProfile(any(), anyString()))
                .thenReturn(new ProfileResolutionResult(false, "import not found"));

        ResolveProfileTool tool = new ResolveProfileTool(svc);
        ToolResult r = tool.invoke(new ToolCall("resolve_profile",
                "{\"profileContent\":\"{}\",\"format\":\"JSON\"}"));

        assertThat(r.ok()).isFalse();
        assertThat(r.summary()).contains("import not found");
    }

    @Test
    void resolverThrows_isWrappedAsToolError_notPropagated() {
        // Tools must never let exceptions escape; the orchestrator relies on
        // every invoke() returning a ToolResult so it can include the failure
        // as an LLM-visible message and continue the loop.
        ProfileResolutionService svc = mock(ProfileResolutionService.class);
        when(svc.resolveProfile(any(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        ResolveProfileTool tool = new ResolveProfileTool(svc);
        ToolResult r = tool.invoke(new ToolCall("resolve_profile",
                "{\"profileContent\":\"{}\",\"format\":\"JSON\"}"));

        assertThat(r.ok()).isFalse();
        assertThat(r.summary()).contains("boom");
    }

    @Test
    void invalidJsonArgs_returnsErrorNotCrash() {
        ResolveProfileTool tool = new ResolveProfileTool(mock(ProfileResolutionService.class));
        ToolResult r = tool.invoke(new ToolCall("resolve_profile", "not-json"));
        assertThat(r.ok()).isFalse();
        assertThat(r.summary()).contains("resolve_profile error:");
    }

    @Test
    void unknownFormat_returnsErrorNotCrash() {
        ResolveProfileTool tool = new ResolveProfileTool(mock(ProfileResolutionService.class));
        ToolResult r = tool.invoke(new ToolCall("resolve_profile",
                "{\"profileContent\":\"{}\",\"format\":\"PROTOBUF\"}"));
        assertThat(r.ok()).isFalse();
    }
}

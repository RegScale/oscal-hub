package gov.nist.oscal.tools.api.service.ai.tools;

import gov.nist.oscal.tools.api.model.ConversionRequest;
import gov.nist.oscal.tools.api.model.ConversionResult;
import gov.nist.oscal.tools.api.model.OscalFormat;
import gov.nist.oscal.tools.api.service.ConversionService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConvertFormatToolTest {

    @Test
    void invokesConversionServiceAndReturnsContent() {
        ConversionService svc = mock(ConversionService.class);
        // ConversionResult(boolean success, String content, OscalFormat fromFormat, OscalFormat toFormat)
        ConversionResult ok = new ConversionResult(true, "<catalog/>", OscalFormat.JSON, OscalFormat.XML);
        when(svc.convert(any(ConversionRequest.class), anyString())).thenReturn(ok);

        ConvertFormatTool tool = new ConvertFormatTool(svc);
        ToolResult r = tool.invoke(new ToolCall("convert_format",
                "{\"content\":\"{}\",\"from\":\"JSON\",\"to\":\"XML\",\"modelType\":\"catalog\"}"));

        assertThat(r.ok()).isTrue();
        assertThat(r.contentJson()).contains("<catalog/>");
    }

    @Test
    void wrapsServiceExceptionsAsToolError() {
        ConversionService svc = mock(ConversionService.class);
        when(svc.convert(any(ConversionRequest.class), anyString()))
                .thenThrow(new RuntimeException("boom"));

        ConvertFormatTool tool = new ConvertFormatTool(svc);
        ToolResult r = tool.invoke(new ToolCall("convert_format",
                "{\"content\":\"{}\",\"from\":\"JSON\",\"to\":\"XML\",\"modelType\":\"catalog\"}"));

        assertThat(r.ok()).isFalse();
        assertThat(r.summary()).contains("boom");
    }
}

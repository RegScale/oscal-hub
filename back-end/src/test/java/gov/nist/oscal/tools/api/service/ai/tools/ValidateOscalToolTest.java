package gov.nist.oscal.tools.api.service.ai.tools;

import gov.nist.oscal.tools.api.model.ValidationError;
import gov.nist.oscal.tools.api.model.ValidationRequest;
import gov.nist.oscal.tools.api.model.ValidationResult;
import gov.nist.oscal.tools.api.service.ValidationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidateOscalToolTest {

    @Test
    void invokesValidationServiceAndReportsOk() {
        ValidationService svc = mock(ValidationService.class);
        ValidationResult ok = new ValidationResult();
        ok.setValid(true);
        when(svc.validate(any(ValidationRequest.class), eq("ai"))).thenReturn(ok);

        ValidateOscalTool tool = new ValidateOscalTool(svc);
        ToolResult r = tool.invoke(new ToolCall("validate_oscal",
                "{\"content\":\"{}\",\"format\":\"JSON\",\"modelType\":\"catalog\"}"));

        assertThat(r.ok()).isTrue();
        assertThat(r.summary()).containsIgnoringCase("valid");
    }

    @Test
    void reportsErrorsFromValidation() {
        ValidationService svc = mock(ValidationService.class);
        ValidationResult bad = new ValidationResult();
        bad.setValid(false);
        ValidationError ve = new ValidationError();
        ve.setMessage("missing metadata.title");
        ve.setSeverity("error");
        bad.setErrors(List.of(ve));
        when(svc.validate(any(ValidationRequest.class), any())).thenReturn(bad);

        ValidateOscalTool tool = new ValidateOscalTool(svc);
        ToolResult r = tool.invoke(new ToolCall("validate_oscal",
                "{\"content\":\"{}\",\"format\":\"JSON\",\"modelType\":\"catalog\"}"));

        assertThat(r.ok()).isFalse();
        assertThat(r.summary()).contains("missing metadata.title");
    }
}

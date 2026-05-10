package gov.nist.oscal.tools.api.model.airulegen;

import jakarta.validation.constraints.NotBlank;

public record SaveRuleRequest(
    @NotBlank String ruleId,
    String category,
    Boolean enabled
) {}

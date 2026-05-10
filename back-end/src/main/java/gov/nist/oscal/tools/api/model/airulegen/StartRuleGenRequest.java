package gov.nist.oscal.tools.api.model.airulegen;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StartRuleGenRequest(
    @NotNull Long organizationId,
    @NotBlank String modelType
) {}

package gov.nist.oscal.tools.api.model.airulegen;

import jakarta.validation.constraints.NotBlank;

public record RuleGenTurnRequest(@NotBlank String userMessage) {}

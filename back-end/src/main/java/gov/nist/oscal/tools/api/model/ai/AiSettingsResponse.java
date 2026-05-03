package gov.nist.oscal.tools.api.model.ai;

public record AiSettingsResponse(
        boolean enabled,
        String fingerprint,
        String defaultModel
) { }

package gov.nist.oscal.tools.api.service.ai.wizard;

import java.util.UUID;

public record WizardContext(
        UUID sessionId,
        Long organizationId,
        Long userId,
        String apiKey,
        String model,
        String input
) { }

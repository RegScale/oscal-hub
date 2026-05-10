package gov.nist.oscal.tools.api.service.ai.wizard;

import java.util.UUID;

public record WizardContext(
        UUID sessionId,
        Long organizationId,
        Long userId,
        String apiKey,
        String model,
        String input,
        byte[] inputBytes,
        String inputFilename,
        String profileHref
) {
    public static WizardContext text(UUID id, Long orgId, Long userId, String apiKey, String model, String input) {
        return new WizardContext(id, orgId, userId, apiKey, model, input, null, null, null);
    }

    public static WizardContext text(UUID id, Long orgId, Long userId, String apiKey, String model, String input, String profileHref) {
        return new WizardContext(id, orgId, userId, apiKey, model, input, null, null, profileHref);
    }

    public static WizardContext file(UUID id, Long orgId, Long userId, String apiKey, String model, byte[] bytes, String filename) {
        return new WizardContext(id, orgId, userId, apiKey, model, null, bytes, filename, null);
    }

    public static WizardContext file(UUID id, Long orgId, Long userId, String apiKey, String model, byte[] bytes, String filename, String profileHref) {
        return new WizardContext(id, orgId, userId, apiKey, model, null, bytes, filename, profileHref);
    }
}

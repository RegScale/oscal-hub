package gov.nist.oscal.tools.api.model.ai;

import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.entity.WizardKind;

import java.time.LocalDateTime;
import java.util.UUID;

public record AiSessionSummary(
        UUID id,
        Long userId,
        String username,
        WizardKind wizardKind,
        AiSessionMode mode,
        String model,
        AiSessionStatus status,
        int tokensIn,
        int tokensOut,
        long costUsdMicros,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String errorCode) {
}

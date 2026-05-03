package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.service.ai.wizard.Wizard;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardContext;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AiOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AiOrchestrator.class);

    private final AiSessionRepository sessions;
    private final AiSettingsServiceFacade settings;
    private final WizardRouter router;

    public AiOrchestrator(AiSessionRepository sessions, AiSettingsServiceFacade settings, WizardRouter router) {
        this.sessions = sessions;
        this.settings = settings;
        this.router = router;
    }

    public UUID start(Long organizationId, Long userId, WizardKind kind, AiSessionMode mode, String input) {
        UUID id = UUID.randomUUID();
        String apiKey = settings.requireApiKey(organizationId);
        String model = settings.getDefaultModel(organizationId);

        AiSession session = new AiSession();
        session.setId(id);
        session.setOrganizationId(organizationId);
        session.setUserId(userId);
        session.setWizardKind(kind);
        session.setMode(mode);
        session.setModel(model);
        session.setStatus(AiSessionStatus.RUNNING);
        session.setStartedAt(LocalDateTime.now());
        sessions.save(session);

        Wizard wizard = router.get(kind);
        runAsync(wizard, new WizardContext(id, organizationId, userId, apiKey, model, input));
        return id;
    }

    @Async
    public void runAsync(Wizard wizard, WizardContext ctx) {
        AiSession session = sessions.findById(ctx.sessionId()).orElseThrow();
        try {
            WizardOutcome outcome = wizard.run(ctx);
            session.setTokensIn(outcome.tokensIn());
            session.setTokensOut(outcome.tokensOut());
            session.setEndedAt(LocalDateTime.now());
            session.setStatus(outcome.success() ? AiSessionStatus.COMPLETED : AiSessionStatus.FAILED);
            session.setErrorCode(outcome.errorCode());
            session.setErrorMessage(outcome.errorMessage());
        } catch (Exception e) {
            log.error("Wizard run failed", e);
            session.setStatus(AiSessionStatus.FAILED);
            session.setErrorCode("orchestrator_error");
            session.setErrorMessage(e.getMessage());
            session.setEndedAt(LocalDateTime.now());
        }
        sessions.save(session);
    }
}

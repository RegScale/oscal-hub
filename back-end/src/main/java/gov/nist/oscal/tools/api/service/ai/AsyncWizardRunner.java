package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.service.ai.wizard.Wizard;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardContext;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AsyncWizardRunner {

    private static final Logger log = LoggerFactory.getLogger(AsyncWizardRunner.class);

    private final AiSessionRepository sessions;

    public AsyncWizardRunner(AiSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Async
    public void run(Wizard wizard, WizardContext ctx) {
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

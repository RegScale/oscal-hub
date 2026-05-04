package gov.nist.oscal.tools.api.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.service.ai.stream.AiSessionEventStream;
import gov.nist.oscal.tools.api.service.ai.stream.SessionEvent;
import gov.nist.oscal.tools.api.service.ai.wizard.Wizard;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardContext;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AsyncWizardRunner {

    private static final Logger log = LoggerFactory.getLogger(AsyncWizardRunner.class);

    private final AiSessionRepository sessions;
    private final AiSessionEventStream stream;
    private final ModelPricing pricing;
    private final ObjectMapper mapper;

    public AsyncWizardRunner(AiSessionRepository sessions,
                             AiSessionEventStream stream,
                             ModelPricing pricing,
                             ObjectMapper mapper) {
        this.sessions = sessions;
        this.stream = stream;
        this.pricing = pricing;
        this.mapper = mapper;
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

        // Drain buffered events, serialize, and persist alongside cost
        List<SessionEvent> events = stream.drainBuffer(ctx.sessionId());
        try {
            List<Map<String, Object>> arr = events.stream()
                    .map(e -> Map.<String, Object>of(
                            "type", e.type().name().toLowerCase(),
                            "data", e.dataJson()))
                    .toList();
            session.setEventsJson(mapper.writeValueAsString(arr));
        } catch (Exception e) {
            log.warn("Failed to serialize events for session {}", ctx.sessionId(), e);
        }
        if (session.getModel() != null) {
            session.setCostUsdMicros(pricing.computeMicros(session.getModel(),
                    session.getTokensIn(), session.getTokensOut()));
        }
        sessions.save(session);
    }
}

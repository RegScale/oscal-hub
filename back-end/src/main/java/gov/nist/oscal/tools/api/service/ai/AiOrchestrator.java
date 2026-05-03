package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.AiSessionMode;
import gov.nist.oscal.tools.api.entity.AiSessionStatus;
import gov.nist.oscal.tools.api.entity.WizardKind;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.service.ai.wizard.Wizard;
import gov.nist.oscal.tools.api.service.ai.wizard.WizardContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AiOrchestrator {

    private final AiSessionRepository sessions;
    private final AiSettingsServiceFacade settings;
    private final WizardRouter router;
    private final AsyncWizardRunner asyncRunner;

    public AiOrchestrator(AiSessionRepository sessions, AiSettingsServiceFacade settings,
                          WizardRouter router, AsyncWizardRunner asyncRunner) {
        this.sessions = sessions;
        this.settings = settings;
        this.router = router;
        this.asyncRunner = asyncRunner;
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
        asyncRunner.run(wizard, new WizardContext(id, organizationId, userId, apiKey, model, input));
        return id;
    }
}

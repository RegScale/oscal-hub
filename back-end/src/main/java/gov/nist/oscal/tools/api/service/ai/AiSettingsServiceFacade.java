package gov.nist.oscal.tools.api.service.ai;

public interface AiSettingsServiceFacade {
    String requireApiKey(Long organizationId);
    String getDefaultModel(Long organizationId);
}

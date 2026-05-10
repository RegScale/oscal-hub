package gov.nist.oscal.tools.api.model.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateAiSettingsRequest {
    @NotBlank
    @Size(min = 20, max = 256)
    private String apiKey;

    @Size(max = 64)
    private String defaultModel;

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }
}

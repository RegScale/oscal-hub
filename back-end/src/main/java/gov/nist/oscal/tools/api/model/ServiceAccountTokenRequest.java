package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ServiceAccountTokenRequest {

    @NotBlank(message = "Token name is required")
    private String tokenName;

    @NotNull(message = "Expiration days is required")
    @Min(value = 1, message = "Expiration must be at least 1 day")
    private Integer expirationDays;

    public ServiceAccountTokenRequest() {
    }

    public ServiceAccountTokenRequest(String tokenName, Integer expirationDays) {
        this.tokenName = tokenName;
        this.expirationDays = expirationDays;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public Integer getExpirationDays() {
        return expirationDays;
    }

    public void setExpirationDays(Integer expirationDays) {
        this.expirationDays = expirationDays;
    }
}

package gov.nist.oscal.tools.api.model;

public class ProfileResolutionResult {
    private boolean success;
    private String resolvedCatalog;
    private String error;
    private Integer controlCount;

    // Constructor for success
    public ProfileResolutionResult(boolean success, String resolvedCatalog, Integer controlCount) {
        this.success = success;
        this.resolvedCatalog = resolvedCatalog;
        this.controlCount = controlCount;
    }

    // Constructor for error
    public ProfileResolutionResult(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getResolvedCatalog() {
        return resolvedCatalog;
    }

    public void setResolvedCatalog(String resolvedCatalog) {
        this.resolvedCatalog = resolvedCatalog;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Integer getControlCount() {
        return controlCount;
    }

    public void setControlCount(Integer controlCount) {
        this.controlCount = controlCount;
    }
}

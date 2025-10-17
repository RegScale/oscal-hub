package gov.nist.oscal.tools.api.model;

import java.util.List;

public class BatchOperationResult {
    private boolean success;
    private String operationId;
    private int totalFiles;
    private int successCount;
    private int failureCount;
    private List<FileResult> results;
    private long totalDurationMs;

    public static class FileResult {
        private String filename;
        private boolean success;
        private String error;
        private Object result; // ValidationResult or ConversionResult
        private long durationMs;

        public FileResult() {
        }

        public FileResult(String filename, boolean success, String error, Object result, long durationMs) {
            this.filename = filename;
            this.success = success;
            this.error = error;
            this.result = result;
            this.durationMs = durationMs;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }
    }

    // Constructor for immediate response
    public BatchOperationResult(String operationId, int totalFiles) {
        this.success = true;
        this.operationId = operationId;
        this.totalFiles = totalFiles;
        this.successCount = 0;
        this.failureCount = 0;
    }

    // Constructor for completed batch
    public BatchOperationResult(boolean success, String operationId, int totalFiles,
                                int successCount, int failureCount, List<FileResult> results,
                                long totalDurationMs) {
        this.success = success;
        this.operationId = operationId;
        this.totalFiles = totalFiles;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.results = results;
        this.totalDurationMs = totalDurationMs;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<FileResult> getResults() {
        return results;
    }

    public void setResults(List<FileResult> results) {
        this.results = results;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }
}

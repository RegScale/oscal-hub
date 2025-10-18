package gov.nist.oscal.tools.api.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class BatchOperationRequest {
    @NotNull(message = "Operation type is required")
    private BatchOperationType operationType;

    @NotNull(message = "Files are required")
    @Size(min = 1, max = 10, message = "Between 1 and 10 files are required")
    private List<FileContent> files;

    @NotNull(message = "Model type is required (for validation operations)")
    private OscalModelType modelType;

    // For conversion operations
    private OscalFormat fromFormat;
    private OscalFormat toFormat;

    public enum BatchOperationType {
        VALIDATE,
        CONVERT
    }

    public static class FileContent {
        private String filename;
        private String content;
        private OscalFormat format;

        public FileContent() {
        }

        public FileContent(String filename, String content, OscalFormat format) {
            this.filename = filename;
            this.content = content;
            this.format = format;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public OscalFormat getFormat() {
            return format;
        }

        public void setFormat(OscalFormat format) {
            this.format = format;
        }
    }

    // Getters and setters
    public BatchOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(BatchOperationType operationType) {
        this.operationType = operationType;
    }

    public List<FileContent> getFiles() {
        return files;
    }

    public void setFiles(List<FileContent> files) {
        this.files = files;
    }

    public OscalModelType getModelType() {
        return modelType;
    }

    public void setModelType(OscalModelType modelType) {
        this.modelType = modelType;
    }

    public OscalFormat getFromFormat() {
        return fromFormat;
    }

    public void setFromFormat(OscalFormat fromFormat) {
        this.fromFormat = fromFormat;
    }

    public OscalFormat getToFormat() {
        return toFormat;
    }

    public void setToFormat(OscalFormat toFormat) {
        this.toFormat = toFormat;
    }
}

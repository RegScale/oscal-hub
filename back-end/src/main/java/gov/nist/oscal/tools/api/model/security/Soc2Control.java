package gov.nist.oscal.tools.api.model.security;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Represents a SOC 2 control with its implementation status.
 */
@Schema(description = "SOC 2 control with implementation status")
public class Soc2Control {

    @Schema(description = "Control identifier", example = "CC6.1")
    private String controlId;

    @Schema(description = "Control name", example = "Logical Access Security Software")
    private String name;

    @Schema(description = "Control description")
    private String description;

    @Schema(description = "Control category")
    private ControlCategory category;

    @Schema(description = "Implementation status")
    private ControlStatus status;

    @Schema(description = "Description of how the control is implemented")
    private String implementation;

    @Schema(description = "Evidence links or references")
    private List<String> evidence;

    public Soc2Control() {
    }

    public Soc2Control(String controlId, String name, String description, ControlCategory category,
                       ControlStatus status, String implementation, List<String> evidence) {
        this.controlId = controlId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.status = status;
        this.implementation = implementation;
        this.evidence = evidence;
    }

    public String getControlId() {
        return controlId;
    }

    public void setControlId(String controlId) {
        this.controlId = controlId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ControlCategory getCategory() {
        return category;
    }

    public void setCategory(ControlCategory category) {
        this.category = category;
    }

    public ControlStatus getStatus() {
        return status;
    }

    public void setStatus(ControlStatus status) {
        this.status = status;
    }

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = evidence;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String controlId;
        private String name;
        private String description;
        private ControlCategory category;
        private ControlStatus status;
        private String implementation;
        private List<String> evidence;

        public Builder controlId(String controlId) {
            this.controlId = controlId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder category(ControlCategory category) {
            this.category = category;
            return this;
        }

        public Builder status(ControlStatus status) {
            this.status = status;
            return this;
        }

        public Builder implementation(String implementation) {
            this.implementation = implementation;
            return this;
        }

        public Builder evidence(List<String> evidence) {
            this.evidence = evidence;
            return this;
        }

        public Soc2Control build() {
            return new Soc2Control(controlId, name, description, category, status, implementation, evidence);
        }
    }
}

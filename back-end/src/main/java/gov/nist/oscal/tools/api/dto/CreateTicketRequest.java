package gov.nist.oscal.tools.api.dto;

import gov.nist.oscal.tools.api.entity.TicketPriority;
import gov.nist.oscal.tools.api.entity.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public class CreateTicketRequest {
    @NotNull private TicketType type;
    @NotBlank @Size(max = 200) private String title;
    @NotBlank private String description;
    private TicketPriority priority = TicketPriority.MEDIUM;
    private Map<String, Object> metadata;

    public TicketType getType() { return type; }
    public void setType(TicketType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}

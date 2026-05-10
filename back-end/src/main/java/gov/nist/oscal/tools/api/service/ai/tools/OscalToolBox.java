package gov.nist.oscal.tools.api.service.ai.tools;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OscalToolBox {

    private final List<Tool> toolList;

    public OscalToolBox(List<Tool> tools) {
        this.toolList = List.copyOf(tools);
    }

    public List<Tool> tools() {
        return toolList;
    }

    public ToolResult invoke(ToolCall call) {
        return toolList.stream()
                .filter(t -> t.name().equals(call.name()))
                .findFirst()
                .map(t -> {
                    try {
                        return t.invoke(call);
                    } catch (Exception e) {
                        return ToolResult.error("Tool error: " + e.getMessage());
                    }
                })
                .orElseGet(() -> ToolResult.error("Unknown tool: " + call.name()));
    }
}

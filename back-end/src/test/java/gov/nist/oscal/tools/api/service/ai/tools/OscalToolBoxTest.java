package gov.nist.oscal.tools.api.service.ai.tools;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OscalToolBoxTest {

    @Test
    void registersExpectedTools() {
        Tool stub = new Tool() {
            public String name() { return "stub"; }
            public String description() { return "stub"; }
            public String inputSchemaJson() { return "{}"; }
            public ToolResult invoke(ToolCall call) { return ToolResult.ok("ok"); }
        };

        OscalToolBox box = new OscalToolBox(List.of(stub, stub));
        assertThat(box.tools()).hasSize(2);
    }

    @Test
    void invokesByName() {
        Tool t = new Tool() {
            public String name() { return "echo"; }
            public String description() { return "echo"; }
            public String inputSchemaJson() { return "{\"type\":\"object\"}"; }
            public ToolResult invoke(ToolCall call) { return ToolResult.ok("echoed: " + call.argsJson()); }
        };
        OscalToolBox box = new OscalToolBox(List.of(t));
        ToolResult r = box.invoke(new ToolCall("echo", "{\"hi\":1}"));
        assertThat(r.ok()).isTrue();
        assertThat(r.summary()).contains("echoed");
    }

    @Test
    void unknownToolReturnsError() {
        OscalToolBox box = new OscalToolBox(List.of());
        ToolResult r = box.invoke(new ToolCall("nope", "{}"));
        assertThat(r.ok()).isFalse();
    }
}

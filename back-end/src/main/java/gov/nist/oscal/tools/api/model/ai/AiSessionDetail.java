package gov.nist.oscal.tools.api.model.ai;

import java.util.List;
import java.util.Map;

public record AiSessionDetail(
        AiSessionSummary summary,
        List<Map<String, Object>> events,
        String errorMessage) {
}

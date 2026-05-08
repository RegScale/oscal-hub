package gov.nist.oscal.tools.api.service.ai.wizard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * Extracts the union of controls explicitly listed under
 * {@code imports[].include-controls[].with-ids} in an OSCAL profile.
 *
 * <p>Returns {@link Optional#empty()} when:
 * <ul>
 *   <li>any import uses {@code include-all} (catalog resolution required)</li>
 *   <li>the profile JSON cannot be parsed</li>
 *   <li>no profile imports are present</li>
 * </ul>
 */
@Component
public class ProfileControlIdExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Optional<List<String>> extract(String profileJson) {
        if (profileJson == null || profileJson.isBlank()) return Optional.empty();
        try {
            JsonNode root = MAPPER.readTree(profileJson);
            JsonNode profile = root.path("profile");
            JsonNode imports = profile.path("imports");
            if (!imports.isArray() || imports.isEmpty()) return Optional.empty();

            LinkedHashSet<String> ids = new LinkedHashSet<>();
            for (JsonNode imp : imports) {
                if (imp.has("include-all")) {
                    return Optional.empty();
                }
                JsonNode includeControls = imp.path("include-controls");
                if (!includeControls.isArray()) continue;
                for (JsonNode inc : includeControls) {
                    JsonNode withIds = inc.path("with-ids");
                    if (!withIds.isArray()) continue;
                    for (JsonNode id : withIds) {
                        if (id.isTextual()) ids.add(id.asText());
                    }
                }
            }
            if (ids.isEmpty()) return Optional.empty();
            return Optional.of(new ArrayList<>(ids));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

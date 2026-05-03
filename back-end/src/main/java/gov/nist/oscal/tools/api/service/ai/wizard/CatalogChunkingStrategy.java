package gov.nist.oscal.tools.api.service.ai.wizard;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CatalogChunkingStrategy {

    public record Family(String id, String title, List<String> controlIds) { }

    public List<List<Family>> chunk(List<Family> families) {
        if (families.size() <= 30) {
            // Group of 6 per call
            List<List<Family>> chunks = new ArrayList<>();
            for (int i = 0; i < families.size(); i += 6) {
                chunks.add(families.subList(i, Math.min(i + 6, families.size())));
            }
            return chunks;
        }
        // 1 family per call for large publications
        List<List<Family>> chunks = new ArrayList<>();
        for (Family f : families) chunks.add(List.of(f));
        return chunks;
    }
}

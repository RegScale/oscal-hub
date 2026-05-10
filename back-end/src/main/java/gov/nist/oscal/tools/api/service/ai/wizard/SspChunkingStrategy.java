package gov.nist.oscal.tools.api.service.ai.wizard;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Chunks SSP control IDs into per-LLM-call sub-lists.
 *
 * <ul>
 *   <li>≤ 50 controls → 10 per chunk (small custom baselines)</li>
 *   <li>&gt; 50 controls → 20 per chunk (FedRAMP-class baselines).
 *       SSP narrative output is leaner than catalog group output, so
 *       we can fit more controls per chunk than CatalogChunkingStrategy.</li>
 * </ul>
 */
@Component
public class SspChunkingStrategy {

    private static final int SMALL_THRESHOLD = 50;
    private static final int SMALL_CHUNK_SIZE = 10;
    private static final int LARGE_CHUNK_SIZE = 20;

    public List<List<String>> chunk(List<String> controlIds) {
        if (controlIds.isEmpty()) return List.of();
        int size = controlIds.size() <= SMALL_THRESHOLD ? SMALL_CHUNK_SIZE : LARGE_CHUNK_SIZE;
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < controlIds.size(); i += size) {
            chunks.add(controlIds.subList(i, Math.min(i + size, controlIds.size())));
        }
        return chunks;
    }
}

package gov.nist.oscal.tools.api.service.ai.wizard;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Chunks a flat list of OSCAL control IDs into sub-lists suitable for a single
 * Anthropic call.
 *
 * <ul>
 *   <li>≤ 50 controls → 8 per chunk (small guides such as vendor hardening docs)</li>
 *   <li>&gt; 50 controls → 4 per chunk (large guides such as DISA STIGs or CIS
 *       Benchmarks which can have hundreds of items)</li>
 * </ul>
 */
@Component
public class ComponentDefChunkingStrategy {

    private static final int SMALL_THRESHOLD = 50;
    private static final int SMALL_CHUNK_SIZE = 8;
    private static final int LARGE_CHUNK_SIZE = 4;

    /**
     * Splits {@code controlIds} into sub-lists.
     *
     * @param controlIds flat list of canonical control IDs (e.g. ["ac-1", "ac-2", ...])
     * @return list of chunks, each containing at most {@value #SMALL_CHUNK_SIZE} or
     *         {@value #LARGE_CHUNK_SIZE} IDs depending on total size
     */
    public List<List<String>> chunk(List<String> controlIds) {
        int size = controlIds.size() <= SMALL_THRESHOLD ? SMALL_CHUNK_SIZE : LARGE_CHUNK_SIZE;
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < controlIds.size(); i += size) {
            chunks.add(controlIds.subList(i, Math.min(i + size, controlIds.size())));
        }
        return chunks;
    }
}

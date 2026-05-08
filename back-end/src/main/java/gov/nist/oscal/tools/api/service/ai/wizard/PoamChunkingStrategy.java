package gov.nist.oscal.tools.api.service.ai.wizard;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Chunks POA&amp;M item identifiers into per-LLM-call sub-lists.
 *
 * <p>POA&amp;M items have richer per-row content than SSP control narratives
 * (multi-paragraph descriptions, multiple props, milestones), so we use a
 * smaller chunk size than SspChunkingStrategy.
 */
@Component
public class PoamChunkingStrategy {

    private static final int CHUNK_SIZE = 8;

    public List<List<String>> chunk(List<String> itemIds) {
        if (itemIds.isEmpty()) return List.of();
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < itemIds.size(); i += CHUNK_SIZE) {
            chunks.add(itemIds.subList(i, Math.min(i + CHUNK_SIZE, itemIds.size())));
        }
        return chunks;
    }
}

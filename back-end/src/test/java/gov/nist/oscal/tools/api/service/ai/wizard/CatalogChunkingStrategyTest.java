package gov.nist.oscal.tools.api.service.ai.wizard;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogChunkingStrategyTest {

    private final CatalogChunkingStrategy strategy = new CatalogChunkingStrategy();

    @Test
    void smallCatalogChunksInGroupsOfSix() {
        List<CatalogChunkingStrategy.Family> fams = mk(20);
        List<List<CatalogChunkingStrategy.Family>> chunks = strategy.chunk(fams);
        // 20 / 6 = 4 chunks (6,6,6,2)
        assertThat(chunks).hasSize(4);
        assertThat(chunks.get(0)).hasSize(6);
        assertThat(chunks.get(3)).hasSize(2);
    }

    @Test
    void largeCatalogChunksOneByOne() {
        List<CatalogChunkingStrategy.Family> fams = mk(100);
        List<List<CatalogChunkingStrategy.Family>> chunks = strategy.chunk(fams);
        assertThat(chunks).hasSize(100);
        assertThat(chunks.get(0)).hasSize(1);
    }

    private List<CatalogChunkingStrategy.Family> mk(int n) {
        List<CatalogChunkingStrategy.Family> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(new CatalogChunkingStrategy.Family("f" + i, "Family " + i, List.of("f" + i + "-1")));
        }
        return out;
    }
}

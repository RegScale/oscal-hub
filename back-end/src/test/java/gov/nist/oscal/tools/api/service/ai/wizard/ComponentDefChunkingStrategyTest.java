package gov.nist.oscal.tools.api.service.ai.wizard;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentDefChunkingStrategyTest {

    private final ComponentDefChunkingStrategy strategy = new ComponentDefChunkingStrategy();

    @Test
    void smallGuideChunksInGroupsOfEight() {
        List<String> ids = mk(24);
        List<List<String>> chunks = strategy.chunk(ids);
        // 24 / 8 = 3 full chunks
        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(8);
        assertThat(chunks.get(2)).hasSize(8);
    }

    @Test
    void smallGuideWithRemainderChunksCorrectly() {
        List<String> ids = mk(20);
        List<List<String>> chunks = strategy.chunk(ids);
        // 20 / 8 = 2 full chunks of 8 + 1 chunk of 4
        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(8);
        assertThat(chunks.get(1)).hasSize(8);
        assertThat(chunks.get(2)).hasSize(4);
    }

    @Test
    void largeGuideChunksInGroupsOfFour() {
        List<String> ids = mk(100);
        List<List<String>> chunks = strategy.chunk(ids);
        // 100 / 4 = 25 chunks
        assertThat(chunks).hasSize(25);
        assertThat(chunks.get(0)).hasSize(4);
        assertThat(chunks.get(24)).hasSize(4);
    }

    @Test
    void boundaryAt50UsesSmallChunkSize() {
        List<String> ids = mk(50);
        List<List<String>> chunks = strategy.chunk(ids);
        // 50 / 8 = 6 chunks of 8 + 1 chunk of 2
        assertThat(chunks).hasSize(7);
        assertThat(chunks.get(0)).hasSize(8);
    }

    @Test
    void emptyListReturnsEmptyChunks() {
        List<List<String>> chunks = strategy.chunk(List.of());
        assertThat(chunks).isEmpty();
    }

    private List<String> mk(int n) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add("ac-" + (i + 1));
        }
        return out;
    }
}

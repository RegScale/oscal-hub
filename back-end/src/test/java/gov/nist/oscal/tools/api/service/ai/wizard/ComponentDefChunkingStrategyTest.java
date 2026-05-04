package gov.nist.oscal.tools.api.service.ai.wizard;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentDefChunkingStrategyTest {

    private final ComponentDefChunkingStrategy strategy = new ComponentDefChunkingStrategy();

    @Test
    void smallGuideChunksInGroupsOfTen() {
        List<String> ids = mk(30);
        List<List<String>> chunks = strategy.chunk(ids);
        // 30 / 10 = 3 full chunks
        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(10);
        assertThat(chunks.get(2)).hasSize(10);
    }

    @Test
    void smallGuideWithRemainderChunksCorrectly() {
        List<String> ids = mk(25);
        List<List<String>> chunks = strategy.chunk(ids);
        // 25 / 10 = 2 full chunks of 10 + 1 chunk of 5
        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(10);
        assertThat(chunks.get(1)).hasSize(10);
        assertThat(chunks.get(2)).hasSize(5);
    }

    @Test
    void largeGuideChunksInGroupsOfEight() {
        List<String> ids = mk(100);
        List<List<String>> chunks = strategy.chunk(ids);
        // 100 / 8 = 12 chunks of 8 + 1 chunk of 4
        assertThat(chunks).hasSize(13);
        assertThat(chunks.get(0)).hasSize(8);
        assertThat(chunks.get(12)).hasSize(4);
    }

    @Test
    void boundaryAt50UsesSmallChunkSize() {
        List<String> ids = mk(50);
        List<List<String>> chunks = strategy.chunk(ids);
        // 50 / 10 = 5 chunks of 10
        assertThat(chunks).hasSize(5);
        assertThat(chunks.get(0)).hasSize(10);
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

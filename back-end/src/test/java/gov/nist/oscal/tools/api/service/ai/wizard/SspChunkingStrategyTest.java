package gov.nist.oscal.tools.api.service.ai.wizard;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SspChunkingStrategyTest {

    private final SspChunkingStrategy strategy = new SspChunkingStrategy();

    @Test
    void emptyListProducesNoChunks() {
        assertThat(strategy.chunk(List.of())).isEmpty();
    }

    @Test
    void singleControlProducesOneChunkOfOne() {
        List<List<String>> chunks = strategy.chunk(List.of("ac-1"));
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).containsExactly("ac-1");
    }

    @Test
    void smallBaselineUsesSmallChunkSize() {
        List<String> ids = IntStream.range(0, 25).mapToObj(i -> "ac-" + i).toList();
        List<List<String>> chunks = strategy.chunk(ids);
        // 25 controls / 10 per chunk = 3 chunks (10, 10, 5)
        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(10);
        assertThat(chunks.get(1)).hasSize(10);
        assertThat(chunks.get(2)).hasSize(5);
    }

    @Test
    void boundaryAt50UsesSmallChunkSize() {
        List<String> ids = IntStream.range(0, 50).mapToObj(i -> "ac-" + i).toList();
        List<List<String>> chunks = strategy.chunk(ids);
        // 50 / 10 = 5 chunks of 10
        assertThat(chunks).hasSize(5);
        assertThat(chunks.get(0)).hasSize(10);
    }

    @Test
    void largeBaselineUsesLargeChunkSize() {
        List<String> ids = IntStream.range(0, 100).mapToObj(i -> "ac-" + i).toList();
        List<List<String>> chunks = strategy.chunk(ids);
        // 100 controls / 20 per chunk = 5 chunks
        assertThat(chunks).hasSize(5);
        assertThat(chunks.get(0)).hasSize(20);
    }

    @Test
    void fedrampModerateApproximation() {
        List<String> ids = IntStream.range(0, 325).mapToObj(i -> "c-" + i).toList();
        List<List<String>> chunks = strategy.chunk(ids);
        // 325 / 20 = 17 chunks (16 of 20, 1 of 5)
        assertThat(chunks).hasSize(17);
        assertThat(chunks.get(16)).hasSize(5);
    }
}

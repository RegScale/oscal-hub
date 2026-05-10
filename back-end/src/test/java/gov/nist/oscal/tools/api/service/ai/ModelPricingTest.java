package gov.nist.oscal.tools.api.service.ai;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ModelPricingTest {
    private final ModelPricing p = new ModelPricing();

    @Test
    void opus47PricingMatchesPublished() {
        // 1M in + 1M out at Opus rate = $15 + $75 = $90 = 90_000_000 micros
        assertThat(p.computeMicros("claude-opus-4-7", 1_000_000, 1_000_000)).isEqualTo(90_000_000L);
    }

    @Test
    void haiku45PricingMatchesPublished() {
        // 1M in + 1M out at Haiku rate = $1 + $5 = $6 = 6_000_000 micros
        assertThat(p.computeMicros("claude-haiku-4-5-20251001", 1_000_000, 1_000_000)).isEqualTo(6_000_000L);
    }

    @Test
    void unknownModelFallsBackToOpusRate() {
        assertThat(p.computeMicros("unknown-model", 1_000_000, 1_000_000)).isEqualTo(90_000_000L);
    }
}

package gov.nist.oscal.tools.api.service.ai;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ModelPricing {

    // USD per million tokens. Update when Anthropic publishes pricing changes.
    // https://www.anthropic.com/pricing
    private static final Map<String, double[]> RATES = Map.of(
            // model -> [input USD/MTok, output USD/MTok]
            "claude-opus-4-7",           new double[]{15.00, 75.00},
            "claude-opus-4-6",           new double[]{15.00, 75.00},
            "claude-sonnet-4-6",         new double[]{ 3.00, 15.00},
            "claude-sonnet-4-5",         new double[]{ 3.00, 15.00},
            "claude-haiku-4-5-20251001", new double[]{ 1.00,  5.00},
            "claude-haiku-4-5",          new double[]{ 1.00,  5.00}
    );
    private static final double[] FALLBACK = new double[]{15.00, 75.00};

    public long computeMicros(String model, int tokensIn, int tokensOut) {
        double[] r = RATES.getOrDefault(model, FALLBACK);
        double usd = (tokensIn * r[0] + tokensOut * r[1]) / 1_000_000.0;
        return Math.round(usd * 1_000_000.0);
    }
}

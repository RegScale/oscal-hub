package gov.nist.oscal.tools.api.service.ai.wizard;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileControlIdExtractorTest {

    private final ProfileControlIdExtractor extractor = new ProfileControlIdExtractor();

    @Test
    void includeControlsWithIdsExtractsControlIds() {
        String json = """
            {
              "profile": {
                "uuid": "00000000-0000-0000-0000-000000000001",
                "imports": [{
                  "href": "catalog.json",
                  "include-controls": [{
                    "with-ids": ["ac-1", "ac-2", "au-3"]
                  }]
                }]
              }
            }
            """;
        Optional<List<String>> result = extractor.extract(json);
        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("ac-1", "ac-2", "au-3");
    }

    @Test
    void multipleImportsAreUnioned() {
        String json = """
            {
              "profile": {
                "imports": [
                  { "include-controls": [{ "with-ids": ["ac-1"] }] },
                  { "include-controls": [{ "with-ids": ["au-1", "au-2"] }] }
                ]
              }
            }
            """;
        Optional<List<String>> result = extractor.extract(json);
        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("ac-1", "au-1", "au-2");
    }

    @Test
    void duplicateControlIdsAreDeduplicatedPreservingOrder() {
        String json = """
            {
              "profile": {
                "imports": [{
                  "include-controls": [
                    { "with-ids": ["ac-1", "ac-2"] },
                    { "with-ids": ["ac-2", "ac-3"] }
                  ]
                }]
              }
            }
            """;
        Optional<List<String>> result = extractor.extract(json);
        assertThat(result).isPresent();
        assertThat(result.get()).containsExactly("ac-1", "ac-2", "ac-3");
    }

    @Test
    void includeAllReturnsEmptyOptional() {
        String json = """
            {
              "profile": {
                "imports": [{ "include-all": {} }]
              }
            }
            """;
        Optional<List<String>> result = extractor.extract(json);
        assertThat(result).isEmpty();
    }

    @Test
    void mixedIncludeAllAndWithIdsReturnsEmptyOptional() {
        String json = """
            {
              "profile": {
                "imports": [
                  { "include-controls": [{ "with-ids": ["ac-1"] }] },
                  { "include-all": {} }
                ]
              }
            }
            """;
        Optional<List<String>> result = extractor.extract(json);
        assertThat(result).isEmpty();
    }

    @Test
    void malformedJsonReturnsEmptyOptional() {
        Optional<List<String>> result = extractor.extract("not json");
        assertThat(result).isEmpty();
    }

    @Test
    void profileWithoutImportsReturnsEmptyOptional() {
        String json = "{\"profile\":{\"uuid\":\"x\"}}";
        Optional<List<String>> result = extractor.extract(json);
        assertThat(result).isEmpty();
    }
}

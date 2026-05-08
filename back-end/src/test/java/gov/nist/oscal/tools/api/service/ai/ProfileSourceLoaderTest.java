package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.service.LibraryService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProfileSourceLoaderTest {

    @Test
    void libraryHrefDelegatesToLibraryService() {
        LibraryService library = mock(LibraryService.class);
        RestTemplate rest = mock(RestTemplate.class);
        User caller = new User();

        when(library.getCurrentVersionContent(eq("abc-123"), eq(caller))).thenReturn("{\"profile\":{}}");
        ProfileSourceLoader loader = new ProfileSourceLoader(library, rest);

        String content = loader.load("library:abc-123", caller);
        assertThat(content).isEqualTo("{\"profile\":{}}");
    }

    @Test
    void httpHrefDelegatesToRestTemplate() {
        LibraryService library = mock(LibraryService.class);
        RestTemplate rest = mock(RestTemplate.class);
        when(rest.getForObject(eq("https://example.test/profile.json"), eq(String.class)))
                .thenReturn("{\"profile\":{\"uuid\":\"x\"}}");

        ProfileSourceLoader loader = new ProfileSourceLoader(library, rest);
        String content = loader.load("https://example.test/profile.json", new User());
        assertThat(content).contains("\"profile\"");
    }

    @Test
    void invalidSchemeThrowsIllegalArgument() {
        ProfileSourceLoader loader = new ProfileSourceLoader(mock(LibraryService.class), mock(RestTemplate.class));
        assertThatLoading(() -> loader.load("ftp://x", new User()));
    }

    @Test
    void httpFailureWraps() {
        LibraryService library = mock(LibraryService.class);
        RestTemplate rest = mock(RestTemplate.class);
        when(rest.getForObject(eq("https://bad.test/p.json"), eq(String.class)))
                .thenThrow(new RestClientException("boom"));

        ProfileSourceLoader loader = new ProfileSourceLoader(library, rest);
        assertThatLoading(() -> loader.load("https://bad.test/p.json", new User()));
    }

    private void assertThatLoading(Runnable r) {
        try {
            r.run();
            assertThat(true).as("should have thrown").isFalse();
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}

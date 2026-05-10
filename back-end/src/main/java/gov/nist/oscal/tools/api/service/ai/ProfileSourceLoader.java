package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.service.LibraryService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Loads OSCAL profile content given a {@code profileHref} chosen by the
 * SSP wizard caller.
 *
 * <p>Recognized schemes:
 * <ul>
 *   <li>{@code library:<itemId>} — fetches via {@link LibraryService}</li>
 *   <li>{@code http://...} or {@code https://...} — fetches via {@link RestTemplate}</li>
 * </ul>
 *
 * <p>Throws {@link IllegalArgumentException} on unsupported schemes or HTTP failures
 * — the wizard catches this and falls back to outline-derived control IDs.
 */
@Service
public class ProfileSourceLoader {

    private static final String LIBRARY_PREFIX = "library:";

    private final LibraryService library;
    private final RestTemplate rest;

    public ProfileSourceLoader(LibraryService library, RestTemplate rest) {
        this.library = library;
        this.rest = rest;
    }

    public String load(String href, User caller) {
        if (href == null || href.isBlank()) {
            throw new IllegalArgumentException("profileHref is empty");
        }
        if (href.startsWith(LIBRARY_PREFIX)) {
            String itemId = href.substring(LIBRARY_PREFIX.length());
            try {
                return library.getCurrentVersionContent(itemId, caller);
            } catch (Exception e) {
                throw new IllegalArgumentException("Library profile not readable: " + itemId, e);
            }
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            try {
                String body = rest.getForObject(href, String.class);
                if (body == null) throw new IllegalArgumentException("Empty response from " + href);
                return body;
            } catch (RestClientException e) {
                throw new IllegalArgumentException("Failed to fetch " + href + ": " + e.getMessage(), e);
            }
        }
        throw new IllegalArgumentException("Unsupported profileHref scheme: " + href);
    }
}

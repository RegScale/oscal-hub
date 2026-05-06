package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.library.PublicCatalogAnalytics;
import gov.nist.oscal.tools.api.model.library.PublicCatalogTopContributors;
import gov.nist.oscal.tools.api.model.library.PublicItemSummary;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.LibraryService;
import gov.nist.oscal.tools.api.service.LibraryStorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/catalog")
@Tag(name = "Public Catalog",
     description = "Anonymous-readable browse + download of PUBLIC library items")
public class PublicCatalogController {

    private final LibraryService libraryService;
    private final LibraryStorageService storageService;
    private final UserRepository userRepository;

    @Autowired
    public PublicCatalogController(LibraryService libraryService,
                                    LibraryStorageService storageService,
                                    UserRepository userRepository) {
        this.libraryService = libraryService;
        this.storageService = storageService;
        this.userRepository = userRepository;
    }

    /**
     * Resolve the currently authenticated User (or null for anonymous calls).
     * The /content endpoints below aren't permitAll(), but we still pass the
     * caller through so the audit log records who downloaded what.
     */
    private User currentCaller() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    @GetMapping("/items")
    public ResponseEntity<Page<PublicItemSummary>> listPublic(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        size = Math.min(size, 100);
        // Native query — Sort uses raw column names (snake_case), not entity field names.
        Sort.Order order = switch (sort) {
            case "downloads" -> Sort.Order.desc("download_count");
            case "rating" -> Sort.Order.desc("download_count");  // proxy until rating sort added
            default /* "newest" */ -> Sort.Order.desc("last_published_at");
        };
        var pageable = PageRequest.of(page, size, Sort.by(order));
        return ResponseEntity.ok(libraryService.searchPublic(q, type, tag, pageable));
    }

    @GetMapping("/items/{itemId}")
    public ResponseEntity<?> getPublic(@PathVariable String itemId) {
        return libraryService.getPublic(itemId)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "not found")));
    }

    @GetMapping("/items/{itemId}/content")
    public ResponseEntity<?> downloadLatest(@PathVariable String itemId) {
        return libraryService.getPublicLatestContent(itemId, currentCaller())
            .<ResponseEntity<?>>map(this::toFileResponse)
            .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "not found")));
    }

    @GetMapping("/items/{itemId}/versions/{versionId}/content")
    public ResponseEntity<?> downloadVersion(@PathVariable String itemId,
                                              @PathVariable String versionId) {
        return libraryService.getPublicVersionContent(itemId, versionId, currentCaller())
            .<ResponseEntity<?>>map(this::toFileResponse)
            .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "not found")));
    }

    // ==================== Analytics tabs ====================
    // All four endpoints are anonymous-readable (permitAll in SecurityConfig)
    // and aggregate over PUBLIC visibility items only.

    @GetMapping("/most-downloaded")
    public ResponseEntity<List<PublicItemSummary>> mostDownloaded(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(libraryService.getMostDownloadedPublic(limit));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<List<PublicItemSummary>> topRated(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(name = "minRatings", defaultValue = "1") long minRatings) {
        return ResponseEntity.ok(libraryService.getTopRatedPublic(limit, minRatings));
    }

    @GetMapping("/top-contributors")
    public ResponseEntity<PublicCatalogTopContributors> topContributors(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(libraryService.getTopContributorsPublic(limit));
    }

    @GetMapping("/analytics")
    public ResponseEntity<PublicCatalogAnalytics> analytics(
            @RequestParam(name = "weeks", defaultValue = "26") int weeksBack) {
        return ResponseEntity.ok(libraryService.getPublicAnalytics(weeksBack));
    }

    private ResponseEntity<byte[]> toFileResponse(LibraryService.VersionDownload dl) {
        byte[] body = dl.content().getBytes(StandardCharsets.UTF_8);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        h.setContentDispositionFormData("attachment", dl.filename());
        h.setContentLength(body.length);
        return new ResponseEntity<>(body, h, 200);
    }
}

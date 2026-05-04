package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.model.library.PublicItemSummary;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/public/catalog")
@Tag(name = "Public Catalog",
     description = "Anonymous-readable browse + download of PUBLIC library items")
public class PublicCatalogController {

    private final LibraryService libraryService;
    private final LibraryStorageService storageService;

    @Autowired
    public PublicCatalogController(LibraryService libraryService,
                                    LibraryStorageService storageService) {
        this.libraryService = libraryService;
        this.storageService = storageService;
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
        return libraryService.getPublicLatestContent(itemId)
            .<ResponseEntity<?>>map(this::toFileResponse)
            .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "not found")));
    }

    @GetMapping("/items/{itemId}/versions/{versionId}/content")
    public ResponseEntity<?> downloadVersion(@PathVariable String itemId,
                                              @PathVariable String versionId) {
        return libraryService.getPublicVersionContent(itemId, versionId)
            .<ResponseEntity<?>>map(this::toFileResponse)
            .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "not found")));
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

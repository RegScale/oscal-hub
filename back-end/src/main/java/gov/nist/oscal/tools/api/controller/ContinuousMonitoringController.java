package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;
import gov.nist.oscal.tools.api.entity.ConMonReconciliation;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.conmon.ConMonAnalyticsResponse;
import gov.nist.oscal.tools.api.model.conmon.ConMonPoamItemResponse;
import gov.nist.oscal.tools.api.model.conmon.ConMonReconciliationResponse;
import gov.nist.oscal.tools.api.model.conmon.ConMonSnapshotSummary;
import gov.nist.oscal.tools.api.repository.ConMonPoamItemRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.AuthorizationAccessGuard;
import gov.nist.oscal.tools.api.service.AuthorizationService;
import gov.nist.oscal.tools.api.service.conmon.ConMonAnalyticsService;
import gov.nist.oscal.tools.api.service.conmon.ConMonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/authorizations/{authorizationId}/conmon")
@Tag(name = "Continuous Monitoring", description = "POAM snapshot upload, reconciliation, and analytics")
public class ContinuousMonitoringController {

    private final AuthorizationService authorizationService;
    private final AuthorizationAccessGuard accessGuard;
    private final ConMonService conMonService;
    private final ConMonAnalyticsService analyticsService;
    private final ConMonPoamItemRepository itemRepository;
    private final UserRepository userRepository;

    public ContinuousMonitoringController(AuthorizationService authorizationService,
                                          AuthorizationAccessGuard accessGuard,
                                          ConMonService conMonService,
                                          ConMonAnalyticsService analyticsService,
                                          ConMonPoamItemRepository itemRepository,
                                          UserRepository userRepository) {
        this.authorizationService = authorizationService;
        this.accessGuard = accessGuard;
        this.conMonService = conMonService;
        this.analyticsService = analyticsService;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/snapshots", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConMonSnapshotSummary> upload(
            @PathVariable Long authorizationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "notes", required = false) String notes,
            Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        User currentUser = requireCurrentUser(principal);
        accessGuard.requireUploadConMon(authorization, currentUser);

        ConMonSnapshot snapshot = conMonService.upload(authorization, currentUser, file, notes);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ConMonSnapshotSummary(snapshot, conMonService.findReconciliation(snapshot).orElse(null)));
    }

    @GetMapping("/snapshots")
    public ResponseEntity<List<ConMonSnapshotSummary>> list(@PathVariable Long authorizationId,
                                                            Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        // Use the service method so LAZY associations are initialized inside a transaction
        // before we build DTOs here (preventing LazyInitializationException in production).
        List<ConMonSnapshot> snaps = conMonService.listSnapshots(authorization);
        List<ConMonSnapshotSummary> out = new ArrayList<>(snaps.size());
        for (ConMonSnapshot s : snaps) {
            out.add(new ConMonSnapshotSummary(s, conMonService.findReconciliation(s).orElse(null)));
        }
        return ResponseEntity.ok(out);
    }

    @GetMapping("/snapshots/{snapshotId}")
    public ResponseEntity<ConMonSnapshotSummary> get(@PathVariable Long authorizationId,
                                                     @PathVariable Long snapshotId,
                                                     Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        // Use the service method so LAZY associations (authorization, uploadedBy, items)
        // are eagerly loaded within the transaction before DTO mapping.
        ConMonSnapshot snap = requireSnapshot(authorization, snapshotId);
        return ResponseEntity.ok(new ConMonSnapshotSummary(snap, conMonService.findReconciliation(snap).orElse(null)));
    }

    @GetMapping("/snapshots/{snapshotId}/items")
    public ResponseEntity<Map<String, Object>> items(@PathVariable Long authorizationId,
                                                     @PathVariable Long snapshotId,
                                                     @RequestParam(value = "status", required = false) ConMonItemStatus status,
                                                     @RequestParam(value = "severity", required = false) String severity,
                                                     @RequestParam(value = "overdue", defaultValue = "false") boolean overdue,
                                                     @RequestParam(value = "q", required = false) String q,
                                                     @RequestParam(value = "page", defaultValue = "0") int page,
                                                     @RequestParam(value = "size", defaultValue = "50") int size,
                                                     Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        ConMonSnapshot snap = requireSnapshot(authorization, snapshotId);

        Page<ConMonPoamItem> result = itemRepository.search(snap, status, severity, overdue, java.time.LocalDate.now(), q,
                PageRequest.of(page, Math.min(size, 200)));

        List<ConMonPoamItemResponse> rows = result.stream().map(ConMonPoamItemResponse::new).toList();
        Map<String, Object> body = new HashMap<>();
        body.put("items", rows);
        body.put("totalElements", result.getTotalElements());
        body.put("totalPages", result.getTotalPages());
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/snapshots/{snapshotId}/reconciliation")
    public ResponseEntity<ConMonReconciliationResponse> reconciliation(@PathVariable Long authorizationId,
                                                                       @PathVariable Long snapshotId,
                                                                       Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        // requireSnapshot already eagerly loads current.getItems() via findSnapshotWithAssociations.
        ConMonSnapshot current = requireSnapshot(authorization, snapshotId);
        // Use the eager-loading variant so rec.getPreviousSnapshot() and its items
        // collection are initialized inside a transaction before buildDiff iterates them.
        ConMonReconciliation rec = conMonService.findReconciliationWithAssociations(current).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No reconciliation for snapshot " + snapshotId + " (likely the first snapshot)."));

        ConMonSnapshot prev = rec.getPreviousSnapshot();
        return ResponseEntity.ok(buildDiff(current, prev, rec));
    }

    @GetMapping("/snapshots/{snapshotId}/download")
    public ResponseEntity<ByteArrayResource> download(@PathVariable Long authorizationId,
                                                      @PathVariable Long snapshotId,
                                                      Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        ConMonSnapshot snap = requireSnapshot(authorization, snapshotId);
        byte[] bytes = conMonService.downloadOriginal(snap);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(snap.getOriginalFilename()).build());
        headers.setContentLength(bytes.length);
        return ResponseEntity.ok().headers(headers).body(new ByteArrayResource(bytes));
    }

    @DeleteMapping("/snapshots/{snapshotId}")
    public ResponseEntity<Void> delete(@PathVariable Long authorizationId,
                                       @PathVariable Long snapshotId,
                                       Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        User currentUser = requireCurrentUser(principal);
        ConMonSnapshot snap = requireSnapshot(authorization, snapshotId);
        // ConMon snapshots are org-level artifacts: any EDITOR+ may delete them;
        // CONTRIBUTORs can delete only their own (matches the documents pattern).
        accessGuard.requireDeleteOwnedItem(authorization, currentUser, snap.getUploadedBy().getId());
        conMonService.delete(snap);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/analytics")
    public ResponseEntity<ConMonAnalyticsResponse> analytics(@PathVariable Long authorizationId,
                                                             Principal principal) {
        Authorization authorization = authorizationService.getAuthorizationForUser(authorizationId, principal.getName());
        return ResponseEntity.ok(analyticsService.forAuthorization(authorization));
    }

    private ConMonReconciliationResponse buildDiff(ConMonSnapshot current, ConMonSnapshot previous,
                                                   ConMonReconciliation rec) {
        Map<String, ConMonPoamItem> prevByExt = new HashMap<>();
        for (ConMonPoamItem p : previous.getItems()) prevByExt.put(p.getExternalId(), p);
        Map<String, ConMonPoamItem> currByExt = new HashMap<>();
        for (ConMonPoamItem c : current.getItems()) currByExt.put(c.getExternalId(), c);

        List<ConMonPoamItemResponse> news = new ArrayList<>();
        List<ConMonPoamItemResponse> closed = new ArrayList<>();
        List<ConMonPoamItemResponse> reopened = new ArrayList<>();
        List<ConMonPoamItemResponse> removed = new ArrayList<>();
        List<ConMonReconciliationResponse.ChangedItem> changed = new ArrayList<>();

        for (var e : currByExt.entrySet()) {
            ConMonPoamItem curr = e.getValue();
            ConMonPoamItem prev = prevByExt.get(e.getKey());
            if (prev == null) { news.add(new ConMonPoamItemResponse(curr)); continue; }
            if (prev.getStatus() == ConMonItemStatus.OPEN && curr.getStatus() == ConMonItemStatus.CLOSED) {
                closed.add(new ConMonPoamItemResponse(curr));
            } else if (prev.getStatus() == ConMonItemStatus.CLOSED && curr.getStatus() == ConMonItemStatus.OPEN) {
                reopened.add(new ConMonPoamItemResponse(curr));
            } else {
                List<String> diffs = diffFields(prev, curr);
                if (!diffs.isEmpty()) {
                    changed.add(new ConMonReconciliationResponse.ChangedItem(
                            new ConMonPoamItemResponse(curr),
                            new ConMonPoamItemResponse(prev),
                            diffs));
                }
            }
        }
        for (var e : prevByExt.entrySet()) {
            if (!currByExt.containsKey(e.getKey())) removed.add(new ConMonPoamItemResponse(e.getValue()));
        }

        ConMonReconciliationResponse r = new ConMonReconciliationResponse();
        r.setSnapshotId(current.getId());
        r.setPreviousSnapshotId(previous.getId());
        r.setNewCount(rec.getNewCount());
        r.setClosedCount(rec.getClosedCount());
        r.setReopenedCount(rec.getReopenedCount());
        r.setStillOpenCount(rec.getStillOpenCount());
        r.setRemovedCount(rec.getRemovedCount());
        r.setChangedCount(rec.getChangedCount());
        r.setNewItems(news);
        r.setNewlyClosedItems(closed);
        r.setReopenedItems(reopened);
        r.setRemovedItems(removed);
        r.setChangedItems(changed);
        return r;
    }

    private static List<String> diffFields(ConMonPoamItem a, ConMonPoamItem b) {
        List<String> diffs = new ArrayList<>();
        if (!java.util.Objects.equals(a.getTitle(), b.getTitle())) diffs.add("title");
        if (!java.util.Objects.equals(a.getSeverity(), b.getSeverity())) diffs.add("severity");
        if (!java.util.Objects.equals(a.getScheduledCompletionDate(), b.getScheduledCompletionDate()))
            diffs.add("scheduledCompletionDate");
        if (!java.util.Objects.equals(a.getStatus(), b.getStatus())) diffs.add("status");
        return diffs;
    }

    private ConMonSnapshot requireSnapshot(Authorization authorization, Long snapshotId) {
        // Use the service method so that LAZY associations (authorization, uploadedBy,
        // items) are force-initialized inside a transaction before any DTO mapping or
        // guard checks that touch those fields (e.g. snap.getUploadedBy().getId()).
        return conMonService.findSnapshotWithAssociations(snapshotId, authorization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Snapshot " + snapshotId + " not found on authorization " + authorization.getId()));
    }

    private User requireCurrentUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User '" + principal.getName() + "' not found."));
    }
}

package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.dto.*;
import gov.nist.oscal.tools.api.entity.*;
import gov.nist.oscal.tools.api.repository.*;
import gov.nist.oscal.tools.api.service.TicketAttachmentStorageService;
import gov.nist.oscal.tools.api.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "User-facing ticket APIs")
public class TicketController {

    private final TicketService service;
    private final UserRepository users;
    private final TicketAttachmentRepository attachments;
    private final TicketCommentRepository comments;
    private final TicketAttachmentStorageService storage;
    private final ObjectMapper mapper = new ObjectMapper();

    public TicketController(TicketService service, UserRepository users,
                            TicketAttachmentRepository attachments,
                            TicketCommentRepository comments,
                            TicketAttachmentStorageService storage) {
        this.service = service;
        this.users = users;
        this.attachments = attachments;
        this.comments = comments;
        this.storage = storage;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketSummaryResponse> create(
            @RequestParam("type") TicketType type,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "priority", defaultValue = "MEDIUM") TicketPriority priority,
            @RequestParam(value = "metadata", required = false) String metadataJson,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Principal principal) throws JsonProcessingException {

        User reporter = users.findByUsername(principal.getName()).orElseThrow();
        Map<String, Object> metadata = metadataJson == null ? Map.of()
            : mapper.readValue(metadataJson, new TypeReference<>() {});
        Ticket t = service.createTicket(reporter, type, title, description, priority, metadata, files);
        return ResponseEntity.status(201).body(TicketSummaryResponse.from(t));
    }

    @GetMapping("/mine")
    public Page<TicketSummaryResponse> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Principal principal) {
        User reporter = users.findByUsername(principal.getName()).orElseThrow();
        return service.listMyTickets(reporter, PageRequest.of(page, Math.min(size, 100)))
            .map(TicketSummaryResponse::from);
    }

    @GetMapping("/{id}")
    public TicketDetailResponse get(@PathVariable Long id, Principal principal) {
        User caller = users.findByUsername(principal.getName()).orElseThrow();
        boolean isAdmin = isSuperAdmin(caller);
        Ticket t = service.getTicket(id, caller, isAdmin);

        List<AttachmentResponse> origAtts = attachments
            .findByTicketAndCommentIsNull(t).stream()
            .map(AttachmentResponse::from).toList();
        List<CommentResponse> threadedComments = comments
            .findByTicketOrderByCreatedAtAsc(t).stream()
            .map(c -> CommentResponse.from(c, attachments.findByComment(c).stream()
                .map(AttachmentResponse::from).toList()))
            .toList();
        return TicketDetailResponse.from(t, origAtts, threadedComments);
    }

    @PostMapping(value = "/{id}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @RequestParam("body") String body,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Principal principal) {
        User caller = users.findByUsername(principal.getName()).orElseThrow();
        boolean isAdmin = isSuperAdmin(caller);
        TicketComment c = service.addComment(id, caller, isAdmin, body, files);
        List<AttachmentResponse> atts = attachments.findByComment(c).stream()
            .map(AttachmentResponse::from).toList();
        return ResponseEntity.status(201).body(CommentResponse.from(c, atts));
    }

    @GetMapping("/attachments/{id}")
    public ResponseEntity<ByteArrayResource> downloadAttachment(
            @PathVariable Long id, Principal principal) throws IOException {
        TicketAttachment a = attachments.findById(id)
            .orElseThrow(() -> new java.util.NoSuchElementException("Attachment " + id));
        User caller = users.findByUsername(principal.getName()).orElseThrow();
        boolean isAdmin = isSuperAdmin(caller);
        service.getTicket(a.getTicket().getId(), caller, isAdmin);

        byte[] bytes = storage.download(a.getStoragePath());
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(a.getContentType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + a.getFilename() + "\"")
            .body(new ByteArrayResource(bytes));
    }

    private boolean isSuperAdmin(User u) {
        return u.getGlobalRole() == User.GlobalRole.SUPER_ADMIN;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Void> forbidden() { return ResponseEntity.status(403).build(); }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<Void> notFound() { return ResponseEntity.status(404).build(); }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

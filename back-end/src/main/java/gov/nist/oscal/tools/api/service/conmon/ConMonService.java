package gov.nist.oscal.tools.api.service.conmon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.Authorization;
import gov.nist.oscal.tools.api.entity.ConMonItemStatus;
import gov.nist.oscal.tools.api.entity.ConMonPoamItem;
import gov.nist.oscal.tools.api.entity.ConMonReconciliation;
import gov.nist.oscal.tools.api.entity.ConMonSnapshot;
import gov.nist.oscal.tools.api.entity.ConMonSourceFormat;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.exception.UnsupportedConMonFormatException;
import gov.nist.oscal.tools.api.repository.ConMonPoamItemRepository;
import gov.nist.oscal.tools.api.repository.ConMonReconciliationRepository;
import gov.nist.oscal.tools.api.repository.ConMonSnapshotRepository;
import gov.nist.oscal.tools.api.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConMonService {

    private final ConMonSnapshotRepository snapshotRepository;
    private final ConMonPoamItemRepository itemRepository;
    private final ConMonReconciliationRepository reconciliationRepository;
    private final OscalPoamParser oscalParser;
    private final FedrampPoamExcelParser excelParser;
    private final ConMonReconciliationService reconciliationService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public ConMonService(ConMonSnapshotRepository snapshotRepository,
                         ConMonPoamItemRepository itemRepository,
                         ConMonReconciliationRepository reconciliationRepository,
                         OscalPoamParser oscalParser,
                         FedrampPoamExcelParser excelParser,
                         ConMonReconciliationService reconciliationService,
                         FileStorageService fileStorageService,
                         ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.itemRepository = itemRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.oscalParser = oscalParser;
        this.excelParser = excelParser;
        this.reconciliationService = reconciliationService;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConMonSnapshot upload(Authorization authorization, User uploader,
                                 MultipartFile file, String notes) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }
        ConMonSourceFormat format = ConMonSourceFormat.fromFilename(file.getOriginalFilename());
        if (format == null) {
            throw new UnsupportedConMonFormatException(file.getOriginalFilename());
        }

        ParsedPoam parsed;
        try {
            parsed = (format == ConMonSourceFormat.FEDRAMP_XLSX)
                    ? excelParser.parse(file.getInputStream())
                    : oscalParser.parse(file.getInputStream(), format);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read upload stream", e);
        }

        // Persist the original blob via existing FileStorageService binary primitives (PR 3).
        String storagePath = "authorizations/" + authorization.getId()
                + "/conmon/" + UUID.randomUUID() + "-" + sanitizeFilename(file.getOriginalFilename());
        try {
            fileStorageService.saveBinary(storagePath, file.getBytes(), file.getContentType());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read upload bytes for storage", e);
        }

        ConMonSnapshot snapshot = new ConMonSnapshot();
        snapshot.setAuthorization(authorization);
        snapshot.setUploadedBy(uploader);
        snapshot.setSourceFormat(format);
        snapshot.setOriginalFilename(file.getOriginalFilename());
        snapshot.setFileStoragePath(storagePath);
        snapshot.setOscalUuid(parsed.oscalUuid());
        snapshot.setOscalVersion(parsed.oscalVersion());
        snapshot.setMetadataTitle(parsed.metadataTitle());
        snapshot.setMetadataLastModified(parsed.metadataLastModified());
        snapshot.setNotes(notes);

        int open = 0, closed = 0, unknown = 0;
        for (ParsedPoamItem pi : parsed.items()) {
            ConMonPoamItem item = toItem(pi, snapshot);
            snapshot.getItems().add(item);
            switch (item.getStatus()) {
                case OPEN -> open++;
                case CLOSED -> closed++;
                case UNKNOWN -> unknown++;
            }
        }
        snapshot.setSummaryOpenCount(open);
        snapshot.setSummaryClosedCount(closed);
        snapshot.setSummaryUnknownCount(unknown);

        ConMonSnapshot saved = snapshotRepository.save(snapshot);

        // Compute reconciliation against the prior (now second-newest) snapshot
        Optional<ConMonSnapshot> prev = findPriorSnapshot(authorization, saved.getId());
        if (prev.isPresent()) {
            ConMonReconciliation rec = reconciliationService.compute(saved, prev.get());
            reconciliationRepository.save(rec);
        }
        return saved;
    }

    /**
     * Returns all snapshots for an authorization with the LAZY {@code authorization}
     * and {@code uploadedBy} associations eagerly initialized so that the controller
     * can safely build response DTOs after the transaction boundary closes.
     */
    @Transactional(readOnly = true)
    public List<ConMonSnapshot> listSnapshots(Authorization authorization) {
        List<ConMonSnapshot> snaps =
                snapshotRepository.findByAuthorizationOrderByUploadedAtDesc(authorization);
        for (ConMonSnapshot s : snaps) {
            if (s.getAuthorization() != null) s.getAuthorization().getId();
            if (s.getUploadedBy() != null) s.getUploadedBy().getUsername();
        }
        return snaps;
    }

    /**
     * Returns a single snapshot by id and authorization with LAZY associations
     * eagerly initialized (authorization, uploadedBy, and the items collection
     * needed by reconciliation / buildDiff). Returns an empty Optional if not found.
     */
    @Transactional(readOnly = true)
    public Optional<ConMonSnapshot> findSnapshotWithAssociations(Long snapshotId,
                                                                  Authorization authorization) {
        return snapshotRepository.findByIdAndAuthorization(snapshotId, authorization)
                .map(s -> {
                    if (s.getAuthorization() != null) s.getAuthorization().getId();
                    if (s.getUploadedBy() != null) s.getUploadedBy().getUsername();
                    // Force-load the items collection so buildDiff / reconciliation
                    // can iterate it after the transaction closes.
                    s.getItems().size();
                    return s;
                });
    }

    public Optional<ConMonReconciliation> findReconciliation(ConMonSnapshot snapshot) {
        return reconciliationRepository.findBySnapshot(snapshot);
    }

    /**
     * Returns the reconciliation for a snapshot with the previousSnapshot's
     * associations eagerly initialized (including its items collection for buildDiff).
     */
    @Transactional(readOnly = true)
    public Optional<ConMonReconciliation> findReconciliationWithAssociations(ConMonSnapshot snapshot) {
        return reconciliationRepository.findBySnapshot(snapshot).map(rec -> {
            ConMonSnapshot prev = rec.getPreviousSnapshot();
            if (prev != null) {
                if (prev.getAuthorization() != null) prev.getAuthorization().getId();
                if (prev.getUploadedBy() != null) prev.getUploadedBy().getUsername();
                prev.getItems().size();
            }
            return rec;
        });
    }

    @Transactional
    public void delete(ConMonSnapshot snapshot) {
        // The blob lives outside the DB — best-effort delete (idempotent).
        try {
            fileStorageService.deleteBinary(snapshot.getFileStoragePath());
        } catch (RuntimeException ignored) {
            // Don't block row deletion on storage hiccups.
        }
        snapshotRepository.delete(snapshot);
    }

    public byte[] downloadOriginal(ConMonSnapshot snapshot) {
        byte[] bytes = fileStorageService.loadBinary(snapshot.getFileStoragePath());
        if (bytes == null) {
            throw new RuntimeException("Original blob missing for snapshot " + snapshot.getId());
        }
        return bytes;
    }

    private Optional<ConMonSnapshot> findPriorSnapshot(Authorization authorization, Long currentId) {
        return snapshotRepository.findByAuthorizationOrderByUploadedAtDesc(authorization).stream()
                .filter(s -> !s.getId().equals(currentId))
                .findFirst();
    }

    private ConMonPoamItem toItem(ParsedPoamItem pi, ConMonSnapshot snapshot) {
        ConMonPoamItem item = new ConMonPoamItem();
        item.setSnapshot(snapshot);
        item.setExternalId(pi.externalId() == null ? UUID.randomUUID().toString() : pi.externalId());
        item.setTitle(pi.title());
        item.setDescription(pi.description());
        item.setStatus(pi.status() == null ? ConMonItemStatus.UNKNOWN : pi.status());
        item.setRawStatus(pi.rawStatus());
        item.setSeverity(pi.severity());
        item.setWeaknessSource(pi.weaknessSource());
        item.setScheduledCompletionDate(pi.scheduledCompletionDate());
        item.setActualCompletionDate(pi.actualCompletionDate());
        item.setPointOfContact(pi.pointOfContact());
        item.setRiskRating(pi.riskRating());
        if (pi.extraProps() != null && !pi.extraProps().isEmpty()) {
            try {
                item.setExtraPropsJson(objectMapper.writeValueAsString(pi.extraProps()));
            } catch (JsonProcessingException ignored) {}
        }
        return item;
    }

    private static String sanitizeFilename(String raw) {
        if (raw == null || raw.isBlank()) return "file";
        String trimmed = raw.replace("\\", "/");
        int slash = trimmed.lastIndexOf('/');
        String basename = slash < 0 ? trimmed : trimmed.substring(slash + 1);
        return basename.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}

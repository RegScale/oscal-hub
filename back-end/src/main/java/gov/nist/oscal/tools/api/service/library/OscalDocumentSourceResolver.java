package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.OscalDocument;
import gov.nist.oscal.tools.api.entity.OscalModelType;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.OscalDocumentRepository;
import gov.nist.oscal.tools.api.service.StorageService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * Loads SSP / AP / AR / POAM rows from the unified {@code oscal_documents}
 * table for Save-to-Library. The four document types share a single resolver
 * because they share storage; the per-type {@link SourceType} is derived from
 * {@link OscalDocument#getModelType()}.
 */
@Component
public class OscalDocumentSourceResolver implements SourceContentResolver {

    private final OscalDocumentRepository repo;
    private final StorageService storage;

    public OscalDocumentSourceResolver(OscalDocumentRepository repo, StorageService storage) {
        this.repo = repo;
        this.storage = storage;
    }

    @Override
    public SourceType supportedType() {
        return SourceType.SSP;
    }

    @Override
    public Set<SourceType> supportedTypes() {
        return Set.of(SourceType.SSP, SourceType.AP, SourceType.AR, SourceType.POAM);
    }

    @Override
    public SourceContent resolve(Long builderRowId, String callerUsername) {
        OscalDocument d = repo.findById(builderRowId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "oscal-document not found: " + builderRowId));

        User creator = d.getCreatedBy();
        if (creator == null || !callerUsername.equals(creator.getUsername())) {
            throw new SecurityException("not your oscal-document");
        }

        String oscalType = oscalTypeFor(d.getModelType());

        String content = storage.downloadComponent(d.getStoragePath());
        byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);

        UUID sourceUuid = UUID.fromString(d.getOscalUuid());

        return new SourceContent(
                bytes,
                "json",
                d.getFilename(),
                oscalType,
                sourceUuid,
                d.getTitle());
    }

    private static String oscalTypeFor(OscalModelType modelType) {
        if (modelType == null) {
            throw new IllegalStateException("oscal-document modelType is null");
        }
        switch (modelType) {
            case SYSTEM_SECURITY_PLAN: return "ssp";
            case ASSESSMENT_PLAN: return "ap";
            case ASSESSMENT_RESULTS: return "ar";
            case PLAN_OF_ACTION_AND_MILESTONES: return "poam";
            default:
                throw new IllegalStateException("unknown model_type " + modelType);
        }
    }
}

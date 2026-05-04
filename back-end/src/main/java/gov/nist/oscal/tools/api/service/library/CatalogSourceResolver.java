package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.Catalog;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.CatalogRepository;
import gov.nist.oscal.tools.api.service.StorageService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Loads a Catalog builder row and its serialized content for Save-to-Library.
 */
@Component
public class CatalogSourceResolver implements SourceContentResolver {

    private final CatalogRepository catalogRepo;
    private final StorageService storage;

    public CatalogSourceResolver(CatalogRepository catalogRepo, StorageService storage) {
        this.catalogRepo = catalogRepo;
        this.storage = storage;
    }

    @Override
    public SourceType supportedType() {
        return SourceType.CATALOG;
    }

    @Override
    public SourceContent resolve(Long builderRowId, String callerUsername) {
        Catalog c = catalogRepo.findById(builderRowId)
                .orElseThrow(() -> new IllegalArgumentException("catalog not found: " + builderRowId));

        User creator = c.getCreatedBy();
        if (creator == null || !callerUsername.equals(creator.getUsername())) {
            throw new SecurityException("not your catalog");
        }

        String content = storage.downloadComponent(c.getStoragePath());
        byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);

        UUID sourceUuid = UUID.fromString(c.getOscalUuid());

        return new SourceContent(
                bytes,
                "json",
                c.getFilename(),
                "catalog",
                sourceUuid,
                c.getTitle());
    }
}

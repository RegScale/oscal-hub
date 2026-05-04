package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.ComponentDefinition;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.ComponentDefinitionRepository;
import gov.nist.oscal.tools.api.service.StorageService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Loads a Component Definition builder row and its serialized content for
 * Save-to-Library.
 */
@Component
public class ComponentDefinitionSourceResolver implements SourceContentResolver {

    private final ComponentDefinitionRepository componentRepo;
    private final StorageService storage;

    public ComponentDefinitionSourceResolver(ComponentDefinitionRepository componentRepo,
                                             StorageService storage) {
        this.componentRepo = componentRepo;
        this.storage = storage;
    }

    @Override
    public SourceType supportedType() {
        return SourceType.COMPONENT_DEFINITION;
    }

    @Override
    public SourceContent resolve(Long builderRowId, String callerUsername) {
        ComponentDefinition cd = componentRepo.findById(builderRowId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "component-definition not found: " + builderRowId));

        User creator = cd.getCreatedBy();
        if (creator == null || !callerUsername.equals(creator.getUsername())) {
            throw new SecurityException("not your component-definition");
        }

        String content = storage.downloadComponent(cd.getStoragePath());
        byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);

        UUID sourceUuid = UUID.fromString(cd.getOscalUuid());

        return new SourceContent(
                bytes,
                "json",
                cd.getFilename(),
                "component-definition",
                sourceUuid,
                cd.getTitle());
    }
}

package gov.nist.oscal.tools.api.service.library;

import gov.nist.oscal.tools.api.entity.Profile;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.library.SourceContent;
import gov.nist.oscal.tools.api.repository.ProfileRepository;
import gov.nist.oscal.tools.api.service.StorageService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Loads a Profile builder row and its serialized content for Save-to-Library.
 */
@Component
public class ProfileSourceResolver implements SourceContentResolver {

    private final ProfileRepository profileRepo;
    private final StorageService storage;

    public ProfileSourceResolver(ProfileRepository profileRepo, StorageService storage) {
        this.profileRepo = profileRepo;
        this.storage = storage;
    }

    @Override
    public SourceType supportedType() {
        return SourceType.PROFILE;
    }

    @Override
    public SourceContent resolve(Long builderRowId, String callerUsername) {
        Profile p = profileRepo.findById(builderRowId)
                .orElseThrow(() -> new IllegalArgumentException("profile not found: " + builderRowId));

        User creator = p.getCreatedBy();
        if (creator == null || !callerUsername.equals(creator.getUsername())) {
            throw new SecurityException("not your profile");
        }

        String content = storage.downloadComponent(p.getStoragePath());
        byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);

        UUID sourceUuid = UUID.fromString(p.getOscalUuid());

        return new SourceContent(
                bytes,
                "json",
                p.getFilename(),
                "profile",
                sourceUuid,
                p.getTitle());
    }
}

package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.model.OscalFormat;
import gov.nist.oscal.tools.api.model.ProfileResolutionRequest;
import gov.nist.oscal.tools.api.model.ProfileResolutionResult;
import dev.metaschema.databind.io.IDeserializer;
import dev.metaschema.oscal.lib.OscalBindingContext;
import dev.metaschema.oscal.lib.model.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ProfileResolutionService {

    private final OscalBindingContext bindingContext;

    public ProfileResolutionService() {
        this.bindingContext = OscalBindingContext.instance();
    }

    public ProfileResolutionResult resolveProfile(ProfileResolutionRequest request, String username) {
        try {
            // Step 1: Validate that the content is a valid profile
            Profile profile = deserializeProfile(request.getProfileContent(), request.getFormat());

            // Step 2: Check if profile has imports
            if (profile.getImports() == null || profile.getImports().isEmpty()) {
                return new ProfileResolutionResult(
                    false,
                    "Profile resolution requires a profile with catalog imports. This profile has no imports."
                );
            }

            // Step 3: Return message indicating external catalog resolution is needed
            int importCount = profile.getImports().size();
            return new ProfileResolutionResult(
                false,
                String.format(
                    "Profile resolution requires external catalog resolution which is not yet implemented. " +
                    "This profile imports %d catalog(s). Full profile resolution will be available in a future update.",
                    importCount
                )
            );

        } catch (Exception e) {
            return new ProfileResolutionResult(false, "Profile resolution failed: " + e.getMessage());
        }
    }

    private Profile deserializeProfile(String content, OscalFormat format) throws IOException {
        IDeserializer<Profile> deserializer = bindingContext.newDeserializer(
            convertFormat(format),
            Profile.class
        );

        // Write content to temporary file (required by OSCAL library)
        Path tempFile = Files.createTempFile("oscal-profile-", getFileExtension(format));
        try {
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            return deserializer.deserialize(tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private dev.metaschema.databind.io.Format convertFormat(OscalFormat format) {
        switch (format) {
            case JSON:
                return dev.metaschema.databind.io.Format.JSON;
            case XML:
                return dev.metaschema.databind.io.Format.XML;
            case YAML:
                return dev.metaschema.databind.io.Format.YAML;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    private String getFileExtension(OscalFormat format) {
        switch (format) {
            case JSON:
                return ".json";
            case XML:
                return ".xml";
            case YAML:
                return ".yaml";
            default:
                return ".txt";
        }
    }
}

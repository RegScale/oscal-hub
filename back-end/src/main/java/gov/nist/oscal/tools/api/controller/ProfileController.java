package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.Profile;
import gov.nist.oscal.tools.api.entity.SourceType;
import gov.nist.oscal.tools.api.model.LibraryItemResponse;
import gov.nist.oscal.tools.api.model.ProfileRequest;
import gov.nist.oscal.tools.api.model.ProfileResponse;
import gov.nist.oscal.tools.api.model.library.SaveToLibraryRequest;
import gov.nist.oscal.tools.api.service.ProfileService;
import gov.nist.oscal.tools.api.service.library.LibraryIngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/build/profiles")
@Tag(name = "Profiles", description = "APIs for managing OSCAL profiles created in the builder")
public class ProfileController {

    private final ProfileService profileService;
    private final LibraryIngestService libraryIngestService;

    @Autowired
    public ProfileController(ProfileService profileService, LibraryIngestService libraryIngestService) {
        this.profileService = profileService;
        this.libraryIngestService = libraryIngestService;
    }

    @Operation(summary = "Create new profile")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Profile created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "409", description = "UUID already exists")
    })
    @PostMapping
    public ResponseEntity<ProfileResponse> create(@Valid @RequestBody ProfileRequest request, Principal principal) {
        try {
            Profile profile = profileService.createProfile(
                    request.getTitle(), request.getDescription(), request.getVersion(),
                    request.getOscalVersion(), request.getFilename(), request.getJsonContent(),
                    request.getOscalUuid(), request.getImportCount(), request.getControlCount(),
                    request.getAlterCount(), request.getDraft(), principal.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ProfileResponse.fromEntity(profile));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Update profile")
    @PutMapping("/{profileId}")
    public ResponseEntity<ProfileResponse> update(
            @PathVariable Long profileId,
            @RequestBody ProfileRequest request,
            Principal principal) {
        try {
            Profile profile = profileService.updateProfile(
                    profileId, request.getTitle(), request.getDescription(), request.getVersion(),
                    request.getJsonContent(), request.getImportCount(), request.getControlCount(),
                    request.getAlterCount(), request.getDraft(), principal.getName());
            return ResponseEntity.ok(ProfileResponse.fromEntity(profile));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Only the creator")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get profile metadata by ID")
    @GetMapping("/{profileId}")
    public ResponseEntity<ProfileResponse> get(@PathVariable Long profileId) {
        try {
            return ResponseEntity.ok(ProfileResponse.fromEntity(profileService.getProfile(profileId)));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get profile by OSCAL UUID")
    @GetMapping("/uuid/{oscalUuid}")
    public ResponseEntity<ProfileResponse> getByUuid(@PathVariable String oscalUuid) {
        try {
            return ResponseEntity.ok(ProfileResponse.fromEntity(profileService.getProfileByUuid(oscalUuid)));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get profile JSON content")
    @GetMapping("/{profileId}/content")
    public ResponseEntity<Map<String, String>> getContent(@PathVariable Long profileId) {
        try {
            return ResponseEntity.ok(Map.of("content", profileService.getProfileContent(profileId)));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "List user's profiles")
    @GetMapping
    public ResponseEntity<List<ProfileResponse>> list(Principal principal) {
        try {
            List<ProfileResponse> resp = profileService.getUserProfiles(principal.getName())
                    .stream().map(ProfileResponse::fromEntity).collect(Collectors.toList());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Search profiles")
    @GetMapping("/search")
    public ResponseEntity<List<ProfileResponse>> search(@RequestParam(required = false) String q, Principal principal) {
        try {
            List<ProfileResponse> resp = profileService.searchProfiles(principal.getName(), q)
                    .stream().map(ProfileResponse::fromEntity).collect(Collectors.toList());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Delete profile (creator only)")
    @DeleteMapping("/{profileId}")
    public ResponseEntity<Void> delete(@PathVariable Long profileId, Principal principal) {
        try {
            profileService.deleteProfile(profileId, principal.getName());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Only the creator")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get profile statistics")
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> stats(Principal principal) {
        try {
            return ResponseEntity.ok(profileService.getStatistics(principal.getName()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(
        summary = "Save this profile to the user's library",
        description = "Idempotent on (creator, source). First call creates a library item linked to the profile; "
                + "subsequent calls append a new version to the existing item."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Profile saved to library"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Not your profile"),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @PostMapping("/{profileId}/save-to-library")
    public ResponseEntity<?> saveToLibrary(
            @PathVariable Long profileId,
            @Valid @RequestBody SaveToLibraryRequest req,
            Principal principal) {
        try {
            LibraryItem saved = libraryIngestService.saveToLibrary(
                    SourceType.PROFILE,
                    profileId,
                    req.getTitle(),
                    req.getDescription(),
                    req.getTags(),
                    req.getVisibility(),
                    req.getOrganizationId(),
                    principal.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(LibraryItemResponse.fromEntity(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}

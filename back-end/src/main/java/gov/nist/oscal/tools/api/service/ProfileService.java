package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Profile;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.ProfileRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing OSCAL profiles created via the builder.
 */
@Service
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StorageService storageService;

    @Transactional
    public Profile createProfile(
            String title, String description, String version, String oscalVersion,
            String filename, String jsonContent, String oscalUuid,
            Integer importCount, Integer controlCount, Integer alterCount,
            Boolean draft, String username) {

        logger.info("Creating profile: {} by user: {}", title, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (oscalUuid == null || oscalUuid.trim().isEmpty()) {
            oscalUuid = UUID.randomUUID().toString();
        }

        if (profileRepository.findByOscalUuid(oscalUuid).isPresent()) {
            throw new RuntimeException("Profile with UUID " + oscalUuid + " already exists");
        }

        String storagePath = storageService.buildPath(username, filename);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("title", title);
        metadata.put("oscalVersion", oscalVersion);
        metadata.put("uploadedBy", username);
        metadata.put("docType", "profile");

        storageService.uploadComponent(username, filename, jsonContent, metadata);

        long fileSize = storageService.getFileSize(storagePath);

        Profile profile = new Profile(oscalUuid, title, storagePath, user);
        profile.setDescription(description);
        profile.setVersion(version);
        profile.setOscalVersion(oscalVersion);
        profile.setFilename(filename);
        profile.setFileSize(fileSize);
        profile.setImportCount(importCount);
        profile.setControlCount(controlCount);
        profile.setAlterCount(alterCount);
        profile.setDraft(Boolean.TRUE.equals(draft));
        profile.setLastUpdatedBy(user);

        profile = profileRepository.save(profile);
        logger.info("Created profile id={} uuid={}", profile.getId(), oscalUuid);
        return profile;
    }

    @Transactional
    public Profile updateProfile(
            Long profileId, String title, String description, String version,
            String jsonContent, Integer importCount, Integer controlCount, Integer alterCount,
            Boolean draft, String username) {

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found: " + profileId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (!profile.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can update this profile");
        }

        if (title != null) profile.setTitle(title);
        if (description != null) profile.setDescription(description);
        if (version != null) profile.setVersion(version);
        if (importCount != null) profile.setImportCount(importCount);
        if (controlCount != null) profile.setControlCount(controlCount);
        if (alterCount != null) profile.setAlterCount(alterCount);
        if (draft != null) profile.setDraft(draft);

        if (jsonContent != null) {
            storageService.uploadComponent(username, profile.getFilename(), jsonContent, null);
            profile.setFileSize(storageService.getFileSize(profile.getStoragePath()));
        }

        profile.setLastUpdatedBy(user);
        return profileRepository.save(profile);
    }

    public Profile getProfile(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found: " + profileId));
    }

    public Profile getProfileByUuid(String oscalUuid) {
        return profileRepository.findByOscalUuid(oscalUuid)
                .orElseThrow(() -> new RuntimeException("Profile not found with UUID: " + oscalUuid));
    }

    public String getProfileContent(Long profileId) {
        Profile profile = getProfile(profileId);
        return storageService.downloadComponent(profile.getStoragePath());
    }

    public List<Profile> getUserProfiles(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        return profileRepository.findByCreatedByOrderByCreatedAtDesc(user);
    }

    public List<Profile> searchProfiles(String username, String searchTerm) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return profileRepository.findByCreatedByOrderByCreatedAtDesc(user);
        }
        return profileRepository.findByCreatedByAndSearch(user, searchTerm);
    }

    @Transactional
    public void deleteProfile(Long profileId, String username) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found: " + profileId));
        if (!profile.getCreatedBy().getUsername().equals(username)) {
            throw new RuntimeException("Only the creator can delete this profile");
        }
        storageService.deleteComponent(profile.getStoragePath());
        profileRepository.delete(profile);
    }

    public Map<String, Object> getStatistics(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        List<Profile> all = profileRepository.findByCreatedBy(user);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProfiles", all.size());
        stats.put("totalImports", all.stream().mapToInt(p -> p.getImportCount() == null ? 0 : p.getImportCount()).sum());
        stats.put("totalControls", all.stream().mapToInt(p -> p.getControlCount() == null ? 0 : p.getControlCount()).sum());
        stats.put("totalStorageBytes", all.stream().mapToLong(p -> p.getFileSize() == null ? 0L : p.getFileSize()).sum());
        Map<String, Long> versions = all.stream()
                .filter(p -> p.getOscalVersion() != null)
                .collect(Collectors.groupingBy(Profile::getOscalVersion, Collectors.counting()));
        stats.put("oscalVersions", versions);
        return stats;
    }
}

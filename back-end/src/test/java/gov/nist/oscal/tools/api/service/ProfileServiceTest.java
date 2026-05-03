package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.Profile;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.ProfileRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private ProfileService profileService;

    private User mockUser;
    private Profile mockProfile;
    private final String testUuid = "660e8400-e29b-41d4-a716-446655440000";
    private final String storagePath = "build/testuser/profile-" + testUuid + ".json";

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        mockProfile = new Profile(testUuid, "Test Profile", storagePath, mockUser);
        mockProfile.setId(1L);
        mockProfile.setDescription("desc");
        mockProfile.setVersion("1.0.0");
        mockProfile.setOscalVersion("1.1.3");
        mockProfile.setFilename("profile-" + testUuid + ".json");
        mockProfile.setFileSize(1024L);
        mockProfile.setImportCount(2);
        mockProfile.setControlCount(50);
        mockProfile.setAlterCount(5);
        mockProfile.setLastUpdatedBy(mockUser);
    }

    @Test
    void create_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByOscalUuid(testUuid)).thenReturn(Optional.empty());
        when(storageService.buildPath("testuser", mockProfile.getFilename())).thenReturn(storagePath);
        when(storageService.getFileSize(storagePath)).thenReturn(1024L);
        when(profileRepository.save(any(Profile.class))).thenReturn(mockProfile);

        Profile result = profileService.createProfile(
                "Test Profile", "desc", "1.0.0", "1.1.3",
                mockProfile.getFilename(), "{\"profile\":{}}", testUuid,
                2, 50, 5, false, "testuser");

        assertNotNull(result);
        verify(storageService).uploadComponent(eq("testuser"), eq(mockProfile.getFilename()), anyString(), any());
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void create_duplicateUuid_throws() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByOscalUuid(testUuid)).thenReturn(Optional.of(mockProfile));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                profileService.createProfile("t", null, null, "1.1.3", "f.json",
                        "{}", testUuid, 0, 0, 0, false, "testuser"));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(profileRepository, never()).save(any());
    }

    @Test
    void create_generatesUuidWhenMissing() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByOscalUuid(anyString())).thenReturn(Optional.empty());
        when(storageService.buildPath(anyString(), anyString())).thenReturn(storagePath);
        when(storageService.getFileSize(anyString())).thenReturn(1024L);
        when(profileRepository.save(any(Profile.class))).thenReturn(mockProfile);

        Profile result = profileService.createProfile(
                "Test", null, null, "1.1.3", "file.json", "{}", null,
                0, 0, 0, false, "testuser");
        assertNotNull(result);
    }

    @Test
    void create_withDraftFlag_marksEntityAsDraft() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByOscalUuid(anyString())).thenReturn(Optional.empty());
        when(storageService.buildPath(anyString(), anyString())).thenReturn(storagePath);
        when(storageService.getFileSize(anyString())).thenReturn(1024L);
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result = profileService.createProfile(
                "Draft Profile", null, null, "1.1.3", "draft.json", "{}", null,
                0, 0, 0, true, "testuser");
        assertTrue(result.isDraft());
    }

    @Test
    void update_byCreator_succeeds() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(mockProfile));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(storageService.getFileSize(anyString())).thenReturn(2048L);
        when(profileRepository.save(any(Profile.class))).thenReturn(mockProfile);

        Profile result = profileService.updateProfile(
                1L, "New", "newdesc", "2.0.0",
                "{\"profile\":{\"u\":1}}", 3, 60, 6, null, "testuser");

        assertNotNull(result);
        verify(storageService).uploadComponent(eq("testuser"), eq(mockProfile.getFilename()), anyString(), any());
    }

    @Test
    void update_notCreator_throwsForbidden() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(mockProfile));
        User other = new User();
        other.setUsername("other");
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                profileService.updateProfile(1L, "New", null, null, null, null, null, null, null, "other"));
        assertTrue(ex.getMessage().contains("Only the creator"));
    }

    @Test
    void update_partialUpdate_skipsNullFields() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(mockProfile));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(profileRepository.save(any(Profile.class))).thenReturn(mockProfile);

        profileService.updateProfile(1L, null, null, null, null, null, null, null, null, "testuser");

        verify(storageService, never()).uploadComponent(anyString(), anyString(), anyString(), any());
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void update_promoteDraftToFinal() {
        mockProfile.setDraft(true);
        when(profileRepository.findById(1L)).thenReturn(Optional.of(mockProfile));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result = profileService.updateProfile(
                1L, null, null, null, null, null, null, null, false, "testuser");

        assertFalse(result.isDraft());
    }

    @Test
    void getProfile_missing_throws() {
        when(profileRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> profileService.getProfile(99L));
    }

    @Test
    void getProfileContent_downloads() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(mockProfile));
        when(storageService.downloadComponent(storagePath)).thenReturn("{}");

        assertEquals("{}", profileService.getProfileContent(1L));
    }

    @Test
    void getUserProfiles_returnsList() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByCreatedByOrderByCreatedAtDesc(mockUser))
                .thenReturn(List.of(mockProfile));

        assertEquals(1, profileService.getUserProfiles("testuser").size());
    }

    @Test
    void searchProfiles_emptyTerm_returnsAll() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByCreatedByOrderByCreatedAtDesc(mockUser))
                .thenReturn(List.of(mockProfile));

        assertEquals(1, profileService.searchProfiles("testuser", null).size());
    }

    @Test
    void searchProfiles_withTerm_callsSearch() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByCreatedByAndSearch(mockUser, "moderate"))
                .thenReturn(List.of(mockProfile));

        assertEquals(1, profileService.searchProfiles("testuser", "moderate").size());
    }

    @Test
    void deleteProfile_byCreator_succeeds() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(mockProfile));

        profileService.deleteProfile(1L, "testuser");

        verify(storageService).deleteComponent(storagePath);
        verify(profileRepository).delete(mockProfile);
    }

    @Test
    void deleteProfile_notCreator_throws() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(mockProfile));

        assertThrows(RuntimeException.class, () -> profileService.deleteProfile(1L, "other"));
        verify(profileRepository, never()).delete(any());
    }

    @Test
    void getStatistics_aggregatesCounts() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByCreatedBy(mockUser)).thenReturn(List.of(mockProfile));

        Map<String, Object> stats = profileService.getStatistics("testuser");
        assertEquals(1, stats.get("totalProfiles"));
        assertEquals(2, stats.get("totalImports"));
        assertEquals(50, stats.get("totalControls"));
        assertEquals(1024L, stats.get("totalStorageBytes"));
    }

    @Test
    void getStatistics_handlesEmpty() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(profileRepository.findByCreatedBy(mockUser)).thenReturn(Collections.emptyList());

        Map<String, Object> stats = profileService.getStatistics("testuser");
        assertEquals(0, stats.get("totalProfiles"));
    }
}

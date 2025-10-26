package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.ComponentDefinition;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.ComponentDefinitionRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
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
class ComponentDefinitionServiceTest {

    @Mock
    private ComponentDefinitionRepository componentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AzureBlobService blobService;

    @InjectMocks
    private ComponentDefinitionService componentDefinitionService;

    private User mockUser;
    private ComponentDefinition mockComponent;
    private String testUuid = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        mockComponent = new ComponentDefinition(testUuid, "Test Component", "azure/blob/path", mockUser);
        mockComponent.setId(1L);
        mockComponent.setDescription("Test description");
        mockComponent.setVersion("1.0.0");
        mockComponent.setOscalVersion("1.0.4");
        mockComponent.setFilename("component.json");
        mockComponent.setFileSize(1024L);
        mockComponent.setComponentCount(5);
        mockComponent.setCapabilityCount(10);
        mockComponent.setControlCount(20);
        mockComponent.setLastUpdatedBy(mockUser);
    }

    @Test
    void testCreateComponentDefinition_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(componentRepository.findByOscalUuid(testUuid)).thenReturn(Optional.empty());
        when(blobService.buildBlobPath("testuser", "component.json")).thenReturn("azure/blob/path");
        when(blobService.getFileSize("azure/blob/path")).thenReturn(1024L);
        when(componentRepository.save(any(ComponentDefinition.class))).thenReturn(mockComponent);

        ComponentDefinition result = componentDefinitionService.createComponentDefinition(
            "Test Component",
            "Test description",
            "1.0.0",
            "1.0.4",
            "component.json",
            "{\"component-definition\": {}}",
            testUuid,
            5,
            10,
            20,
            "testuser"
        );

        assertNotNull(result);
        assertEquals("Test Component", result.getTitle());
        assertEquals(testUuid, result.getOscalUuid());

        verify(userRepository).findByUsername("testuser");
        verify(componentRepository).findByOscalUuid(testUuid);
        verify(blobService).uploadComponent(eq("testuser"), eq("component.json"), anyString(), any());
        verify(componentRepository).save(any(ComponentDefinition.class));
    }

    @Test
    void testCreateComponentDefinition_generatesUuidWhenNotProvided() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(componentRepository.findByOscalUuid(anyString())).thenReturn(Optional.empty());
        when(blobService.buildBlobPath(anyString(), anyString())).thenReturn("azure/blob/path");
        when(blobService.getFileSize(anyString())).thenReturn(1024L);
        when(componentRepository.save(any(ComponentDefinition.class))).thenReturn(mockComponent);

        ComponentDefinition result = componentDefinitionService.createComponentDefinition(
            "Test Component",
            "Test description",
            "1.0.0",
            "1.0.4",
            "component.json",
            "{}",
            null,  // UUID is null
            5,
            10,
            20,
            "testuser"
        );

        assertNotNull(result);
        verify(componentRepository).save(any(ComponentDefinition.class));
    }

    @Test
    void testCreateComponentDefinition_userNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            componentDefinitionService.createComponentDefinition(
                "Test",
                "Desc",
                "1.0",
                "1.0.4",
                "file.json",
                "{}",
                testUuid,
                5,
                10,
                20,
                "nonexistent"
            );
        });

        verify(userRepository).findByUsername("nonexistent");
        verify(componentRepository, never()).save(any());
    }

    @Test
    void testCreateComponentDefinition_duplicateUuid() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(componentRepository.findByOscalUuid(testUuid)).thenReturn(Optional.of(mockComponent));

        assertThrows(RuntimeException.class, () -> {
            componentDefinitionService.createComponentDefinition(
                "Test",
                "Desc",
                "1.0",
                "1.0.4",
                "file.json",
                "{}",
                testUuid,
                5,
                10,
                20,
                "testuser"
            );
        });

        verify(componentRepository).findByOscalUuid(testUuid);
        verify(componentRepository, never()).save(any());
    }

    @Test
    void testUpdateComponentDefinition_success() {
        when(componentRepository.findById(1L)).thenReturn(Optional.of(mockComponent));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(blobService.getFileSize(anyString())).thenReturn(2048L);
        when(componentRepository.save(any(ComponentDefinition.class))).thenReturn(mockComponent);

        ComponentDefinition result = componentDefinitionService.updateComponentDefinition(
            1L,
            "Updated Title",
            "Updated description",
            "2.0.0",
            "{\"updated\": true}",
            6,
            12,
            25,
            "testuser"
        );

        assertNotNull(result);
        verify(componentRepository).findById(1L);
        verify(blobService).uploadComponent(eq("testuser"), eq("component.json"), anyString(), isNull());
        verify(componentRepository).save(any(ComponentDefinition.class));
    }

    @Test
    void testUpdateComponentDefinition_partialUpdate() {
        when(componentRepository.findById(1L)).thenReturn(Optional.of(mockComponent));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(componentRepository.save(any(ComponentDefinition.class))).thenReturn(mockComponent);

        // Only update title, leave other fields null
        ComponentDefinition result = componentDefinitionService.updateComponentDefinition(
            1L,
            "New Title",
            null,
            null,
            null,
            null,
            null,
            null,
            "testuser"
        );

        assertNotNull(result);
        verify(componentRepository).save(any(ComponentDefinition.class));
        verify(blobService, never()).uploadComponent(anyString(), anyString(), anyString(), any());
    }

    @Test
    void testUpdateComponentDefinition_notFound() {
        when(componentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            componentDefinitionService.updateComponentDefinition(
                999L, "Title", "Desc", "1.0", "{}", 5, 10, 20, "testuser"
            );
        });

        verify(componentRepository).findById(999L);
        verify(componentRepository, never()).save(any());
    }

    @Test
    void testUpdateComponentDefinition_notCreator() {
        when(componentRepository.findById(1L)).thenReturn(Optional.of(mockComponent));
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(mockUser));

        assertThrows(RuntimeException.class, () -> {
            componentDefinitionService.updateComponentDefinition(
                1L, "Title", "Desc", "1.0", "{}", 5, 10, 20, "otheruser"
            );
        });

        verify(componentRepository, never()).save(any());
    }

    @Test
    void testGetComponentDefinition_success() {
        when(componentRepository.findById(1L)).thenReturn(Optional.of(mockComponent));

        ComponentDefinition result = componentDefinitionService.getComponentDefinition(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Component", result.getTitle());

        verify(componentRepository).findById(1L);
    }

    @Test
    void testGetComponentDefinition_notFound() {
        when(componentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            componentDefinitionService.getComponentDefinition(999L);
        });

        verify(componentRepository).findById(999L);
    }

    @Test
    void testGetComponentDefinitionByUuid_success() {
        when(componentRepository.findByOscalUuid(testUuid)).thenReturn(Optional.of(mockComponent));

        ComponentDefinition result = componentDefinitionService.getComponentDefinitionByUuid(testUuid);

        assertNotNull(result);
        assertEquals(testUuid, result.getOscalUuid());

        verify(componentRepository).findByOscalUuid(testUuid);
    }

    @Test
    void testGetComponentDefinitionByUuid_notFound() {
        when(componentRepository.findByOscalUuid("nonexistent-uuid")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            componentDefinitionService.getComponentDefinitionByUuid("nonexistent-uuid");
        });

        verify(componentRepository).findByOscalUuid("nonexistent-uuid");
    }

    @Test
    void testGetComponentContent_success() {
        when(componentRepository.findById(1L)).thenReturn(Optional.of(mockComponent));
        when(blobService.downloadComponent("azure/blob/path")).thenReturn("{\"component-definition\": {}}");

        String result = componentDefinitionService.getComponentContent(1L);

        assertNotNull(result);
        assertTrue(result.contains("component-definition"));

        verify(componentRepository).findById(1L);
        verify(blobService).downloadComponent("azure/blob/path");
    }

    @Test
    void testGetUserComponents_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(componentRepository.findByCreatedBy(mockUser)).thenReturn(Arrays.asList(mockComponent));

        List<ComponentDefinition> results = componentDefinitionService.getUserComponents("testuser");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Test Component", results.get(0).getTitle());

        verify(userRepository).findByUsername("testuser");
        verify(componentRepository).findByCreatedBy(mockUser);
    }

    @Test
    void testGetUserComponents_userNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            componentDefinitionService.getUserComponents("nonexistent");
        });

        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void testGetRecentComponents_success() {
        ComponentDefinition component1 = new ComponentDefinition("uuid1", "Component 1", "path1", mockUser);
        ComponentDefinition component2 = new ComponentDefinition("uuid2", "Component 2", "path2", mockUser);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(componentRepository.findByCreatedByOrderByCreatedAtDesc(mockUser))
            .thenReturn(Arrays.asList(component2, component1));

        List<ComponentDefinition> results = componentDefinitionService.getRecentComponents("testuser", 2);

        assertNotNull(results);
        assertEquals(2, results.size());

        verify(componentRepository).findByCreatedByOrderByCreatedAtDesc(mockUser);
    }

    @Test
    void testGetRecentComponents_limitExceedsTotal() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(componentRepository.findByCreatedByOrderByCreatedAtDesc(mockUser))
            .thenReturn(Arrays.asList(mockComponent));

        List<ComponentDefinition> results = componentDefinitionService.getRecentComponents("testuser", 10);

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testSearchComponents_withSearchTerm() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(componentRepository.findByCreatedByAndSearch(mockUser, "test"))
            .thenReturn(Arrays.asList(mockComponent));

        List<ComponentDefinition> results = componentDefinitionService.searchComponents("testuser", "test");

        assertNotNull(results);
        assertEquals(1, results.size());

        verify(componentRepository).findByCreatedByAndSearch(mockUser, "test");
    }

    @Test
    void testSearchComponents_nullSearchTerm() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(componentRepository.findByCreatedBy(mockUser)).thenReturn(Arrays.asList(mockComponent));

        List<ComponentDefinition> results = componentDefinitionService.searchComponents("testuser", null);

        assertNotNull(results);
        verify(componentRepository).findByCreatedBy(mockUser);
        verify(componentRepository, never()).findByCreatedByAndSearch(any(), anyString());
    }

    @Test
    void testSearchComponents_emptySearchTerm() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(componentRepository.findByCreatedBy(mockUser)).thenReturn(Arrays.asList(mockComponent));

        List<ComponentDefinition> results = componentDefinitionService.searchComponents("testuser", "  ");

        assertNotNull(results);
        verify(componentRepository).findByCreatedBy(mockUser);
        verify(componentRepository, never()).findByCreatedByAndSearch(any(), anyString());
    }

    @Test
    void testDeleteComponentDefinition_success() {
        when(componentRepository.findById(1L)).thenReturn(Optional.of(mockComponent));

        componentDefinitionService.deleteComponentDefinition(1L, "testuser");

        verify(componentRepository).findById(1L);
        verify(blobService).deleteComponent("azure/blob/path");
        verify(componentRepository).delete(mockComponent);
    }

    @Test
    void testDeleteComponentDefinition_notFound() {
        when(componentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            componentDefinitionService.deleteComponentDefinition(999L, "testuser");
        });

        verify(componentRepository).findById(999L);
        verify(componentRepository, never()).delete(any());
    }

    @Test
    void testDeleteComponentDefinition_notCreator() {
        when(componentRepository.findById(1L)).thenReturn(Optional.of(mockComponent));

        assertThrows(RuntimeException.class, () -> {
            componentDefinitionService.deleteComponentDefinition(1L, "otheruser");
        });

        verify(componentRepository).findById(1L);
        verify(componentRepository, never()).delete(any());
    }

    @Test
    void testGetComponentStatistics_success() {
        ComponentDefinition component2 = new ComponentDefinition("uuid2", "Component 2", "path2", mockUser);
        component2.setOscalVersion("1.0.4");
        component2.setComponentCount(3);
        component2.setControlCount(15);
        component2.setFileSize(512L);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(componentRepository.findByCreatedBy(mockUser))
            .thenReturn(Arrays.asList(mockComponent, component2));
        when(componentRepository.findByCreatedByOrderByCreatedAtDesc(mockUser))
            .thenReturn(Arrays.asList(mockComponent, component2));

        Map<String, Object> stats = componentDefinitionService.getComponentStatistics("testuser");

        assertNotNull(stats);
        assertTrue(stats.containsKey("totalComponents"));
        assertTrue(stats.containsKey("totalControls"));
        assertTrue(stats.containsKey("totalComponentCount"));
        assertTrue(stats.containsKey("totalStorageBytes"));
        assertTrue(stats.containsKey("oscalVersions"));
        assertTrue(stats.containsKey("recentComponents"));

        assertEquals(2, stats.get("totalComponents"));
        assertEquals(35, stats.get("totalControls")); // 20 + 15
        assertEquals(8, stats.get("totalComponentCount")); // 5 + 3
        assertEquals(1536L, stats.get("totalStorageBytes")); // 1024 + 512

        @SuppressWarnings("unchecked")
        Map<String, Long> versions = (Map<String, Long>) stats.get("oscalVersions");
        assertNotNull(versions);
        assertEquals(2L, versions.get("1.0.4"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recent = (List<Map<String, Object>>) stats.get("recentComponents");
        assertNotNull(recent);
        assertEquals(2, recent.size());

        verify(componentRepository).findByCreatedBy(mockUser);
    }

    @Test
    void testGetComponentStatistics_withNullCounts() {
        ComponentDefinition componentWithNulls = new ComponentDefinition("uuid3", "Null Component", "path3", mockUser);
        componentWithNulls.setComponentCount(null);
        componentWithNulls.setControlCount(null);
        componentWithNulls.setFileSize(null);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(componentRepository.findByCreatedBy(mockUser))
            .thenReturn(Arrays.asList(componentWithNulls));
        when(componentRepository.findByCreatedByOrderByCreatedAtDesc(mockUser))
            .thenReturn(Arrays.asList(componentWithNulls));

        Map<String, Object> stats = componentDefinitionService.getComponentStatistics("testuser");

        assertNotNull(stats);
        assertEquals(0, stats.get("totalControls"));
        assertEquals(0, stats.get("totalComponentCount"));
        assertEquals(0L, stats.get("totalStorageBytes"));
    }

    @Test
    void testComponentExists_true() {
        when(componentRepository.existsById(1L)).thenReturn(true);

        boolean result = componentDefinitionService.componentExists(1L);

        assertTrue(result);
        verify(componentRepository).existsById(1L);
    }

    @Test
    void testComponentExists_false() {
        when(componentRepository.existsById(999L)).thenReturn(false);

        boolean result = componentDefinitionService.componentExists(999L);

        assertFalse(result);
        verify(componentRepository).existsById(999L);
    }

    @Test
    void testComponentExistsByUuid_true() {
        when(componentRepository.findByOscalUuid(testUuid)).thenReturn(Optional.of(mockComponent));

        boolean result = componentDefinitionService.componentExistsByUuid(testUuid);

        assertTrue(result);
        verify(componentRepository).findByOscalUuid(testUuid);
    }

    @Test
    void testComponentExistsByUuid_false() {
        when(componentRepository.findByOscalUuid("nonexistent-uuid")).thenReturn(Optional.empty());

        boolean result = componentDefinitionService.componentExistsByUuid("nonexistent-uuid");

        assertFalse(result);
        verify(componentRepository).findByOscalUuid("nonexistent-uuid");
    }
}

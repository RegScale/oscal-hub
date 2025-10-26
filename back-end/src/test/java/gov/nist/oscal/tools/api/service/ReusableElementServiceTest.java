package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.ReusableElement;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.ReusableElementRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReusableElementServiceTest {

    @Mock
    private ReusableElementRepository elementRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReusableElementService reusableElementService;

    private User mockUser;
    private ReusableElement mockElement;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        mockElement = new ReusableElement(
            ReusableElement.ElementType.ROLE,
            "Test Role",
            "{\"role_id\": \"admin\"}",
            mockUser
        );
        mockElement.setId(1L);
        mockElement.setDescription("Test role description");
        mockElement.setShared(false);
        mockElement.setUseCount(5);
    }

    @Test
    void testCreateElement_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(elementRepository.save(any(ReusableElement.class))).thenReturn(mockElement);

        ReusableElement result = reusableElementService.createElement(
            ReusableElement.ElementType.ROLE,
            "Test Role",
            "{\"role_id\": \"admin\"}",
            "Test description",
            false,
            "testuser"
        );

        assertNotNull(result);
        assertEquals("Test Role", result.getName());
        assertEquals(ReusableElement.ElementType.ROLE, result.getType());

        verify(userRepository).findByUsername("testuser");
        verify(elementRepository).save(any(ReusableElement.class));
    }

    @Test
    void testCreateElement_userNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            reusableElementService.createElement(
                ReusableElement.ElementType.ROLE,
                "Test",
                "{}",
                "Description",
                false,
                "nonexistent"
            );
        });

        verify(userRepository).findByUsername("nonexistent");
        verify(elementRepository, never()).save(any());
    }

    @Test
    void testUpdateElement_success() {
        when(elementRepository.findById(1L)).thenReturn(Optional.of(mockElement));
        when(elementRepository.save(any(ReusableElement.class))).thenReturn(mockElement);

        ReusableElement result = reusableElementService.updateElement(
            1L,
            "Updated Name",
            "{\"updated\": true}",
            "Updated description",
            true,
            "testuser"
        );

        assertNotNull(result);
        verify(elementRepository).findById(1L);
        verify(elementRepository).save(any(ReusableElement.class));
    }

    @Test
    void testUpdateElement_elementNotFound() {
        when(elementRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            reusableElementService.updateElement(999L, "Name", "{}", "Desc", false, "testuser");
        });

        verify(elementRepository).findById(999L);
        verify(elementRepository, never()).save(any());
    }

    @Test
    void testUpdateElement_notCreator() {
        when(elementRepository.findById(1L)).thenReturn(Optional.of(mockElement));

        assertThrows(RuntimeException.class, () -> {
            reusableElementService.updateElement(1L, "Name", "{}", "Desc", false, "otheruser");
        });

        verify(elementRepository).findById(1L);
        verify(elementRepository, never()).save(any());
    }

    @Test
    void testUpdateElement_partialUpdate() {
        when(elementRepository.findById(1L)).thenReturn(Optional.of(mockElement));
        when(elementRepository.save(any(ReusableElement.class))).thenReturn(mockElement);

        // Only update name, leave other fields as null
        ReusableElement result = reusableElementService.updateElement(
            1L,
            "New Name",
            null,
            null,
            null,
            "testuser"
        );

        assertNotNull(result);
        verify(elementRepository).save(any(ReusableElement.class));
    }

    @Test
    void testGetElement_success() {
        when(elementRepository.findById(1L)).thenReturn(Optional.of(mockElement));

        ReusableElement result = reusableElementService.getElement(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Role", result.getName());

        verify(elementRepository).findById(1L);
    }

    @Test
    void testGetElement_notFound() {
        when(elementRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            reusableElementService.getElement(999L);
        });

        verify(elementRepository).findById(999L);
    }

    @Test
    void testGetUserElements_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(elementRepository.findByCreatedBy(mockUser)).thenReturn(Arrays.asList(mockElement));

        List<ReusableElement> results = reusableElementService.getUserElements("testuser");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Test Role", results.get(0).getName());

        verify(userRepository).findByUsername("testuser");
        verify(elementRepository).findByCreatedBy(mockUser);
    }

    @Test
    void testGetUserElements_userNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            reusableElementService.getUserElements("nonexistent");
        });

        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void testGetUserElementsByType_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(elementRepository.findByCreatedByAndType(mockUser, ReusableElement.ElementType.ROLE))
            .thenReturn(Arrays.asList(mockElement));

        List<ReusableElement> results = reusableElementService.getUserElementsByType(
            "testuser",
            ReusableElement.ElementType.ROLE
        );

        assertNotNull(results);
        assertEquals(1, results.size());

        verify(elementRepository).findByCreatedByAndType(mockUser, ReusableElement.ElementType.ROLE);
    }

    @Test
    void testSearchElements_withSearchTerm() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(elementRepository.searchByUserNameAndType(mockUser, "role", ReusableElement.ElementType.ROLE))
            .thenReturn(Arrays.asList(mockElement));

        List<ReusableElement> results = reusableElementService.searchElements(
            "testuser",
            "role",
            ReusableElement.ElementType.ROLE
        );

        assertNotNull(results);
        assertEquals(1, results.size());

        verify(elementRepository).searchByUserNameAndType(mockUser, "role", ReusableElement.ElementType.ROLE);
    }

    @Test
    void testSearchElements_nullSearchTerm_withType() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(elementRepository.findByCreatedByAndType(mockUser, ReusableElement.ElementType.ROLE))
            .thenReturn(Arrays.asList(mockElement));

        List<ReusableElement> results = reusableElementService.searchElements(
            "testuser",
            null,
            ReusableElement.ElementType.ROLE
        );

        assertNotNull(results);
        verify(elementRepository).findByCreatedByAndType(mockUser, ReusableElement.ElementType.ROLE);
        verify(elementRepository, never()).searchByUserNameAndType(any(), any(), any());
    }

    @Test
    void testSearchElements_emptySearchTerm_nullType() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(elementRepository.findByCreatedBy(mockUser)).thenReturn(Arrays.asList(mockElement));

        List<ReusableElement> results = reusableElementService.searchElements(
            "testuser",
            "  ",
            null
        );

        assertNotNull(results);
        verify(elementRepository).findByCreatedBy(mockUser);
        verify(elementRepository, never()).searchByUserNameAndType(any(), any(), any());
    }

    @Test
    void testDeleteElement_success() {
        when(elementRepository.findById(1L)).thenReturn(Optional.of(mockElement));
        doNothing().when(elementRepository).delete(mockElement);

        reusableElementService.deleteElement(1L, "testuser");

        verify(elementRepository).findById(1L);
        verify(elementRepository).delete(mockElement);
    }

    @Test
    void testDeleteElement_notFound() {
        when(elementRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            reusableElementService.deleteElement(999L, "testuser");
        });

        verify(elementRepository).findById(999L);
        verify(elementRepository, never()).delete(any());
    }

    @Test
    void testDeleteElement_notCreator() {
        when(elementRepository.findById(1L)).thenReturn(Optional.of(mockElement));

        assertThrows(RuntimeException.class, () -> {
            reusableElementService.deleteElement(1L, "otheruser");
        });

        verify(elementRepository).findById(1L);
        verify(elementRepository, never()).delete(any());
    }

    @Test
    void testIncrementUseCount_success() {
        when(elementRepository.findById(1L)).thenReturn(Optional.of(mockElement));
        when(elementRepository.save(any(ReusableElement.class))).thenReturn(mockElement);

        reusableElementService.incrementUseCount(1L);

        verify(elementRepository).findById(1L);
        verify(elementRepository).save(any(ReusableElement.class));
    }

    @Test
    void testIncrementUseCount_notFound() {
        when(elementRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            reusableElementService.incrementUseCount(999L);
        });

        verify(elementRepository).findById(999L);
        verify(elementRepository, never()).save(any());
    }

    @Test
    void testGetRecentElements_success() {
        ReusableElement element1 = new ReusableElement(
            ReusableElement.ElementType.ROLE,
            "Element 1",
            "{}",
            mockUser
        );
        ReusableElement element2 = new ReusableElement(
            ReusableElement.ElementType.PARTY,
            "Element 2",
            "{}",
            mockUser
        );

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(elementRepository.findByCreatedByOrderByCreatedAtDesc(mockUser))
            .thenReturn(Arrays.asList(element2, element1));

        List<ReusableElement> results = reusableElementService.getRecentElements("testuser", 2);

        assertNotNull(results);
        assertEquals(2, results.size());

        verify(elementRepository).findByCreatedByOrderByCreatedAtDesc(mockUser);
    }

    @Test
    void testGetRecentElements_limitExceedsTotal() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(elementRepository.findByCreatedByOrderByCreatedAtDesc(mockUser))
            .thenReturn(Arrays.asList(mockElement));

        List<ReusableElement> results = reusableElementService.getRecentElements("testuser", 10);

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGetMostUsedElements_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(elementRepository.findByCreatedByOrderByUseCountDesc(mockUser))
            .thenReturn(Arrays.asList(mockElement));

        List<ReusableElement> results = reusableElementService.getMostUsedElements("testuser", 5);

        assertNotNull(results);
        assertEquals(1, results.size());

        verify(elementRepository).findByCreatedByOrderByUseCountDesc(mockUser);
    }

    @Test
    void testGetElementStatistics_success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(elementRepository.findByCreatedBy(mockUser)).thenReturn(Arrays.asList(mockElement));
        when(elementRepository.countByCreatedByAndType(eq(mockUser), any(ReusableElement.ElementType.class)))
            .thenReturn(1L);
        when(elementRepository.findByCreatedByOrderByUseCountDesc(mockUser))
            .thenReturn(Arrays.asList(mockElement));

        Map<String, Object> stats = reusableElementService.getElementStatistics("testuser");

        assertNotNull(stats);
        assertTrue(stats.containsKey("totalElements"));
        assertTrue(stats.containsKey("countByType"));
        assertTrue(stats.containsKey("totalUses"));
        assertTrue(stats.containsKey("mostUsed"));

        assertEquals(1, stats.get("totalElements"));
        assertEquals(5, stats.get("totalUses"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mostUsed = (List<Map<String, Object>>) stats.get("mostUsed");
        assertNotNull(mostUsed);
        assertEquals(1, mostUsed.size());

        verify(elementRepository).findByCreatedBy(mockUser);
    }

    @Test
    void testGetSharedElements_withType() {
        ReusableElement sharedElement = new ReusableElement(
            ReusableElement.ElementType.ROLE,
            "Shared Role",
            "{}",
            mockUser
        );
        sharedElement.setShared(true);

        when(elementRepository.findByTypeAndIsShared(ReusableElement.ElementType.ROLE, true))
            .thenReturn(Arrays.asList(sharedElement));

        List<ReusableElement> results = reusableElementService.getSharedElements(
            ReusableElement.ElementType.ROLE
        );

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).isShared());

        verify(elementRepository).findByTypeAndIsShared(ReusableElement.ElementType.ROLE, true);
    }

    @Test
    void testGetSharedElements_nullType() {
        ReusableElement sharedElement = new ReusableElement(
            ReusableElement.ElementType.ROLE,
            "Shared Role",
            "{}",
            mockUser
        );
        sharedElement.setShared(true);

        ReusableElement privateElement = new ReusableElement(
            ReusableElement.ElementType.PARTY,
            "Private Party",
            "{}",
            mockUser
        );
        privateElement.setShared(false);

        when(elementRepository.findAll())
            .thenReturn(Arrays.asList(sharedElement, privateElement));

        List<ReusableElement> results = reusableElementService.getSharedElements(null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).isShared());

        verify(elementRepository).findAll();
    }
}

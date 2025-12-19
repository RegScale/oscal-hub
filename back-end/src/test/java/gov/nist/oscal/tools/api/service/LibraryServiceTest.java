/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryTag;
import gov.nist.oscal.tools.api.entity.LibraryVersion;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import gov.nist.oscal.tools.api.repository.LibraryTagRepository;
import gov.nist.oscal.tools.api.repository.LibraryVersionRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    @Mock
    private LibraryItemRepository libraryItemRepository;

    @Mock
    private LibraryVersionRepository libraryVersionRepository;

    @Mock
    private LibraryTagRepository libraryTagRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LibraryStorageService storageService;

    @InjectMocks
    private LibraryService libraryService;

    private User testUser;
    private LibraryItem testItem;
    private LibraryVersion testVersion;
    private LibraryTag testTag;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testTag = new LibraryTag("security");
        testTag.setId(1L);

        testItem = new LibraryItem("item-123", "Test Catalog", "Test description", "catalog", testUser);
        testItem.setId(1L);

        testVersion = new LibraryVersion("version-123", testItem, 1, "catalog.xml", "XML", 1024L,
                "/library/item-123/version-123/catalog.xml", testUser, "Initial version");
        testVersion.setId(1L);

        testItem.setCurrentVersion(testVersion);
    }

    // ========== CREATE LIBRARY ITEM TESTS ==========

    @Test
    void testCreateLibraryItem_success() {
        // Given
        Set<String> tagNames = Set.of("security", "nist");
        String fileContent = "<?xml version=\"1.0\"?><catalog></catalog>";

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(libraryTagRepository.findByName("security")).thenReturn(Optional.of(testTag));
        when(libraryTagRepository.findByName("nist")).thenReturn(Optional.empty());
        when(libraryTagRepository.save(any(LibraryTag.class))).thenAnswer(invocation -> {
            LibraryTag tag = invocation.getArgument(0);
            tag.setId(2L);
            return tag;
        });
        when(libraryItemRepository.save(any(LibraryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(libraryVersionRepository.save(any(LibraryVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storageService.buildBlobPath(anyString(), anyString(), anyString())).thenReturn("/library/test/path");
        when(storageService.saveLibraryFile(anyString(), anyString(), anyMap())).thenReturn(true);

        // When
        LibraryItem result = libraryService.createLibraryItem(
                "Test Catalog", "Test description", "catalog",
                "catalog.xml", "XML", fileContent, tagNames, "testuser"
        );

        // Then
        assertNotNull(result);
        assertEquals("Test Catalog", result.getTitle());
        assertEquals("Test description", result.getDescription());
        assertEquals("catalog", result.getOscalType());
        assertEquals(testUser, result.getCreatedBy());
        assertNotNull(result.getCurrentVersion());

        verify(userRepository).findByUsername("testuser");
        verify(libraryItemRepository, times(2)).save(any(LibraryItem.class));
        verify(libraryVersionRepository).save(any(LibraryVersion.class));
        verify(storageService).saveLibraryFile(eq(fileContent), anyString(), anyMap());
    }

    @Test
    void testCreateLibraryItem_userNotFound() {
        // Given
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            libraryService.createLibraryItem(
                    "Test", "Description", "catalog",
                    "file.xml", "XML", "content", null, "nonexistent"
            );
        });

        verify(libraryItemRepository, never()).save(any(LibraryItem.class));
    }

    @Test
    void testCreateLibraryItem_withoutTags() {
        // Given
        String fileContent = "<?xml version=\"1.0\"?><catalog></catalog>";

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(libraryItemRepository.save(any(LibraryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(libraryVersionRepository.save(any(LibraryVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storageService.buildBlobPath(anyString(), anyString(), anyString())).thenReturn("/library/test/path");
        when(storageService.saveLibraryFile(anyString(), anyString(), anyMap())).thenReturn(true);

        // When
        LibraryItem result = libraryService.createLibraryItem(
                "Test Catalog", "Test description", "catalog",
                "catalog.xml", "XML", fileContent, null, "testuser"
        );

        // Then
        assertNotNull(result);
        assertTrue(result.getTags().isEmpty());
        verify(libraryTagRepository, never()).findByName(anyString());
    }

    // ========== UPDATE LIBRARY ITEM TESTS ==========

    @Test
    void testUpdateLibraryItem_success() {
        // Given
        when(libraryItemRepository.findByItemId("item-123")).thenReturn(Optional.of(testItem));
        when(libraryItemRepository.save(any(LibraryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(libraryTagRepository.findByName(anyString())).thenReturn(Optional.of(testTag));

        // When
        LibraryItem result = libraryService.updateLibraryItem(
                "item-123", "Updated Title", "Updated description",
                Set.of("security"), "testuser"
        );

        // Then
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated description", result.getDescription());
        verify(libraryItemRepository).save(testItem);
    }

    @Test
    void testUpdateLibraryItem_notFound() {
        // Given
        when(libraryItemRepository.findByItemId("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            libraryService.updateLibraryItem("nonexistent", "Title", "Description", null, "testuser");
        });
    }

    @Test
    void testUpdateLibraryItem_partialUpdate() {
        // Given
        when(libraryItemRepository.findByItemId("item-123")).thenReturn(Optional.of(testItem));
        when(libraryItemRepository.save(any(LibraryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When - only update title
        LibraryItem result = libraryService.updateLibraryItem("item-123", "New Title", null, null, "testuser");

        // Then
        assertEquals("New Title", result.getTitle());
        assertEquals("Test description", result.getDescription()); // Original description preserved
    }

    // ========== ADD VERSION TESTS ==========

    @Test
    void testAddVersion_success() {
        // Given
        String fileContent = "<?xml version=\"1.0\"?><catalog v2></catalog>";

        when(libraryItemRepository.findByItemId("item-123")).thenReturn(Optional.of(testItem));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(libraryVersionRepository.getNextVersionNumber(testItem)).thenReturn(2);
        when(libraryVersionRepository.save(any(LibraryVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(libraryItemRepository.save(any(LibraryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storageService.buildBlobPath(anyString(), anyString(), anyString())).thenReturn("/library/test/path-v2");
        when(storageService.saveLibraryFile(anyString(), anyString(), anyMap())).thenReturn(true);

        // When
        LibraryVersion result = libraryService.addVersion(
                "item-123", "catalog-v2.xml", "XML", fileContent, "Updated content", "testuser"
        );

        // Then
        assertNotNull(result);
        assertEquals(2, result.getVersionNumber());
        assertEquals("catalog-v2.xml", result.getFileName());
        assertEquals("Updated content", result.getChangeDescription());
        verify(storageService).saveLibraryFile(eq(fileContent), anyString(), anyMap());
        verify(libraryItemRepository).save(testItem);
    }

    @Test
    void testAddVersion_itemNotFound() {
        // Given
        when(libraryItemRepository.findByItemId("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            libraryService.addVersion("nonexistent", "file.xml", "XML", "content", "change", "testuser");
        });
    }

    @Test
    void testAddVersion_userNotFound() {
        // Given
        when(libraryItemRepository.findByItemId("item-123")).thenReturn(Optional.of(testItem));
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            libraryService.addVersion("item-123", "file.xml", "XML", "content", "change", "nonexistent");
        });
    }

    // ========== GET LIBRARY ITEM TESTS ==========

    @Test
    void testGetLibraryItem_success() {
        // Given
        when(libraryItemRepository.findByItemId("item-123")).thenReturn(Optional.of(testItem));
        when(libraryItemRepository.save(any(LibraryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Long initialViewCount = testItem.getViewCount();

        // When
        LibraryItem result = libraryService.getLibraryItem("item-123");

        // Then
        assertNotNull(result);
        assertEquals("item-123", result.getItemId());
        assertEquals(initialViewCount + 1, result.getViewCount());
        verify(libraryItemRepository).save(testItem);
    }

    @Test
    void testGetLibraryItem_notFound() {
        // Given
        when(libraryItemRepository.findByItemId("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            libraryService.getLibraryItem("nonexistent");
        });
    }

    // ========== GET VERSION CONTENT TESTS ==========

    @Test
    void testGetVersionContent_success() {
        // Given
        when(libraryVersionRepository.findByVersionId("version-123")).thenReturn(Optional.of(testVersion));
        when(storageService.getLibraryFileContent(testVersion.getFilePath())).thenReturn("file content");

        // When
        String result = libraryService.getVersionContent("version-123");

        // Then
        assertEquals("file content", result);
        verify(storageService).getLibraryFileContent(testVersion.getFilePath());
    }

    @Test
    void testGetVersionContent_notFound() {
        // Given
        when(libraryVersionRepository.findByVersionId("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            libraryService.getVersionContent("nonexistent");
        });
    }

    // ========== GET CURRENT VERSION CONTENT TESTS ==========

    @Test
    void testGetCurrentVersionContent_success() {
        // Given
        when(libraryItemRepository.findByItemId("item-123")).thenReturn(Optional.of(testItem));
        when(storageService.getLibraryFileContent(testVersion.getFilePath())).thenReturn("current version content");
        when(libraryItemRepository.save(any(LibraryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Long initialDownloadCount = testItem.getDownloadCount();

        // When
        String result = libraryService.getCurrentVersionContent("item-123");

        // Then
        assertEquals("current version content", result);
        assertEquals(initialDownloadCount + 1, testItem.getDownloadCount());
        verify(storageService).getLibraryFileContent(testVersion.getFilePath());
    }

    @Test
    void testGetCurrentVersionContent_itemNotFound() {
        // Given
        when(libraryItemRepository.findByItemId("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            libraryService.getCurrentVersionContent("nonexistent");
        });
    }

    @Test
    void testGetCurrentVersionContent_noCurrentVersion() {
        // Given
        testItem.setCurrentVersion(null);
        when(libraryItemRepository.findByItemId("item-123")).thenReturn(Optional.of(testItem));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            libraryService.getCurrentVersionContent("item-123");
        });
    }

    // ========== GET VERSION HISTORY TESTS ==========

    @Test
    void testGetVersionHistory_success() {
        // Given
        LibraryVersion version2 = new LibraryVersion("version-456", testItem, 2, "catalog-v2.xml", "XML",
                2048L, "/path/v2", testUser, "Version 2");
        List<LibraryVersion> versions = Arrays.asList(version2, testVersion);

        when(libraryItemRepository.findByItemId("item-123")).thenReturn(Optional.of(testItem));
        when(libraryVersionRepository.findByLibraryItemOrderByVersionNumberDesc(testItem)).thenReturn(versions);

        // When
        List<LibraryVersion> result = libraryService.getVersionHistory("item-123");

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getVersionNumber());
        assertEquals(1, result.get(1).getVersionNumber());
    }

    @Test
    void testGetVersionHistory_itemNotFound() {
        // Given
        when(libraryItemRepository.findByItemId("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            libraryService.getVersionHistory("nonexistent");
        });
    }

    // ========== DELETE LIBRARY ITEM TESTS ==========

    @Test
    void testDeleteLibraryItem_success() {
        // Given
        List<LibraryVersion> versions = Arrays.asList(testVersion);

        when(libraryItemRepository.findByItemId("item-123")).thenReturn(Optional.of(testItem));
        when(libraryVersionRepository.findByLibraryItem(testItem)).thenReturn(versions);
        when(storageService.deleteLibraryFile(anyString())).thenReturn(true);
        doNothing().when(libraryItemRepository).delete(testItem);

        // When
        libraryService.deleteLibraryItem("item-123", "testuser");

        // Then
        verify(storageService).deleteLibraryFile(testVersion.getFilePath());
        verify(libraryItemRepository).delete(testItem);
    }

    @Test
    void testDeleteLibraryItem_notCreator() {
        // Given
        when(libraryItemRepository.findByItemId("item-123")).thenReturn(Optional.of(testItem));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            libraryService.deleteLibraryItem("item-123", "otheruser");
        });

        verify(libraryItemRepository, never()).delete(any());
    }

    @Test
    void testDeleteLibraryItem_notFound() {
        // Given
        when(libraryItemRepository.findByItemId("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            libraryService.deleteLibraryItem("nonexistent", "testuser");
        });
    }

    // ========== SEARCH LIBRARY TESTS ==========

    @Test
    void testSearchLibrary_withFilters() {
        // Given
        List<LibraryItem> items = Arrays.asList(testItem);
        when(libraryItemRepository.advancedSearch("catalog", "catalog", "security")).thenReturn(items);

        // When
        List<LibraryItem> result = libraryService.searchLibrary("catalog", "catalog", "security");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(libraryItemRepository).advancedSearch("catalog", "catalog", "security");
    }

    @Test
    void testSearchLibrary_noFilters() {
        // Given
        List<LibraryItem> items = Arrays.asList(testItem);
        when(libraryItemRepository.findAll()).thenReturn(items);

        // When
        List<LibraryItem> result = libraryService.searchLibrary(null, null, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(libraryItemRepository).findAll();
        verify(libraryItemRepository, never()).advancedSearch(anyString(), anyString(), anyString());
    }

    // ========== GET ALL LIBRARY ITEMS TESTS ==========

    @Test
    void testGetAllLibraryItems() {
        // Given
        List<LibraryItem> items = Arrays.asList(testItem);
        when(libraryItemRepository.findAll()).thenReturn(items);

        // When
        List<LibraryItem> result = libraryService.getAllLibraryItems();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(libraryItemRepository).findAll();
    }

    // ========== GET BY OSCAL TYPE TESTS ==========

    @Test
    void testGetLibraryItemsByOscalType() {
        // Given
        List<LibraryItem> items = Arrays.asList(testItem);
        when(libraryItemRepository.findByOscalType("catalog")).thenReturn(items);

        // When
        List<LibraryItem> result = libraryService.getLibraryItemsByOscalType("catalog");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("catalog", result.get(0).getOscalType());
    }

    // ========== GET MOST POPULAR TESTS ==========

    @Test
    void testGetMostPopular() {
        // Given
        LibraryItem item1 = new LibraryItem("item-1", "Item 1", "Desc 1", "catalog", testUser);
        LibraryItem item2 = new LibraryItem("item-2", "Item 2", "Desc 2", "profile", testUser);
        List<LibraryItem> items = Arrays.asList(item1, item2);

        when(libraryItemRepository.findMostDownloaded()).thenReturn(items);

        // When
        List<LibraryItem> result = libraryService.getMostPopular(2);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetMostPopular_withLimit() {
        // Given
        List<LibraryItem> items = Arrays.asList(
                new LibraryItem("item-1", "Item 1", "Desc 1", "catalog", testUser),
                new LibraryItem("item-2", "Item 2", "Desc 2", "profile", testUser),
                new LibraryItem("item-3", "Item 3", "Desc 3", "ssp", testUser)
        );

        when(libraryItemRepository.findMostDownloaded()).thenReturn(items);

        // When
        List<LibraryItem> result = libraryService.getMostPopular(2);

        // Then
        assertEquals(2, result.size()); // Limited to 2 even though 3 available
    }

    // ========== GET RECENTLY UPDATED TESTS ==========

    @Test
    void testGetRecentlyUpdated() {
        // Given
        List<LibraryItem> items = Arrays.asList(testItem);
        when(libraryItemRepository.findRecentlyUpdated()).thenReturn(items);

        // When
        List<LibraryItem> result = libraryService.getRecentlyUpdated(10);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ========== GET ANALYTICS TESTS ==========

    @Test
    void testGetAnalytics() {
        // Given
        when(libraryItemRepository.count()).thenReturn(10L);
        when(libraryVersionRepository.count()).thenReturn(25L);
        when(libraryTagRepository.count()).thenReturn(5L);

        Object[] typeCount1 = {"catalog", 5L};
        Object[] typeCount2 = {"profile", 3L};
        when(libraryItemRepository.countByOscalType()).thenReturn(Arrays.asList(typeCount1, typeCount2));

        Object[] tagData1 = {1L, "security", "Security tag", 5L}; // id, name, description, usageCount
        List<Object[]> tagsList = new ArrayList<>();
        tagsList.add(tagData1);
        when(libraryTagRepository.findMostPopularWithCounts()).thenReturn(tagsList);

        when(libraryItemRepository.findMostDownloaded()).thenReturn(Arrays.asList(testItem));

        // When
        Map<String, Object> result = libraryService.getAnalytics();

        // Then
        assertNotNull(result);
        assertEquals(10L, result.get("totalItems"));
        assertEquals(25L, result.get("totalVersions"));
        assertEquals(5L, result.get("totalTags"));

        @SuppressWarnings("unchecked")
        Map<String, Long> itemsByType = (Map<String, Long>) result.get("itemsByType");
        assertEquals(5L, itemsByType.get("catalog"));
        assertEquals(3L, itemsByType.get("profile"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> popularTags = (List<Map<String, Object>>) result.get("popularTags");
        assertEquals(1, popularTags.size());
        assertEquals("security", popularTags.get(0).get("name"));
    }

    // ========== TAG MANAGEMENT TESTS ==========

    @Test
    void testGetAllTags() {
        // Given
        Object[] tagData = {1L, "security", "Security tag", 1L}; // id, name, description, usageCount
        List<Object[]> tagsList = new ArrayList<>();
        tagsList.add(tagData);
        when(libraryTagRepository.findAllWithCountsOrderByNameAsc()).thenReturn(tagsList);

        // When
        List<Map<String, Object>> result = libraryService.getAllTags();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("security", result.get(0).get("name"));
        assertEquals(1L, result.get(0).get("usageCount"));
        verify(libraryTagRepository).findAllWithCountsOrderByNameAsc();
    }

    @Test
    void testGetPopularTags() {
        // Given
        Object[] tagData1 = {1L, "security", "Security tag", 10L};
        Object[] tagData2 = {2L, "nist", "NIST tag", 8L};
        Object[] tagData3 = {3L, "compliance", "Compliance tag", 5L};
        when(libraryTagRepository.findMostPopularWithCounts()).thenReturn(Arrays.asList(tagData1, tagData2, tagData3));

        // When
        List<Map<String, Object>> result = libraryService.getPopularTags(2);

        // Then
        assertEquals(2, result.size()); // Limited to 2
        assertEquals("security", result.get(0).get("name"));
        assertEquals(10L, result.get(0).get("usageCount"));
        assertEquals("nist", result.get(1).get("name"));
        assertEquals(8L, result.get(1).get("usageCount"));
    }
}

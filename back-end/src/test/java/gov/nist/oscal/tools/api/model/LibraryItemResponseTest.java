package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.LibraryItem;
import gov.nist.oscal.tools.api.entity.LibraryTag;
import gov.nist.oscal.tools.api.entity.LibraryVersion;
import gov.nist.oscal.tools.api.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LibraryItemResponseTest {

    @Test
    void testNoArgsConstructor() {
        LibraryItemResponse response = new LibraryItemResponse();

        assertNotNull(response);
        assertNull(response.getItemId());
        assertNull(response.getTitle());
        assertNull(response.getDescription());
        assertNull(response.getOscalType());
        assertNull(response.getCreatedBy());
        assertNull(response.getCreatedAt());
        assertNull(response.getUpdatedAt());
        assertNull(response.getTags());
        assertNull(response.getCurrentVersion());
        assertNull(response.getDownloadCount());
        assertNull(response.getViewCount());
        assertNull(response.getVersionCount());
    }

    @Test
    void testSetItemId() {
        LibraryItemResponse response = new LibraryItemResponse();
        response.setItemId("item-001");
        assertEquals("item-001", response.getItemId());
    }

    @Test
    void testSetTitle() {
        LibraryItemResponse response = new LibraryItemResponse();
        response.setTitle("NIST 800-53 Controls");
        assertEquals("NIST 800-53 Controls", response.getTitle());
    }

    @Test
    void testSetDescription() {
        LibraryItemResponse response = new LibraryItemResponse();
        response.setDescription("Security controls catalog");
        assertEquals("Security controls catalog", response.getDescription());
    }

    @Test
    void testSetOscalType() {
        LibraryItemResponse response = new LibraryItemResponse();
        response.setOscalType("catalog");
        assertEquals("catalog", response.getOscalType());
    }

    @Test
    void testSetCreatedBy() {
        LibraryItemResponse response = new LibraryItemResponse();
        response.setCreatedBy("john.doe");
        assertEquals("john.doe", response.getCreatedBy());
    }

    @Test
    void testSetCreatedAt() {
        LibraryItemResponse response = new LibraryItemResponse();
        LocalDateTime now = LocalDateTime.now();
        response.setCreatedAt(now);
        assertEquals(now, response.getCreatedAt());
    }

    @Test
    void testSetUpdatedAt() {
        LibraryItemResponse response = new LibraryItemResponse();
        LocalDateTime now = LocalDateTime.now();
        response.setUpdatedAt(now);
        assertEquals(now, response.getUpdatedAt());
    }

    @Test
    void testSetTags() {
        LibraryItemResponse response = new LibraryItemResponse();
        Set<String> tags = Set.of("security", "nist", "controls");
        response.setTags(tags);
        assertEquals(tags, response.getTags());
    }

    @Test
    void testSetCurrentVersion() {
        LibraryItemResponse response = new LibraryItemResponse();
        LibraryVersionResponse version = new LibraryVersionResponse();
        version.setVersionNumber(1);
        response.setCurrentVersion(version);
        assertEquals(version, response.getCurrentVersion());
        assertEquals(1, response.getCurrentVersion().getVersionNumber());
    }

    @Test
    void testSetDownloadCount() {
        LibraryItemResponse response = new LibraryItemResponse();
        response.setDownloadCount(1000L);
        assertEquals(1000L, response.getDownloadCount());
    }

    @Test
    void testSetViewCount() {
        LibraryItemResponse response = new LibraryItemResponse();
        response.setViewCount(5000L);
        assertEquals(5000L, response.getViewCount());
    }

    @Test
    void testSetVersionCount() {
        LibraryItemResponse response = new LibraryItemResponse();
        response.setVersionCount(3);
        assertEquals(3, response.getVersionCount());
    }

    @Test
    void testSetAllFields() {
        LibraryItemResponse response = new LibraryItemResponse();
        LocalDateTime now = LocalDateTime.now();
        Set<String> tags = Set.of("tag1", "tag2");
        LibraryVersionResponse version = new LibraryVersionResponse();

        response.setItemId("item-001");
        response.setTitle("Test Item");
        response.setDescription("Test Description");
        response.setOscalType("catalog");
        response.setCreatedBy("user123");
        response.setCreatedAt(now);
        response.setUpdatedAt(now);
        response.setTags(tags);
        response.setCurrentVersion(version);
        response.setDownloadCount(100L);
        response.setViewCount(500L);
        response.setVersionCount(2);

        assertEquals("item-001", response.getItemId());
        assertEquals("Test Item", response.getTitle());
        assertEquals("Test Description", response.getDescription());
        assertEquals("catalog", response.getOscalType());
        assertEquals("user123", response.getCreatedBy());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
        assertEquals(tags, response.getTags());
        assertEquals(version, response.getCurrentVersion());
        assertEquals(100L, response.getDownloadCount());
        assertEquals(500L, response.getViewCount());
        assertEquals(2, response.getVersionCount());
    }

    @Test
    void testSetAllFieldsToNull() {
        LibraryItemResponse response = new LibraryItemResponse();

        response.setItemId(null);
        response.setTitle(null);
        response.setDescription(null);
        response.setOscalType(null);
        response.setCreatedBy(null);
        response.setCreatedAt(null);
        response.setUpdatedAt(null);
        response.setTags(null);
        response.setCurrentVersion(null);
        response.setDownloadCount(null);
        response.setViewCount(null);
        response.setVersionCount(null);

        assertNull(response.getItemId());
        assertNull(response.getTitle());
        assertNull(response.getDescription());
        assertNull(response.getOscalType());
        assertNull(response.getCreatedBy());
        assertNull(response.getCreatedAt());
        assertNull(response.getUpdatedAt());
        assertNull(response.getTags());
        assertNull(response.getCurrentVersion());
        assertNull(response.getDownloadCount());
        assertNull(response.getViewCount());
        assertNull(response.getVersionCount());
    }

    @Test
    void testFromEntityWithCurrentVersion() {
        // Create mock LibraryItem
        LibraryItem item = mock(LibraryItem.class);
        User user = mock(User.class);
        User versionUser = mock(User.class);
        LibraryVersion version = mock(LibraryVersion.class);
        Set<LibraryTag> tags = new HashSet<>();
        LibraryTag tag1 = mock(LibraryTag.class);
        LibraryTag tag2 = mock(LibraryTag.class);
        tags.add(tag1);
        tags.add(tag2);

        LocalDateTime now = LocalDateTime.now();

        when(item.getItemId()).thenReturn("item-001");
        when(item.getTitle()).thenReturn("Test Item");
        when(item.getDescription()).thenReturn("Test Description");
        when(item.getOscalType()).thenReturn("catalog");
        when(item.getCreatedBy()).thenReturn(user);
        when(user.getUsername()).thenReturn("testuser");
        when(item.getCreatedAt()).thenReturn(now);
        when(item.getUpdatedAt()).thenReturn(now);
        when(item.getTags()).thenReturn(tags);
        when(tag1.getName()).thenReturn("security");
        when(tag2.getName()).thenReturn("nist");
        when(item.getCurrentVersion()).thenReturn(version);
        when(version.getVersionId()).thenReturn("v1");
        when(version.getVersionNumber()).thenReturn(1);
        when(version.getFileName()).thenReturn("catalog.json");
        when(version.getFormat()).thenReturn("JSON");
        when(version.getFileSize()).thenReturn(1024L);
        when(version.getUploadedBy()).thenReturn(versionUser);
        when(versionUser.getUsername()).thenReturn("uploader");
        when(version.getUploadedAt()).thenReturn(now);
        when(version.getChangeDescription()).thenReturn("Initial version");
        when(item.getDownloadCount()).thenReturn(100L);
        when(item.getViewCount()).thenReturn(500L);
        when(item.getVersions()).thenReturn(Set.of(version));

        LibraryItemResponse response = LibraryItemResponse.fromEntity(item);

        assertEquals("item-001", response.getItemId());
        assertEquals("Test Item", response.getTitle());
        assertEquals("Test Description", response.getDescription());
        assertEquals("catalog", response.getOscalType());
        assertEquals("testuser", response.getCreatedBy());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
        assertTrue(response.getTags().contains("security"));
        assertTrue(response.getTags().contains("nist"));
        assertNotNull(response.getCurrentVersion());
        assertEquals(100L, response.getDownloadCount());
        assertEquals(500L, response.getViewCount());
        assertEquals(1, response.getVersionCount());
    }

    @Test
    void testFromEntityWithoutCurrentVersion() {
        // Create mock LibraryItem with null current version
        LibraryItem item = mock(LibraryItem.class);
        User user = mock(User.class);
        Set<LibraryTag> tags = new HashSet<>();

        LocalDateTime now = LocalDateTime.now();

        when(item.getItemId()).thenReturn("item-002");
        when(item.getTitle()).thenReturn("New Item");
        when(item.getDescription()).thenReturn("New Description");
        when(item.getOscalType()).thenReturn("profile");
        when(item.getCreatedBy()).thenReturn(user);
        when(user.getUsername()).thenReturn("newuser");
        when(item.getCreatedAt()).thenReturn(now);
        when(item.getUpdatedAt()).thenReturn(now);
        when(item.getTags()).thenReturn(tags);
        when(item.getCurrentVersion()).thenReturn(null); // No current version
        when(item.getDownloadCount()).thenReturn(0L);
        when(item.getViewCount()).thenReturn(0L);
        when(item.getVersions()).thenReturn(new HashSet<>());

        LibraryItemResponse response = LibraryItemResponse.fromEntity(item);

        assertEquals("item-002", response.getItemId());
        assertEquals("New Item", response.getTitle());
        assertEquals("New Description", response.getDescription());
        assertEquals("profile", response.getOscalType());
        assertEquals("newuser", response.getCreatedBy());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
        assertTrue(response.getTags().isEmpty());
        assertNull(response.getCurrentVersion()); // Should be null
        assertEquals(0L, response.getDownloadCount());
        assertEquals(0L, response.getViewCount());
        assertEquals(0, response.getVersionCount());
    }

    @Test
    void testWithEmptyStrings() {
        LibraryItemResponse response = new LibraryItemResponse();

        response.setItemId("");
        response.setTitle("");
        response.setDescription("");
        response.setOscalType("");
        response.setCreatedBy("");

        assertEquals("", response.getItemId());
        assertEquals("", response.getTitle());
        assertEquals("", response.getDescription());
        assertEquals("", response.getOscalType());
        assertEquals("", response.getCreatedBy());
    }

    @Test
    void testWithEmptyTags() {
        LibraryItemResponse response = new LibraryItemResponse();
        response.setTags(new HashSet<>());

        assertTrue(response.getTags().isEmpty());
        assertEquals(0, response.getTags().size());
    }

    @Test
    void testWithZeroCounts() {
        LibraryItemResponse response = new LibraryItemResponse();

        response.setDownloadCount(0L);
        response.setViewCount(0L);
        response.setVersionCount(0);

        assertEquals(0L, response.getDownloadCount());
        assertEquals(0L, response.getViewCount());
        assertEquals(0, response.getVersionCount());
    }

    @Test
    void testWithLargeCounts() {
        LibraryItemResponse response = new LibraryItemResponse();

        response.setDownloadCount(999999999L);
        response.setViewCount(999999999L);
        response.setVersionCount(999);

        assertEquals(999999999L, response.getDownloadCount());
        assertEquals(999999999L, response.getViewCount());
        assertEquals(999, response.getVersionCount());
    }

    @Test
    void testWithMultipleTags() {
        LibraryItemResponse response = new LibraryItemResponse();
        Set<String> tags = Set.of("tag1", "tag2", "tag3", "tag4", "tag5");
        response.setTags(tags);

        assertEquals(5, response.getTags().size());
        assertTrue(response.getTags().contains("tag1"));
        assertTrue(response.getTags().contains("tag5"));
    }

    @Test
    void testWithSpecialCharacters() {
        LibraryItemResponse response = new LibraryItemResponse();

        response.setTitle("Test <Title> & \"Description\"");
        response.setDescription("Description with special chars: @#$%^&*()");

        assertEquals("Test <Title> & \"Description\"", response.getTitle());
        assertEquals("Description with special chars: @#$%^&*()", response.getDescription());
    }

    @Test
    void testDifferentOscalTypes() {
        LibraryItemResponse response = new LibraryItemResponse();

        response.setOscalType("catalog");
        assertEquals("catalog", response.getOscalType());

        response.setOscalType("profile");
        assertEquals("profile", response.getOscalType());

        response.setOscalType("component-definition");
        assertEquals("component-definition", response.getOscalType());

        response.setOscalType("system-security-plan");
        assertEquals("system-security-plan", response.getOscalType());
    }

    @Test
    void testFromEntityWithMultipleVersions() {
        LibraryItem item = mock(LibraryItem.class);
        User user = mock(User.class);
        User versionUser = mock(User.class);
        LibraryVersion v1 = mock(LibraryVersion.class);
        LibraryVersion v2 = mock(LibraryVersion.class);
        LibraryVersion v3 = mock(LibraryVersion.class);
        Set<LibraryVersion> versions = Set.of(v1, v2, v3);
        LocalDateTime now = LocalDateTime.now();

        when(item.getItemId()).thenReturn("item-003");
        when(item.getTitle()).thenReturn("Multi-version Item");
        when(item.getDescription()).thenReturn("Item with multiple versions");
        when(item.getOscalType()).thenReturn("catalog");
        when(item.getCreatedBy()).thenReturn(user);
        when(user.getUsername()).thenReturn("admin");
        when(item.getCreatedAt()).thenReturn(now);
        when(item.getUpdatedAt()).thenReturn(now);
        when(item.getTags()).thenReturn(new HashSet<>());
        when(item.getCurrentVersion()).thenReturn(v3);
        when(v3.getVersionId()).thenReturn("v3");
        when(v3.getVersionNumber()).thenReturn(3);
        when(v3.getFileName()).thenReturn("catalog-v3.json");
        when(v3.getFormat()).thenReturn("JSON");
        when(v3.getFileSize()).thenReturn(2048L);
        when(v3.getUploadedBy()).thenReturn(versionUser);
        when(versionUser.getUsername()).thenReturn("admin");
        when(v3.getUploadedAt()).thenReturn(now);
        when(v3.getChangeDescription()).thenReturn("Version 3");
        when(item.getDownloadCount()).thenReturn(1000L);
        when(item.getViewCount()).thenReturn(5000L);
        when(item.getVersions()).thenReturn(versions);

        LibraryItemResponse response = LibraryItemResponse.fromEntity(item);

        assertEquals("item-003", response.getItemId());
        assertEquals("Multi-version Item", response.getTitle());
        assertEquals(3, response.getVersionCount());
        assertNotNull(response.getCurrentVersion());
    }
}

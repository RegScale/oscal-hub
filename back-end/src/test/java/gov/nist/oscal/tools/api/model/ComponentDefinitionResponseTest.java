package gov.nist.oscal.tools.api.model;

import gov.nist.oscal.tools.api.entity.ComponentDefinition;
import gov.nist.oscal.tools.api.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ComponentDefinitionResponseTest {

    @Test
    void testNoArgsConstructor() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();

        assertNotNull(response);
        assertNull(response.getId());
        assertNull(response.getOscalUuid());
        assertNull(response.getTitle());
        assertNull(response.getDescription());
        assertNull(response.getVersion());
        assertNull(response.getOscalVersion());
        assertNull(response.getFilename());
        assertNull(response.getFileSize());
        assertNull(response.getComponentCount());
        assertNull(response.getCapabilityCount());
        assertNull(response.getControlCount());
        assertNull(response.getCreatedBy());
        assertNull(response.getCreatedAt());
        assertNull(response.getLastUpdatedBy());
        assertNull(response.getUpdatedAt());
    }

    @Test
    void testSetId() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setId(123L);
        assertEquals(123L, response.getId());
    }

    @Test
    void testSetOscalUuid() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setOscalUuid("550e8400-e29b-41d4-a716-446655440000");
        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.getOscalUuid());
    }

    @Test
    void testSetTitle() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setTitle("Example Component Definition");
        assertEquals("Example Component Definition", response.getTitle());
    }

    @Test
    void testSetDescription() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setDescription("A comprehensive component definition");
        assertEquals("A comprehensive component definition", response.getDescription());
    }

    @Test
    void testSetVersion() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setVersion("1.0.0");
        assertEquals("1.0.0", response.getVersion());
    }

    @Test
    void testSetOscalVersion() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setOscalVersion("1.0.4");
        assertEquals("1.0.4", response.getOscalVersion());
    }

    @Test
    void testSetFilename() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setFilename("component-definition.json");
        assertEquals("component-definition.json", response.getFilename());
    }

    @Test
    void testSetFileSize() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setFileSize(1024L);
        assertEquals(1024L, response.getFileSize());
    }

    @Test
    void testSetComponentCount() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setComponentCount(15);
        assertEquals(15, response.getComponentCount());
    }

    @Test
    void testSetCapabilityCount() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setCapabilityCount(30);
        assertEquals(30, response.getCapabilityCount());
    }

    @Test
    void testSetControlCount() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setControlCount(100);
        assertEquals(100, response.getControlCount());
    }

    @Test
    void testSetCreatedBy() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setCreatedBy("john.doe");
        assertEquals("john.doe", response.getCreatedBy());
    }

    @Test
    void testSetCreatedAt() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        LocalDateTime now = LocalDateTime.now();
        response.setCreatedAt(now);
        assertEquals(now, response.getCreatedAt());
    }

    @Test
    void testSetLastUpdatedBy() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setLastUpdatedBy("jane.smith");
        assertEquals("jane.smith", response.getLastUpdatedBy());
    }

    @Test
    void testSetUpdatedAt() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        LocalDateTime now = LocalDateTime.now();
        response.setUpdatedAt(now);
        assertEquals(now, response.getUpdatedAt());
    }

    @Test
    void testSetAllFields() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        LocalDateTime createdTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime updatedTime = LocalDateTime.of(2024, 1, 2, 14, 30);

        response.setId(1L);
        response.setOscalUuid("550e8400-e29b-41d4-a716-446655440000");
        response.setTitle("Test Component Definition");
        response.setDescription("Test description");
        response.setVersion("1.0.0");
        response.setOscalVersion("1.0.4");
        response.setFilename("test-component.json");
        response.setFileSize(2048L);
        response.setComponentCount(10);
        response.setCapabilityCount(20);
        response.setControlCount(50);
        response.setCreatedBy("testuser");
        response.setCreatedAt(createdTime);
        response.setLastUpdatedBy("adminuser");
        response.setUpdatedAt(updatedTime);

        assertEquals(1L, response.getId());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.getOscalUuid());
        assertEquals("Test Component Definition", response.getTitle());
        assertEquals("Test description", response.getDescription());
        assertEquals("1.0.0", response.getVersion());
        assertEquals("1.0.4", response.getOscalVersion());
        assertEquals("test-component.json", response.getFilename());
        assertEquals(2048L, response.getFileSize());
        assertEquals(10, response.getComponentCount());
        assertEquals(20, response.getCapabilityCount());
        assertEquals(50, response.getControlCount());
        assertEquals("testuser", response.getCreatedBy());
        assertEquals(createdTime, response.getCreatedAt());
        assertEquals("adminuser", response.getLastUpdatedBy());
        assertEquals(updatedTime, response.getUpdatedAt());
    }

    @Test
    void testSetAllFieldsToNull() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();

        response.setId(null);
        response.setOscalUuid(null);
        response.setTitle(null);
        response.setDescription(null);
        response.setVersion(null);
        response.setOscalVersion(null);
        response.setFilename(null);
        response.setFileSize(null);
        response.setComponentCount(null);
        response.setCapabilityCount(null);
        response.setControlCount(null);
        response.setCreatedBy(null);
        response.setCreatedAt(null);
        response.setLastUpdatedBy(null);
        response.setUpdatedAt(null);

        assertNull(response.getId());
        assertNull(response.getOscalUuid());
        assertNull(response.getTitle());
        assertNull(response.getDescription());
        assertNull(response.getVersion());
        assertNull(response.getOscalVersion());
        assertNull(response.getFilename());
        assertNull(response.getFileSize());
        assertNull(response.getComponentCount());
        assertNull(response.getCapabilityCount());
        assertNull(response.getControlCount());
        assertNull(response.getCreatedBy());
        assertNull(response.getCreatedAt());
        assertNull(response.getLastUpdatedBy());
        assertNull(response.getUpdatedAt());
    }

    @Test
    void testModifyAllFields() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 2, 1, 15, 30);

        response.setId(1L);
        response.setTitle("First Title");
        response.setVersion("1.0.0");
        response.setCreatedBy("user1");
        response.setCreatedAt(time1);

        response.setId(2L);
        response.setTitle("Second Title");
        response.setVersion("2.0.0");
        response.setCreatedBy("user2");
        response.setCreatedAt(time2);

        assertEquals(2L, response.getId());
        assertEquals("Second Title", response.getTitle());
        assertEquals("2.0.0", response.getVersion());
        assertEquals("user2", response.getCreatedBy());
        assertEquals(time2, response.getCreatedAt());
    }

    @Test
    void testFromEntityWithLastUpdatedBy() {
        User creator = new User("john.doe", "password", "john@example.com");
        User updater = new User("jane.smith", "password", "jane@example.com");

        ComponentDefinition entity = new ComponentDefinition();
        entity.setId(1L);
        entity.setOscalUuid("550e8400-e29b-41d4-a716-446655440000");
        entity.setTitle("Test Component");
        entity.setDescription("Test description");
        entity.setVersion("1.0.0");
        entity.setOscalVersion("1.0.4");
        entity.setFilename("component.json");
        entity.setFileSize(1024L);
        entity.setComponentCount(5);
        entity.setCapabilityCount(10);
        entity.setControlCount(25);
        entity.setCreatedBy(creator);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        entity.setLastUpdatedBy(updater);
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 2, 14, 30));

        ComponentDefinitionResponse response = ComponentDefinitionResponse.fromEntity(entity);

        assertEquals(1L, response.getId());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.getOscalUuid());
        assertEquals("Test Component", response.getTitle());
        assertEquals("Test description", response.getDescription());
        assertEquals("1.0.0", response.getVersion());
        assertEquals("1.0.4", response.getOscalVersion());
        assertEquals("component.json", response.getFilename());
        assertEquals(1024L, response.getFileSize());
        assertEquals(5, response.getComponentCount());
        assertEquals(10, response.getCapabilityCount());
        assertEquals(25, response.getControlCount());
        assertEquals("john.doe", response.getCreatedBy());
        assertEquals(LocalDateTime.of(2024, 1, 1, 10, 0), response.getCreatedAt());
        assertEquals("jane.smith", response.getLastUpdatedBy());
        assertEquals(LocalDateTime.of(2024, 1, 2, 14, 30), response.getUpdatedAt());
    }

    @Test
    void testFromEntityWithoutLastUpdatedBy() {
        User creator = new User("john.doe", "password", "john@example.com");

        ComponentDefinition entity = new ComponentDefinition();
        entity.setId(1L);
        entity.setOscalUuid("550e8400-e29b-41d4-a716-446655440000");
        entity.setTitle("Test Component");
        entity.setDescription("Test description");
        entity.setVersion("1.0.0");
        entity.setOscalVersion("1.0.4");
        entity.setFilename("component.json");
        entity.setFileSize(1024L);
        entity.setComponentCount(5);
        entity.setCapabilityCount(10);
        entity.setControlCount(25);
        entity.setCreatedBy(creator);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        entity.setLastUpdatedBy(null);
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));

        ComponentDefinitionResponse response = ComponentDefinitionResponse.fromEntity(entity);

        assertEquals(1L, response.getId());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.getOscalUuid());
        assertEquals("Test Component", response.getTitle());
        assertEquals("john.doe", response.getCreatedBy());
        assertNull(response.getLastUpdatedBy());
    }

    @Test
    void testWithLargeFileSize() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setFileSize(10485760L); // 10 MB
        assertEquals(10485760L, response.getFileSize());
    }

    @Test
    void testWithZeroCounts() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setComponentCount(0);
        response.setCapabilityCount(0);
        response.setControlCount(0);

        assertEquals(0, response.getComponentCount());
        assertEquals(0, response.getCapabilityCount());
        assertEquals(0, response.getControlCount());
    }

    @Test
    void testWithEmptyStrings() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setTitle("");
        response.setDescription("");
        response.setVersion("");
        response.setFilename("");
        response.setCreatedBy("");

        assertEquals("", response.getTitle());
        assertEquals("", response.getDescription());
        assertEquals("", response.getVersion());
        assertEquals("", response.getFilename());
        assertEquals("", response.getCreatedBy());
    }

    @Test
    void testWithLongStrings() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        String longTitle = "A".repeat(1000);
        String longDescription = "B".repeat(5000);

        response.setTitle(longTitle);
        response.setDescription(longDescription);

        assertEquals(longTitle, response.getTitle());
        assertEquals(longDescription, response.getDescription());
    }

    @Test
    void testWithSpecialCharacters() {
        ComponentDefinitionResponse response = new ComponentDefinitionResponse();
        response.setTitle("Test <Component> & \"Definition\"");
        response.setDescription("Description with special chars: @#$%^&*()");
        response.setFilename("file-name_with.special-chars.json");

        assertEquals("Test <Component> & \"Definition\"", response.getTitle());
        assertEquals("Description with special chars: @#$%^&*()", response.getDescription());
        assertEquals("file-name_with.special-chars.json", response.getFilename());
    }

    @Test
    void testCompleteComponentDefinitionScenario() {
        User creator = new User("admin", "password", "admin@example.com");
        User updater = new User("editor", "password", "editor@example.com");

        ComponentDefinition entity = new ComponentDefinition(
            "550e8400-e29b-41d4-a716-446655440000",
            "Enterprise Security Components",
            "build/admin/enterprise-components.json",
            creator
        );
        entity.setId(42L);
        entity.setDescription("A comprehensive set of security components for enterprise applications");
        entity.setVersion("2.1.0");
        entity.setOscalVersion("1.0.4");
        entity.setFilename("enterprise-components.json");
        entity.setFileSize(8192L);
        entity.setComponentCount(25);
        entity.setCapabilityCount(75);
        entity.setControlCount(150);
        entity.setLastUpdatedBy(updater);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 15, 9, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 3, 20, 16, 45));

        ComponentDefinitionResponse response = ComponentDefinitionResponse.fromEntity(entity);

        assertEquals(42L, response.getId());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.getOscalUuid());
        assertEquals("Enterprise Security Components", response.getTitle());
        assertEquals("A comprehensive set of security components for enterprise applications", response.getDescription());
        assertEquals("2.1.0", response.getVersion());
        assertEquals("1.0.4", response.getOscalVersion());
        assertEquals("enterprise-components.json", response.getFilename());
        assertEquals(8192L, response.getFileSize());
        assertEquals(25, response.getComponentCount());
        assertEquals(75, response.getCapabilityCount());
        assertEquals(150, response.getControlCount());
        assertEquals("admin", response.getCreatedBy());
        assertEquals("editor", response.getLastUpdatedBy());
        assertEquals(LocalDateTime.of(2024, 1, 15, 9, 0), response.getCreatedAt());
        assertEquals(LocalDateTime.of(2024, 3, 20, 16, 45), response.getUpdatedAt());
    }
}

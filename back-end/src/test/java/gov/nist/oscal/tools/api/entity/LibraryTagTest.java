package gov.nist.oscal.tools.api.entity;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LibraryTagTest {

    @Test
    void testNoArgsConstructor() {
        LibraryTag tag = new LibraryTag();

        assertNotNull(tag);
        assertNull(tag.getId());
        assertNull(tag.getName());
        assertNull(tag.getDescription());
        assertNotNull(tag.getLibraryItems()); // initialized to new HashSet
        assertEquals(0, tag.getLibraryItems().size());
    }

    @Test
    void testOneArgConstructor() {
        String tagName = "NIST800-53";

        LibraryTag tag = new LibraryTag(tagName);

        assertNotNull(tag);
        assertEquals("nist800-53", tag.getName()); // normalized to lowercase
        assertNull(tag.getDescription());
    }

    @Test
    void testOneArgConstructorWithWhitespace() {
        String tagName = "  FedRAMP  ";

        LibraryTag tag = new LibraryTag(tagName);

        assertEquals("fedramp", tag.getName()); // normalized: lowercased and trimmed
    }

    @Test
    void testTwoArgsConstructor() {
        String tagName = "Encryption";
        String description = "Components related to encryption";

        LibraryTag tag = new LibraryTag(tagName, description);

        assertEquals("encryption", tag.getName()); // normalized to lowercase
        assertEquals(description, tag.getDescription());
    }

    @Test
    void testIdGetterAndSetter() {
        LibraryTag tag = new LibraryTag();

        Long id = 42L;
        tag.setId(id);

        assertEquals(id, tag.getId());
    }

    @Test
    void testNameGetterAndSetter() {
        LibraryTag tag = new LibraryTag();

        String name = "Security";
        tag.setName(name);

        assertEquals("security", tag.getName()); // normalized to lowercase
    }

    @Test
    void testNameSetterNormalizesInput() {
        LibraryTag tag = new LibraryTag();

        tag.setName("  AUTHENTICATION  ");

        assertEquals("authentication", tag.getName()); // trimmed and lowercased
    }

    @Test
    void testDescriptionGetterAndSetter() {
        LibraryTag tag = new LibraryTag();

        String description = "Authentication-related components";
        tag.setDescription(description);

        assertEquals(description, tag.getDescription());
    }

    @Test
    void testLibraryItemsGetterAndSetter() {
        LibraryTag tag = new LibraryTag();

        Set<LibraryItem> items = new HashSet<>();
        LibraryItem item1 = new LibraryItem();
        item1.setTitle("Component 1");
        LibraryItem item2 = new LibraryItem();
        item2.setTitle("Component 2");

        items.add(item1);
        items.add(item2);

        tag.setLibraryItems(items);

        assertEquals(2, tag.getLibraryItems().size());
        assertTrue(tag.getLibraryItems().contains(item1));
        assertTrue(tag.getLibraryItems().contains(item2));
    }

    @Test
    void testGetUsageCountWithNoItems() {
        LibraryTag tag = new LibraryTag();

        assertEquals(0, tag.getUsageCount());
    }

    @Test
    void testGetUsageCountWithItems() {
        LibraryTag tag = new LibraryTag();

        Set<LibraryItem> items = new HashSet<>();
        items.add(new LibraryItem());
        items.add(new LibraryItem());
        items.add(new LibraryItem());

        tag.setLibraryItems(items);

        assertEquals(3, tag.getUsageCount());
    }

    @Test
    void testGetUsageCountWhenLibraryItemsIsNull() {
        LibraryTag tag = new LibraryTag();
        tag.setLibraryItems(null);

        assertEquals(0, tag.getUsageCount());
    }

    @Test
    void testEqualsWithSameInstance() {
        LibraryTag tag = new LibraryTag("security");

        assertTrue(tag.equals(tag));
    }

    @Test
    void testEqualsWithNull() {
        LibraryTag tag = new LibraryTag("security");

        assertFalse(tag.equals(null));
    }

    @Test
    void testEqualsWithDifferentClass() {
        LibraryTag tag = new LibraryTag("security");
        String notATag = "security";

        assertFalse(tag.equals(notATag));
    }

    @Test
    void testEqualsWithSameName() {
        LibraryTag tag1 = new LibraryTag("security");
        LibraryTag tag2 = new LibraryTag("security");

        assertTrue(tag1.equals(tag2));
        assertTrue(tag2.equals(tag1));
    }

    @Test
    void testEqualsWithDifferentName() {
        LibraryTag tag1 = new LibraryTag("security");
        LibraryTag tag2 = new LibraryTag("encryption");

        assertFalse(tag1.equals(tag2));
        assertFalse(tag2.equals(tag1));
    }

    @Test
    void testEqualsWhenNameIsNull() {
        LibraryTag tag1 = new LibraryTag();
        LibraryTag tag2 = new LibraryTag();

        // Both have null names
        assertFalse(tag1.equals(tag2));
    }

    @Test
    void testEqualsWhenOneNameIsNull() {
        LibraryTag tag1 = new LibraryTag("security");
        LibraryTag tag2 = new LibraryTag();

        assertFalse(tag1.equals(tag2));
        assertFalse(tag2.equals(tag1));
    }

    @Test
    void testHashCodeConsistency() {
        LibraryTag tag = new LibraryTag("security");

        int hashCode1 = tag.hashCode();
        int hashCode2 = tag.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    void testHashCodeEqualityForSameName() {
        LibraryTag tag1 = new LibraryTag("security");
        LibraryTag tag2 = new LibraryTag("security");

        assertEquals(tag1.hashCode(), tag2.hashCode());
    }

    @Test
    void testHashCodeWhenNameIsNull() {
        LibraryTag tag = new LibraryTag();

        assertEquals(0, tag.hashCode());
    }

    @Test
    void testHashCodeDifferentForDifferentNames() {
        LibraryTag tag1 = new LibraryTag("security");
        LibraryTag tag2 = new LibraryTag("encryption");

        // While hash codes can collide, they should generally be different for different names
        assertNotEquals(tag1.hashCode(), tag2.hashCode());
    }
}

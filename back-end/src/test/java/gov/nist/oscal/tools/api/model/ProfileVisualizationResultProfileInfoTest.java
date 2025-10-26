package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileVisualizationResultProfileInfoTest {

    @Test
    void testNoArgsConstructor() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();

        assertNotNull(profileInfo);
        assertNull(profileInfo.getUuid());
        assertNull(profileInfo.getTitle());
        assertNull(profileInfo.getVersion());
        assertNull(profileInfo.getOscalVersion());
        assertNull(profileInfo.getLastModified());
        assertNull(profileInfo.getPublished());
    }

    @Test
    void testSetUuid() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();
        profileInfo.setUuid("550e8400-e29b-41d4-a716-446655440000");
        assertEquals("550e8400-e29b-41d4-a716-446655440000", profileInfo.getUuid());
    }

    @Test
    void testSetTitle() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();
        profileInfo.setTitle("NIST SP 800-53 High Baseline");
        assertEquals("NIST SP 800-53 High Baseline", profileInfo.getTitle());
    }

    @Test
    void testSetVersion() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();
        profileInfo.setVersion("1.0.0");
        assertEquals("1.0.0", profileInfo.getVersion());
    }

    @Test
    void testSetOscalVersion() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();
        profileInfo.setOscalVersion("1.0.4");
        assertEquals("1.0.4", profileInfo.getOscalVersion());
    }

    @Test
    void testSetLastModified() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();
        profileInfo.setLastModified("2024-01-15T10:30:00Z");
        assertEquals("2024-01-15T10:30:00Z", profileInfo.getLastModified());
    }

    @Test
    void testSetPublished() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();
        profileInfo.setPublished("2023-12-01T00:00:00Z");
        assertEquals("2023-12-01T00:00:00Z", profileInfo.getPublished());
    }

    @Test
    void testSetAllFields() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();

        profileInfo.setUuid("123e4567-e89b-12d3-a456-426614174000");
        profileInfo.setTitle("FedRAMP High Baseline");
        profileInfo.setVersion("2.1.0");
        profileInfo.setOscalVersion("1.0.4");
        profileInfo.setLastModified("2024-03-20T15:45:00Z");
        profileInfo.setPublished("2024-01-01T00:00:00Z");

        assertEquals("123e4567-e89b-12d3-a456-426614174000", profileInfo.getUuid());
        assertEquals("FedRAMP High Baseline", profileInfo.getTitle());
        assertEquals("2.1.0", profileInfo.getVersion());
        assertEquals("1.0.4", profileInfo.getOscalVersion());
        assertEquals("2024-03-20T15:45:00Z", profileInfo.getLastModified());
        assertEquals("2024-01-01T00:00:00Z", profileInfo.getPublished());
    }

    @Test
    void testSetFieldsToNull() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();

        profileInfo.setUuid("uuid");
        profileInfo.setTitle("title");
        profileInfo.setVersion("version");

        profileInfo.setUuid(null);
        profileInfo.setTitle(null);
        profileInfo.setVersion(null);
        profileInfo.setOscalVersion(null);
        profileInfo.setLastModified(null);
        profileInfo.setPublished(null);

        assertNull(profileInfo.getUuid());
        assertNull(profileInfo.getTitle());
        assertNull(profileInfo.getVersion());
        assertNull(profileInfo.getOscalVersion());
        assertNull(profileInfo.getLastModified());
        assertNull(profileInfo.getPublished());
    }

    @Test
    void testModifyAllFields() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();

        profileInfo.setUuid("first-uuid");
        profileInfo.setTitle("First Title");
        profileInfo.setVersion("1.0.0");

        profileInfo.setUuid("second-uuid");
        profileInfo.setTitle("Second Title");
        profileInfo.setVersion("2.0.0");

        assertEquals("second-uuid", profileInfo.getUuid());
        assertEquals("Second Title", profileInfo.getTitle());
        assertEquals("2.0.0", profileInfo.getVersion());
    }

    @Test
    void testWithEmptyStrings() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();

        profileInfo.setUuid("");
        profileInfo.setTitle("");
        profileInfo.setVersion("");
        profileInfo.setOscalVersion("");
        profileInfo.setLastModified("");
        profileInfo.setPublished("");

        assertEquals("", profileInfo.getUuid());
        assertEquals("", profileInfo.getTitle());
        assertEquals("", profileInfo.getVersion());
        assertEquals("", profileInfo.getOscalVersion());
        assertEquals("", profileInfo.getLastModified());
        assertEquals("", profileInfo.getPublished());
    }

    @Test
    void testWithValidUuidFormat() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();
        String uuid = "a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6";
        profileInfo.setUuid(uuid);

        assertTrue(profileInfo.getUuid().contains("-"));
        assertEquals(36, profileInfo.getUuid().length());
    }

    @Test
    void testWithLongTitle() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();
        String longTitle = "This is a very long profile title that describes the security controls " +
                          "and baselines in great detail for a specific use case";
        profileInfo.setTitle(longTitle);
        assertEquals(longTitle, profileInfo.getTitle());
    }

    @Test
    void testWithSemanticVersioning() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();

        String[] versions = {"1.0.0", "2.1.3", "3.0.0-beta", "4.5.6-rc.1"};

        for (String version : versions) {
            profileInfo.setVersion(version);
            assertEquals(version, profileInfo.getVersion());
        }
    }

    @Test
    void testWithDifferentOscalVersions() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();

        String[] oscalVersions = {"1.0.0", "1.0.4", "1.1.0", "2.0.0"};

        for (String oscalVersion : oscalVersions) {
            profileInfo.setOscalVersion(oscalVersion);
            assertEquals(oscalVersion, profileInfo.getOscalVersion());
        }
    }

    @Test
    void testWithIso8601Timestamps() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();

        profileInfo.setLastModified("2024-03-20T15:45:30.123Z");
        profileInfo.setPublished("2024-01-01T00:00:00.000Z");

        assertTrue(profileInfo.getLastModified().contains("T"));
        assertTrue(profileInfo.getLastModified().contains("Z"));
        assertTrue(profileInfo.getPublished().contains("T"));
        assertTrue(profileInfo.getPublished().contains("Z"));
    }

    @Test
    void testCompleteProfileScenario() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();

        profileInfo.setUuid("f8a3e2b1-c5d4-4a1b-9e8f-7d6c5b4a3e2f");
        profileInfo.setTitle("NIST SP 800-53 Revision 5 High Baseline");
        profileInfo.setVersion("5.1.1");
        profileInfo.setOscalVersion("1.0.4");
        profileInfo.setPublished("2023-09-01T00:00:00Z");
        profileInfo.setLastModified("2024-03-15T12:30:00Z");

        assertNotNull(profileInfo);
        assertTrue(profileInfo.getTitle().contains("NIST"));
        assertTrue(profileInfo.getVersion().startsWith("5"));
        assertTrue(profileInfo.getOscalVersion().startsWith("1.0"));
    }

    @Test
    void testMultipleInstancesIndependence() {
        ProfileVisualizationResult.ProfileInfo profile1 =
            new ProfileVisualizationResult.ProfileInfo();
        ProfileVisualizationResult.ProfileInfo profile2 =
            new ProfileVisualizationResult.ProfileInfo();

        profile1.setUuid("uuid-1");
        profile1.setTitle("Profile 1");

        profile2.setUuid("uuid-2");
        profile2.setTitle("Profile 2");

        assertNotEquals(profile1.getUuid(), profile2.getUuid());
        assertNotEquals(profile1.getTitle(), profile2.getTitle());
    }

    @Test
    void testWithSpecialCharacters() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();

        profileInfo.setTitle("NIST SP 800-53 (Rev. 5) - High Baseline");
        profileInfo.setVersion("5.1.1-final");

        assertTrue(profileInfo.getTitle().contains("("));
        assertTrue(profileInfo.getTitle().contains(")"));
        assertTrue(profileInfo.getTitle().contains("-"));
        assertTrue(profileInfo.getVersion().contains("-"));
    }

    @Test
    void testPublishedBeforeLastModified() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();

        profileInfo.setPublished("2024-01-01T00:00:00Z");
        profileInfo.setLastModified("2024-06-15T10:30:00Z");

        assertNotNull(profileInfo.getPublished());
        assertNotNull(profileInfo.getLastModified());
        // In a real scenario, you'd parse and compare timestamps
        assertTrue(profileInfo.getPublished().compareTo(profileInfo.getLastModified()) < 0);
    }

    @Test
    void testFieldAssignmentOrder() {
        ProfileVisualizationResult.ProfileInfo profileInfo =
            new ProfileVisualizationResult.ProfileInfo();

        // Set in different order
        profileInfo.setPublished("2024-01-01T00:00:00Z");
        profileInfo.setUuid("uuid-123");
        profileInfo.setOscalVersion("1.0.4");
        profileInfo.setTitle("Test Profile");
        profileInfo.setLastModified("2024-06-01T00:00:00Z");
        profileInfo.setVersion("1.0.0");

        // Order shouldn't matter, all should be set correctly
        assertEquals("uuid-123", profileInfo.getUuid());
        assertEquals("Test Profile", profileInfo.getTitle());
        assertEquals("1.0.0", profileInfo.getVersion());
        assertEquals("1.0.4", profileInfo.getOscalVersion());
        assertEquals("2024-06-01T00:00:00Z", profileInfo.getLastModified());
        assertEquals("2024-01-01T00:00:00Z", profileInfo.getPublished());
    }
}

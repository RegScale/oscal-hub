/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.model.health;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComponentHealthTest {

    // ========== Constructor Tests ==========

    @Test
    void testDefaultConstructor() {
        ComponentHealth health = new ComponentHealth();

        assertNull(health.getStatus());
        assertNull(health.getMessage());
        assertNull(health.getDetails());
        assertNull(health.getResponseTimeMs());
    }

    @Test
    void testTwoArgConstructor() {
        ComponentHealth health = new ComponentHealth("UP", "Component is healthy");

        assertEquals("UP", health.getStatus());
        assertEquals("Component is healthy", health.getMessage());
        assertNull(health.getDetails());
        assertNull(health.getResponseTimeMs());
    }

    @Test
    void testFullConstructor() {
        Map<String, Object> details = new HashMap<>();
        details.put("database", "PostgreSQL");
        details.put("version", "15.0");

        ComponentHealth health = new ComponentHealth("UP", "Database is healthy", details, 15L);

        assertEquals("UP", health.getStatus());
        assertEquals("Database is healthy", health.getMessage());
        assertEquals(details, health.getDetails());
        assertEquals(15L, health.getResponseTimeMs());
    }

    // ========== Setter/Getter Tests ==========

    @Test
    void testSettersAndGetters() {
        ComponentHealth health = new ComponentHealth();

        Map<String, Object> details = new HashMap<>();
        details.put("key", "value");

        health.setStatus("DEGRADED");
        health.setMessage("High memory usage");
        health.setDetails(details);
        health.setResponseTimeMs(100L);

        assertEquals("DEGRADED", health.getStatus());
        assertEquals("High memory usage", health.getMessage());
        assertEquals(details, health.getDetails());
        assertEquals(100L, health.getResponseTimeMs());
    }

    @Test
    void testNullValues() {
        ComponentHealth health = new ComponentHealth("UP", "Test", new HashMap<>(), 10L);

        health.setStatus(null);
        health.setMessage(null);
        health.setDetails(null);
        health.setResponseTimeMs(null);

        assertNull(health.getStatus());
        assertNull(health.getMessage());
        assertNull(health.getDetails());
        assertNull(health.getResponseTimeMs());
    }

    // ========== Builder Pattern Tests ==========

    @Test
    void testBuilder_allFields() {
        Map<String, Object> details = new HashMap<>();
        details.put("heapUsedMb", 450);
        details.put("heapMaxMb", 1024);

        ComponentHealth health = ComponentHealth.builder()
                .status("UP")
                .message("Memory usage is healthy")
                .details(details)
                .responseTimeMs(5L)
                .build();

        assertEquals("UP", health.getStatus());
        assertEquals("Memory usage is healthy", health.getMessage());
        assertEquals(details, health.getDetails());
        assertEquals(5L, health.getResponseTimeMs());
    }

    @Test
    void testBuilder_statusOnly() {
        ComponentHealth health = ComponentHealth.builder()
                .status("DOWN")
                .build();

        assertEquals("DOWN", health.getStatus());
        assertNull(health.getMessage());
        assertNull(health.getDetails());
        assertNull(health.getResponseTimeMs());
    }

    @Test
    void testBuilder_statusAndMessage() {
        ComponentHealth health = ComponentHealth.builder()
                .status("UNKNOWN")
                .message("Unable to determine status")
                .build();

        assertEquals("UNKNOWN", health.getStatus());
        assertEquals("Unable to determine status", health.getMessage());
        assertNull(health.getDetails());
        assertNull(health.getResponseTimeMs());
    }

    @Test
    void testBuilder_withDetails() {
        Map<String, Object> details = Map.of("provider", "azure_blob_storage", "configured", true);

        ComponentHealth health = ComponentHealth.builder()
                .status("UP")
                .message("Storage configured")
                .details(details)
                .build();

        assertEquals("UP", health.getStatus());
        assertEquals(details, health.getDetails());
        assertTrue((Boolean) health.getDetails().get("configured"));
    }

    @Test
    void testBuilder_withResponseTime() {
        ComponentHealth health = ComponentHealth.builder()
                .status("UP")
                .responseTimeMs(150L)
                .build();

        assertEquals(150L, health.getResponseTimeMs());
    }

    @Test
    void testBuilder_emptyBuild() {
        ComponentHealth health = ComponentHealth.builder().build();

        assertNull(health.getStatus());
        assertNull(health.getMessage());
        assertNull(health.getDetails());
        assertNull(health.getResponseTimeMs());
    }

    // ========== Status Value Tests ==========

    @Test
    void testStatusUp() {
        ComponentHealth health = ComponentHealth.builder()
                .status("UP")
                .message("Component is fully operational")
                .build();

        assertEquals("UP", health.getStatus());
    }

    @Test
    void testStatusDown() {
        ComponentHealth health = ComponentHealth.builder()
                .status("DOWN")
                .message("Component is not working")
                .build();

        assertEquals("DOWN", health.getStatus());
    }

    @Test
    void testStatusDegraded() {
        ComponentHealth health = ComponentHealth.builder()
                .status("DEGRADED")
                .message("Component is experiencing issues")
                .build();

        assertEquals("DEGRADED", health.getStatus());
    }

    @Test
    void testStatusUnknown() {
        ComponentHealth health = ComponentHealth.builder()
                .status("UNKNOWN")
                .message("Unknown component: foo")
                .build();

        assertEquals("UNKNOWN", health.getStatus());
    }

    // ========== Details Tests ==========

    @Test
    void testDetailsWithDatabaseInfo() {
        Map<String, Object> details = new HashMap<>();
        details.put("database", "PostgreSQL");
        details.put("databaseVersion", "15.0");
        details.put("driverName", "PostgreSQL JDBC Driver");
        details.put("url", "jdbc:postgresql://localhost:5432/test");

        ComponentHealth health = ComponentHealth.builder()
                .status("UP")
                .details(details)
                .build();

        assertEquals("PostgreSQL", health.getDetails().get("database"));
        assertEquals("15.0", health.getDetails().get("databaseVersion"));
    }

    @Test
    void testDetailsWithMemoryInfo() {
        Map<String, Object> details = new HashMap<>();
        details.put("heapUsedMb", 450L);
        details.put("heapMaxMb", 1024L);
        details.put("usagePercent", 45);

        ComponentHealth health = ComponentHealth.builder()
                .status("UP")
                .details(details)
                .build();

        assertEquals(450L, health.getDetails().get("heapUsedMb"));
        assertEquals(45, health.getDetails().get("usagePercent"));
    }

    @Test
    void testDetailsWithError() {
        Map<String, Object> details = new HashMap<>();
        details.put("error", "SQLException");
        details.put("errorMessage", "Connection refused");

        ComponentHealth health = ComponentHealth.builder()
                .status("DOWN")
                .message("Database connection failed")
                .details(details)
                .build();

        assertEquals("SQLException", health.getDetails().get("error"));
        assertEquals("Connection refused", health.getDetails().get("errorMessage"));
    }

    @Test
    void testDetailsModification() {
        Map<String, Object> details = new HashMap<>();
        details.put("key", "value");

        ComponentHealth health = ComponentHealth.builder()
                .details(details)
                .build();

        // Verify the details can be retrieved and potentially modified
        Map<String, Object> retrievedDetails = health.getDetails();
        assertNotNull(retrievedDetails);
        assertEquals("value", retrievedDetails.get("key"));
    }
}

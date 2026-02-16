/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.model.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleHealthResponseTest {

    @Test
    void testDefaultConstructor() {
        SimpleHealthResponse response = new SimpleHealthResponse();

        assertNull(response.getStatus());
        assertNull(response.getTimestamp());
        assertNull(response.getVersion());
    }

    @Test
    void testParameterizedConstructor() {
        String status = "UP";
        String timestamp = "2025-02-16T10:30:00Z";
        String version = "1.0.0";

        SimpleHealthResponse response = new SimpleHealthResponse(status, timestamp, version);

        assertEquals(status, response.getStatus());
        assertEquals(timestamp, response.getTimestamp());
        assertEquals(version, response.getVersion());
    }

    @Test
    void testSettersAndGetters() {
        SimpleHealthResponse response = new SimpleHealthResponse();

        response.setStatus("DOWN");
        response.setTimestamp("2025-02-16T12:00:00Z");
        response.setVersion("2.0.0");

        assertEquals("DOWN", response.getStatus());
        assertEquals("2025-02-16T12:00:00Z", response.getTimestamp());
        assertEquals("2.0.0", response.getVersion());
    }

    @Test
    void testStatusUp() {
        SimpleHealthResponse response = new SimpleHealthResponse("UP", "2025-02-16T10:30:00Z", "1.0.0");

        assertEquals("UP", response.getStatus());
    }

    @Test
    void testStatusDown() {
        SimpleHealthResponse response = new SimpleHealthResponse("DOWN", "2025-02-16T10:30:00Z", "1.0.0");

        assertEquals("DOWN", response.getStatus());
    }

    @Test
    void testNullValues() {
        SimpleHealthResponse response = new SimpleHealthResponse(null, null, null);

        assertNull(response.getStatus());
        assertNull(response.getTimestamp());
        assertNull(response.getVersion());
    }

    @Test
    void testSetNullValues() {
        SimpleHealthResponse response = new SimpleHealthResponse("UP", "2025-02-16T10:30:00Z", "1.0.0");

        response.setStatus(null);
        response.setTimestamp(null);
        response.setVersion(null);

        assertNull(response.getStatus());
        assertNull(response.getTimestamp());
        assertNull(response.getVersion());
    }

    @Test
    void testOverwriteValues() {
        SimpleHealthResponse response = new SimpleHealthResponse("UP", "2025-02-16T10:30:00Z", "1.0.0");

        response.setStatus("DOWN");
        response.setTimestamp("2025-02-16T11:00:00Z");
        response.setVersion("1.1.0");

        assertEquals("DOWN", response.getStatus());
        assertEquals("2025-02-16T11:00:00Z", response.getTimestamp());
        assertEquals("1.1.0", response.getVersion());
    }
}

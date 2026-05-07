/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.model.health;

import gov.nist.oscal.tools.api.model.health.DetailedHealthResponse.ApplicationInfo;
import gov.nist.oscal.tools.api.model.health.DetailedHealthResponse.EnvironmentInfo;
import gov.nist.oscal.tools.api.model.health.DetailedHealthResponse.SystemInfo;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetailedHealthResponseTest {

    // ========== DetailedHealthResponse Tests ==========

    @Test
    void testDefaultConstructor() {
        DetailedHealthResponse response = new DetailedHealthResponse();

        assertNull(response.getStatus());
        assertNull(response.getTimestamp());
        assertNull(response.getApplication());
        assertNull(response.getComponents());
        assertNull(response.getSystem());
        assertNull(response.getEnvironment());
    }

    @Test
    void testSettersAndGetters() {
        DetailedHealthResponse response = new DetailedHealthResponse();

        response.setStatus("UP");
        response.setTimestamp("2025-02-16T10:30:00Z");
        response.setApplication(new ApplicationInfo());
        response.setComponents(new HashMap<>());
        response.setSystem(new SystemInfo());
        response.setEnvironment(new EnvironmentInfo());

        assertEquals("UP", response.getStatus());
        assertEquals("2025-02-16T10:30:00Z", response.getTimestamp());
        assertNotNull(response.getApplication());
        assertNotNull(response.getComponents());
        assertNotNull(response.getSystem());
        assertNotNull(response.getEnvironment());
    }

    @Test
    void testNullValues() {
        DetailedHealthResponse response = new DetailedHealthResponse();
        response.setStatus("UP");

        response.setStatus(null);
        response.setTimestamp(null);
        response.setApplication(null);
        response.setComponents(null);
        response.setSystem(null);
        response.setEnvironment(null);

        assertNull(response.getStatus());
        assertNull(response.getTimestamp());
        assertNull(response.getApplication());
        assertNull(response.getComponents());
        assertNull(response.getSystem());
        assertNull(response.getEnvironment());
    }

    @Test
    void testWithComponents() {
        DetailedHealthResponse response = new DetailedHealthResponse();

        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        components.put("database", ComponentHealth.builder()
                .status("UP")
                .message("Database is healthy")
                .build());
        components.put("memory", ComponentHealth.builder()
                .status("UP")
                .message("Memory usage normal")
                .build());

        response.setComponents(components);

        assertEquals(2, response.getComponents().size());
        assertTrue(response.getComponents().containsKey("database"));
        assertTrue(response.getComponents().containsKey("memory"));
        assertEquals("UP", response.getComponents().get("database").getStatus());
    }

    @Test
    void testFullResponse() {
        DetailedHealthResponse response = new DetailedHealthResponse();

        // Set status
        response.setStatus("UP");
        response.setTimestamp("2025-02-16T10:30:00Z");

        // Set application info
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.setName("oscal-cli-api");
        appInfo.setVersion("1.0.0");
        appInfo.setStartTime("2025-02-14T05:00:00Z");
        response.setApplication(appInfo);

        // Set components
        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        components.put("database", ComponentHealth.builder().status("UP").build());
        components.put("storage", ComponentHealth.builder().status("UP").build());
        components.put("memory", ComponentHealth.builder().status("UP").build());
        components.put("diskSpace", ComponentHealth.builder().status("UP").build());
        components.put("oscalLibrary", ComponentHealth.builder().status("UP").build());
        response.setComponents(components);

        // Set system info
        SystemInfo sysInfo = new SystemInfo();
        sysInfo.setTotalMemoryMb(1024);
        sysInfo.setUsedMemoryMb(450);
        sysInfo.setFreeMemoryMb(574);
        response.setSystem(sysInfo);

        // Set environment info
        EnvironmentInfo envInfo = new EnvironmentInfo();
        envInfo.setJavaVersion("17.0.2");
        envInfo.setOsName("Linux");
        response.setEnvironment(envInfo);

        // Assertions
        assertEquals("UP", response.getStatus());
        assertEquals("oscal-cli-api", response.getApplication().getName());
        assertEquals(5, response.getComponents().size());
        assertEquals(1024, response.getSystem().getTotalMemoryMb());
        assertEquals("17.0.2", response.getEnvironment().getJavaVersion());
    }

    // ========== ApplicationInfo Tests ==========

    @Test
    void testApplicationInfo_defaultConstructor() {
        ApplicationInfo appInfo = new ApplicationInfo();

        assertNull(appInfo.getName());
        assertNull(appInfo.getVersion());
        assertNull(appInfo.getStartTime());
    }

    @Test
    void testApplicationInfo_settersAndGetters() {
        ApplicationInfo appInfo = new ApplicationInfo();

        appInfo.setName("oscal-cli-api");
        appInfo.setVersion("1.0.0");
        appInfo.setStartTime("2025-02-06T08:00:00Z");

        assertEquals("oscal-cli-api", appInfo.getName());
        assertEquals("1.0.0", appInfo.getVersion());
        assertEquals("2025-02-06T08:00:00Z", appInfo.getStartTime());
    }

    @Test
    void testApplicationInfo_nullValues() {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.setName("test");

        appInfo.setName(null);
        appInfo.setVersion(null);
        appInfo.setStartTime(null);

        assertNull(appInfo.getName());
        assertNull(appInfo.getVersion());
        assertNull(appInfo.getStartTime());
    }

    // ========== SystemInfo Tests ==========

    @Test
    void testSystemInfo_defaultConstructor() {
        SystemInfo sysInfo = new SystemInfo();

        assertEquals(0, sysInfo.getTotalMemoryMb());
        assertEquals(0, sysInfo.getUsedMemoryMb());
        assertEquals(0, sysInfo.getFreeMemoryMb());
        assertEquals(0.0, sysInfo.getMemoryUsagePercent());
        assertEquals(0, sysInfo.getAvailableProcessors());
        assertEquals(0.0, sysInfo.getSystemLoadAverage());
        assertEquals(0, sysInfo.getTotalDiskSpaceGb());
        assertEquals(0, sysInfo.getFreeDiskSpaceGb());
        assertEquals(0.0, sysInfo.getDiskUsagePercent());
    }

    @Test
    void testSystemInfo_settersAndGetters() {
        SystemInfo sysInfo = new SystemInfo();

        sysInfo.setTotalMemoryMb(4096);
        sysInfo.setUsedMemoryMb(2048);
        sysInfo.setFreeMemoryMb(2048);
        sysInfo.setMemoryUsagePercent(50.0);
        sysInfo.setAvailableProcessors(8);
        sysInfo.setSystemLoadAverage(2.5);
        sysInfo.setTotalDiskSpaceGb(500);
        sysInfo.setFreeDiskSpaceGb(250);
        sysInfo.setDiskUsagePercent(50.0);

        assertEquals(4096, sysInfo.getTotalMemoryMb());
        assertEquals(2048, sysInfo.getUsedMemoryMb());
        assertEquals(2048, sysInfo.getFreeMemoryMb());
        assertEquals(50.0, sysInfo.getMemoryUsagePercent());
        assertEquals(8, sysInfo.getAvailableProcessors());
        assertEquals(2.5, sysInfo.getSystemLoadAverage());
        assertEquals(500, sysInfo.getTotalDiskSpaceGb());
        assertEquals(250, sysInfo.getFreeDiskSpaceGb());
        assertEquals(50.0, sysInfo.getDiskUsagePercent());
    }

    @Test
    void testSystemInfo_memoryCalculations() {
        SystemInfo sysInfo = new SystemInfo();

        // Simulate 45% memory usage
        sysInfo.setTotalMemoryMb(1024);
        sysInfo.setUsedMemoryMb(460);
        sysInfo.setFreeMemoryMb(564);
        sysInfo.setMemoryUsagePercent(45.0);

        assertTrue(sysInfo.getUsedMemoryMb() < sysInfo.getTotalMemoryMb());
        assertTrue(sysInfo.getFreeMemoryMb() > 0);
        assertTrue(sysInfo.getMemoryUsagePercent() < 100);
    }

    @Test
    void testSystemInfo_diskCalculations() {
        SystemInfo sysInfo = new SystemInfo();

        // Simulate 35% disk usage
        sysInfo.setTotalDiskSpaceGb(500);
        sysInfo.setFreeDiskSpaceGb(325);
        sysInfo.setDiskUsagePercent(35.0);

        assertTrue(sysInfo.getFreeDiskSpaceGb() < sysInfo.getTotalDiskSpaceGb());
        assertTrue(sysInfo.getDiskUsagePercent() < 100);
    }

    @Test
    void testSystemInfo_highUsage() {
        SystemInfo sysInfo = new SystemInfo();

        // High memory usage (95%)
        sysInfo.setTotalMemoryMb(1024);
        sysInfo.setUsedMemoryMb(972);
        sysInfo.setFreeMemoryMb(52);
        sysInfo.setMemoryUsagePercent(95.0);

        // High disk usage (90%)
        sysInfo.setTotalDiskSpaceGb(500);
        sysInfo.setFreeDiskSpaceGb(50);
        sysInfo.setDiskUsagePercent(90.0);

        assertTrue(sysInfo.getMemoryUsagePercent() > 90);
        assertTrue(sysInfo.getDiskUsagePercent() >= 90);
    }

    @Test
    void testSystemInfo_loadAverage() {
        SystemInfo sysInfo = new SystemInfo();

        sysInfo.setAvailableProcessors(4);
        sysInfo.setSystemLoadAverage(2.0);

        // Load average < processors typically means system is not overloaded
        assertTrue(sysInfo.getSystemLoadAverage() < sysInfo.getAvailableProcessors());

        // High load scenario
        sysInfo.setSystemLoadAverage(8.0);
        assertTrue(sysInfo.getSystemLoadAverage() > sysInfo.getAvailableProcessors());
    }

    // ========== EnvironmentInfo Tests ==========

    @Test
    void testEnvironmentInfo_defaultConstructor() {
        EnvironmentInfo envInfo = new EnvironmentInfo();

        assertNull(envInfo.getJavaVersion());
        assertNull(envInfo.getOsName());
        assertNull(envInfo.getTimezone());
    }

    @Test
    void testEnvironmentInfo_settersAndGetters() {
        EnvironmentInfo envInfo = new EnvironmentInfo();

        envInfo.setJavaVersion("17.0.2");
        envInfo.setOsName("Linux");
        envInfo.setTimezone("America/New_York");

        assertEquals("17.0.2", envInfo.getJavaVersion());
        assertEquals("Linux", envInfo.getOsName());
        assertEquals("America/New_York", envInfo.getTimezone());
    }

    @Test
    void testEnvironmentInfo_nullValues() {
        EnvironmentInfo envInfo = new EnvironmentInfo();
        envInfo.setJavaVersion("17");

        envInfo.setJavaVersion(null);
        envInfo.setOsName(null);
        envInfo.setTimezone(null);

        assertNull(envInfo.getJavaVersion());
        assertNull(envInfo.getOsName());
        assertNull(envInfo.getTimezone());
    }

    @Test
    void testEnvironmentInfo_linuxEnvironment() {
        EnvironmentInfo envInfo = new EnvironmentInfo();

        envInfo.setJavaVersion("17.0.2");
        envInfo.setOsName("Linux");
        envInfo.setTimezone("UTC");

        assertEquals("Linux", envInfo.getOsName());
    }

    @Test
    void testEnvironmentInfo_macEnvironment() {
        EnvironmentInfo envInfo = new EnvironmentInfo();

        envInfo.setJavaVersion("17.0.2");
        envInfo.setOsName("Mac OS X");
        envInfo.setTimezone("America/Los_Angeles");

        assertEquals("Mac OS X", envInfo.getOsName());
    }

    @Test
    void testEnvironmentInfo_windowsEnvironment() {
        EnvironmentInfo envInfo = new EnvironmentInfo();

        envInfo.setJavaVersion("17.0.2");
        envInfo.setOsName("Windows 11");
        envInfo.setTimezone("America/New_York");

        assertEquals("Windows 11", envInfo.getOsName());
    }

    @Test
    void testEnvironmentInfo_differentTimezones() {
        EnvironmentInfo est = new EnvironmentInfo();
        est.setTimezone("America/New_York");

        EnvironmentInfo pst = new EnvironmentInfo();
        pst.setTimezone("America/Los_Angeles");

        EnvironmentInfo utc = new EnvironmentInfo();
        utc.setTimezone("UTC");

        assertEquals("America/New_York", est.getTimezone());
        assertEquals("America/Los_Angeles", pst.getTimezone());
        assertEquals("UTC", utc.getTimezone());
    }
}

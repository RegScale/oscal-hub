package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SspVisualizationResultTest {

    // Test SystemInfo.SystemId nested class (0% coverage)
    @Test
    void testSystemIdNoArgsConstructor() {
        SspVisualizationResult.SystemInfo.SystemId systemId = new SspVisualizationResult.SystemInfo.SystemId();

        assertNotNull(systemId);
        assertNull(systemId.getIdentifierType());
        assertNull(systemId.getId());
    }

    @Test
    void testSystemIdAllArgsConstructor() {
        String type = "fedramp";
        String id = "F00000001";

        SspVisualizationResult.SystemInfo.SystemId systemId =
            new SspVisualizationResult.SystemInfo.SystemId(type, id);

        assertNotNull(systemId);
        assertEquals(type, systemId.getIdentifierType());
        assertEquals(id, systemId.getId());
    }

    @Test
    void testSystemIdGettersAndSetters() {
        SspVisualizationResult.SystemInfo.SystemId systemId = new SspVisualizationResult.SystemInfo.SystemId();

        String type = "internal";
        String id = "SYS-001";

        systemId.setIdentifierType(type);
        systemId.setId(id);

        assertEquals(type, systemId.getIdentifierType());
        assertEquals(id, systemId.getId());
    }

    @Test
    void testSystemIdSetToNull() {
        SspVisualizationResult.SystemInfo.SystemId systemId =
            new SspVisualizationResult.SystemInfo.SystemId("type", "id");

        systemId.setIdentifierType(null);
        systemId.setId(null);

        assertNull(systemId.getIdentifierType());
        assertNull(systemId.getId());
    }

    // Test SystemInfo class
    @Test
    void testSystemInfoGettersAndSetters() {
        SspVisualizationResult.SystemInfo systemInfo = new SspVisualizationResult.SystemInfo();

        systemInfo.setUuid("uuid-123");
        systemInfo.setName("Test System");
        systemInfo.setShortName("TS");
        systemInfo.setDescription("A test system");
        systemInfo.setStatus("operational");

        assertEquals("uuid-123", systemInfo.getUuid());
        assertEquals("Test System", systemInfo.getName());
        assertEquals("TS", systemInfo.getShortName());
        assertEquals("A test system", systemInfo.getDescription());
        assertEquals("operational", systemInfo.getStatus());
    }

    @Test
    void testSystemInfoWithSystemIds() {
        SspVisualizationResult.SystemInfo systemInfo = new SspVisualizationResult.SystemInfo();

        List<SspVisualizationResult.SystemInfo.SystemId> systemIds = new ArrayList<>();
        systemIds.add(new SspVisualizationResult.SystemInfo.SystemId("fedramp", "F00001"));
        systemIds.add(new SspVisualizationResult.SystemInfo.SystemId("internal", "SYS-001"));

        systemInfo.setSystemIds(systemIds);

        assertEquals(2, systemInfo.getSystemIds().size());
        assertEquals("fedramp", systemInfo.getSystemIds().get(0).getIdentifierType());
        assertEquals("F00001", systemInfo.getSystemIds().get(0).getId());
    }

    // Test SecurityCategorization class
    @Test
    void testSecurityCategorizationGettersAndSetters() {
        SspVisualizationResult.SecurityCategorization categorization =
            new SspVisualizationResult.SecurityCategorization();

        categorization.setConfidentiality("moderate");
        categorization.setIntegrity("high");
        categorization.setAvailability("low");
        categorization.setOverall("moderate");

        assertEquals("moderate", categorization.getConfidentiality());
        assertEquals("high", categorization.getIntegrity());
        assertEquals("low", categorization.getAvailability());
        assertEquals("moderate", categorization.getOverall());
    }

    // Test InformationType.ImpactLevel nested class
    @Test
    void testImpactLevelNoArgsConstructor() {
        SspVisualizationResult.InformationType.ImpactLevel impactLevel =
            new SspVisualizationResult.InformationType.ImpactLevel();

        assertNotNull(impactLevel);
        assertNull(impactLevel.getBase());
        assertNull(impactLevel.getSelected());
    }

    @Test
    void testImpactLevelAllArgsConstructor() {
        String base = "fips-199-low";
        String selected = "fips-199-moderate";

        SspVisualizationResult.InformationType.ImpactLevel impactLevel =
            new SspVisualizationResult.InformationType.ImpactLevel(base, selected);

        assertEquals(base, impactLevel.getBase());
        assertEquals(selected, impactLevel.getSelected());
    }

    @Test
    void testImpactLevelGettersAndSetters() {
        SspVisualizationResult.InformationType.ImpactLevel impactLevel =
            new SspVisualizationResult.InformationType.ImpactLevel();

        impactLevel.setBase("fips-199-low");
        impactLevel.setSelected("fips-199-high");

        assertEquals("fips-199-low", impactLevel.getBase());
        assertEquals("fips-199-high", impactLevel.getSelected());
    }

    // Test InformationType class
    @Test
    void testInformationTypeGettersAndSetters() {
        SspVisualizationResult.InformationType infoType = new SspVisualizationResult.InformationType();

        infoType.setUuid("uuid-456");
        infoType.setTitle("Customer Data");
        infoType.setDescription("Sensitive customer information");

        List<String> categorizations = List.of("cat1", "cat2");
        infoType.setCategorizations(categorizations);

        SspVisualizationResult.InformationType.ImpactLevel confidentiality =
            new SspVisualizationResult.InformationType.ImpactLevel("low", "moderate");
        infoType.setConfidentiality(confidentiality);

        SspVisualizationResult.InformationType.ImpactLevel integrity =
            new SspVisualizationResult.InformationType.ImpactLevel("low", "high");
        infoType.setIntegrity(integrity);

        SspVisualizationResult.InformationType.ImpactLevel availability =
            new SspVisualizationResult.InformationType.ImpactLevel("low", "low");
        infoType.setAvailability(availability);

        assertEquals("uuid-456", infoType.getUuid());
        assertEquals("Customer Data", infoType.getTitle());
        assertEquals("Sensitive customer information", infoType.getDescription());
        assertEquals(2, infoType.getCategorizations().size());
        assertEquals("moderate", infoType.getConfidentiality().getSelected());
        assertEquals("high", infoType.getIntegrity().getSelected());
        assertEquals("low", infoType.getAvailability().getSelected());
    }

    // Test PersonnelRole.Person nested class
    @Test
    void testPersonNoArgsConstructor() {
        SspVisualizationResult.PersonnelRole.Person person =
            new SspVisualizationResult.PersonnelRole.Person();

        assertNotNull(person);
        assertNull(person.getUuid());
        assertNull(person.getName());
        assertNull(person.getJobTitle());
        assertNull(person.getType());
    }

    @Test
    void testPersonAllArgsConstructor() {
        String uuid = "person-uuid";
        String name = "John Doe";
        String jobTitle = "Security Engineer";
        String type = "internal";

        SspVisualizationResult.PersonnelRole.Person person =
            new SspVisualizationResult.PersonnelRole.Person(uuid, name, jobTitle, type);

        assertEquals(uuid, person.getUuid());
        assertEquals(name, person.getName());
        assertEquals(jobTitle, person.getJobTitle());
        assertEquals(type, person.getType());
    }

    @Test
    void testPersonGettersAndSetters() {
        SspVisualizationResult.PersonnelRole.Person person =
            new SspVisualizationResult.PersonnelRole.Person();

        person.setUuid("uuid-789");
        person.setName("Jane Smith");
        person.setJobTitle("CISO");
        person.setType("external");

        assertEquals("uuid-789", person.getUuid());
        assertEquals("Jane Smith", person.getName());
        assertEquals("CISO", person.getJobTitle());
        assertEquals("external", person.getType());
    }

    // Test PersonnelRole class
    @Test
    void testPersonnelRoleGettersAndSetters() {
        SspVisualizationResult.PersonnelRole role = new SspVisualizationResult.PersonnelRole();

        role.setRoleId("role-123");
        role.setRoleTitle("System Administrator");
        role.setRoleShortName("SysAdmin");

        List<SspVisualizationResult.PersonnelRole.Person> personnel = new ArrayList<>();
        personnel.add(new SspVisualizationResult.PersonnelRole.Person(
            "p1", "Alice", "Admin", "internal"));
        personnel.add(new SspVisualizationResult.PersonnelRole.Person(
            "p2", "Bob", "Admin", "internal"));

        role.setAssignedPersonnel(personnel);

        assertEquals("role-123", role.getRoleId());
        assertEquals("System Administrator", role.getRoleTitle());
        assertEquals("SysAdmin", role.getRoleShortName());
        assertEquals(2, role.getAssignedPersonnel().size());
        assertEquals("Alice", role.getAssignedPersonnel().get(0).getName());
    }

    // Test ControlFamilyStatus.ControlStatus nested class
    @Test
    void testControlStatusNoArgsConstructor() {
        SspVisualizationResult.ControlFamilyStatus.ControlStatus controlStatus =
            new SspVisualizationResult.ControlFamilyStatus.ControlStatus();

        assertNotNull(controlStatus);
        assertNull(controlStatus.getControlId());
        assertNull(controlStatus.getImplementationStatus());
        assertNull(controlStatus.getControlOrigination());
    }

    @Test
    void testControlStatusAllArgsConstructor() {
        String controlId = "AC-1";
        String implStatus = "implemented";
        String origination = "customer-configured";

        SspVisualizationResult.ControlFamilyStatus.ControlStatus controlStatus =
            new SspVisualizationResult.ControlFamilyStatus.ControlStatus(
                controlId, implStatus, origination);

        assertEquals(controlId, controlStatus.getControlId());
        assertEquals(implStatus, controlStatus.getImplementationStatus());
        assertEquals(origination, controlStatus.getControlOrigination());
    }

    @Test
    void testControlStatusGettersAndSetters() {
        SspVisualizationResult.ControlFamilyStatus.ControlStatus controlStatus =
            new SspVisualizationResult.ControlFamilyStatus.ControlStatus();

        controlStatus.setControlId("AC-2");
        controlStatus.setImplementationStatus("planned");
        controlStatus.setControlOrigination("inherited");

        assertEquals("AC-2", controlStatus.getControlId());
        assertEquals("planned", controlStatus.getImplementationStatus());
        assertEquals("inherited", controlStatus.getControlOrigination());
    }

    // Test ControlFamilyStatus class
    @Test
    void testControlFamilyStatusGettersAndSetters() {
        SspVisualizationResult.ControlFamilyStatus familyStatus =
            new SspVisualizationResult.ControlFamilyStatus();

        familyStatus.setFamilyId("AC");
        familyStatus.setFamilyName("Access Control");
        familyStatus.setTotalControls(10);

        Map<String, Integer> statusCounts = new HashMap<>();
        statusCounts.put("implemented", 5);
        statusCounts.put("planned", 3);
        statusCounts.put("not-applicable", 2);
        familyStatus.setStatusCounts(statusCounts);

        List<SspVisualizationResult.ControlFamilyStatus.ControlStatus> controls = new ArrayList<>();
        controls.add(new SspVisualizationResult.ControlFamilyStatus.ControlStatus(
            "AC-1", "implemented", "customer-configured"));
        controls.add(new SspVisualizationResult.ControlFamilyStatus.ControlStatus(
            "AC-2", "planned", "inherited"));
        familyStatus.setControls(controls);

        assertEquals("AC", familyStatus.getFamilyId());
        assertEquals("Access Control", familyStatus.getFamilyName());
        assertEquals(10, familyStatus.getTotalControls());
        assertEquals(3, familyStatus.getStatusCounts().size());
        assertEquals(5, familyStatus.getStatusCounts().get("implemented"));
        assertEquals(2, familyStatus.getControls().size());
        assertEquals("AC-1", familyStatus.getControls().get(0).getControlId());
    }

    // Test Asset class
    @Test
    void testAssetGettersAndSetters() {
        SspVisualizationResult.Asset asset = new SspVisualizationResult.Asset();

        asset.setUuid("asset-uuid");
        asset.setDescription("Web Server");
        asset.setAssetType("hardware");
        asset.setFunction("web-server");
        asset.setFqdn("web.example.com");
        asset.setIpv4Address("192.168.1.100");
        asset.setIpv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        asset.setMacAddress("00:1B:44:11:3A:B7");
        asset.setVirtual(true);
        asset.setPublicAccess(false);
        asset.setSoftwareName("Apache");
        asset.setSoftwareVersion("2.4.52");
        asset.setVendorName("Apache Software Foundation");
        asset.setScanned(true);

        assertEquals("asset-uuid", asset.getUuid());
        assertEquals("Web Server", asset.getDescription());
        assertEquals("hardware", asset.getAssetType());
        assertEquals("web-server", asset.getFunction());
        assertEquals("web.example.com", asset.getFqdn());
        assertEquals("192.168.1.100", asset.getIpv4Address());
        assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", asset.getIpv6Address());
        assertEquals("00:1B:44:11:3A:B7", asset.getMacAddress());
        assertTrue(asset.isVirtual());
        assertFalse(asset.isPublicAccess());
        assertEquals("Apache", asset.getSoftwareName());
        assertEquals("2.4.52", asset.getSoftwareVersion());
        assertEquals("Apache Software Foundation", asset.getVendorName());
        assertTrue(asset.isScanned());
    }

    @Test
    void testAssetBooleanDefaults() {
        SspVisualizationResult.Asset asset = new SspVisualizationResult.Asset();

        // Test default boolean values
        assertFalse(asset.isVirtual());
        assertFalse(asset.isPublicAccess());
        assertFalse(asset.isScanned());
    }

    // Test main SspVisualizationResult class
    @Test
    void testSspVisualizationResultNoArgsConstructor() {
        SspVisualizationResult result = new SspVisualizationResult();

        assertNotNull(result);
        assertNotNull(result.getTimestamp()); // timestamp is set in constructor
        assertFalse(result.isSuccess()); // default boolean is false
        assertNull(result.getMessage());
        assertNotNull(result.getInformationTypes()); // initialized to new ArrayList
        assertNotNull(result.getPersonnel()); // initialized to new ArrayList
        assertNotNull(result.getControlsByFamily()); // initialized to new HashMap
        assertNotNull(result.getAssets()); // initialized to new ArrayList
    }

    @Test
    void testSspVisualizationResultTwoArgsConstructor() {
        boolean success = true;
        String message = "Visualization successful";

        SspVisualizationResult result = new SspVisualizationResult(success, message);

        assertTrue(result.isSuccess());
        assertEquals(message, result.getMessage());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testSspVisualizationResultGettersAndSetters() {
        SspVisualizationResult result = new SspVisualizationResult();

        result.setSuccess(true);
        result.setMessage("Test message");
        result.setTimestamp("2024-01-15T10:30:00Z");

        SspVisualizationResult.SystemInfo systemInfo = new SspVisualizationResult.SystemInfo();
        systemInfo.setName("Test System");
        result.setSystemInfo(systemInfo);

        SspVisualizationResult.SecurityCategorization categorization =
            new SspVisualizationResult.SecurityCategorization();
        categorization.setOverall("moderate");
        result.setCategorization(categorization);

        List<SspVisualizationResult.InformationType> infoTypes = new ArrayList<>();
        infoTypes.add(new SspVisualizationResult.InformationType());
        result.setInformationTypes(infoTypes);

        List<SspVisualizationResult.PersonnelRole> personnel = new ArrayList<>();
        personnel.add(new SspVisualizationResult.PersonnelRole());
        result.setPersonnel(personnel);

        Map<String, SspVisualizationResult.ControlFamilyStatus> controlsByFamily = new HashMap<>();
        controlsByFamily.put("AC", new SspVisualizationResult.ControlFamilyStatus());
        result.setControlsByFamily(controlsByFamily);

        List<SspVisualizationResult.Asset> assets = new ArrayList<>();
        assets.add(new SspVisualizationResult.Asset());
        result.setAssets(assets);

        assertTrue(result.isSuccess());
        assertEquals("Test message", result.getMessage());
        assertEquals("2024-01-15T10:30:00Z", result.getTimestamp());
        assertEquals("Test System", result.getSystemInfo().getName());
        assertEquals("moderate", result.getCategorization().getOverall());
        assertEquals(1, result.getInformationTypes().size());
        assertEquals(1, result.getPersonnel().size());
        assertEquals(1, result.getControlsByFamily().size());
        assertEquals(1, result.getAssets().size());
    }
}

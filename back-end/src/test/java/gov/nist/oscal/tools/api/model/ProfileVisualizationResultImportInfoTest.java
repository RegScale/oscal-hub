package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProfileVisualizationResultImportInfoTest {

    @Test
    void testDefaultConstructor() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();

        assertNotNull(importInfo);
        assertNull(importInfo.getHref());
        assertNotNull(importInfo.getIncludeAllIds());
        assertNotNull(importInfo.getExcludeIds());
        assertEquals(0, importInfo.getIncludeAllIds().size());
        assertEquals(0, importInfo.getExcludeIds().size());
        assertEquals(0, importInfo.getEstimatedControlCount());
    }

    @Test
    void testSetHref() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        importInfo.setHref("https://example.com/catalogs/nist-800-53-rev5.json");
        assertEquals("https://example.com/catalogs/nist-800-53-rev5.json", importInfo.getHref());
    }

    @Test
    void testSetIncludeAllIds() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        List<String> includeIds = Arrays.asList("ac-1", "ac-2", "ac-3");
        importInfo.setIncludeAllIds(includeIds);

        assertEquals(3, importInfo.getIncludeAllIds().size());
        assertTrue(importInfo.getIncludeAllIds().contains("ac-1"));
        assertTrue(importInfo.getIncludeAllIds().contains("ac-2"));
    }

    @Test
    void testSetExcludeIds() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        List<String> excludeIds = Arrays.asList("sc-7", "sc-8");
        importInfo.setExcludeIds(excludeIds);

        assertEquals(2, importInfo.getExcludeIds().size());
        assertTrue(importInfo.getExcludeIds().contains("sc-7"));
        assertTrue(importInfo.getExcludeIds().contains("sc-8"));
    }

    @Test
    void testSetEstimatedControlCount() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        importInfo.setEstimatedControlCount(325);
        assertEquals(325, importInfo.getEstimatedControlCount());
    }

    @Test
    void testWithEmptyLists() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        importInfo.setIncludeAllIds(new ArrayList<>());
        importInfo.setExcludeIds(new ArrayList<>());

        assertEquals(0, importInfo.getIncludeAllIds().size());
        assertEquals(0, importInfo.getExcludeIds().size());
    }

    @Test
    void testWithLargeIncludeList() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        List<String> largeList = new ArrayList<>();
        for (int i = 1; i <= 200; i++) {
            largeList.add("control-" + i);
        }
        importInfo.setIncludeAllIds(largeList);
        assertEquals(200, importInfo.getIncludeAllIds().size());
    }

    @Test
    void testWithLargeExcludeList() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        List<String> excludeList = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            excludeList.add("exc-" + i);
        }
        importInfo.setExcludeIds(excludeList);
        assertEquals(50, importInfo.getExcludeIds().size());
    }

    @Test
    void testSetAllFields() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        importInfo.setHref("https://nist.gov/oscal/catalog.json");
        importInfo.setIncludeAllIds(Arrays.asList("ac-1", "ac-2", "au-1"));
        importInfo.setExcludeIds(Arrays.asList("sc-1"));
        importInfo.setEstimatedControlCount(150);

        assertEquals("https://nist.gov/oscal/catalog.json", importInfo.getHref());
        assertEquals(3, importInfo.getIncludeAllIds().size());
        assertEquals(1, importInfo.getExcludeIds().size());
        assertEquals(150, importInfo.getEstimatedControlCount());
    }

    @Test
    void testSetFieldsToNull() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        importInfo.setHref("test");
        importInfo.setIncludeAllIds(Arrays.asList("ac-1"));
        importInfo.setExcludeIds(Arrays.asList("sc-1"));

        importInfo.setHref(null);
        importInfo.setIncludeAllIds(null);
        importInfo.setExcludeIds(null);

        assertNull(importInfo.getHref());
        assertNull(importInfo.getIncludeAllIds());
        assertNull(importInfo.getExcludeIds());
    }

    @Test
    void testModifyAllFields() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();

        importInfo.setHref("old-href");
        importInfo.setIncludeAllIds(Arrays.asList("old-1", "old-2"));
        importInfo.setExcludeIds(Arrays.asList("exc-1"));
        importInfo.setEstimatedControlCount(100);

        importInfo.setHref("new-href");
        importInfo.setIncludeAllIds(Arrays.asList("new-1", "new-2", "new-3"));
        importInfo.setExcludeIds(Arrays.asList("exc-2", "exc-3"));
        importInfo.setEstimatedControlCount(200);

        assertEquals("new-href", importInfo.getHref());
        assertEquals(3, importInfo.getIncludeAllIds().size());
        assertEquals(2, importInfo.getExcludeIds().size());
        assertEquals(200, importInfo.getEstimatedControlCount());
    }

    @Test
    void testWithFilePathHref() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        importInfo.setHref("file:///local/path/to/catalog.xml");
        assertTrue(importInfo.getHref().startsWith("file://"));
    }

    @Test
    void testWithRelativeHref() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        importInfo.setHref("../catalogs/nist-800-53-r5.json");
        assertTrue(importInfo.getHref().contains("../"));
    }

    @Test
    void testCompleteImportScenario() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        importInfo.setHref("https://raw.githubusercontent.com/usnistgov/oscal-content/main/nist.gov/SP800-53/rev5/json/NIST_SP-800-53_rev5_catalog.json");

        List<String> includes = Arrays.asList(
                "ac-1", "ac-2", "ac-3", "ac-4", "ac-5", "ac-6",
                "au-1", "au-2", "au-3", "au-4", "au-5"
        );
        importInfo.setIncludeAllIds(includes);

        List<String> excludes = Arrays.asList("ac-2.1", "ac-2.2");
        importInfo.setExcludeIds(excludes);

        importInfo.setEstimatedControlCount(11);

        assertTrue(importInfo.getHref().contains("NIST_SP-800-53"));
        assertEquals(11, importInfo.getIncludeAllIds().size());
        assertEquals(2, importInfo.getExcludeIds().size());
        assertEquals(11, importInfo.getEstimatedControlCount());
    }

    @Test
    void testWithSpecialCharactersInControlIds() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        List<String> includeIds = Arrays.asList("ac-2.1", "ac-2.2", "ac-2(1)", "ac-2(2)");
        importInfo.setIncludeAllIds(includeIds);

        assertEquals(4, importInfo.getIncludeAllIds().size());
        assertTrue(importInfo.getIncludeAllIds().contains("ac-2(1)"));
    }

    @Test
    void testZeroEstimatedCount() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        importInfo.setEstimatedControlCount(0);
        assertEquals(0, importInfo.getEstimatedControlCount());
    }

    @Test
    void testOnlyIncludeAllIds() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        importInfo.setIncludeAllIds(Arrays.asList("ac-1", "ac-2", "ac-3"));

        assertEquals(3, importInfo.getIncludeAllIds().size());
        assertEquals(0, importInfo.getExcludeIds().size());
    }

    @Test
    void testOnlyExcludeIds() {
        ProfileVisualizationResult.ImportInfo importInfo = new ProfileVisualizationResult.ImportInfo();
        importInfo.setExcludeIds(Arrays.asList("sc-1", "sc-2"));

        assertEquals(0, importInfo.getIncludeAllIds().size());
        assertEquals(2, importInfo.getExcludeIds().size());
    }
}

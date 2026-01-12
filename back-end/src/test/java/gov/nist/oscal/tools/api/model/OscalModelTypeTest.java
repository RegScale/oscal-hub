package gov.nist.oscal.tools.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OscalModelTypeTest {

    @Test
    void testEnumValues() {
        OscalModelType[] types = OscalModelType.values();
        assertEquals(8, types.length);
    }

    @Test
    void testCatalogType() {
        OscalModelType type = OscalModelType.CATALOG;
        assertEquals("catalog", type.getValue());
    }

    @Test
    void testProfileType() {
        OscalModelType type = OscalModelType.PROFILE;
        assertEquals("profile", type.getValue());
    }

    @Test
    void testComponentDefinitionType() {
        OscalModelType type = OscalModelType.COMPONENT_DEFINITION;
        assertEquals("component-definition", type.getValue());
    }

    @Test
    void testSystemSecurityPlanType() {
        OscalModelType type = OscalModelType.SYSTEM_SECURITY_PLAN;
        assertEquals("system-security-plan", type.getValue());
    }

    @Test
    void testAssessmentPlanType() {
        OscalModelType type = OscalModelType.ASSESSMENT_PLAN;
        assertEquals("assessment-plan", type.getValue());
    }

    @Test
    void testAssessmentResultsType() {
        OscalModelType type = OscalModelType.ASSESSMENT_RESULTS;
        assertEquals("assessment-results", type.getValue());
    }

    @Test
    void testPlanOfActionAndMilestonesType() {
        OscalModelType type = OscalModelType.PLAN_OF_ACTION_AND_MILESTONES;
        assertEquals("plan-of-action-and-milestones", type.getValue());
    }

    @Test
    void testMappingCollectionType() {
        OscalModelType type = OscalModelType.MAPPING_COLLECTION;
        assertEquals("mapping-collection", type.getValue());
    }

    @Test
    void testFromStringWithCatalog() {
        OscalModelType type = OscalModelType.fromString("catalog");
        assertEquals(OscalModelType.CATALOG, type);
    }

    @Test
    void testFromStringWithProfile() {
        OscalModelType type = OscalModelType.fromString("profile");
        assertEquals(OscalModelType.PROFILE, type);
    }

    @Test
    void testFromStringWithComponentDefinition() {
        OscalModelType type = OscalModelType.fromString("component-definition");
        assertEquals(OscalModelType.COMPONENT_DEFINITION, type);
    }

    @Test
    void testFromStringWithSystemSecurityPlan() {
        OscalModelType type = OscalModelType.fromString("system-security-plan");
        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, type);
    }

    @Test
    void testFromStringWithAssessmentPlan() {
        OscalModelType type = OscalModelType.fromString("assessment-plan");
        assertEquals(OscalModelType.ASSESSMENT_PLAN, type);
    }

    @Test
    void testFromStringWithAssessmentResults() {
        OscalModelType type = OscalModelType.fromString("assessment-results");
        assertEquals(OscalModelType.ASSESSMENT_RESULTS, type);
    }

    @Test
    void testFromStringWithPlanOfActionAndMilestones() {
        OscalModelType type = OscalModelType.fromString("plan-of-action-and-milestones");
        assertEquals(OscalModelType.PLAN_OF_ACTION_AND_MILESTONES, type);
    }

    @Test
    void testFromStringWithMappingCollection() {
        OscalModelType type = OscalModelType.fromString("mapping-collection");
        assertEquals(OscalModelType.MAPPING_COLLECTION, type);
    }

    @Test
    void testFromStringCaseInsensitive() {
        OscalModelType type1 = OscalModelType.fromString("CATALOG");
        assertEquals(OscalModelType.CATALOG, type1);

        OscalModelType type2 = OscalModelType.fromString("Profile");
        assertEquals(OscalModelType.PROFILE, type2);

        OscalModelType type3 = OscalModelType.fromString("COMPONENT-DEFINITION");
        assertEquals(OscalModelType.COMPONENT_DEFINITION, type3);
    }

    @Test
    void testFromStringWithNull() {
        OscalModelType type = OscalModelType.fromString(null);
        assertNull(type);
    }

    @Test
    void testFromStringWithInvalidValue() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            OscalModelType.fromString("invalid-type");
        });

        assertEquals("Unknown OSCAL model type: invalid-type", exception.getMessage());
    }

    @Test
    void testFromStringWithEmptyString() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            OscalModelType.fromString("");
        });

        assertEquals("Unknown OSCAL model type: ", exception.getMessage());
    }

    @Test
    void testFromStringWithWhitespace() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            OscalModelType.fromString("   ");
        });

        assertEquals("Unknown OSCAL model type:    ", exception.getMessage());
    }

    @Test
    void testAllEnumValuesCanBeFoundByString() {
        for (OscalModelType type : OscalModelType.values()) {
            OscalModelType found = OscalModelType.fromString(type.getValue());
            assertEquals(type, found);
        }
    }

    @Test
    void testGetValueReturnsCorrectString() {
        assertEquals("catalog", OscalModelType.CATALOG.getValue());
        assertEquals("profile", OscalModelType.PROFILE.getValue());
        assertEquals("component-definition", OscalModelType.COMPONENT_DEFINITION.getValue());
        assertEquals("system-security-plan", OscalModelType.SYSTEM_SECURITY_PLAN.getValue());
        assertEquals("assessment-plan", OscalModelType.ASSESSMENT_PLAN.getValue());
        assertEquals("assessment-results", OscalModelType.ASSESSMENT_RESULTS.getValue());
        assertEquals("plan-of-action-and-milestones", OscalModelType.PLAN_OF_ACTION_AND_MILESTONES.getValue());
        assertEquals("mapping-collection", OscalModelType.MAPPING_COLLECTION.getValue());
    }

    @Test
    void testFromStringWithMixedCase() {
        assertEquals(OscalModelType.CATALOG, OscalModelType.fromString("CaTaLoG"));
        assertEquals(OscalModelType.PROFILE, OscalModelType.fromString("pRoFiLe"));
        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, OscalModelType.fromString("SyStEm-SeCuRiTy-PlAn"));
    }

    @Test
    void testEnumOrdering() {
        OscalModelType[] types = OscalModelType.values();
        assertEquals(OscalModelType.CATALOG, types[0]);
        assertEquals(OscalModelType.PROFILE, types[1]);
        assertEquals(OscalModelType.COMPONENT_DEFINITION, types[2]);
        assertEquals(OscalModelType.SYSTEM_SECURITY_PLAN, types[3]);
        assertEquals(OscalModelType.ASSESSMENT_PLAN, types[4]);
        assertEquals(OscalModelType.ASSESSMENT_RESULTS, types[5]);
        assertEquals(OscalModelType.PLAN_OF_ACTION_AND_MILESTONES, types[6]);
        assertEquals(OscalModelType.MAPPING_COLLECTION, types[7]);
    }
}

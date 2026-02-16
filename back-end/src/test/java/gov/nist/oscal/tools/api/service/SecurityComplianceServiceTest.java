package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.model.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityComplianceServiceTest {

    private SecurityComplianceService securityComplianceService;

    @BeforeEach
    void setUp() {
        securityComplianceService = new SecurityComplianceService();
    }

    // ========== getComplianceSummary() Tests ==========

    @Test
    void testGetComplianceSummary_returnsValidSummary() {
        // Act
        ComplianceSummary summary = securityComplianceService.getComplianceSummary();

        // Assert
        assertNotNull(summary);
        assertTrue(summary.getTotalControls() > 0);
        assertNotNull(summary.getAssessmentDate());
        assertTrue(summary.getCompliancePercentage() >= 0 && summary.getCompliancePercentage() <= 100);
    }

    @Test
    void testGetComplianceSummary_countsMatchTotal() {
        // Act
        ComplianceSummary summary = securityComplianceService.getComplianceSummary();

        // Assert
        int calculatedTotal = summary.getImplementedControls() +
                              summary.getPartialControls() +
                              summary.getGapControls();
        assertEquals(summary.getTotalControls(), calculatedTotal);
    }

    @Test
    void testGetComplianceSummary_hasCategoryBreakdown() {
        // Act
        ComplianceSummary summary = securityComplianceService.getComplianceSummary();

        // Assert
        assertNotNull(summary.getByCategory());
        assertFalse(summary.getByCategory().isEmpty());

        // Should have CC6 category
        assertTrue(summary.getByCategory().containsKey("CC6"));
    }

    @Test
    void testGetComplianceSummary_categorySummaryIsAccurate() {
        // Act
        ComplianceSummary summary = securityComplianceService.getComplianceSummary();

        // Assert - each category's counts should add up
        for (ComplianceSummary.CategorySummary categorySummary : summary.getByCategory().values()) {
            int categoryTotal = categorySummary.getImplemented() +
                               categorySummary.getPartial() +
                               categorySummary.getGaps();
            assertEquals(categorySummary.getTotal(), categoryTotal,
                    "Category " + categorySummary.getDisplayName() + " counts don't add up");
        }
    }

    // ========== getAllControls() Tests ==========

    @Test
    void testGetAllControls_returnsNonEmptyList() {
        // Act
        List<Soc2Control> controls = securityComplianceService.getAllControls();

        // Assert
        assertNotNull(controls);
        assertFalse(controls.isEmpty());
    }

    @Test
    void testGetAllControls_containsExpectedControls() {
        // Act
        List<Soc2Control> controls = securityComplianceService.getAllControls();

        // Assert - should contain CC6.1 (Logical Access)
        boolean hasCC61 = controls.stream()
                .anyMatch(c -> "CC6.1".equals(c.getControlId()));
        assertTrue(hasCC61, "Should contain CC6.1 control");
    }

    @Test
    void testGetAllControls_allControlsHaveRequiredFields() {
        // Act
        List<Soc2Control> controls = securityComplianceService.getAllControls();

        // Assert
        for (Soc2Control control : controls) {
            assertNotNull(control.getControlId(), "Control should have controlId");
            assertNotNull(control.getName(), "Control should have name");
            assertNotNull(control.getCategory(), "Control should have category");
            assertNotNull(control.getStatus(), "Control should have status");
            assertNotNull(control.getImplementation(), "Control should have implementation");
        }
    }

    @Test
    void testGetAllControls_hasMultipleCategories() {
        // Act
        List<Soc2Control> controls = securityComplianceService.getAllControls();

        // Assert - should have controls from multiple categories
        long distinctCategories = controls.stream()
                .map(Soc2Control::getCategory)
                .distinct()
                .count();
        assertTrue(distinctCategories >= 4, "Should have controls from at least 4 categories");
    }

    // ========== getControlsByCategory() Tests ==========

    @Test
    void testGetControlsByCategory_CC6_returnsAccessControls() {
        // Act
        List<Soc2Control> controls = securityComplianceService.getControlsByCategory("CC6");

        // Assert
        assertNotNull(controls);
        assertFalse(controls.isEmpty());
        assertTrue(controls.stream().allMatch(c -> c.getCategory() == ControlCategory.CC6));
    }

    @Test
    void testGetControlsByCategory_CC7_returnsOperationsControls() {
        // Act
        List<Soc2Control> controls = securityComplianceService.getControlsByCategory("CC7");

        // Assert
        assertNotNull(controls);
        assertFalse(controls.isEmpty());
        assertTrue(controls.stream().allMatch(c -> c.getCategory() == ControlCategory.CC7));
    }

    @Test
    void testGetControlsByCategory_caseInsensitive() {
        // Act
        List<Soc2Control> controlsLower = securityComplianceService.getControlsByCategory("cc6");
        List<Soc2Control> controlsUpper = securityComplianceService.getControlsByCategory("CC6");

        // Assert
        assertEquals(controlsLower.size(), controlsUpper.size());
    }

    @Test
    void testGetControlsByCategory_invalidCategory_returnsEmptyList() {
        // Act
        List<Soc2Control> controls = securityComplianceService.getControlsByCategory("INVALID");

        // Assert
        assertNotNull(controls);
        assertTrue(controls.isEmpty());
    }

    @Test
    void testGetControlsByCategory_DATA_returnsDataProtectionControls() {
        // Act
        List<Soc2Control> controls = securityComplianceService.getControlsByCategory("DATA");

        // Assert
        assertNotNull(controls);
        assertFalse(controls.isEmpty());
        assertTrue(controls.stream().allMatch(c -> c.getCategory() == ControlCategory.DATA_PROTECTION));
    }

    @Test
    void testGetControlsByCategory_AUDIT_returnsAuditControls() {
        // Act
        List<Soc2Control> controls = securityComplianceService.getControlsByCategory("AUDIT");

        // Assert
        assertNotNull(controls);
        assertFalse(controls.isEmpty());
        assertTrue(controls.stream().allMatch(c -> c.getCategory() == ControlCategory.AUDIT));
    }

    // ========== getGapAnalysis() Tests ==========

    @Test
    void testGetGapAnalysis_returnsNonEmptyList() {
        // Act
        List<GapAnalysis> gaps = securityComplianceService.getGapAnalysis();

        // Assert
        assertNotNull(gaps);
        assertFalse(gaps.isEmpty());
    }

    @Test
    void testGetGapAnalysis_gapsHaveRequiredFields() {
        // Act
        List<GapAnalysis> gaps = securityComplianceService.getGapAnalysis();

        // Assert
        for (GapAnalysis gap : gaps) {
            assertNotNull(gap.getGapId(), "Gap should have gapId");
            assertNotNull(gap.getControlId(), "Gap should have controlId");
            assertNotNull(gap.getTitle(), "Gap should have title");
            assertNotNull(gap.getSeverity(), "Gap should have severity");
            assertNotNull(gap.getRecommendation(), "Gap should have recommendation");
            assertTrue(gap.getPriority() > 0, "Gap should have positive priority");
        }
    }

    @Test
    void testGetGapAnalysis_containsMfaGap() {
        // Act
        List<GapAnalysis> gaps = securityComplianceService.getGapAnalysis();

        // Assert - CC6.8 (MFA) should be identified as a gap
        boolean hasMfaGap = gaps.stream()
                .anyMatch(g -> "CC6.8".equals(g.getControlId()));
        assertTrue(hasMfaGap, "Should identify MFA (CC6.8) as a gap");
    }

    @Test
    void testGetGapAnalysis_containsMalwareScanningGap() {
        // Act
        List<GapAnalysis> gaps = securityComplianceService.getGapAnalysis();

        // Assert - CC9.1 (Malware Scanning) should be identified as a gap
        boolean hasMalwareGap = gaps.stream()
                .anyMatch(g -> "CC9.1".equals(g.getControlId()));
        assertTrue(hasMalwareGap, "Should identify malware scanning (CC9.1) as a gap");
    }

    @Test
    void testGetGapAnalysis_highPriorityGapsFirst() {
        // Act
        List<GapAnalysis> gaps = securityComplianceService.getGapAnalysis();

        // Assert - gaps should be ordered by priority
        int previousPriority = 0;
        for (GapAnalysis gap : gaps) {
            assertTrue(gap.getPriority() >= previousPriority,
                    "Gaps should be ordered by priority");
            previousPriority = gap.getPriority();
        }
    }

    @Test
    void testGetGapAnalysis_highSeverityGapsExist() {
        // Act
        List<GapAnalysis> gaps = securityComplianceService.getGapAnalysis();

        // Assert - should have at least one HIGH severity gap
        boolean hasHighSeverity = gaps.stream()
                .anyMatch(g -> g.getSeverity() == GapSeverity.HIGH);
        assertTrue(hasHighSeverity, "Should have at least one HIGH severity gap");
    }

    // ========== Control Status Tests ==========

    @Test
    void testControls_haveGapStatus() {
        // Act
        List<Soc2Control> controls = securityComplianceService.getAllControls();

        // Assert - should have at least one GAP control
        boolean hasGap = controls.stream()
                .anyMatch(c -> c.getStatus() == ControlStatus.GAP);
        assertTrue(hasGap, "Should have at least one GAP control");
    }

    @Test
    void testControls_havePartialStatus() {
        // Act
        List<Soc2Control> controls = securityComplianceService.getAllControls();

        // Assert - should have at least one PARTIAL control
        boolean hasPartial = controls.stream()
                .anyMatch(c -> c.getStatus() == ControlStatus.PARTIAL);
        assertTrue(hasPartial, "Should have at least one PARTIAL control");
    }

    @Test
    void testControls_haveImplementedStatus() {
        // Act
        List<Soc2Control> controls = securityComplianceService.getAllControls();

        // Assert - should have implemented controls (majority)
        long implementedCount = controls.stream()
                .filter(c -> c.getStatus() == ControlStatus.IMPLEMENTED)
                .count();
        assertTrue(implementedCount > controls.size() / 2,
                "Should have majority of controls implemented");
    }

    // ========== Evidence Tests ==========

    @Test
    void testControls_implementedHaveEvidence() {
        // Act
        List<Soc2Control> controls = securityComplianceService.getAllControls();

        // Assert - implemented controls should have evidence
        for (Soc2Control control : controls) {
            if (control.getStatus() == ControlStatus.IMPLEMENTED) {
                assertNotNull(control.getEvidence(),
                        "Implemented control " + control.getControlId() + " should have evidence list");
                assertFalse(control.getEvidence().isEmpty(),
                        "Implemented control " + control.getControlId() + " should have at least one evidence item");
            }
        }
    }
}

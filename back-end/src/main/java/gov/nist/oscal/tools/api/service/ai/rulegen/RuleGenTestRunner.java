package gov.nist.oscal.tools.api.service.ai.rulegen;

import gov.nist.oscal.tools.api.model.airulegen.TestCase;
import gov.nist.oscal.tools.api.model.airulegen.TestResult;
import gov.nist.oscal.tools.api.service.ConstraintXmlBuilder;
import gov.nist.secauto.metaschema.core.model.IConstraintLoader;
import gov.nist.secauto.metaschema.core.model.constraint.IConstraintSet;
import gov.nist.secauto.metaschema.databind.IBindingContext;
import gov.nist.secauto.metaschema.databind.io.Format;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.model.AssessmentPlan;
import gov.nist.secauto.oscal.lib.model.AssessmentResults;
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.ComponentDefinition;
import gov.nist.secauto.oscal.lib.model.PlanOfActionAndMilestones;
import gov.nist.secauto.oscal.lib.model.Profile;
import gov.nist.secauto.oscal.lib.model.SystemSecurityPlan;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Evaluates a candidate constraint against AI-generated synthetic test cases.
 * Each test case is a small OSCAL JSON fragment labeled "pass" or "fail";
 * we deserialize the fragment with the constraint registered and observe
 * whether the deserialization throws (treated as a violation = "fail") or
 * succeeds (treated as "pass").
 */
@Component
public class RuleGenTestRunner {

    private final ConstraintXmlBuilder builder;

    public RuleGenTestRunner(ConstraintXmlBuilder builder) {
        this.builder = builder;
    }

    public List<TestResult> run(String ruleId, String modelType, String constraintBody, List<TestCase> cases) {
        IBindingContext context = buildContext(ruleId, modelType, constraintBody);
        Class<?> klass = modelClass(modelType);
        List<TestResult> results = new ArrayList<>(cases.size());

        for (int i = 0; i < cases.size(); i++) {
            TestCase tc = cases.get(i);
            String actual;
            String violation = null;
            try {
                context.newDeserializer(Format.JSON, klass)
                    .deserialize(new ByteArrayInputStream(
                        tc.fragmentJson().getBytes(StandardCharsets.UTF_8)));
                actual = "pass";
            } catch (Exception e) {
                actual = "fail";
                violation = e.getMessage();
            }
            boolean passed = actual.equals(tc.expected());
            results.add(new TestResult(i, tc.description(), tc.expected(), actual, passed, violation));
        }
        return results;
    }

    private IBindingContext buildContext(String ruleId, String modelType, String body) {
        try {
            String wrapped = builder.build(ruleId, modelType, body);
            Path tmp = Files.createTempFile("rulegen-constraints-", ".xml");
            try {
                Files.writeString(tmp, wrapped, StandardCharsets.UTF_8);
                IConstraintLoader loader = IBindingContext.getConstraintLoader();
                Set<IConstraintSet> set = new LinkedHashSet<>(loader.load(tmp));
                return OscalBindingContext.builder().constraintSet(set).build();
            } finally {
                try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build context for test runner", e);
        }
    }

    private static Class<?> modelClass(String modelType) {
        return switch (modelType) {
            case "catalog" -> Catalog.class;
            case "profile" -> Profile.class;
            case "system-security-plan", "ssp" -> SystemSecurityPlan.class;
            case "component-definition" -> ComponentDefinition.class;
            case "assessment-plan", "ap" -> AssessmentPlan.class;
            case "assessment-results", "ar" -> AssessmentResults.class;
            case "plan-of-action-and-milestones", "poam" -> PlanOfActionAndMilestones.class;
            default -> throw new IllegalArgumentException("Unknown OSCAL model type: " + modelType);
        };
    }
}

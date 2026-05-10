package gov.nist.oscal.tools.api.service.ai.rulegen;

import gov.nist.oscal.tools.api.model.airulegen.TestCase;
import gov.nist.oscal.tools.api.model.airulegen.TestResult;
import gov.nist.oscal.tools.api.service.ConstraintXmlBuilder;
import gov.nist.secauto.metaschema.core.model.IConstraintLoader;
import gov.nist.secauto.metaschema.core.model.constraint.IConstraintSet;
import gov.nist.secauto.metaschema.databind.IBindingContext;
import gov.nist.secauto.metaschema.databind.io.Format;
import gov.nist.secauto.metaschema.databind.io.IDeserializer;
import gov.nist.secauto.oscal.lib.OscalBindingContext;
import gov.nist.secauto.oscal.lib.model.AssessmentPlan;
import gov.nist.secauto.oscal.lib.model.AssessmentResults;
import gov.nist.secauto.oscal.lib.model.Catalog;
import gov.nist.secauto.oscal.lib.model.ComponentDefinition;
import gov.nist.secauto.oscal.lib.model.PlanOfActionAndMilestones;
import gov.nist.secauto.oscal.lib.model.Profile;
import gov.nist.secauto.oscal.lib.model.SystemSecurityPlan;
import org.springframework.stereotype.Component;

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
        IDeserializer<?> deserializer = newDeserializer(context, modelType);
        List<TestResult> results = new ArrayList<>(cases.size());

        for (int i = 0; i < cases.size(); i++) {
            TestCase tc = cases.get(i);
            String actual;
            String violation = null;
            Path fragTmp = null;
            try {
                fragTmp = Files.createTempFile("rulegen-frag-", ".json");
                Files.writeString(fragTmp, tc.fragmentJson(), StandardCharsets.UTF_8);
                deserializer.deserialize(fragTmp);
                actual = "pass";
            } catch (Exception e) {
                actual = "fail";
                violation = e.getMessage();
            } finally {
                if (fragTmp != null) {
                    try { Files.deleteIfExists(fragTmp); } catch (Exception ignored) {}
                }
            }
            boolean passed = actual.equals(tc.expected());
            results.add(new TestResult(i, tc.description(), tc.expected(), actual, passed, violation));
        }
        return results;
    }

    private static IDeserializer<?> newDeserializer(IBindingContext ctx, String modelType) {
        return switch (modelType) {
            case "catalog" -> ctx.newDeserializer(Format.JSON, Catalog.class);
            case "profile" -> ctx.newDeserializer(Format.JSON, Profile.class);
            case "system-security-plan", "ssp" -> ctx.newDeserializer(Format.JSON, SystemSecurityPlan.class);
            case "component-definition" -> ctx.newDeserializer(Format.JSON, ComponentDefinition.class);
            case "assessment-plan", "ap" -> ctx.newDeserializer(Format.JSON, AssessmentPlan.class);
            case "assessment-results", "ar" -> ctx.newDeserializer(Format.JSON, AssessmentResults.class);
            case "plan-of-action-and-milestones", "poam" -> ctx.newDeserializer(Format.JSON, PlanOfActionAndMilestones.class);
            default -> throw new IllegalArgumentException("Unknown OSCAL model type: " + modelType);
        };
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

}

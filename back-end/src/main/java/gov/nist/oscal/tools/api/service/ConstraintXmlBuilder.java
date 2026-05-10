package gov.nist.oscal.tools.api.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Wraps a Metapath rule body fragment in the boilerplate
 * {@code <METASCHEMA-CONSTRAINTS>} envelope expected by
 * {@link gov.nist.secauto.metaschema.core.model.IConstraintLoader}.
 *
 * <p>If the input already begins with the root element it is returned
 * unchanged — a small convenience for fully-authored rules.
 */
@Component
public class ConstraintXmlBuilder {

    private static final String NS = "http://csrc.nist.gov/ns/oscal/metaschema/1.0";
    private static final String OSCAL_NS = "http://csrc.nist.gov/ns/oscal/1.0";

    private static final Map<String, String> SHORT_NAMES = Map.of(
        "catalog",                            "oscal-catalog",
        "profile",                            "oscal-profile",
        "system-security-plan",               "oscal-ssp",
        "ssp",                                "oscal-ssp",
        "component-definition",               "oscal-component-definition",
        "assessment-plan",                    "oscal-ap",
        "assessment-results",                 "oscal-ar",
        "plan-of-action-and-milestones",      "oscal-poam",
        "poam",                               "oscal-poam"
    );

    public String build(String ruleId, String modelType, String body) {
        if (body != null && body.trim().startsWith("<METASCHEMA-CONSTRAINTS")) {
            return body;
        }
        String shortName = SHORT_NAMES.get(modelType);
        if (shortName == null) {
            throw new IllegalArgumentException("Unknown OSCAL model type: " + modelType);
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<METASCHEMA-CONSTRAINTS xmlns=\"" + NS + "\">"
            + "<name>" + escape(ruleId) + "</name>"
            + "<version>1.0.0</version>"
            + "<scope metaschema-namespace=\"" + OSCAL_NS + "\""
            + "       metaschema-short-name=\"" + shortName + "\">"
            + body
            + "</scope>"
            + "</METASCHEMA-CONSTRAINTS>";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}

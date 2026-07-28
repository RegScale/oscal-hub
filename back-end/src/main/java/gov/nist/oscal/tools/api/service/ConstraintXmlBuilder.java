package gov.nist.oscal.tools.api.service;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

import org.xml.sax.InputSource;

/**
 * Wraps a Metapath rule body fragment in the boilerplate
 * {@code <METASCHEMA-CONSTRAINTS>} envelope expected by
 * {@link dev.metaschema.core.model.IConstraintLoader}.
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

    /**
     * Constraint elements that metaschema-framework 3.0.0.M4+ models as
     * "targeted" constraints, whose {@code target} flag is now required in
     * external constraint files. Older stored rules omitted it and relied on
     * the implicit default of the current context.
     */
    private static final Set<String> TARGETED_CONSTRAINTS = Set.of(
        "expect", "allowed-values", "matches", "index",
        "index-has-key", "is-unique", "has-cardinality");

    public String build(String ruleId, String modelType, String body) {
        if (body != null && body.trim().startsWith("<METASCHEMA-CONSTRAINTS")) {
            return defaultMissingTargets(body);
        }
        String shortName = SHORT_NAMES.get(modelType);
        if (shortName == null) {
            throw new IllegalArgumentException("Unknown OSCAL model type: " + modelType);
        }
        return defaultMissingTargets(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<METASCHEMA-CONSTRAINTS xmlns=\"" + NS + "\">"
            + "<name>" + escape(ruleId) + "</name>"
            + "<version>1.0.0</version>"
            + "<scope metaschema-namespace=\"" + OSCAL_NS + "\""
            + "       metaschema-short-name=\"" + shortName + "\">"
            + body
            + "</scope>"
            + "</METASCHEMA-CONSTRAINTS>");
    }

    /**
     * Adds {@code target="."} to targeted constraint elements that omit it.
     * Rules authored against metaschema-framework 3.0.0.M1 could leave the
     * target implicit; M4 rejects such documents with "Missing required
     * attribute 'target'". Defaulting to the self axis preserves the old
     * behavior for stored rules.
     */
    private static String defaultMissingTargets(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList all = doc.getElementsByTagName("*");
            boolean changed = false;
            for (int i = 0; i < all.getLength(); i++) {
                Element el = (Element) all.item(i);
                if (TARGETED_CONSTRAINTS.contains(el.getLocalName()) && !el.hasAttribute("target")) {
                    el.setAttribute("target", ".");
                    changed = true;
                }
            }
            if (!changed) {
                return xml;
            }
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            StringWriter out = new StringWriter();
            var transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            return out.toString();
        } catch (Exception e) {
            // Malformed rule XML: return unchanged and let the constraint
            // loader produce its own (more descriptive) parse error.
            return xml;
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}

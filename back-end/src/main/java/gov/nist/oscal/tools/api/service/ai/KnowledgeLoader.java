package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.WizardKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Loads claude-plugins skill markdown files and assembles per-wizard system prompts.
 *
 * <p>Skills are sourced from the {@code claude-plugins} git submodule mounted at
 * {@code back-end/src/main/resources/claude-plugins} — Maven bundles its contents
 * into the packaged JAR under {@code BOOT-INF/classes/claude-plugins/...}. At
 * runtime we read those entries through Spring's
 * {@link PathMatchingResourcePatternResolver}, which works the same whether the
 * app is running from an exploded {@code target/classes} (dev) or from inside
 * the JAR (production).
 *
 * <p>Expected layout under the plugin root:
 * <pre>
 * plugins/
 *   oscal/skills/
 *     oscal-basics/SKILL.md
 *     oscal-catalog/SKILL.md
 *     oscal-component-definition/SKILL.md
 *     oscal-ssp/SKILL.md
 *     oscal-poam/SKILL.md
 *     ...
 *   metaschema/skills/
 *     metaschema-basics/SKILL.md
 *     ...
 * </pre>
 */
@Service
public class KnowledgeLoader {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeLoader.class);

    /**
     * Default location of the claude-plugins root. The {@code classpath:} prefix
     * makes the loader read from the packaged JAR or exploded classes directory,
     * so the loader doesn't depend on the process working directory.
     */
    private static final String DEFAULT_PLUGIN_ROOT_LOCATION = "classpath:/claude-plugins";

    private final ResourcePatternResolver resolver;
    private final String pluginRootLocation;

    /** Spring-managed constructor. Root overridable via {@code ai.plugins.root}. */
    @Autowired
    public KnowledgeLoader(
            @Value("${ai.plugins.root:" + DEFAULT_PLUGIN_ROOT_LOCATION + "}") String pluginRootLocation,
            ResourcePatternResolver resolver) {
        this.pluginRootLocation = normalizeLocation(pluginRootLocation);
        this.resolver = resolver;
    }

    /**
     * Convenience constructor for tests / manual wiring. Accepts either a
     * Spring resource location ({@code classpath:/...}, {@code file:/...}) or a
     * bare filesystem path.
     */
    public KnowledgeLoader(String pluginRootLocation) {
        this(pluginRootLocation, new PathMatchingResourcePatternResolver());
    }

    private static String normalizeLocation(String s) {
        // Already a Spring URL location (classpath:, file:, http:, etc.)
        if (s.matches("^[a-z]+:.*")) {
            return s.endsWith("/") ? s : s + "/";
        }
        // Bare path — treat as filesystem.
        return "file:" + s + (s.endsWith("/") ? "" : "/");
    }

    public String systemFor(WizardKind kind) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert OSCAL author working inside OSCAL Hub. ");
        sb.append("Always produce schema-valid OSCAL output. Use the validate_oscal tool to confirm.\n\n");

        if (kind == WizardKind.SMOKE) {
            sb.append("This is a smoke-test wizard. Reply concisely.\n");
            return sb.toString();
        }

        if (kind == WizardKind.CATALOG) {
            appendSkillsFrom(sb, "plugins/oscal/skills/oscal-basics");
            appendSkillsFrom(sb, "plugins/oscal/skills/oscal-catalog");
            appendSkillsFrom(sb, "plugins/metaschema/skills/metaschema-basics");
            sb.append("\nFocus: produce an OSCAL Catalog with controls, parts, params, groups.\n");
            return sb.toString();
        }

        if (kind == WizardKind.COMPONENT_DEF) {
            appendSkillsFrom(sb, "plugins/oscal/skills/oscal-basics");
            appendSkillsFrom(sb, "plugins/oscal/skills/oscal-component-definition");
            appendSkillsFrom(sb, "plugins/metaschema/skills/metaschema-basics");
            sb.append("\nFocus: produce an OSCAL Component-definition mapping product features to controls.\n");
            return sb.toString();
        }

        if (kind == WizardKind.SSP) {
            appendSkillsFrom(sb, "plugins/oscal/skills/oscal-basics");
            appendSkillsFrom(sb, "plugins/oscal/skills/oscal-ssp");
            appendSkillsFrom(sb, "plugins/metaschema/skills/metaschema-basics");
            sb.append("\nFocus: produce SSP per-control implementation narratives grounded in the system description.\n");
            return sb.toString();
        }

        if (kind == WizardKind.POAM) {
            appendSkillsFrom(sb, "plugins/oscal/skills/oscal-basics");
            appendSkillsFrom(sb, "plugins/oscal/skills/oscal-poam");
            appendSkillsFrom(sb, "plugins/metaschema/skills/metaschema-basics");
            sb.append("\nFocus: produce OSCAL POA&M items with severity, status, due dates, and remediation narratives drawn from the source document.\n");
            return sb.toString();
        }

        // Other kinds keep load-all behavior until their own wizard plans land.
        appendSkillsFrom(sb, "plugins/oscal/skills");
        appendSkillsFrom(sb, "plugins/metaschema/skills");

        switch (kind) {
            case PROFILE ->
                sb.append("Focus: produce an OSCAL Profile with imports, includes, and modifications.\n");
            case BUILDER_ASSIST ->
                sb.append("Focus: act as an inline assistant for the document the user is editing.\n");
            default -> { /* no extra focus line */ }
        }

        return sb.toString();
    }

    /**
     * Loads every {@code .md} file under {@code subpath} (relative to the plugin
     * root), in sorted order, and appends the concatenated content to {@code sb}.
     */
    private void appendSkillsFrom(StringBuilder sb, String subpath) {
        String pattern = pluginRootLocation + subpath + "/**/*.md";
        Resource[] resources;
        try {
            resources = resolver.getResources(pattern);
        } catch (IOException e) {
            log.warn("Failed to resolve knowledge resources at {}: {}", pattern, e.toString());
            return;
        }

        if (resources.length == 0) {
            log.warn("No knowledge resources found at {} (claude-plugins submodule may not be initialized in the build)", pattern);
            return;
        }

        Arrays.sort(resources, Comparator.comparing(r -> {
            try {
                return r.getURI().toString();
            } catch (IOException e) {
                return r.getFilename() == null ? "" : r.getFilename();
            }
        }));

        StringBuilder local = new StringBuilder();
        for (Resource r : resources) {
            try (var in = r.getInputStream()) {
                local.append(new String(in.readAllBytes(), StandardCharsets.UTF_8)).append("\n\n");
            } catch (IOException e) {
                log.warn("Failed to read skill resource {}: {}", r, e.toString());
            }
        }
        String content = local.toString().stripTrailing();
        if (!content.isEmpty()) {
            sb.append(content).append("\n\n");
        }
    }
}

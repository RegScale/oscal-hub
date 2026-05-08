package gov.nist.oscal.tools.api.service.ai;

import gov.nist.oscal.tools.api.entity.WizardKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads claude-plugins skill markdown files and assembles per-wizard system prompts.
 *
 * <p>The plugin root is expected to contain the following layout (matching the
 * {@code claude-plugins} submodule checked in at
 * {@code back-end/src/main/resources/claude-plugins}):
 *
 * <pre>
 * plugins/
 *   oscal/skills/
 *     oscal-basics/SKILL.md
 *     oscal-catalog/SKILL.md
 *     oscal-profile/SKILL.md
 *     oscal-component-definition/SKILL.md
 *     oscal-ssp/SKILL.md
 *     oscal-poam/SKILL.md
 *     ... (other skills)
 *   metaschema/skills/
 *     metaschema-basics/SKILL.md
 *     metaschema-constraints-authoring/SKILL.md
 *     ... (other skills)
 * </pre>
 *
 * <p>For non-SMOKE wizards, {@link #systemFor(WizardKind)} loads ALL {@code .md} files
 * recursively from both {@code plugins/oscal/skills/} and
 * {@code plugins/metaschema/skills/}. This approach is simple and keeps the loader
 * correct as new skill directories are added to the submodule.
 */
@Service
public class KnowledgeLoader {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeLoader.class);

    /**
     * Root of the claude-plugins submodule relative to the working directory.
     * Overridable via {@code ai.plugins.root} in {@code application.properties}.
     */
    private static final String DEFAULT_PLUGIN_ROOT = "back-end/src/main/resources/claude-plugins";

    private final Path pluginRoot;

    /** Spring-managed constructor — root resolved from property or default. */
    @Autowired
    public KnowledgeLoader(
            @Value("${ai.plugins.root:" + DEFAULT_PLUGIN_ROOT + "}") String root) {
        this(Paths.get(root));
    }

    /** Direct constructor for tests and manual wiring. */
    public KnowledgeLoader(Path pluginRoot) {
        this.pluginRoot = pluginRoot;
    }

    /**
     * Returns the system prompt appropriate for the given {@link WizardKind}.
     *
     * <ul>
     *   <li>SMOKE — short static prompt only (no skill markdown loaded)</li>
     *   <li>CATALOG — targeted: loads only {@code oscal-basics}, {@code oscal-catalog},
     *       and {@code metaschema-basics} skill directories</li>
     *   <li>COMPONENT_DEF — targeted: loads only {@code oscal-basics},
     *       {@code oscal-component-definition}, and {@code metaschema-basics}</li>
     *   <li>All other kinds — load-all behavior (all OSCAL + Metaschema skills) until
     *       their own wizard plans tighten them</li>
     * </ul>
     */
    public String systemFor(WizardKind kind) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert OSCAL author working inside OSCAL Hub. ");
        sb.append("Always produce schema-valid OSCAL output. Use the validate_oscal tool to confirm.\n\n");

        if (kind == WizardKind.SMOKE) {
            sb.append("This is a smoke-test wizard. Reply concisely.\n");
            return sb.toString();
        }

        if (kind == WizardKind.CATALOG) {
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/oscal/skills/oscal-basics"));
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/oscal/skills/oscal-catalog"));
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/metaschema/skills/metaschema-basics"));
            sb.append("\nFocus: produce an OSCAL Catalog with controls, parts, params, groups.\n");
            return sb.toString();
        }

        if (kind == WizardKind.COMPONENT_DEF) {
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/oscal/skills/oscal-basics"));
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/oscal/skills/oscal-component-definition"));
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/metaschema/skills/metaschema-basics"));
            sb.append("\nFocus: produce an OSCAL Component-definition mapping product features to controls.\n");
            return sb.toString();
        }

        if (kind == WizardKind.SSP) {
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/oscal/skills/oscal-basics"));
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/oscal/skills/oscal-ssp"));
            appendSkillsFrom(sb, pluginRoot.resolve("plugins/metaschema/skills/metaschema-basics"));
            sb.append("\nFocus: produce SSP per-control implementation narratives grounded in the system description.\n");
            return sb.toString();
        }

        // Other kinds keep load-all behavior until their own wizard plans land.
        String oscalSkills = loadAllMarkdown(pluginRoot.resolve("plugins/oscal/skills"));
        if (!oscalSkills.isEmpty()) {
            sb.append(oscalSkills).append("\n\n");
        }

        String metaSkills = loadAllMarkdown(pluginRoot.resolve("plugins/metaschema/skills"));
        if (!metaSkills.isEmpty()) {
            sb.append(metaSkills).append("\n\n");
        }

        // Kind-specific focus instruction
        switch (kind) {
            case PROFILE ->
                sb.append("Focus: produce an OSCAL Profile with imports, includes, and modifications.\n");
            case COMPONENT_DEF ->
                sb.append("Focus: produce an OSCAL Component-definition mapping product features to controls.\n");
            case SSP ->
                sb.append("Focus: produce SSP per-control implementation narratives grounded in the system description.\n");
            case POAM ->
                sb.append("Focus: produce OSCAL POA&M items with risk ratings, milestones, and control mappings.\n");
            case BUILDER_ASSIST ->
                sb.append("Focus: act as an inline assistant for the document the user is editing.\n");
            default -> { /* no extra focus line */ }
        }

        return sb.toString();
    }

    /**
     * Appends all markdown content from a single skill directory (and its subdirectories)
     * into the given {@link StringBuilder}.
     *
     * @param sb  target string builder
     * @param dir directory to walk; may be absent (logs a warning and appends nothing)
     */
    private void appendSkillsFrom(StringBuilder sb, Path dir) {
        String content = loadAllMarkdown(dir);
        if (!content.isEmpty()) {
            sb.append(content).append("\n\n");
        }
    }

    /**
     * Walks {@code dir} recursively and concatenates the contents of every
     * {@code .md} file found, separated by double newlines.
     *
     * @param dir directory to walk; may be absent (logs a warning and returns empty string)
     * @return concatenated markdown content, or an empty string if none found
     */
    private String loadAllMarkdown(Path dir) {
        if (!Files.isDirectory(dir)) {
            log.warn("Knowledge directory not found: {}", dir);
            return "";
        }

        StringBuilder sb = new StringBuilder();
        try (Stream<Path> stream = Files.walk(dir)) {
            List<Path> mdFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .sorted()
                    .toList();
            for (Path file : mdFiles) {
                try {
                    sb.append(Files.readString(file)).append("\n\n");
                } catch (IOException e) {
                    log.warn("Failed to read skill file {}: {}", file, e.toString());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to walk knowledge directory {}: {}", dir, e.toString());
        }

        return sb.toString().stripTrailing();
    }
}

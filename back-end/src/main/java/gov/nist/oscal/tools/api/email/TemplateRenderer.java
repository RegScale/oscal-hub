package gov.nist.oscal.tools.api.email;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(\\w+)\\}");

    public String render(String template, Map<String, String> values) {
        if (template == null) {
            throw new IllegalArgumentException("template must not be null");
        }
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String raw = values.get(key);
            String replacement = raw == null ? m.group(0) : escapeHtml(raw);
            // Quote the replacement so backslashes and dollar signs in user input are treated literally.
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    public String renderFromClasspath(String resourcePath, Map<String, String> values) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("template not found on classpath: " + resourcePath);
            }
            String template = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            return render(template, values);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load template " + resourcePath, e);
        }
    }

    /**
     * Renders a template without HTML-escaping substituted values.
     * Use this for plain-text (.txt) email bodies so that values like
     * "Tom &amp; Jerry" are inserted as-is rather than as "Tom &amp;amp; Jerry".
     */
    public String renderText(String template, Map<String, String> values) {
        if (template == null) {
            throw new IllegalArgumentException("template must not be null");
        }
        Matcher m = PLACEHOLDER.matcher(template);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            String raw = values.get(key);
            String replacement = raw == null ? m.group(0) : raw;
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    /**
     * Loads a plain-text template from the classpath and renders it without HTML-escaping.
     */
    public String renderTextFromClasspath(String resourcePath, Map<String, String> values) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalArgumentException("template not found on classpath: " + resourcePath);
            }
            String template = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            return renderText(template, values);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load template " + resourcePath, e);
        }
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

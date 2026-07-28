package gov.nist.oscal.tools.api.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import gov.nist.oscal.tools.api.entity.CustomValidationRule;
import gov.nist.oscal.tools.api.repository.CustomValidationRuleRepository;
import dev.metaschema.core.model.IConstraintLoader;
import dev.metaschema.core.model.constraint.IConstraintSet;
import dev.metaschema.databind.IBindingContext;
import dev.metaschema.oscal.lib.OscalBindingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds an {@link IBindingContext} pre-loaded with the user's enabled custom
 * validation rules.
 *
 * <p>{@code OscalBindingContext.instance()} is immutable, so we must construct
 * a fresh context whenever custom rules apply. Contexts are cached by SHA-256
 * of the concatenated constraint XML to keep hot-path validations cheap.
 */
@Service
public class MetapathConstraintService {

    private static final Logger log = LoggerFactory.getLogger(MetapathConstraintService.class);

    private final CustomValidationRuleRepository repo;
    private final ConstraintXmlBuilder builder;

    private final Cache<String, IBindingContext> contextCache = Caffeine.newBuilder()
            .maximumSize(256)
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    public MetapathConstraintService(CustomValidationRuleRepository repo, ConstraintXmlBuilder builder) {
        this.repo = repo;
        this.builder = builder;
    }

    /**
     * Returns an {@link IBindingContext} that enforces the user's enabled custom
     * rules for the given OSCAL model type. Falls back to the immutable singleton
     * when no custom rules apply.
     */
    public IBindingContext contextFor(String modelType, Long userId) {
        if (userId == null) {
            return OscalBindingContext.instance();
        }
        List<CustomValidationRule> rules =
            repo.findEnabledRulesForModelTypeAndUser(modelType, userId);
        if (rules.isEmpty()) {
            return OscalBindingContext.instance();
        }

        List<String> xmls = new ArrayList<>(rules.size());
        for (CustomValidationRule r : rules) {
            xmls.add(builder.build(r.getRuleId(), modelType, r.getRuleExpression()));
        }
        String key = sha256(xmls);
        return contextCache.get(key, k -> {
            try {
                return buildContext(xmls);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to build constraint context", e);
            }
        });
    }

    /** Evict every cache entry that may have been built from this user's rules. */
    public void evictForUser(Long userId) {
        // Coarse-grained: invalidate everything. Cache rebuilds lazily on next
        // request and the bound is the number of distinct (model,user) tuples
        // currently in flight, which is small.
        contextCache.invalidateAll();
        log.debug("Evicted MetapathConstraintService cache after rule change for user {}", userId);
    }

    private IBindingContext buildContext(List<String> constraintXmls) throws Exception {
        IConstraintLoader loader = IBindingContext.getConstraintLoader();
        Set<IConstraintSet> all = new LinkedHashSet<>();
        List<Path> tempFiles = new ArrayList<>();
        try {
            for (String xml : constraintXmls) {
                Path tmp = Files.createTempFile("oscal-constraints-", ".xml");
                Files.writeString(tmp, xml, StandardCharsets.UTF_8);
                tempFiles.add(tmp);
                all.addAll(loader.load(tmp));
            }
        } finally {
            for (Path p : tempFiles) {
                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
            }
        }
        return OscalBindingContext.builder()
                .constraintSet(all)
                .build();
    }

    private static String sha256(List<String> xmls) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (String xml : xmls) {
                md.update(xml.getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

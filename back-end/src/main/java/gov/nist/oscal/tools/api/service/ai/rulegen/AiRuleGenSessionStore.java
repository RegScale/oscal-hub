package gov.nist.oscal.tools.api.service.ai.rulegen;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * In-memory store for {@link AiRuleGenSession}s with a 30-minute access TTL.
 * Sessions are not persisted across backend restarts in v1; the wizard is
 * a transient flow.
 */
@Component
public class AiRuleGenSessionStore {

    private final Cache<UUID, AiRuleGenSession> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(1024)
            .build();

    public UUID create(long organizationId, long userId, String modelType, String anthropicModel) {
        UUID id = UUID.randomUUID();
        cache.put(id, new AiRuleGenSession(id, organizationId, userId, modelType, anthropicModel));
        return id;
    }

    public AiRuleGenSession get(UUID id) {
        AiRuleGenSession s = cache.getIfPresent(id);
        if (s == null) {
            throw new RuleGenSessionExpiredException(
                "Unknown or expired rule-gen session: " + id);
        }
        return s;
    }

    public void appendUser(UUID id, String text) {
        get(id).transcript().add(new AiRuleGenSession.TranscriptEntry("user", text));
    }

    public void appendAssistant(UUID id, String text) {
        get(id).transcript().add(new AiRuleGenSession.TranscriptEntry("assistant", text));
    }

    public void close(UUID id) {
        cache.invalidate(id);
    }
}

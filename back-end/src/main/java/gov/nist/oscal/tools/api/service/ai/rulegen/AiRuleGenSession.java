package gov.nist.oscal.tools.api.service.ai.rulegen;

import gov.nist.oscal.tools.api.model.airulegen.RuleProposal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mutable in-memory state for one rule-gen wizard session. Lives in
 * {@link AiRuleGenSessionStore} for up to 30 minutes after last access.
 */
public final class AiRuleGenSession {

    public record TranscriptEntry(String role, String text) {}

    private final UUID id;
    private final long organizationId;
    private final long userId;
    private final String modelType;
    private final String anthropicModel;
    private final List<TranscriptEntry> transcript = new ArrayList<>();
    private final AtomicInteger tokensIn = new AtomicInteger();
    private final AtomicInteger tokensOut = new AtomicInteger();

    private RuleProposal currentProposal;

    AiRuleGenSession(UUID id, long organizationId, long userId, String modelType, String anthropicModel) {
        this.id = id;
        this.organizationId = organizationId;
        this.userId = userId;
        this.modelType = modelType;
        this.anthropicModel = anthropicModel;
    }

    public UUID id() { return id; }
    public long organizationId() { return organizationId; }
    public long userId() { return userId; }
    public String modelType() { return modelType; }
    public String anthropicModel() { return anthropicModel; }
    public List<TranscriptEntry> transcript() { return transcript; }
    public RuleProposal currentProposal() { return currentProposal; }
    public void setCurrentProposal(RuleProposal p) { this.currentProposal = p; }
    public int tokensIn() { return tokensIn.get(); }
    public int tokensOut() { return tokensOut.get(); }
    public void addTokens(int in, int out) { tokensIn.addAndGet(in); tokensOut.addAndGet(out); }
}

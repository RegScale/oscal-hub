package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.entity.CustomValidationRule;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.airulegen.EditProposalRequest;
import gov.nist.oscal.tools.api.model.airulegen.RuleGenTurnRequest;
import gov.nist.oscal.tools.api.model.airulegen.RuleGenTurnResponse;
import gov.nist.oscal.tools.api.model.airulegen.RuleProposal;
import gov.nist.oscal.tools.api.model.airulegen.SaveRuleRequest;
import gov.nist.oscal.tools.api.model.airulegen.StartRuleGenRequest;
import gov.nist.oscal.tools.api.model.airulegen.StartRuleGenResponse;
import gov.nist.oscal.tools.api.repository.CustomValidationRuleRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import gov.nist.oscal.tools.api.service.MetapathConstraintService;
import gov.nist.oscal.tools.api.service.ai.rulegen.AiRuleGenService;
import gov.nist.oscal.tools.api.service.ai.rulegen.AiRuleGenSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AiRuleGenController — exercises the controller methods
 * directly (no MockMvc) since the focus is logic branches: ownership
 * enforcement, save() pre-conditions (no proposal, duplicate ruleId,
 * cross-user attempt), and the handful of small mappings between session
 * state and the persisted CustomValidationRule.
 */
class AiRuleGenControllerTest {

    private AiRuleGenService service;
    private UserRepository userRepo;
    private CustomValidationRuleRepository ruleRepo;
    private MetapathConstraintService constraintService;
    private AiRuleGenController controller;

    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = mock(AiRuleGenService.class);
        userRepo = mock(UserRepository.class);
        ruleRepo = mock(CustomValidationRuleRepository.class);
        constraintService = mock(MetapathConstraintService.class);
        controller = new AiRuleGenController(service, userRepo, ruleRepo, constraintService);
    }

    // ---------- start ----------

    @Test
    void start_returnsSessionId_fromService() {
        User user = stubUser(7L, "alice");
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(service.start(42L, 7L, "ssp")).thenReturn(sessionId);

        ResponseEntity<StartRuleGenResponse> res = controller.start(
                new StartRuleGenRequest(42L, "ssp"),
                principal("alice"));

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody().sessionId()).isEqualTo(sessionId);
    }

    @Test
    void start_unknownUser_throws() {
        // If the JWT is somehow valid for a username that no longer exists,
        // we want a clear failure rather than silently calling service with userId=0.
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.start(
                new StartRuleGenRequest(1L, "catalog"), principal("ghost")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Authenticated user not found");
    }

    // ---------- turn ----------

    @Test
    void turn_owner_delegatesToService() {
        wireOwner("alice", 7L, sessionId);
        RuleGenTurnResponse expected = sampleResponse();
        when(service.turn(sessionId, "rule for ac-1")).thenReturn(expected);

        ResponseEntity<RuleGenTurnResponse> res = controller.turn(
                sessionId,
                new RuleGenTurnRequest("rule for ac-1"),
                principal("alice"));

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isSameAs(expected);
    }

    @Test
    void turn_nonOwner_throwsAccessDenied_andDoesNotCallService() {
        // Hostile or stale-tab case: someone else's userId on a session not
        // belonging to them must not be able to drive turns.
        wireNonOwner("bob", 99L, sessionId, /* sessionOwner */ 7L);

        assertThatThrownBy(() -> controller.turn(sessionId,
                new RuleGenTurnRequest("hi"),
                principal("bob")))
                .isInstanceOf(AccessDeniedException.class);

        verify(service, never()).turn(any(), any());
    }

    // ---------- edit ----------

    @Test
    void edit_owner_delegatesToService() {
        wireOwner("alice", 7L, sessionId);
        when(service.rerunTests(sessionId, "<assembly/>")).thenReturn(sampleResponse());

        ResponseEntity<RuleGenTurnResponse> res = controller.edit(
                sessionId,
                new EditProposalRequest("<assembly/>"),
                principal("alice"));

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        verify(service, times(1)).rerunTests(sessionId, "<assembly/>");
    }

    @Test
    void edit_nonOwner_isRejected() {
        wireNonOwner("bob", 99L, sessionId, 7L);

        assertThatThrownBy(() -> controller.edit(sessionId,
                new EditProposalRequest("<x/>"),
                principal("bob")))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------- save ----------

    @Test
    void save_happyPath_persistsRule_evictsCache_closesSession() {
        User user = stubUser(7L, "alice");
        AiRuleGenSession session = stubSession(7L, "claude-opus-4-7", sampleProposal());
        when(service.session(sessionId)).thenReturn(session);
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(ruleRepo.existsByRuleId("MY-RULE-1")).thenReturn(false);
        when(ruleRepo.save(any(CustomValidationRule.class)))
                .thenAnswer(inv -> { CustomValidationRule r = inv.getArgument(0); r.setId(123L); return r; });

        ResponseEntity<Long> res = controller.save(sessionId,
                new SaveRuleRequest("MY-RULE-1", "AccessControl", true),
                principal("alice"));

        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(res.getBody()).isEqualTo(123L);
        verify(ruleRepo, times(1)).save(any(CustomValidationRule.class));
        // Cache must be evicted so the saved rule is visible on the next validation pass.
        verify(constraintService, times(1)).evictForUser(7L);
        // Session should be closed so the in-memory state and tokens are released.
        verify(service, times(1)).close(sessionId);
    }

    @Test
    void save_foreignUser_returns403_doesNotPersist() {
        // The session belongs to user 7, but user 99 is logged in. Must not
        // be allowed to save the proposal as their own rule.
        AiRuleGenSession session = stubSession(7L, "claude-opus-4-7", sampleProposal());
        when(service.session(sessionId)).thenReturn(session);
        when(userRepo.findByUsername("eve")).thenReturn(Optional.of(stubUser(99L, "eve")));

        ResponseEntity<Long> res = controller.save(sessionId,
                new SaveRuleRequest("MY-RULE-1", "X", true),
                principal("eve"));

        assertThat(res.getStatusCode().value()).isEqualTo(403);
        verify(ruleRepo, never()).save(any());
        verify(service, never()).close(any());
    }

    @Test
    void save_noProposalYet_returns400() {
        // The user clicked Save before generating a proposal — controller must
        // reject with 400 rather than persist a half-built rule.
        AiRuleGenSession session = stubSession(7L, "claude-opus-4-7", null);
        when(service.session(sessionId)).thenReturn(session);
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(stubUser(7L, "alice")));

        ResponseEntity<Long> res = controller.save(sessionId,
                new SaveRuleRequest("MY-RULE-1", null, null),
                principal("alice"));

        assertThat(res.getStatusCode().value()).isEqualTo(400);
        verify(ruleRepo, never()).save(any());
    }

    @Test
    void save_duplicateRuleId_returns409() {
        // ruleId is the user-facing unique key. If they pick a name that's
        // already taken, surface 409 so the UI can prompt for a different one.
        AiRuleGenSession session = stubSession(7L, "claude-opus-4-7", sampleProposal());
        when(service.session(sessionId)).thenReturn(session);
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(stubUser(7L, "alice")));
        when(ruleRepo.existsByRuleId("DUPE")).thenReturn(true);

        ResponseEntity<Long> res = controller.save(sessionId,
                new SaveRuleRequest("DUPE", null, null),
                principal("alice"));

        assertThat(res.getStatusCode().value()).isEqualTo(409);
        verify(ruleRepo, never()).save(any());
        // Important: we must NOT close the session on 409 — the user is going
        // to retry with a different ruleId on the same proposal.
        verify(service, never()).close(any());
    }

    @Test
    void save_enabledFieldDefaultsToTrue_whenOmitted() {
        // SaveRuleRequest.enabled is nullable; the controller must default
        // to true so a freshly-saved rule actually runs.
        User user = stubUser(7L, "alice");
        AiRuleGenSession session = stubSession(7L, "claude-opus-4-7", sampleProposal());
        when(service.session(sessionId)).thenReturn(session);
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(ruleRepo.existsByRuleId(any())).thenReturn(false);
        org.mockito.ArgumentCaptor<CustomValidationRule> cap =
                org.mockito.ArgumentCaptor.forClass(CustomValidationRule.class);
        when(ruleRepo.save(cap.capture())).thenAnswer(inv -> {
            CustomValidationRule r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        controller.save(sessionId,
                new SaveRuleRequest("R", "cat", null /* enabled omitted */),
                principal("alice"));

        assertThat(cap.getValue().getEnabled()).isTrue();
    }

    @Test
    void save_capturesAiProvenance_andFirstUserMessage() {
        // The persisted rule should carry AI-generated metadata so SUPER_ADMIN
        // dashboards can distinguish AI rules from hand-authored ones.
        User user = stubUser(7L, "alice");
        AiRuleGenSession session = stubSessionWithTranscript(7L, "claude-opus-4-7",
                sampleProposal(), "I want a rule about ac-1");
        when(service.session(sessionId)).thenReturn(session);
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(user));
        when(ruleRepo.existsByRuleId(any())).thenReturn(false);
        org.mockito.ArgumentCaptor<CustomValidationRule> cap =
                org.mockito.ArgumentCaptor.forClass(CustomValidationRule.class);
        when(ruleRepo.save(cap.capture())).thenAnswer(inv -> {
            CustomValidationRule r = inv.getArgument(0); r.setId(1L); return r;
        });

        controller.save(sessionId,
                new SaveRuleRequest("R", "cat", true),
                principal("alice"));

        CustomValidationRule saved = cap.getValue();
        assertThat(saved.getAiGenerated()).isTrue();
        assertThat(saved.getGenerationModel()).isEqualTo("claude-opus-4-7");
        assertThat(saved.getGenerationPrompt()).isEqualTo("I want a rule about ac-1");
        assertThat(saved.getCreatedBy()).isEqualTo("alice");
        assertThat(saved.getUser()).isSameAs(user);
    }

    @Test
    void save_emptyTranscript_storesNullPrompt_notEmptyString() {
        // Edge case: the user saved before sending any message (e.g. via API).
        // generationPrompt should be null, not "" — distinguishes "no prompt"
        // from "empty string prompt" in audit/dashboard queries.
        AiRuleGenSession session = stubSession(7L, "claude-opus-4-7", sampleProposal());
        when(service.session(sessionId)).thenReturn(session);
        when(userRepo.findByUsername("alice")).thenReturn(Optional.of(stubUser(7L, "alice")));
        when(ruleRepo.existsByRuleId(any())).thenReturn(false);
        org.mockito.ArgumentCaptor<CustomValidationRule> cap =
                org.mockito.ArgumentCaptor.forClass(CustomValidationRule.class);
        when(ruleRepo.save(cap.capture())).thenAnswer(inv -> {
            CustomValidationRule r = inv.getArgument(0); r.setId(1L); return r;
        });

        controller.save(sessionId, new SaveRuleRequest("R", null, null), principal("alice"));

        assertThat(cap.getValue().getGenerationPrompt()).isNull();
    }

    // ---------- abandon ----------

    @Test
    void abandon_owner_closesSession_returns204() {
        wireOwner("alice", 7L, sessionId);

        ResponseEntity<Void> res = controller.abandon(sessionId, principal("alice"));

        assertThat(res.getStatusCode().value()).isEqualTo(204);
        verify(service, times(1)).close(sessionId);
    }

    @Test
    void abandon_nonOwner_isRejected_andSessionStaysOpen() {
        wireNonOwner("bob", 99L, sessionId, 7L);

        assertThatThrownBy(() -> controller.abandon(sessionId, principal("bob")))
                .isInstanceOf(AccessDeniedException.class);

        verify(service, never()).close(any());
    }

    // ---------- helpers ----------

    private void wireOwner(String username, long userId, UUID sessionId) {
        User user = stubUser(userId, username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));
        AiRuleGenSession session = stubSession(userId, "claude-opus-4-7", null);
        when(service.session(sessionId)).thenReturn(session);
    }

    private void wireNonOwner(String username, long callerId, UUID sessionId, long sessionOwnerId) {
        User user = stubUser(callerId, username);
        when(userRepo.findByUsername(username)).thenReturn(Optional.of(user));
        AiRuleGenSession session = stubSession(sessionOwnerId, "claude-opus-4-7", null);
        when(service.session(sessionId)).thenReturn(session);
    }

    private static User stubUser(long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private static AiRuleGenSession stubSession(long userId, String model, RuleProposal proposal) {
        AiRuleGenSession s = ReflectionConstructor.newSession(UUID.randomUUID(), 1L, userId, "ssp", model);
        if (proposal != null) s.setCurrentProposal(proposal);
        return s;
    }

    private static AiRuleGenSession stubSessionWithTranscript(long userId, String model,
                                                              RuleProposal proposal, String userMsg) {
        AiRuleGenSession s = stubSession(userId, model, proposal);
        s.transcript().add(new AiRuleGenSession.TranscriptEntry("user", userMsg));
        return s;
    }

    private static RuleProposal sampleProposal() {
        return new RuleProposal("My Rule", "desc", "error", "$.metadata", "<assembly/>", List.of());
    }

    private static RuleGenTurnResponse sampleResponse() {
        return new RuleGenTurnResponse("clarify", "q?", null, null, null, null, 1, 0, 0);
    }

    private static Principal principal(String name) {
        return () -> name;
    }

    /**
     * AiRuleGenSession's package-private constructor isn't visible from this
     * test package. Hop through reflection to construct one for unit tests.
     */
    static class ReflectionConstructor {
        static AiRuleGenSession newSession(UUID id, long org, long user, String model, String anthropic) {
            try {
                var ctor = AiRuleGenSession.class.getDeclaredConstructor(
                        UUID.class, long.class, long.class, String.class, String.class);
                ctor.setAccessible(true);
                return ctor.newInstance(id, org, user, model, anthropic);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

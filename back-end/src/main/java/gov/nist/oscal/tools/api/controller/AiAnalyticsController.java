package gov.nist.oscal.tools.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nist.oscal.tools.api.entity.AiSession;
import gov.nist.oscal.tools.api.entity.OrganizationMembership;
import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.ai.AiSessionDetail;
import gov.nist.oscal.tools.api.model.ai.AiSessionSummary;
import gov.nist.oscal.tools.api.model.ai.AiUsageTotals;
import gov.nist.oscal.tools.api.repository.AiSessionRepository;
import gov.nist.oscal.tools.api.repository.OrganizationMembershipRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai/analytics")
@Tag(name = "AI Analytics", description = "Per-organization AI usage analytics")
public class AiAnalyticsController {

    private static final TypeReference<List<Map<String, Object>>> EVENTS_TYPE =
            new TypeReference<>() {};

    private final AiSessionRepository sessions;
    private final UserRepository users;
    private final OrganizationMembershipRepository memberships;
    private final ObjectMapper objectMapper;

    public AiAnalyticsController(AiSessionRepository sessions,
                                  UserRepository users,
                                  OrganizationMembershipRepository memberships,
                                  ObjectMapper objectMapper) {
        this.sessions = sessions;
        this.users = users;
        this.memberships = memberships;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "List sessions for an organization (paginated, newest first)")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/sessions")
    public ResponseEntity<List<AiSessionSummary>> listSessions(
            @RequestParam Long organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        requireOrgAdmin(organizationId);

        List<AiSession> rows = sessions.findByOrganizationIdOrderByStartedAtDesc(
                organizationId, PageRequest.of(page, size));

        Map<Long, String> usernameMap = buildUsernameMap(rows);

        List<AiSessionSummary> summaries = rows.stream()
                .map(s -> toSummary(s, usernameMap))
                .collect(Collectors.toList());

        return ResponseEntity.ok(summaries);
    }

    @Operation(summary = "Get a single session with full event log")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/sessions/{id}")
    public ResponseEntity<AiSessionDetail> getSession(
            @PathVariable UUID id,
            @RequestParam Long organizationId) {

        requireOrgAdmin(organizationId);

        AiSession session = sessions.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + id));

        if (!session.getOrganizationId().equals(organizationId)) {
            throw new AccessDeniedException("Session does not belong to organization " + organizationId);
        }

        Map<Long, String> usernameMap = Map.of(
                session.getUserId(),
                resolveUsername(session.getUserId()));

        AiSessionSummary summary = toSummary(session, usernameMap);
        List<Map<String, Object>> events = parseEvents(session.getEventsJson());

        return ResponseEntity.ok(new AiSessionDetail(summary, events, session.getErrorMessage()));
    }

    @Operation(summary = "Get aggregate usage totals for an organization")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'SUPER_ADMIN')")
    @GetMapping("/totals")
    public ResponseEntity<AiUsageTotals> getTotals(@RequestParam Long organizationId) {

        requireOrgAdmin(organizationId);

        Map<String, Object> all = sessions.sumForOrg(organizationId);
        LocalDateTime firstOfMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        Map<String, Object> monthly = sessions.sumForOrgSince(organizationId, firstOfMonth);

        int totalSessions = toLong(all.get("count")).intValue();
        long totalTokensIn = toLong(all.get("ti"));
        long totalTokensOut = toLong(all.get("to_"));
        long totalCost = toLong(all.get("cost"));
        int sessionsThisMonth = toLong(monthly.get("count")).intValue();
        long costThisMonth = toLong(monthly.get("cost"));

        return ResponseEntity.ok(new AiUsageTotals(
                totalSessions, totalTokensIn, totalTokensOut, totalCost,
                sessionsThisMonth, costThisMonth));
    }

    // --- helpers ---

    private AiSessionSummary toSummary(AiSession s, Map<Long, String> usernameMap) {
        return new AiSessionSummary(
                s.getId(),
                s.getUserId(),
                usernameMap.getOrDefault(s.getUserId(), null),
                s.getWizardKind(),
                s.getMode(),
                s.getModel(),
                s.getStatus(),
                s.getTokensIn(),
                s.getTokensOut(),
                s.getCostUsdMicros(),
                s.getStartedAt(),
                s.getEndedAt(),
                s.getErrorCode());
    }

    private Map<Long, String> buildUsernameMap(List<AiSession> rows) {
        List<Long> ids = rows.stream()
                .map(AiSession::getUserId)
                .distinct()
                .collect(Collectors.toList());
        return users.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
    }

    private String resolveUsername(Long userId) {
        return users.findById(userId).map(User::getUsername).orElse(null);
    }

    private List<Map<String, Object>> parseEvents(String eventsJson) {
        if (eventsJson == null || eventsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(eventsJson, EVENTS_TYPE);
        } catch (Exception e) {
            return List.of();
        }
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return 0L;
    }

    private void requireOrgAdmin(Long organizationId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccessDeniedException("No Authentication in SecurityContext");
        }
        String username = auth.getName();
        User user = users.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException(
                        "User '" + username + "' not found in database"));
        if (user.getGlobalRole() == User.GlobalRole.SUPER_ADMIN) return;

        Optional<OrganizationMembership> mOpt =
                memberships.findByUserIdAndOrganizationId(user.getId(), organizationId);
        if (mOpt.isEmpty()) {
            throw new AccessDeniedException(
                    "User " + user.getId() + " (" + username + ") has no membership row for org " + organizationId);
        }
        OrganizationMembership m = mOpt.get();
        if (m.getStatus() != OrganizationMembership.MembershipStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Membership status is " + m.getStatus() + " (not ACTIVE) for user " + user.getId()
                    + " in org " + organizationId);
        }
        if (m.getRole() != OrganizationMembership.OrganizationRole.ORG_ADMIN) {
            throw new AccessDeniedException(
                    "Membership role is " + m.getRole() + " (not ORG_ADMIN) for user " + user.getId()
                    + " in org " + organizationId);
        }
    }

    /**
     * Translate AccessDeniedException to a 403 with the exception's message
     * in the body. Bypasses Spring Security's default ExceptionTranslationFilter,
     * which (combined with the custom AuthenticationEntryPoint at the SecurityConfig
     * level) was masking the real reason for denial as a generic 401.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Forbidden", "message", e.getMessage() == null ? "Access denied" : e.getMessage()));
    }
}

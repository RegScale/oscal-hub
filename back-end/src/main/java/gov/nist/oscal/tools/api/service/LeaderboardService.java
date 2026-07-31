/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.service;

import gov.nist.oscal.tools.api.entity.User;
import gov.nist.oscal.tools.api.model.LeaderboardEntry;
import gov.nist.oscal.tools.api.model.LeaderboardResponse;
import gov.nist.oscal.tools.api.repository.ArtifactRepository;
import gov.nist.oscal.tools.api.repository.AuthorizationRepository;
import gov.nist.oscal.tools.api.repository.HistoryRepository;
import gov.nist.oscal.tools.api.repository.LibraryItemRepository;
import gov.nist.oscal.tools.api.repository.OscalDocumentRepository;
import gov.nist.oscal.tools.api.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Leaderboard Service
 * <p>
 * Computes the two global gamification boards by aggregating existing
 * activity records on read — no dedicated event table:
 * <ul>
 *   <li><b>Most Active Users</b>: operations run + library publishes +
 *       artifacts created + documents built + authorizations created.</li>
 *   <li><b>Top Contributors</b>: items shared into the library
 *       (visibility other than PRIVATE).</li>
 * </ul>
 * Disabled users are excluded; ties break by username; boards are capped
 * at {@value #BOARD_SIZE} rows.
 */
@Service
public class LeaderboardService {

    static final int BOARD_SIZE = 20;
    static final LocalDateTime ALL_TIME_CUTOFF = LocalDateTime.of(1970, 1, 1, 0, 0);

    private final HistoryRepository historyRepository;
    private final LibraryItemRepository libraryItemRepository;
    private final ArtifactRepository artifactRepository;
    private final OscalDocumentRepository oscalDocumentRepository;
    private final AuthorizationRepository authorizationRepository;
    private final UserRepository userRepository;

    public LeaderboardService(HistoryRepository historyRepository,
                              LibraryItemRepository libraryItemRepository,
                              ArtifactRepository artifactRepository,
                              OscalDocumentRepository oscalDocumentRepository,
                              AuthorizationRepository authorizationRepository,
                              UserRepository userRepository) {
        this.historyRepository = historyRepository;
        this.libraryItemRepository = libraryItemRepository;
        this.artifactRepository = artifactRepository;
        this.oscalDocumentRepository = oscalDocumentRepository;
        this.authorizationRepository = authorizationRepository;
        this.userRepository = userRepository;
    }

    /**
     * @param window {@code "30d"} or {@code "all"}
     * @throws IllegalArgumentException for any other window value
     */
    public LeaderboardResponse getLeaderboard(String window) {
        LocalDateTime cutoff = resolveCutoff(window);

        List<Object[]> libraryRows = libraryItemRepository.countSharedItemsPerUserSince(cutoff);

        Map<Long, Map<String, Long>> breakdowns = new HashMap<>();
        mergeSource(breakdowns, "operations", historyRepository.countOperationsPerUserSince(cutoff));
        mergeSource(breakdowns, "libraryPublishes", libraryRows);
        mergeSource(breakdowns, "artifacts", artifactRepository.countCreatedPerUserSince(cutoff));
        mergeSource(breakdowns, "documents", oscalDocumentRepository.countCreatedPerUserSince(cutoff));
        mergeSource(breakdowns, "authorizations", authorizationRepository.countCreatedPerUserSince(cutoff));

        Map<Long, Long> contributions = new HashMap<>();
        for (Object[] row : libraryRows) {
            Long userId = (Long) row[0];
            long count = ((Number) row[1]).longValue();
            if (userId != null && count > 0) {
                contributions.merge(userId, count, Long::sum);
            }
        }

        Set<Long> userIds = new HashSet<>(breakdowns.keySet());
        userIds.addAll(contributions.keySet());
        Map<Long, User> activeUsers = loadActiveUsers(userIds);

        List<LeaderboardEntry> mostActive = rank(breakdowns.keySet(), activeUsers,
                userId -> breakdowns.get(userId).values().stream().mapToLong(Long::longValue).sum(),
                userId -> breakdowns.get(userId));
        List<LeaderboardEntry> topContributors = rank(contributions.keySet(), activeUsers,
                contributions::get, userId -> null);

        return new LeaderboardResponse(window, Instant.now(), mostActive, topContributors);
    }

    private static LocalDateTime resolveCutoff(String window) {
        if ("all".equals(window)) {
            return ALL_TIME_CUTOFF;
        }
        if ("30d".equals(window)) {
            return LocalDateTime.now().minusDays(30);
        }
        throw new IllegalArgumentException("Invalid window '" + window + "': use '30d' or 'all'");
    }

    /** Folds [userId, count] rows from one source into the per-user breakdown maps. */
    private static void mergeSource(Map<Long, Map<String, Long>> target, String source, List<Object[]> rows) {
        for (Object[] row : rows) {
            Long userId = (Long) row[0];
            Long count = ((Number) row[1]).longValue();
            if (userId == null || count == null || count == 0) {
                continue;
            }
            target.computeIfAbsent(userId, k -> new LinkedHashMap<>()).merge(source, count, Long::sum);
        }
    }

    private Map<Long, User> loadActiveUsers(Set<Long> userIds) {
        Map<Long, User> users = new HashMap<>();
        if (userIds.isEmpty()) {
            return users;
        }
        for (User user : userRepository.findAllById(new ArrayList<>(userIds))) {
            if (Boolean.TRUE.equals(user.getEnabled())) {
                users.put(user.getId(), user);
            }
        }
        return users;
    }

    private static List<LeaderboardEntry> rank(Set<Long> userIds, Map<Long, User> activeUsers,
                                               java.util.function.Function<Long, Long> score,
                                               java.util.function.Function<Long, Map<String, Long>> breakdown) {
        List<Long> ranked = new ArrayList<>();
        for (Long userId : userIds) {
            if (activeUsers.containsKey(userId)) {
                ranked.add(userId);
            }
        }
        ranked.sort(Comparator.<Long>comparingLong(id -> -score.apply(id))
                .thenComparing(id -> activeUsers.get(id).getUsername()));

        List<LeaderboardEntry> entries = new ArrayList<>();
        for (int i = 0; i < ranked.size() && i < BOARD_SIZE; i++) {
            Long userId = ranked.get(i);
            User user = activeUsers.get(userId);
            entries.add(new LeaderboardEntry(i + 1, user.getUsername(),
                    displayName(user), score.apply(userId), breakdown.apply(userId)));
        }
        return entries;
    }

    private static String displayName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? user.getUsername() : full;
    }
}

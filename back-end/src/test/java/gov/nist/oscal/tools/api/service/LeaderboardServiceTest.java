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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock HistoryRepository historyRepository;
    @Mock LibraryItemRepository libraryItemRepository;
    @Mock ArtifactRepository artifactRepository;
    @Mock OscalDocumentRepository oscalDocumentRepository;
    @Mock AuthorizationRepository authorizationRepository;
    @Mock UserRepository userRepository;

    LeaderboardService service;

    @BeforeEach
    void setUp() {
        service = new LeaderboardService(historyRepository, libraryItemRepository,
                artifactRepository, oscalDocumentRepository, authorizationRepository,
                userRepository);
        lenient().when(historyRepository.countOperationsPerUserSince(any())).thenReturn(Collections.emptyList());
        lenient().when(libraryItemRepository.countSharedItemsPerUserSince(any())).thenReturn(Collections.emptyList());
        lenient().when(artifactRepository.countCreatedPerUserSince(any())).thenReturn(Collections.emptyList());
        lenient().when(oscalDocumentRepository.countCreatedPerUserSince(any())).thenReturn(Collections.emptyList());
        lenient().when(authorizationRepository.countCreatedPerUserSince(any())).thenReturn(Collections.emptyList());
        lenient().when(userRepository.findAllById(anyList())).thenReturn(Collections.emptyList());
    }

    private static Object[] row(long userId, long count) {
        return new Object[] {userId, count};
    }

    private static User user(long id, String username, String first, String last, boolean enabled) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setFirstName(first);
        u.setLastName(last);
        u.setEnabled(enabled);
        return u;
    }

    @Test
    void invalidWindowThrows() {
        assertThatThrownBy(() -> service.getLeaderboard("weekly"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("window");
    }

    @Test
    void allWindowUsesEpochCutoff() {
        service.getLeaderboard("all");

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(historyRepository).countOperationsPerUserSince(cutoff.capture());
        assertThat(cutoff.getValue()).isEqualTo(LocalDateTime.of(1970, 1, 1, 0, 0));
    }

    @Test
    void thirtyDayWindowUsesRecentCutoff() {
        service.getLeaderboard("30d");

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(historyRepository).countOperationsPerUserSince(cutoff.capture());
        assertThat(cutoff.getValue())
                .isCloseTo(LocalDateTime.now().minusDays(30), org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MINUTES));
    }

    @Test
    void mergesSourcesIntoScoreWithBreakdown() {
        when(historyRepository.countOperationsPerUserSince(any()))
                .thenReturn(List.<Object[]>of(row(1L, 30L)));
        when(libraryItemRepository.countSharedItemsPerUserSince(any()))
                .thenReturn(List.<Object[]>of(row(1L, 4L)));
        when(artifactRepository.countCreatedPerUserSince(any()))
                .thenReturn(List.<Object[]>of(row(1L, 3L)));
        when(oscalDocumentRepository.countCreatedPerUserSince(any()))
                .thenReturn(List.<Object[]>of(row(1L, 2L)));
        when(authorizationRepository.countCreatedPerUserSince(any()))
                .thenReturn(List.<Object[]>of(row(1L, 1L)));
        when(userRepository.findAllById(anyList()))
                .thenReturn(List.of(user(1L, "alice", "Alice", "Ames", true)));

        LeaderboardResponse response = service.getLeaderboard("all");

        assertThat(response.getMostActive()).hasSize(1);
        LeaderboardEntry entry = response.getMostActive().get(0);
        assertThat(entry.getRank()).isEqualTo(1);
        assertThat(entry.getUsername()).isEqualTo("alice");
        assertThat(entry.getDisplayName()).isEqualTo("Alice Ames");
        assertThat(entry.getScore()).isEqualTo(40L);
        assertThat(entry.getBreakdown())
                .containsEntry("operations", 30L)
                .containsEntry("libraryPublishes", 4L)
                .containsEntry("artifacts", 3L)
                .containsEntry("documents", 2L)
                .containsEntry("authorizations", 1L);
    }

    @Test
    void breakdownOmitsZeroSources() {
        when(historyRepository.countOperationsPerUserSince(any()))
                .thenReturn(List.<Object[]>of(row(1L, 5L)));
        when(userRepository.findAllById(anyList()))
                .thenReturn(List.of(user(1L, "alice", null, null, true)));

        LeaderboardResponse response = service.getLeaderboard("all");

        LeaderboardEntry entry = response.getMostActive().get(0);
        assertThat(entry.getBreakdown()).containsOnlyKeys("operations");
        assertThat(entry.getDisplayName()).isEqualTo("alice");
    }

    @Test
    void topContributorsOnlyCountLibraryItemsAndHaveNoBreakdown() {
        when(historyRepository.countOperationsPerUserSince(any()))
                .thenReturn(List.<Object[]>of(row(1L, 10L)));
        when(libraryItemRepository.countSharedItemsPerUserSince(any()))
                .thenReturn(List.<Object[]>of(row(2L, 7L)));
        when(userRepository.findAllById(anyList()))
                .thenReturn(List.of(
                        user(1L, "alice", "Alice", "Ames", true),
                        user(2L, "bob", "Bob", "Brown", true)));

        LeaderboardResponse response = service.getLeaderboard("all");

        assertThat(response.getTopContributors()).hasSize(1);
        LeaderboardEntry entry = response.getTopContributors().get(0);
        assertThat(entry.getUsername()).isEqualTo("bob");
        assertThat(entry.getScore()).isEqualTo(7L);
        assertThat(entry.getBreakdown()).isNull();
    }

    @Test
    void tiesBreakByUsernameAndRanksAreOrdinal() {
        when(historyRepository.countOperationsPerUserSince(any()))
                .thenReturn(Arrays.asList(row(1L, 5L), row(2L, 5L), row(3L, 9L)));
        when(userRepository.findAllById(anyList()))
                .thenReturn(List.of(
                        user(1L, "zed", null, null, true),
                        user(2L, "amy", null, null, true),
                        user(3L, "cat", null, null, true)));

        LeaderboardResponse response = service.getLeaderboard("all");

        List<LeaderboardEntry> board = response.getMostActive();
        assertThat(board).extracting(LeaderboardEntry::getUsername)
                .containsExactly("cat", "amy", "zed");
        assertThat(board).extracting(LeaderboardEntry::getRank)
                .containsExactly(1, 2, 3);
    }

    @Test
    void disabledUsersAreExcluded() {
        when(historyRepository.countOperationsPerUserSince(any()))
                .thenReturn(Arrays.asList(row(1L, 5L), row(2L, 50L)));
        when(libraryItemRepository.countSharedItemsPerUserSince(any()))
                .thenReturn(List.<Object[]>of(row(2L, 6L)));
        when(userRepository.findAllById(anyList()))
                .thenReturn(List.of(
                        user(1L, "alice", null, null, true),
                        user(2L, "mallory", null, null, false)));

        LeaderboardResponse response = service.getLeaderboard("all");

        assertThat(response.getMostActive()).extracting(LeaderboardEntry::getUsername)
                .containsExactly("alice");
        assertThat(response.getTopContributors()).isEmpty();
    }

    @Test
    void boardsTruncateToTopTwenty() {
        List<Object[]> rows = new ArrayList<>();
        List<User> users = new ArrayList<>();
        for (long i = 1; i <= 25; i++) {
            rows.add(row(i, i));
            users.add(user(i, "user" + String.format("%02d", i), null, null, true));
        }
        when(historyRepository.countOperationsPerUserSince(any())).thenReturn(rows);
        when(userRepository.findAllById(anyList())).thenReturn(users);

        LeaderboardResponse response = service.getLeaderboard("all");

        assertThat(response.getMostActive()).hasSize(20);
        assertThat(response.getMostActive().get(0).getUsername()).isEqualTo("user25");
        assertThat(response.getMostActive().get(0).getRank()).isEqualTo(1);
        assertThat(response.getMostActive().get(19).getUsername()).isEqualTo("user06");
    }

    @Test
    void emptySourcesGiveEmptyBoards() {
        LeaderboardResponse response = service.getLeaderboard("30d");

        assertThat(response.getMostActive()).isEmpty();
        assertThat(response.getTopContributors()).isEmpty();
        assertThat(response.getWindow()).isEqualTo("30d");
        assertThat(response.getGeneratedAt()).isNotNull();
    }
}

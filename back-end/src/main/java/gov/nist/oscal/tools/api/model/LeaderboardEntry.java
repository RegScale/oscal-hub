/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * One ranked row on a leaderboard.
 * <p>
 * {@code breakdown} is only populated for the "most active" board and maps
 * activity source (operations, libraryPublishes, artifacts, documents,
 * authorizations) to the number of contributions from that source; zero-count
 * sources are omitted.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeaderboardEntry {

    private final int rank;
    private final String username;
    private final String displayName;
    private final long score;
    private final Map<String, Long> breakdown;

    public LeaderboardEntry(int rank, String username, String displayName,
                            long score, Map<String, Long> breakdown) {
        this.rank = rank;
        this.username = username;
        this.displayName = displayName;
        this.score = score;
        this.breakdown = breakdown;
    }

    public int getRank() {
        return rank;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getScore() {
        return score;
    }

    public Map<String, Long> getBreakdown() {
        return breakdown;
    }
}

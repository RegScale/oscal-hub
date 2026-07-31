/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.model;

import java.time.Instant;
import java.util.List;

/**
 * Response for {@code GET /api/leaderboard}: both boards for one time window.
 */
public class LeaderboardResponse {

    private final String window;
    private final Instant generatedAt;
    private final List<LeaderboardEntry> mostActive;
    private final List<LeaderboardEntry> topContributors;

    public LeaderboardResponse(String window, Instant generatedAt,
                               List<LeaderboardEntry> mostActive,
                               List<LeaderboardEntry> topContributors) {
        this.window = window;
        this.generatedAt = generatedAt;
        this.mostActive = mostActive;
        this.topContributors = topContributors;
    }

    public String getWindow() {
        return window;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public List<LeaderboardEntry> getMostActive() {
        return mostActive;
    }

    public List<LeaderboardEntry> getTopContributors() {
        return topContributors;
    }
}

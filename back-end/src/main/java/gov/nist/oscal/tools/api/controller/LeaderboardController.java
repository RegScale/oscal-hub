/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.model.LeaderboardResponse;
import gov.nist.oscal.tools.api.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Leaderboard Controller
 * Global gamification boards: most active users and top library contributors.
 * Requires authentication (any role); the boards are platform-wide.
 */
@RestController
@RequestMapping("/api/leaderboard")
@Tag(name = "Leaderboard", description = "Global activity and contribution leaderboards")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    @Operation(summary = "Get leaderboards",
               description = "Most active users and top library contributors for a time window (30d or all)")
    public ResponseEntity<LeaderboardResponse> getLeaderboard(
            @RequestParam(name = "window", defaultValue = "all") String window) {
        try {
            return ResponseEntity.ok(leaderboardService.getLeaderboard(window));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}

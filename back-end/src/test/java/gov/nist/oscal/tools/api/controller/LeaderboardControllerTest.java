/*
 * SPDX-FileCopyrightText: none
 * SPDX-License-Identifier: CC0-1.0
 */

package gov.nist.oscal.tools.api.controller;

import gov.nist.oscal.tools.api.config.RateLimitConfig;
import gov.nist.oscal.tools.api.config.SecurityHeadersConfig;
import gov.nist.oscal.tools.api.model.LeaderboardEntry;
import gov.nist.oscal.tools.api.model.LeaderboardResponse;
import gov.nist.oscal.tools.api.security.JwtUtil;
import gov.nist.oscal.tools.api.service.LeaderboardService;
import gov.nist.oscal.tools.api.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaderboardController.class)
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaderboardService leaderboardService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    @MockitoBean
    private SecurityHeadersConfig securityHeadersConfig;

    private LeaderboardResponse sampleResponse(String window) {
        return new LeaderboardResponse(window, Instant.parse("2026-07-31T12:00:00Z"),
                List.of(new LeaderboardEntry(1, "alice", "Alice Ames", 42,
                        Map.of("operations", 40L, "libraryPublishes", 2L))),
                List.of(new LeaderboardEntry(1, "bob", "Bob Brown", 7, null)));
    }

    @Test
    void anonymousRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice")
    void authenticatedRequestReturnsBoards() throws Exception {
        when(leaderboardService.getLeaderboard("30d")).thenReturn(sampleResponse("30d"));

        mockMvc.perform(get("/api/leaderboard").param("window", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.window").value("30d"))
                .andExpect(jsonPath("$.mostActive[0].rank").value(1))
                .andExpect(jsonPath("$.mostActive[0].username").value("alice"))
                .andExpect(jsonPath("$.mostActive[0].displayName").value("Alice Ames"))
                .andExpect(jsonPath("$.mostActive[0].score").value(42))
                .andExpect(jsonPath("$.mostActive[0].breakdown.operations").value(40))
                .andExpect(jsonPath("$.topContributors[0].username").value("bob"))
                .andExpect(jsonPath("$.topContributors[0].breakdown").doesNotExist());
    }

    @Test
    @WithMockUser(username = "alice")
    void defaultWindowIsAllTime() throws Exception {
        when(leaderboardService.getLeaderboard("all")).thenReturn(sampleResponse("all"));

        mockMvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.window").value("all"));

        verify(leaderboardService).getLeaderboard("all");
    }

    @Test
    @WithMockUser(username = "alice")
    void invalidWindowIsBadRequest() throws Exception {
        when(leaderboardService.getLeaderboard("bogus"))
                .thenThrow(new IllegalArgumentException("Invalid window 'bogus': use '30d' or 'all'"));

        mockMvc.perform(get("/api/leaderboard").param("window", "bogus"))
                .andExpect(status().isBadRequest());
    }
}

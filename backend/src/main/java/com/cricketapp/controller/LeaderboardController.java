package com.cricketapp.controller;

import com.cricketapp.dto.LeaderboardDto;
import com.cricketapp.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public ResponseEntity<LeaderboardDto> getLeaderboard(
            @RequestParam(defaultValue = "runs") String category,
            @RequestParam(defaultValue = "All Time") String timeframe) {
        LeaderboardDto response = leaderboardService.getLeaderboard(category, timeframe);
        return ResponseEntity.ok(response);
    }
}

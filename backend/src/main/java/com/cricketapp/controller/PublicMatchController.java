package com.cricketapp.controller;

import com.cricketapp.dto.ScoringDashboardStateDto;
import com.cricketapp.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/matches")
public class PublicMatchController {

    private final MatchService matchService;

    public PublicMatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/{matchId}/scorecard")
    public ResponseEntity<ScoringDashboardStateDto> getPublicScorecard(@PathVariable("matchId") String matchId) {
        ScoringDashboardStateDto response = matchService.getPublicScorecard(matchId);
        return ResponseEntity.ok(response);
    }
}

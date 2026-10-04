package com.cricketapp.controller;

import com.cricketapp.dto.CreateMatchRequestDto;
import com.cricketapp.dto.MatchResponseDto;
import com.cricketapp.dto.UpdateMatchRequestDto;
import com.cricketapp.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public ResponseEntity<MatchResponseDto> createMatch(
            Principal principal,
            @RequestBody CreateMatchRequestDto request) {
        MatchResponseDto response = matchService.createMatch(principal.getName(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<MatchResponseDto>> getMyMatches(Principal principal) {
        List<MatchResponseDto> matches = matchService.getMyMatches(principal.getName());
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/live")
    public ResponseEntity<List<MatchResponseDto>> getLiveMatches(Principal principal) {
        String email = principal != null ? principal.getName() : null;
        List<MatchResponseDto> matches = matchService.getLiveMatches(email);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/{matchId}/live")
    public ResponseEntity<com.cricketapp.dto.ScoringDashboardStateDto> getLiveMatchState(
            @PathVariable("matchId") String matchId) {
        com.cricketapp.dto.ScoringDashboardStateDto response = matchService.getLiveMatchState(matchId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{matchId}")
    public ResponseEntity<MatchResponseDto> getMatchDetails(
            Principal principal,
            @PathVariable("matchId") String matchId) {
        MatchResponseDto response = matchService.getMatchDetails(principal.getName(), matchId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{matchId}")
    public ResponseEntity<MatchResponseDto> updateMatch(
            Principal principal,
            @PathVariable("matchId") String matchId,
            @RequestBody UpdateMatchRequestDto request) {
        MatchResponseDto response = matchService.updateMatch(principal.getName(), matchId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/cancel")
    public ResponseEntity<MatchResponseDto> cancelMatch(
            Principal principal,
            @PathVariable("matchId") String matchId) {
        MatchResponseDto response = matchService.cancelMatch(principal.getName(), matchId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/teams/{teamId}/playing-xi")
    public ResponseEntity<com.cricketapp.dto.TeamPlayingXiResponseDto> savePlayingXi(
            Principal principal,
            @PathVariable("matchId") String matchId,
            @PathVariable("teamId") String teamId,
            @RequestBody com.cricketapp.dto.SavePlayingXiRequestDto request) {
        com.cricketapp.dto.TeamPlayingXiResponseDto response = matchService.savePlayingXi(principal.getName(), matchId, teamId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{matchId}/teams/{teamId}/playing-xi")
    public ResponseEntity<com.cricketapp.dto.TeamPlayingXiResponseDto> getMatchPlayingXiForTeam(
            Principal principal,
            @PathVariable("matchId") String matchId,
            @PathVariable("teamId") String teamId) {
        com.cricketapp.dto.TeamPlayingXiResponseDto response = matchService.getMatchPlayingXiForTeam(principal.getName(), matchId, teamId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{matchId}/playing-xi")
    public ResponseEntity<java.util.Map<String, com.cricketapp.dto.TeamPlayingXiResponseDto>> getAllMatchPlayingXi(
            Principal principal,
            @PathVariable("matchId") String matchId) {
        java.util.Map<String, com.cricketapp.dto.TeamPlayingXiResponseDto> response = matchService.getAllMatchPlayingXi(principal.getName(), matchId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/scorer")
    public ResponseEntity<MatchResponseDto> assignScorer(
            Principal principal,
            @PathVariable("matchId") String matchId,
            @RequestBody com.cricketapp.dto.AssignScorerRequestDto request) {
        MatchResponseDto response = matchService.assignScorer(principal.getName(), matchId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{matchId}/scorer")
    public ResponseEntity<MatchResponseDto> removeScorer(
            Principal principal,
            @PathVariable("matchId") String matchId) {
        MatchResponseDto response = matchService.removeScorer(principal.getName(), matchId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/scorer")
    public ResponseEntity<List<MatchResponseDto>> getAssignedMatchesForScorer(Principal principal) {
        List<MatchResponseDto> matches = matchService.getAssignedMatchesForScorer(principal.getName());
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/{matchId}/scoring/checklist")
    public ResponseEntity<com.cricketapp.dto.MatchChecklistDto> getMatchChecklist(@PathVariable("matchId") String matchId) {
        com.cricketapp.dto.MatchChecklistDto response = matchService.getMatchChecklist(matchId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/start")
    public ResponseEntity<MatchResponseDto> startMatch(
            Principal principal,
            @PathVariable("matchId") String matchId) {
        MatchResponseDto response = matchService.startMatch(principal.getName(), matchId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/toss")
    public ResponseEntity<com.cricketapp.dto.ScoringDashboardStateDto> recordToss(
            Principal principal,
            @PathVariable("matchId") String matchId,
            @RequestBody com.cricketapp.dto.RecordTossRequestDto request) {
        com.cricketapp.dto.ScoringDashboardStateDto response = matchService.recordToss(principal.getName(), matchId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/innings/start")
    public ResponseEntity<com.cricketapp.dto.ScoringDashboardStateDto> startInnings(
            Principal principal,
            @PathVariable("matchId") String matchId,
            @RequestBody com.cricketapp.dto.StartInningsRequestDto request) {
        com.cricketapp.dto.ScoringDashboardStateDto response = matchService.startInnings(principal.getName(), matchId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{matchId}/scoring")
    public ResponseEntity<com.cricketapp.dto.ScoringDashboardStateDto> getScoringDashboardState(
            Principal principal,
            @PathVariable("matchId") String matchId) {
        com.cricketapp.dto.ScoringDashboardStateDto response = matchService.getScoringDashboardState(principal.getName(), matchId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/scoring/ball")
    public ResponseEntity<com.cricketapp.dto.ScoringDashboardStateDto> recordBall(
            Principal principal,
            @PathVariable("matchId") String matchId,
            @RequestBody com.cricketapp.dto.RecordBallRequestDto request) {
        com.cricketapp.dto.ScoringDashboardStateDto response = matchService.recordBall(principal.getName(), matchId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/scoring/undo")
    public ResponseEntity<com.cricketapp.dto.ScoringDashboardStateDto> undoLastBall(
            Principal principal,
            @PathVariable("matchId") String matchId) {
        com.cricketapp.dto.ScoringDashboardStateDto response = matchService.undoLastBall(principal.getName(), matchId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/scoring/next-bowler")
    public ResponseEntity<com.cricketapp.dto.ScoringDashboardStateDto> selectNextBowler(
            Principal principal,
            @PathVariable("matchId") String matchId,
            @RequestBody com.cricketapp.dto.SelectNextBowlerRequestDto request) {
        com.cricketapp.dto.ScoringDashboardStateDto response = matchService.selectNextBowler(principal.getName(), matchId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/scoring/next-batter")
    public ResponseEntity<com.cricketapp.dto.ScoringDashboardStateDto> selectNextBatter(
            Principal principal,
            @PathVariable("matchId") String matchId,
            @RequestBody com.cricketapp.dto.SelectNextBatterRequestDto request) {
        com.cricketapp.dto.ScoringDashboardStateDto response = matchService.selectNextBatter(principal.getName(), matchId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{matchId}/scoring/settings")
    public ResponseEntity<com.cricketapp.dto.ScoringDashboardStateDto> updateMatchSettings(
            Principal principal,
            @PathVariable("matchId") String matchId,
            @RequestBody com.cricketapp.dto.UpdateMatchSettingsRequestDto request) {
        com.cricketapp.dto.ScoringDashboardStateDto response = matchService.updateMatchSettings(principal.getName(), matchId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/scoring/change-keeper")
    public ResponseEntity<com.cricketapp.dto.ScoringDashboardStateDto> changeWicketKeeper(
            Principal principal,
            @PathVariable("matchId") String matchId,
            @RequestBody com.cricketapp.dto.ChangeWicketKeeperRequestDto request) {
        com.cricketapp.dto.ScoringDashboardStateDto response = matchService.changeWicketKeeper(principal.getName(), matchId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/scoring/retire-batter")
    public ResponseEntity<com.cricketapp.dto.ScoringDashboardStateDto> retireBatter(
            Principal principal,
            @PathVariable("matchId") String matchId,
            @RequestBody com.cricketapp.dto.RetireBatterRequestDto request) {
        com.cricketapp.dto.ScoringDashboardStateDto response = matchService.retireBatter(principal.getName(), matchId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/scoring/ball-in-progress")
    public ResponseEntity<Void> notifyBallInProgress(
            Principal principal,
            @PathVariable("matchId") String matchId) {
        String email = principal != null ? principal.getName() : null;
        matchService.notifyBallInProgress(email, matchId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{matchId}/scoring/pause")
    public ResponseEntity<com.cricketapp.dto.ScoringDashboardStateDto> pauseMatch(
            Principal principal,
            @PathVariable("matchId") String matchId,
            @RequestBody com.cricketapp.dto.PauseMatchRequestDto request) {
        com.cricketapp.dto.ScoringDashboardStateDto response = matchService.pauseMatch(principal.getName(), matchId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{matchId}/scoring/resume")
    public ResponseEntity<com.cricketapp.dto.ScoringDashboardStateDto> resumeMatch(
            Principal principal,
            @PathVariable("matchId") String matchId) {
        com.cricketapp.dto.ScoringDashboardStateDto response = matchService.resumeMatch(principal.getName(), matchId);
        return ResponseEntity.ok(response);
    }
}

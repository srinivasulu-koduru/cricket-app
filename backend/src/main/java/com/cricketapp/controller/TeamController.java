package com.cricketapp.controller;

import com.cricketapp.dto.*;
import com.cricketapp.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<TeamResponseDto> createTeam(
            Principal principal,
            @RequestBody CreateTeamRequestDto request) {
        TeamResponseDto response = teamService.createTeam(principal.getName(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<TeamResponseDto>> getMyTeams(Principal principal) {
        List<TeamResponseDto> teams = teamService.getMyTeams(principal.getName());
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/all")
    public ResponseEntity<List<TeamResponseDto>> getAllTeams(Principal principal) {
        List<TeamResponseDto> teams = teamService.getAllTeams(principal.getName());
        return ResponseEntity.ok(teams);
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponseDto> getTeam(
            Principal principal,
            @PathVariable("teamId") String teamId) {
        TeamResponseDto response = teamService.getTeamDetails(principal.getName(), teamId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<TeamResponseDto> updateTeam(
            Principal principal,
            @PathVariable("teamId") String teamId,
            @RequestBody UpdateTeamRequestDto request) {
        TeamResponseDto response = teamService.updateTeam(principal.getName(), teamId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{teamId}/logo")
    public ResponseEntity<TeamResponseDto> uploadTeamLogo(
            Principal principal,
            @PathVariable("teamId") String teamId,
            @RequestParam("file") MultipartFile file) {
        TeamResponseDto response = teamService.uploadTeamLogo(principal.getName(), teamId, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponseDto>> getTeamMembers(
            Principal principal,
            @PathVariable("teamId") String teamId) {
        List<TeamMemberResponseDto> members = teamService.getTeamMembers(principal.getName(), teamId);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{teamId}/invitations")
    public ResponseEntity<TeamInvitationResponseDto> invitePlayer(
            Principal principal,
            @PathVariable("teamId") String teamId,
            @RequestBody InvitePlayerRequestDto request) {
        TeamInvitationResponseDto response = teamService.invitePlayer(principal.getName(), teamId, request.getCricketUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{teamId}/invitations/{invitationId}/cancel")
    public ResponseEntity<MessageResponse> cancelInvitation(
            Principal principal,
            @PathVariable("teamId") String teamId,
            @PathVariable("invitationId") Long invitationId) {
        MessageResponse response = teamService.cancelInvitation(principal.getName(), teamId, invitationId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{teamId}/members/{targetCricketUserId}")
    public ResponseEntity<MessageResponse> removePlayer(
            Principal principal,
            @PathVariable("teamId") String teamId,
            @PathVariable("targetCricketUserId") String targetCricketUserId) {
        MessageResponse response = teamService.removePlayer(principal.getName(), teamId, targetCricketUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{teamId}/leave")
    public ResponseEntity<MessageResponse> leaveTeam(
            Principal principal,
            @PathVariable("teamId") String teamId) {
        MessageResponse response = teamService.leaveTeam(principal.getName(), teamId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<MessageResponse> deleteTeam(
            Principal principal,
            @PathVariable("teamId") String teamId) {
        MessageResponse response = teamService.deleteTeam(principal.getName(), teamId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{teamId}/members/{targetCricketUserId}/role")
    public ResponseEntity<TeamMemberResponseDto> updateMemberRole(
            Principal principal,
            @PathVariable("teamId") String teamId,
            @PathVariable("targetCricketUserId") String targetCricketUserId,
            @RequestBody UpdateMemberRoleRequestDto request) {
        TeamMemberResponseDto response = teamService.updateMemberRole(principal.getName(), teamId, targetCricketUserId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/join/{joinToken}")
    public ResponseEntity<TeamResponseDto> getTeamByJoinToken(@PathVariable("joinToken") String joinToken) {
        TeamResponseDto response = teamService.getTeamByJoinToken(joinToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join/{joinToken}")
    public ResponseEntity<MessageResponse> joinTeamByToken(
            Principal principal,
            @PathVariable("joinToken") String joinToken) {
        MessageResponse response = teamService.joinTeamByToken(principal.getName(), joinToken);
        return ResponseEntity.ok(response);
    }
}

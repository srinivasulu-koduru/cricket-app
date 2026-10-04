package com.cricketapp.controller;

import com.cricketapp.dto.MessageResponse;
import com.cricketapp.dto.TeamInvitationResponseDto;
import com.cricketapp.service.TeamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/team-invitations")
public class TeamInvitationController {

    private final TeamService teamService;

    public TeamInvitationController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<TeamInvitationResponseDto>> getMyInvitations(Principal principal) {
        List<TeamInvitationResponseDto> invitations = teamService.getMyInvitations(principal.getName());
        return ResponseEntity.ok(invitations);
    }

    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<MessageResponse> acceptInvitation(
            Principal principal,
            @PathVariable("invitationId") Long invitationId) {
        MessageResponse response = teamService.acceptInvitation(principal.getName(), invitationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<MessageResponse> rejectInvitation(
            Principal principal,
            @PathVariable("invitationId") Long invitationId) {
        MessageResponse response = teamService.rejectInvitation(principal.getName(), invitationId);
        return ResponseEntity.ok(response);
    }
}

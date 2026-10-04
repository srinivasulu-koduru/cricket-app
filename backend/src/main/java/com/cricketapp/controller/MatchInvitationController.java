package com.cricketapp.controller;

import com.cricketapp.dto.MatchInvitationResponseDto;
import com.cricketapp.service.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/match-invitations")
public class MatchInvitationController {

    private final MatchService matchService;

    public MatchInvitationController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<MatchInvitationResponseDto>> getMyMatchInvitations(Principal principal) {
        List<MatchInvitationResponseDto> invitations = matchService.getMyMatchInvitations(principal.getName());
        return ResponseEntity.ok(invitations);
    }

    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<MatchInvitationResponseDto> acceptMatchInvitation(
            Principal principal,
            @PathVariable("invitationId") Long invitationId) {
        MatchInvitationResponseDto response = matchService.acceptMatchInvitation(principal.getName(), invitationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<MatchInvitationResponseDto> rejectMatchInvitation(
            Principal principal,
            @PathVariable("invitationId") Long invitationId) {
        MatchInvitationResponseDto response = matchService.rejectMatchInvitation(principal.getName(), invitationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{invitationId}/cancel")
    public ResponseEntity<MatchInvitationResponseDto> cancelMatchInvitation(
            Principal principal,
            @PathVariable("invitationId") Long invitationId) {
        MatchInvitationResponseDto response = matchService.cancelMatchInvitation(principal.getName(), invitationId);
        return ResponseEntity.ok(response);
    }
}

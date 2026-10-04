package com.cricketapp.service;

import com.cricketapp.dto.*;
import com.cricketapp.entity.*;
import com.cricketapp.exception.AuthException;
import com.cricketapp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TeamServiceTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository memberRepository;

    @Autowired
    private TeamInvitationRepository invitationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User userOwner;
    private User userPlayer;
    private User userThird;

    @BeforeEach
    public void setup() {
        invitationRepository.deleteAll();
        memberRepository.deleteAll();
        teamRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users (Stage 2 setup)
        userOwner = new User("CRK102847", "Srinivasulu Owner", "owner@example.com", passwordEncoder.encode("Password123"), true);
        userOwner = userRepository.save(userOwner);

        userPlayer = new User("CRK203451", "Rahul Player", "player@example.com", passwordEncoder.encode("Password123"), true);
        userPlayer = userRepository.save(userPlayer);

        userThird = new User("CRK384920", "Kiran Third", "third@example.com", passwordEncoder.encode("Password123"), true);
        userThird = userRepository.save(userThird);
    }

    @Test
    public void test1_AuthenticatedUserCanCreateTeam() {
        CreateTeamRequestDto req = new CreateTeamRequestDto("Nellore Warriors", "Best cricket team in Nellore");
        TeamResponseDto response = teamService.createTeam("owner@example.com", req);

        assertNotNull(response);
        assertNotNull(response.getTeamId());
        assertTrue(response.getTeamId().startsWith("TEAM"));
        assertEquals("Nellore Warriors", response.getName());
        assertEquals("Best cricket team in Nellore", response.getDescription());
        assertEquals("CRK102847", response.getCreatedByUserId());
        assertEquals(TeamRole.OWNER, response.getCurrentUserRole());
        assertEquals(1, response.getMemberCount());
    }

    @Test
    public void test2_UnauthenticatedUserCannotCreateTeam() {
        CreateTeamRequestDto req = new CreateTeamRequestDto("Nellore Warriors", "Description");
        assertThrows(AuthException.class, () -> teamService.createTeam("nonexistent@example.com", req));
    }

    @Test
    public void test3_CreatorAutomaticallyAssignedAsOwnerAndActiveMember() {
        CreateTeamRequestDto req = new CreateTeamRequestDto("Chennai Super Stars", "CSK Fan Club");
        TeamResponseDto teamDto = teamService.createTeam("owner@example.com", req);

        List<TeamMemberResponseDto> members = teamService.getTeamMembers("owner@example.com", teamDto.getTeamId());
        assertEquals(1, members.size());

        TeamMemberResponseDto ownerMember = members.get(0);
        assertEquals("CRK102847", ownerMember.getUserId());
        assertEquals(TeamRole.OWNER, ownerMember.getRole());
        assertEquals(MemberStatus.ACTIVE, ownerMember.getStatus());
    }

    @Test
    public void test4_GetMyTeamsReturnsUserActiveTeams() {
        teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Team Alpha", "Alpha team"));
        teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Team Beta", "Beta team"));

        List<TeamResponseDto> myTeams = teamService.getMyTeams("owner@example.com");
        assertEquals(2, myTeams.size());
    }

    @Test
    public void test5_InvitePlayerWithValidCricketUserId() {
        TeamResponseDto team = teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Nellore Warriors", "Desc"));

        TeamInvitationResponseDto invitation = teamService.invitePlayer("owner@example.com", team.getTeamId(), "CRK203451");

        assertNotNull(invitation);
        assertEquals(team.getTeamId(), invitation.getTeamId());
        assertEquals("CRK203451", invitation.getInvitedUserId());
        assertEquals("CRK102847", invitation.getInvitedByUserId());
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
    }

    @Test
    public void test6_InvitePlayerWithInvalidCricketUserIdRejected() {
        TeamResponseDto team = teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Nellore Warriors", "Desc"));

        AuthException ex = assertThrows(AuthException.class, () ->
                teamService.invitePlayer("owner@example.com", team.getTeamId(), "CRK999999"));

        assertTrue(ex.getMessage().contains("Player not found"));
    }

    @Test
    public void test7_DuplicatePendingInvitationPrevented() {
        TeamResponseDto team = teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Nellore Warriors", "Desc"));
        teamService.invitePlayer("owner@example.com", team.getTeamId(), "CRK203451");

        AuthException ex = assertThrows(AuthException.class, () ->
                teamService.invitePlayer("owner@example.com", team.getTeamId(), "CRK203451"));

        assertTrue(ex.getMessage().contains("already pending"));
    }

    @Test
    public void test8_InvitedPlayerCanAcceptInvitation() {
        TeamResponseDto team = teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Nellore Warriors", "Desc"));
        TeamInvitationResponseDto inv = teamService.invitePlayer("owner@example.com", team.getTeamId(), "CRK203451");

        MessageResponse response = teamService.acceptInvitation("player@example.com", inv.getInvitationId());
        assertTrue(response.getMessage().contains("accepted"));

        // Verify Player B is now an active member
        List<TeamMemberResponseDto> members = teamService.getTeamMembers("owner@example.com", team.getTeamId());
        assertEquals(2, members.size());

        boolean playerFound = members.stream().anyMatch(m -> m.getUserId().equals("CRK203451") && m.getRole() == TeamRole.PLAYER);
        assertTrue(playerFound);
    }

    @Test
    public void test9_InvitedPlayerCanRejectInvitation() {
        TeamResponseDto team = teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Nellore Warriors", "Desc"));
        TeamInvitationResponseDto inv = teamService.invitePlayer("owner@example.com", team.getTeamId(), "CRK203451");

        MessageResponse response = teamService.rejectInvitation("player@example.com", inv.getInvitationId());
        assertTrue(response.getMessage().contains("rejected"));

        // Verify user is NOT added to team
        List<TeamMemberResponseDto> members = teamService.getTeamMembers("owner@example.com", team.getTeamId());
        assertEquals(1, members.size());
    }

    @Test
    public void test10_OwnerCanCancelInvitation() {
        TeamResponseDto team = teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Nellore Warriors", "Desc"));
        TeamInvitationResponseDto inv = teamService.invitePlayer("owner@example.com", team.getTeamId(), "CRK203451");

        MessageResponse response = teamService.cancelInvitation("owner@example.com", team.getTeamId(), inv.getInvitationId());
        assertTrue(response.getMessage().contains("cancelled"));
    }

    @Test
    public void test11_OwnerCanEditTeam() {
        TeamResponseDto team = teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Old Name", "Old Desc"));
        UpdateTeamRequestDto updateReq = new UpdateTeamRequestDto("Updated Warriors", "New Description");

        TeamResponseDto updated = teamService.updateTeam("owner@example.com", team.getTeamId(), updateReq);

        assertEquals("Updated Warriors", updated.getName());
        assertEquals("New Description", updated.getDescription());
    }

    @Test
    public void test12_NonOwnerCannotEditTeam() {
        TeamResponseDto team = teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Nellore Warriors", "Desc"));
        TeamInvitationResponseDto inv = teamService.invitePlayer("owner@example.com", team.getTeamId(), "CRK203451");
        teamService.acceptInvitation("player@example.com", inv.getInvitationId());

        UpdateTeamRequestDto updateReq = new UpdateTeamRequestDto("Hacked Name", "Hacked Desc");

        AuthException ex = assertThrows(AuthException.class, () ->
                teamService.updateTeam("player@example.com", team.getTeamId(), updateReq));

        assertTrue(ex.getMessage().contains("Only the team owner or captain"));
    }

    @Test
    public void test13_OwnerCanRemovePlayer() {
        TeamResponseDto team = teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Nellore Warriors", "Desc"));
        TeamInvitationResponseDto inv = teamService.invitePlayer("owner@example.com", team.getTeamId(), "CRK203451");
        teamService.acceptInvitation("player@example.com", inv.getInvitationId());

        MessageResponse response = teamService.removePlayer("owner@example.com", team.getTeamId(), "CRK203451");
        assertTrue(response.getMessage().contains("removed"));

        // Verify active member count is 1
        List<TeamMemberResponseDto> members = teamService.getTeamMembers("owner@example.com", team.getTeamId());
        assertEquals(1, members.size());
    }

    @Test
    public void test14_NonOwnerCannotRemovePlayer() {
        TeamResponseDto team = teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Nellore Warriors", "Desc"));
        TeamInvitationResponseDto inv2 = teamService.invitePlayer("owner@example.com", team.getTeamId(), "CRK203451");
        teamService.acceptInvitation("player@example.com", inv2.getInvitationId());

        TeamInvitationResponseDto inv3 = teamService.invitePlayer("owner@example.com", team.getTeamId(), "CRK384920");
        teamService.acceptInvitation("third@example.com", inv3.getInvitationId());

        // Player B attempts to remove Player C
        assertThrows(AuthException.class, () ->
                teamService.removePlayer("player@example.com", team.getTeamId(), "CRK384920"));
    }

    @Test
    public void test15_PlayerCanLeaveTeam() {
        TeamResponseDto team = teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Nellore Warriors", "Desc"));
        TeamInvitationResponseDto inv = teamService.invitePlayer("owner@example.com", team.getTeamId(), "CRK203451");
        teamService.acceptInvitation("player@example.com", inv.getInvitationId());

        MessageResponse response = teamService.leaveTeam("player@example.com", team.getTeamId());
        assertTrue(response.getMessage().contains("left the team"));

        List<TeamMemberResponseDto> members = teamService.getTeamMembers("owner@example.com", team.getTeamId());
        assertEquals(1, members.size());
    }

    @Test
    public void test16_OwnerCannotLeaveTeamWithoutTransfer() {
        TeamResponseDto team = teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Nellore Warriors", "Desc"));

        AuthException ex = assertThrows(AuthException.class, () ->
                teamService.leaveTeam("owner@example.com", team.getTeamId()));

        assertTrue(ex.getMessage().contains("cannot leave the team without transferring ownership"));
    }

    @Test
    public void test17_ShareableJoinTokenWorks() {
        TeamResponseDto team = teamService.createTeam("owner@example.com", new CreateTeamRequestDto("Nellore Warriors", "Desc"));
        String token = team.getJoinToken();

        // Get details via public token
        TeamResponseDto preview = teamService.getTeamByJoinToken(token);
        assertEquals("Nellore Warriors", preview.getName());

        // Player B joins via token
        MessageResponse response = teamService.joinTeamByToken("player@example.com", token);
        assertTrue(response.getMessage().contains("successfully joined"));

        List<TeamMemberResponseDto> members = teamService.getTeamMembers("owner@example.com", team.getTeamId());
        assertEquals(2, members.size());
    }
}

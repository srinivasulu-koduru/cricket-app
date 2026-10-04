package com.cricketapp.service;

import com.cricketapp.dto.*;
import com.cricketapp.entity.*;
import com.cricketapp.exception.AuthException;
import com.cricketapp.repository.MatchInvitationRepository;
import com.cricketapp.repository.MatchPlayingXiRepository;
import com.cricketapp.repository.MatchRepository;
import com.cricketapp.repository.PlayerProfileRepository;
import com.cricketapp.repository.TeamMemberRepository;
import com.cricketapp.repository.TeamRepository;
import com.cricketapp.repository.UserRepository;
import com.cricketapp.util.MatchIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MatchService {

    private static final Logger logger = LoggerFactory.getLogger(MatchService.class);

    private final MatchRepository matchRepository;
    private final MatchInvitationRepository matchInvitationRepository;
    private final MatchPlayingXiRepository matchPlayingXiRepository;
    private final PlayerProfileRepository playerProfileRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final MatchIdGenerator matchIdGenerator;
    private final EmailService emailService;
    private final com.cricketapp.repository.MatchScorerRepository matchScorerRepository;
    private final NotificationService notificationService;
    private final com.cricketapp.repository.MatchTossRepository matchTossRepository;
    private final com.cricketapp.repository.InningsRepository inningsRepository;
    private final com.cricketapp.repository.BallEventRepository ballEventRepository;
    private final com.cricketapp.repository.MatchPauseRepository matchPauseRepository;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public MatchService(MatchRepository matchRepository,
                        MatchInvitationRepository matchInvitationRepository,
                        MatchPlayingXiRepository matchPlayingXiRepository,
                        PlayerProfileRepository playerProfileRepository,
                        TeamRepository teamRepository,
                        TeamMemberRepository teamMemberRepository,
                        UserRepository userRepository,
                        MatchIdGenerator matchIdGenerator,
                        EmailService emailService,
                        com.cricketapp.repository.MatchScorerRepository matchScorerRepository,
                        NotificationService notificationService,
                        com.cricketapp.repository.MatchTossRepository matchTossRepository,
                        com.cricketapp.repository.InningsRepository inningsRepository,
                        com.cricketapp.repository.BallEventRepository ballEventRepository,
                        com.cricketapp.repository.MatchPauseRepository matchPauseRepository,
                        @org.springframework.context.annotation.Lazy org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.matchRepository = matchRepository;
        this.matchInvitationRepository = matchInvitationRepository;
        this.matchPlayingXiRepository = matchPlayingXiRepository;
        this.playerProfileRepository = playerProfileRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.matchIdGenerator = matchIdGenerator;
        this.emailService = emailService;
        this.matchScorerRepository = matchScorerRepository;
        this.notificationService = notificationService;
        this.matchTossRepository = matchTossRepository;
        this.inningsRepository = inningsRepository;
        this.ballEventRepository = ballEventRepository;
        this.matchPauseRepository = matchPauseRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public MatchResponseDto createMatch(String email, CreateMatchRequestDto request) {
        User creator = getRequiredUserByEmail(email);

        if (!StringUtils.hasText(request.getMatchName())) {
            throw new AuthException("Match name cannot be blank");
        }

        String matchName = request.getMatchName().trim();
        if (matchName.length() > 150) {
            throw new AuthException("Match name cannot exceed 150 characters");
        }

        if (!StringUtils.hasText(request.getTeamAId()) || !StringUtils.hasText(request.getTeamBId())) {
            throw new AuthException("Please select both Team A and Team B");
        }

        String teamAId = request.getTeamAId().trim();
        String teamBId = request.getTeamBId().trim();

        if (teamAId.equalsIgnoreCase(teamBId)) {
            throw new AuthException("Please select two different teams for Team A and Team B");
        }

        Team teamA = teamRepository.findByTeamId(teamAId)
                .orElseThrow(() -> new AuthException("Team A not found with Team ID: " + teamAId));

        Team teamB = teamRepository.findByTeamId(teamBId)
                .orElseThrow(() -> new AuthException("Team B not found with Team ID: " + teamBId));

        // 1. Authorization check: Creator must be a Captain or Owner of Team A
        TeamMember creatorMemberA = teamMemberRepository.findByTeamAndUser(teamA, creator)
                .orElseThrow(() -> new AuthException("Only a Captain or Owner of " + teamA.getName() + " can schedule a match for Team A."));

        if (creatorMemberA.getStatus() != MemberStatus.ACTIVE ||
                (creatorMemberA.getRole() != TeamRole.OWNER && creatorMemberA.getRole() != TeamRole.CAPTAIN)) {
            throw new AuthException("Only a Captain or Owner of " + teamA.getName() + " can schedule a match for Team A.");
        }

        // 2. Validate no duplicate active players exist in both Team A and Team B
        List<TeamMember> membersA = teamMemberRepository.findByTeamAndStatus(teamA, MemberStatus.ACTIVE);
        List<TeamMember> membersB = teamMemberRepository.findByTeamAndStatus(teamB, MemberStatus.ACTIVE);

        java.util.Set<Long> userIdsA = membersA.stream()
                .map(m -> m.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());

        List<String> duplicatePlayers = membersB.stream()
                .filter(m -> userIdsA.contains(m.getUser().getId()))
                .map(m -> m.getUser().getName() + " (" + m.getUser().getUserId() + ")")
                .collect(java.util.stream.Collectors.toList());

        if (!duplicatePlayers.isEmpty()) {
            throw new AuthException("Cannot create match: Duplicate player(s) found in both teams (" 
                    + String.join(", ", duplicatePlayers) 
                    + "). A player cannot belong to both teams for a match.");
        }

        // 3. Find Opponent Captain from Team B
        User opponentCaptain = findOpponentCaptain(teamB);

        if (request.getMatchDate() == null) {
            throw new AuthException("Please select a valid match date");
        }

        if (request.getMatchTime() == null) {
            throw new AuthException("Please select a valid match time");
        }

        if (!StringUtils.hasText(request.getVenue())) {
            throw new AuthException("Venue cannot be blank");
        }

        String venue = request.getVenue().trim();
        if (venue.length() > 150) {
            throw new AuthException("Venue cannot exceed 150 characters");
        }

        if (request.getFormat() == null) {
            throw new AuthException("Please select a match format");
        }

        MatchFormat format = request.getFormat();
        int overs;

        switch (format) {
            case T10:
                overs = 10;
                break;
            case T20:
                overs = 20;
                break;
            case ODI:
                overs = 50;
                break;
            case CUSTOM:
                if (request.getOvers() == null || request.getOvers() <= 0 || request.getOvers() > 100) {
                    throw new AuthException("Please enter a valid number of overs (1 to 100) for custom format.");
                }
                overs = request.getOvers();
                break;
            default:
                throw new AuthException("Unsupported match format");
        }

        String description = request.getDescription() != null ? request.getDescription().trim() : null;
        if (description != null && description.length() > 500) {
            throw new AuthException("Description cannot exceed 500 characters");
        }

        String permanentMatchId = matchIdGenerator.generateUniqueMatchId();

        // Match starts in PENDING_CONFIRMATION status until Opponent Captain responds
        Match match = new Match(
                permanentMatchId,
                matchName,
                teamA,
                teamB,
                creator,
                request.getMatchDate(),
                request.getMatchTime(),
                venue,
                format,
                overs,
                description
        );
        match.setStatus(MatchStatus.PENDING_CONFIRMATION);

        match = matchRepository.save(match);

        // Create Match Invitation for Opponent Captain
        MatchInvitation invitation = new MatchInvitation(
                match,
                teamA,
                teamB,
                opponentCaptain,
                creator
        );
        invitation = matchInvitationRepository.save(invitation);

        logger.info("Match {} ({}) created in PENDING_CONFIRMATION state. Invitation sent to Opponent Captain {} ({})",
                match.getMatchName(), match.getMatchId(), opponentCaptain.getName(), opponentCaptain.getEmail());

        // Send Real Email Invitation to Opponent Captain's registered email
        try {
            emailService.sendMatchInvitationEmail(
                    opponentCaptain.getEmail(),
                    opponentCaptain.getName(),
                    creator.getName(),
                    teamA.getName(),
                    teamB.getName(),
                    match.getMatchDate().toString(),
                    match.getMatchTime().toString(),
                    match.getVenue(),
                    format.name(),
                    overs
            );
        } catch (Exception e) {
            logger.error("Non-fatal error sending match invitation email to {}: {}", opponentCaptain.getEmail(), e.getMessage());
        }

        return mapToMatchResponseDto(match, creator);
    }

    @Transactional(readOnly = true)
    public List<MatchResponseDto> getMyMatches(String email) {
        User user = findUserByEmail(email);
        List<Match> matches = matchRepository.findMyMatches(user);

        return matches.stream()
                .map(m -> mapToMatchResponseDto(m, user))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MatchResponseDto> getLiveMatches(String email) {
        User user = StringUtils.hasText(email) ? userRepository.findByEmail(email.toLowerCase().trim()).orElse(null) : null;
        List<Match> matches = matchRepository.findByStatusOrderByCreatedAtDesc(MatchStatus.LIVE);

        return matches.stream()
                .map(m -> mapToMatchResponseDto(m, user))
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MatchResponseDto getMatchDetails(String email, String matchId) {
        User user = findUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        return mapToMatchResponseDto(match, user);
    }

    @Transactional
    public MatchResponseDto updateMatch(String email, String matchId, UpdateMatchRequestDto request) {
        User user = findUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (!match.getCreatedBy().getId().equals(user.getId())) {
            throw new AuthException("Only the match creator can update this match");
        }

        if (match.getStatus() != MatchStatus.SCHEDULED && match.getStatus() != MatchStatus.PENDING_CONFIRMATION) {
            throw new AuthException("Completed or cancelled matches cannot be updated");
        }

        if (!StringUtils.hasText(request.getMatchName())) {
            throw new AuthException("Match name cannot be blank");
        }

        String matchName = request.getMatchName().trim();
        if (matchName.length() > 150) {
            throw new AuthException("Match name cannot exceed 150 characters");
        }

        if (request.getMatchDate() == null) {
            throw new AuthException("Please select a valid match date");
        }

        if (request.getMatchTime() == null) {
            throw new AuthException("Please select a valid match time");
        }

        if (!StringUtils.hasText(request.getVenue())) {
            throw new AuthException("Venue cannot be blank");
        }

        String venue = request.getVenue().trim();
        if (venue.length() > 150) {
            throw new AuthException("Venue cannot exceed 150 characters");
        }

        if (request.getFormat() == null) {
            throw new AuthException("Please select a match format");
        }

        MatchFormat format = request.getFormat();
        int overs;

        switch (format) {
            case T10:
                overs = 10;
                break;
            case T20:
                overs = 20;
                break;
            case ODI:
                overs = 50;
                break;
            case CUSTOM:
                if (request.getOvers() == null || request.getOvers() <= 0 || request.getOvers() > 100) {
                    throw new AuthException("Please enter a valid number of overs (1 to 100) for custom format.");
                }
                overs = request.getOvers();
                break;
            default:
                throw new AuthException("Unsupported match format");
        }

        String description = request.getDescription() != null ? request.getDescription().trim() : null;
        if (description != null && description.length() > 500) {
            throw new AuthException("Description cannot exceed 500 characters");
        }

        match.setMatchName(matchName);
        match.setMatchDate(request.getMatchDate());
        match.setMatchTime(request.getMatchTime());
        match.setVenue(venue);
        match.setFormat(format);
        match.setOvers(overs);
        match.setDescription(description);

        Match updated = matchRepository.save(match);
        return mapToMatchResponseDto(updated, user);
    }

    @Transactional
    public MatchResponseDto cancelMatch(String email, String matchId) {
        User user = findUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (!match.getCreatedBy().getId().equals(user.getId())) {
            throw new AuthException("Only the match creator can cancel this match");
        }

        if (match.getStatus() == MatchStatus.CANCELLED) {
            throw new AuthException("Match is already cancelled");
        }

        match.setStatus(MatchStatus.CANCELLED);
        Match saved = matchRepository.save(match);

        // Also update invitation to CANCELLED if pending
        Optional<MatchInvitation> invOpt = matchInvitationRepository.findByMatch(match);
        if (invOpt.isPresent()) {
            MatchInvitation inv = invOpt.get();
            if (inv.getStatus() == MatchInvitationStatus.PENDING) {
                inv.setStatus(MatchInvitationStatus.CANCELLED);
                matchInvitationRepository.save(inv);
            }
        }

        return mapToMatchResponseDto(saved, user);
    }

    // ==========================================
    // MATCH INVITATION WORKFLOW
    // ==========================================

    @Transactional(readOnly = true)
    public List<MatchInvitationResponseDto> getMyMatchInvitations(String email) {
        User user = findUserByEmail(email);
        List<MatchInvitation> invitations = matchInvitationRepository.findByInvitedCaptain_EmailOrderByCreatedAtDesc(user.getEmail());

        return invitations.stream()
                .map(this::mapToMatchInvitationResponseDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public MatchInvitationResponseDto acceptMatchInvitation(String email, Long invitationId) {
        User user = findUserByEmail(email);
        MatchInvitation invitation = matchInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new AuthException("Match invitation not found"));

        if (!invitation.getInvitedCaptain().getId().equals(user.getId())) {
            throw new AuthException("Only the invited opponent team captain can accept this match invitation.");
        }

        if (invitation.getStatus() != MatchInvitationStatus.PENDING) {
            throw new AuthException("This match invitation is no longer pending.");
        }

        invitation.setStatus(MatchInvitationStatus.ACCEPTED);
        invitation = matchInvitationRepository.save(invitation);

        Match match = invitation.getMatch();
        match.setStatus(MatchStatus.SCHEDULED);
        matchRepository.save(match);

        logger.info("Match invitation {} ACCEPTED by Captain {}. Match {} is now SCHEDULED.",
                invitationId, user.getName(), match.getMatchId());

        return mapToMatchInvitationResponseDto(invitation);
    }

    @Transactional
    public MatchInvitationResponseDto rejectMatchInvitation(String email, Long invitationId) {
        User user = findUserByEmail(email);
        MatchInvitation invitation = matchInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new AuthException("Match invitation not found"));

        if (!invitation.getInvitedCaptain().getId().equals(user.getId())) {
            throw new AuthException("Only the invited opponent team captain can reject this match invitation.");
        }

        if (invitation.getStatus() != MatchInvitationStatus.PENDING) {
            throw new AuthException("This match invitation is no longer pending.");
        }

        invitation.setStatus(MatchInvitationStatus.REJECTED);
        invitation = matchInvitationRepository.save(invitation);

        Match match = invitation.getMatch();
        match.setStatus(MatchStatus.CANCELLED);
        matchRepository.save(match);

        logger.info("Match invitation {} REJECTED by Captain {}. Match {} is now CANCELLED.",
                invitationId, user.getName(), match.getMatchId());

        return mapToMatchInvitationResponseDto(invitation);
    }

    @Transactional
    public MatchInvitationResponseDto cancelMatchInvitation(String email, Long invitationId) {
        User user = findUserByEmail(email);
        MatchInvitation invitation = matchInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new AuthException("Match invitation not found"));

        if (!invitation.getInvitedBy().getId().equals(user.getId())) {
            throw new AuthException("Only the match creator can cancel this invitation.");
        }

        if (invitation.getStatus() != MatchInvitationStatus.PENDING) {
            throw new AuthException("Match invitation is no longer pending.");
        }

        invitation.setStatus(MatchInvitationStatus.CANCELLED);
        invitation = matchInvitationRepository.save(invitation);

        Match match = invitation.getMatch();
        match.setStatus(MatchStatus.CANCELLED);
        matchRepository.save(match);

        return mapToMatchInvitationResponseDto(invitation);
    }

    private User findOpponentCaptain(Team opponentTeam) {
        List<TeamMember> activeMembers = teamMemberRepository.findByTeamAndStatus(opponentTeam, MemberStatus.ACTIVE);

        Optional<TeamMember> captainOpt = activeMembers.stream()
                .filter(m -> m.getRole() == TeamRole.CAPTAIN || m.getRole() == TeamRole.OWNER)
                .findFirst();

        if (captainOpt.isEmpty()) {
            throw new AuthException("Opponent team " + opponentTeam.getName() + " does not have an active Captain or Owner to receive the match invitation.");
        }

        return captainOpt.get().getUser();
    }

    private User findUserByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        String normalized = email.toLowerCase().trim();
        return userRepository.findByEmail(normalized).orElse(null);
    }

    private User getRequiredUserByEmail(String email) {
        User user = findUserByEmail(email);
        if (user == null) {
            throw new AuthException("User authentication required.");
        }
        return user;
    }

    private Match findMatchByMatchId(String matchId) {
        return matchRepository.findByMatchId(matchId.trim())
                .orElseThrow(() -> new AuthException("Match not found with Match ID: " + matchId));
    }

    private MatchResponseDto mapToMatchResponseDto(Match match, User currentUser) {
        MatchResponseDto dto = new MatchResponseDto();
        dto.setMatchId(match.getMatchId());
        dto.setMatchName(match.getMatchName());

        Team tA = match.getTeamA();
        dto.setTeamA(new MatchResponseDto.TeamSummaryDto(tA.getTeamId(), tA.getName(), tA.getLogoUrl()));

        Team tB = match.getTeamB();
        dto.setTeamB(new MatchResponseDto.TeamSummaryDto(tB.getTeamId(), tB.getName(), tB.getLogoUrl()));

        User creator = match.getCreatedBy();
        dto.setCreatedByUserId(creator.getUserId());
        dto.setCreatedByName(creator.getName());

        User scorer = match.getScorer();
        if (scorer != null) {
            dto.setScorerUserId(scorer.getUserId());
            dto.setScorerName(scorer.getName());
            dto.setIsScorer(currentUser != null && scorer.getId().equals(currentUser.getId()));
        } else {
            dto.setIsScorer(false);
        }

        dto.setMatchDate(match.getMatchDate());
        dto.setMatchTime(match.getMatchTime());
        dto.setVenue(match.getVenue());
        dto.setFormat(match.getFormat());
        dto.setOvers(match.getOvers());
        dto.setDescription(match.getDescription());
        dto.setStatus(match.getStatus());
        boolean isCreator = currentUser != null && creator.getId().equals(currentUser.getId());
        dto.setIsCreator(isCreator);
        dto.setCreatedAt(match.getCreatedAt());
        dto.setUpdatedAt(match.getUpdatedAt());

        boolean isInvitedCap = false;
        Optional<MatchInvitation> invOpt = matchInvitationRepository.findByMatch(match);
        if (invOpt.isPresent()) {
            MatchInvitation inv = invOpt.get();
            dto.setInvitationId(inv.getId());
            dto.setInvitationStatus(inv.getStatus());
            dto.setInvitedCaptainUserId(inv.getInvitedCaptain().getUserId());
            dto.setInvitedCaptainName(inv.getInvitedCaptain().getName());
            isInvitedCap = currentUser != null && inv.getInvitedCaptain().getId().equals(currentUser.getId());
            dto.setIsInvitedCaptain(isInvitedCap);
        } else {
            dto.setIsInvitedCaptain(false);
        }

        boolean isCaptain = isCreator || isInvitedCap;
        if (!isCaptain && currentUser != null) {
            Optional<TeamMember> memA = teamMemberRepository.findByTeamAndUser(match.getTeamA(), currentUser);
            if (memA.isPresent() && memA.get().getStatus() == MemberStatus.ACTIVE &&
                    (memA.get().getRole() == TeamRole.CAPTAIN || memA.get().getRole() == TeamRole.OWNER)) {
                isCaptain = true;
            }
            if (!isCaptain) {
                Optional<TeamMember> memB = teamMemberRepository.findByTeamAndUser(match.getTeamB(), currentUser);
                if (memB.isPresent() && memB.get().getStatus() == MemberStatus.ACTIVE &&
                        (memB.get().getRole() == TeamRole.CAPTAIN || memB.get().getRole() == TeamRole.OWNER)) {
                    isCaptain = true;
                }
            }
        }
        dto.setIsCaptain(isCaptain);

        return dto;
    }

    private MatchInvitationResponseDto mapToMatchInvitationResponseDto(MatchInvitation inv) {
        Match match = inv.getMatch();
        MatchInvitationResponseDto dto = new MatchInvitationResponseDto();

        dto.setInvitationId(inv.getId());
        dto.setMatchId(match.getMatchId());
        dto.setMatchName(match.getMatchName());

        Team tA = inv.getInvitingTeam();
        dto.setInvitingTeamId(tA.getTeamId());
        dto.setInvitingTeamName(tA.getName());
        dto.setInvitingTeamLogoUrl(tA.getLogoUrl());

        Team tB = inv.getOpponentTeam();
        dto.setOpponentTeamId(tB.getTeamId());
        dto.setOpponentTeamName(tB.getName());
        dto.setOpponentTeamLogoUrl(tB.getLogoUrl());

        User inviter = inv.getInvitedBy();
        dto.setInvitedByUserId(inviter.getUserId());
        dto.setInvitedByName(inviter.getName());

        User captain = inv.getInvitedCaptain();
        dto.setInvitedCaptainUserId(captain.getUserId());
        dto.setInvitedCaptainName(captain.getName());

        dto.setMatchDate(match.getMatchDate());
        dto.setMatchTime(match.getMatchTime());
        dto.setVenue(match.getVenue());
        dto.setFormat(match.getFormat());
        dto.setOvers(match.getOvers());
        dto.setDescription(match.getDescription());
        dto.setInvitationStatus(inv.getStatus());
        dto.setMatchStatus(match.getStatus());
        dto.setCreatedAt(inv.getCreatedAt());
        dto.setUpdatedAt(inv.getUpdatedAt());

        return dto;
    }

    // ==========================================
    // PLAYING XI SELECTION WORKFLOW
    // ==========================================

    @Transactional
    public TeamPlayingXiResponseDto savePlayingXi(String email, String matchId, String teamId, SavePlayingXiRequestDto request) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        Team team;
        Team opponentTeam;
        if (match.getTeamA().getTeamId().equalsIgnoreCase(teamId.trim())) {
            team = match.getTeamA();
            opponentTeam = match.getTeamB();
        } else if (match.getTeamB().getTeamId().equalsIgnoreCase(teamId.trim())) {
            team = match.getTeamB();
            opponentTeam = match.getTeamA();
        } else {
            throw new AuthException("Team " + teamId + " is not participating in this match.");
        }

        // 1. Authorization check: Requesting user must be active Captain or Owner of team
        TeamMember member = teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new AuthException("Only a Captain or Owner of " + team.getName() + " can select the Playing XI."));

        if (member.getStatus() != MemberStatus.ACTIVE ||
                (member.getRole() != TeamRole.OWNER && member.getRole() != TeamRole.CAPTAIN)) {
            throw new AuthException("Only a Captain or Owner of " + team.getName() + " can select the Playing XI.");
        }

        // 2. Validate player list size
        List<String> playerUserIds = request != null ? request.getPlayerUserIds() : null;
        if (playerUserIds == null || playerUserIds.isEmpty()) {
            throw new AuthException("Please select players to form the Playing XI.");
        }

        if (playerUserIds.size() > 11) {
            throw new AuthException("Playing XI cannot exceed 11 players per team.");
        }

        // 3. Validate players belong to team
        List<TeamMember> teamActiveMembers = teamMemberRepository.findByTeamAndStatus(team, MemberStatus.ACTIVE);
        java.util.Map<String, TeamMember> memberMap = teamActiveMembers.stream()
                .collect(java.util.stream.Collectors.toMap(m -> m.getUser().getUserId(), m -> m, (k1, k2) -> k1));

        for (String pUserId : playerUserIds) {
            if (!memberMap.containsKey(pUserId)) {
                throw new AuthException("Player " + pUserId + " is not an active member of " + team.getName());
            }
        }

        // 3b. Validate that team Captain/Owner is ALWAYS included in Playing XI
        Optional<TeamMember> teamCaptainOpt = teamActiveMembers.stream()
                .filter(m -> m.getRole() == TeamRole.CAPTAIN || m.getRole() == TeamRole.OWNER)
                .findFirst();

        if (teamCaptainOpt.isPresent()) {
            String teamCapUserId = teamCaptainOpt.get().getUser().getUserId();
            if (!playerUserIds.contains(teamCapUserId)) {
                if (playerUserIds.size() >= 11) {
                    throw new AuthException("Team Captain " + teamCaptainOpt.get().getUser().getName() + " (" + teamCapUserId + ") must be included in the Playing XI. Please remove another player first.");
                }
                playerUserIds = new java.util.ArrayList<>(playerUserIds);
                playerUserIds.add(0, teamCapUserId);
            }
        }

        // 4. Validate no selected player is in opponent team's saved Playing XI
        List<MatchPlayingXi> opponentXi = matchPlayingXiRepository.findByMatchAndTeam(match, opponentTeam);
        java.util.Set<String> opponentPlayerIds = opponentXi.stream()
                .map(xi -> xi.getUser().getUserId())
                .collect(java.util.stream.Collectors.toSet());

        for (String pUserId : playerUserIds) {
            if (opponentPlayerIds.contains(pUserId)) {
                User pUser = memberMap.get(pUserId).getUser();
                throw new AuthException("Player " + pUser.getName() + " (" + pUserId + ") is already selected in opponent team's Playing XI.");
            }
        }

        // 5. Delete existing records for this team & match
        matchPlayingXiRepository.deleteByMatchAndTeam(match, team);
        matchPlayingXiRepository.flush();

        // 6. Save new records
        String wkUserId = request.getWicketKeeperUserId();
        String capUserId = request.getCaptainUserId();

        List<MatchPlayingXi> newXiList = new java.util.ArrayList<>();
        for (String pUserId : playerUserIds) {
            TeamMember tm = memberMap.get(pUserId);
            boolean isCap = (capUserId != null && capUserId.equalsIgnoreCase(pUserId)) || tm.getRole() == TeamRole.CAPTAIN || tm.getRole() == TeamRole.OWNER;
            boolean isWk = wkUserId != null && wkUserId.equalsIgnoreCase(pUserId);

            MatchPlayingXi xi = new MatchPlayingXi(match, team, tm.getUser(), isCap, isWk);
            newXiList.add(xi);
        }

        matchPlayingXiRepository.saveAll(newXiList);

        logger.info("Saved Playing XI ({} players) for team {} in match {}", newXiList.size(), team.getName(), match.getMatchId());

        return buildTeamPlayingXiResponseDto(match, team, user);
    }

    @Transactional(readOnly = true)
    public TeamPlayingXiResponseDto getMatchPlayingXiForTeam(String email, String matchId, String teamId) {
        User user = findUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        Team team;
        if (match.getTeamA().getTeamId().equalsIgnoreCase(teamId.trim())) {
            team = match.getTeamA();
        } else if (match.getTeamB().getTeamId().equalsIgnoreCase(teamId.trim())) {
            team = match.getTeamB();
        } else {
            throw new AuthException("Team " + teamId + " is not participating in this match.");
        }

        return buildTeamPlayingXiResponseDto(match, team, user);
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, TeamPlayingXiResponseDto> getAllMatchPlayingXi(String email, String matchId) {
        User user = findUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        java.util.Map<String, TeamPlayingXiResponseDto> result = new java.util.HashMap<>();
        result.put("teamA", buildTeamPlayingXiResponseDto(match, match.getTeamA(), user));
        result.put("teamB", buildTeamPlayingXiResponseDto(match, match.getTeamB(), user));

        return result;
    }

    private TeamPlayingXiResponseDto buildTeamPlayingXiResponseDto(Match match, Team team, User currentUser) {
        TeamPlayingXiResponseDto dto = new TeamPlayingXiResponseDto();
        dto.setTeamId(team.getTeamId());
        dto.setTeamName(team.getName());

        Optional<TeamMember> userMember = teamMemberRepository.findByTeamAndUser(team, currentUser);
        boolean isCap = userMember.isPresent() && userMember.get().getStatus() == MemberStatus.ACTIVE &&
                (userMember.get().getRole() == TeamRole.CAPTAIN || userMember.get().getRole() == TeamRole.OWNER);
        dto.setIsCaptainOfTeam(isCap);

        List<MatchPlayingXi> xiList = matchPlayingXiRepository.findByMatchAndTeam(match, team);
        boolean isSaved = !xiList.isEmpty();
        dto.setIsSaved(isSaved);

        List<PlayingXiPlayerDto> players = new java.util.ArrayList<>();

        if (isSaved) {
            for (MatchPlayingXi xi : xiList) {
                User u = xi.getUser();
                Optional<PlayerProfile> profileOpt = playerProfileRepository.findByUser(u);
                Optional<TeamMember> tmOpt = teamMemberRepository.findByTeamAndUser(team, u);

                PlayingXiPlayerDto pDto = new PlayingXiPlayerDto();
                pDto.setUserId(u.getUserId());
                pDto.setName(u.getName());
                if (profileOpt.isPresent()) {
                    PlayerProfile profile = profileOpt.get();
                    pDto.setProfilePhotoUrl(profile.getProfilePhotoUrl());
                    pDto.setPlayingRole(profile.getPlayingRole() != null ? profile.getPlayingRole().name() : null);
                    pDto.setBattingStyle(profile.getBattingStyle() != null ? profile.getBattingStyle().name() : null);
                    pDto.setBowlingStyle(profile.getBowlingStyle() != null ? profile.getBowlingStyle().name() : null);
                }
                pDto.setIsCaptain(xi.isCaptain());
                pDto.setIsWicketKeeper(xi.isWicketKeeper());
                pDto.setTeamRole(tmOpt.isPresent() && tmOpt.get().getRole() != null ? tmOpt.get().getRole().name() : null);

                players.add(pDto);
            }
        } else {
            // If not saved yet, list active squad members for team
            List<TeamMember> activeMembers = teamMemberRepository.findByTeamAndStatus(team, MemberStatus.ACTIVE);
            for (TeamMember tm : activeMembers) {
                User u = tm.getUser();
                Optional<PlayerProfile> profileOpt = playerProfileRepository.findByUser(u);

                PlayingXiPlayerDto pDto = new PlayingXiPlayerDto();
                pDto.setUserId(u.getUserId());
                pDto.setName(u.getName());
                if (profileOpt.isPresent()) {
                    PlayerProfile profile = profileOpt.get();
                    pDto.setProfilePhotoUrl(profile.getProfilePhotoUrl());
                    pDto.setPlayingRole(profile.getPlayingRole() != null ? profile.getPlayingRole().name() : null);
                    pDto.setBattingStyle(profile.getBattingStyle() != null ? profile.getBattingStyle().name() : null);
                    pDto.setBowlingStyle(profile.getBowlingStyle() != null ? profile.getBowlingStyle().name() : null);
                }
                pDto.setIsCaptain(tm.getRole() == TeamRole.CAPTAIN || tm.getRole() == TeamRole.OWNER);
                pDto.setIsWicketKeeper(profileOpt.isPresent() && profileOpt.get().getPlayingRole() == PlayingRole.WICKET_KEEPER);
                pDto.setTeamRole(tm.getRole() != null ? tm.getRole().name() : null);

                players.add(pDto);
            }
        }

        dto.setPlayers(players);
        return dto;
    }

    @Transactional
    public MatchResponseDto assignScorer(String email, String matchId, AssignScorerRequestDto request) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.CANCELLED) {
            throw new AuthException("Cannot assign match scorer for completed or cancelled matches.");
        }

        // Authorization check: Requesting user MUST be a captain/owner of Team A, captain/owner of Team B, or Match Creator
        boolean isCreator = match.getCreatedBy().getId().equals(user.getId());

        boolean isCapA = false;
        Optional<TeamMember> memberA = teamMemberRepository.findByTeamAndUser(match.getTeamA(), user);
        if (memberA.isPresent() && memberA.get().getStatus() == MemberStatus.ACTIVE &&
                (memberA.get().getRole() == TeamRole.CAPTAIN || memberA.get().getRole() == TeamRole.OWNER)) {
            isCapA = true;
        }

        boolean isCapB = false;
        Optional<TeamMember> memberB = teamMemberRepository.findByTeamAndUser(match.getTeamB(), user);
        if (memberB.isPresent() && memberB.get().getStatus() == MemberStatus.ACTIVE &&
                (memberB.get().getRole() == TeamRole.CAPTAIN || memberB.get().getRole() == TeamRole.OWNER)) {
            isCapB = true;
        }

        if (!isCreator && !isCapA && !isCapB) {
            throw new AuthException("Only a Team Captain, Owner, or Match Creator can select and assign the Match Scorer.");
        }

        if (request == null || !StringUtils.hasText(request.getScorerUserId())) {
            throw new AuthException("Please select a valid user to assign as Match Scorer.");
        }

        String targetScorerId = request.getScorerUserId().trim();
        User targetUser = userRepository.findByUserId(targetScorerId)
                .orElseGet(() -> userRepository.findByEmail(targetScorerId.toLowerCase())
                        .orElseThrow(() -> new AuthException("Target scorer not found with CRK ID or Email: " + targetScorerId)));

        match.setScorer(targetUser);
        Match saved = matchRepository.save(match);

        // Record MatchScorer relationship audit entity
        com.cricketapp.entity.MatchScorer matchScorer = new com.cricketapp.entity.MatchScorer(saved, targetUser, user);
        matchScorerRepository.save(matchScorer);

        // Trigger Notification
        String notifTitle = "Assigned as Match Scorer";
        String notifMsg = "You have been assigned as official match scorer for " + match.getMatchName() 
                + " (" + match.getMatchId() + ") scheduled on " + match.getMatchDate() + " at " + match.getVenue() + ".";
        notificationService.createNotification(targetUser, notifTitle, notifMsg, "SCORER_ASSIGNMENT", match.getMatchId());

        logger.info("Assigned Match Scorer {} ({}) for match {} by captain/creator {}",
                targetUser.getName(), targetUser.getUserId(), match.getMatchId(), user.getName());

        return mapToMatchResponseDto(saved, user);
    }

    @Transactional
    public MatchResponseDto removeScorer(String email, String matchId) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (match.getStatus() == MatchStatus.LIVE || match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.CANCELLED) {
            throw new AuthException("Cannot remove scorer for live, completed, or cancelled matches.");
        }

        boolean isCreator = match.getCreatedBy().getId().equals(user.getId());
        boolean isCapA = false;
        Optional<TeamMember> memberA = teamMemberRepository.findByTeamAndUser(match.getTeamA(), user);
        if (memberA.isPresent() && memberA.get().getStatus() == MemberStatus.ACTIVE &&
                (memberA.get().getRole() == TeamRole.CAPTAIN || memberA.get().getRole() == TeamRole.OWNER)) {
            isCapA = true;
        }
        boolean isCapB = false;
        Optional<TeamMember> memberB = teamMemberRepository.findByTeamAndUser(match.getTeamB(), user);
        if (memberB.isPresent() && memberB.get().getStatus() == MemberStatus.ACTIVE &&
                (memberB.get().getRole() == TeamRole.CAPTAIN || memberB.get().getRole() == TeamRole.OWNER)) {
            isCapB = true;
        }

        if (!isCreator && !isCapA && !isCapB) {
            throw new AuthException("Only a Team Captain, Owner, or Match Creator can remove the Match Scorer.");
        }

        User oldScorer = match.getScorer();
        if (oldScorer != null) {
            Optional<com.cricketapp.entity.MatchScorer> msOpt = matchScorerRepository.findByMatchAndStatus(match, com.cricketapp.entity.ScorerStatus.ASSIGNED);
            if (msOpt.isPresent()) {
                com.cricketapp.entity.MatchScorer ms = msOpt.get();
                ms.setStatus(com.cricketapp.entity.ScorerStatus.REMOVED);
                matchScorerRepository.save(ms);
            }

            notificationService.createNotification(oldScorer, "Scorer Assignment Updated",
                    "You have been removed as scorer for match " + match.getMatchName() + " (" + match.getMatchId() + ").",
                    "SCORER_REMOVAL", match.getMatchId());
        }

        match.setScorer(null);
        Match saved = matchRepository.save(match);
        return mapToMatchResponseDto(saved, user);
    }

    @Transactional(readOnly = true)
    public List<MatchResponseDto> getAssignedMatchesForScorer(String email) {
        User user = getRequiredUserByEmail(email);
        List<Match> matches = matchRepository.findAll().stream()
                .filter(m -> m.getScorer() != null && m.getScorer().getId().equals(user.getId()))
                .filter(m -> m.getStatus() != MatchStatus.CANCELLED && m.getStatus() != MatchStatus.COMPLETED)
                .collect(java.util.stream.Collectors.toList());

        return matches.stream()
                .map(m -> mapToMatchResponseDto(m, user))
                .collect(java.util.stream.Collectors.toList());
    }

    private void autoPopulatePlayingXiIfEmpty(Match match) {
        if (match == null) return;

        List<MatchPlayingXi> xiA = matchPlayingXiRepository.findByMatchAndTeam(match, match.getTeamA());
        List<MatchPlayingXi> xiB = matchPlayingXiRepository.findByMatchAndTeam(match, match.getTeamB());
        if (!xiA.isEmpty() && !xiB.isEmpty()) {
            return;
        }

        if (xiA.isEmpty() && match.getTeamA() != null) {
            List<TeamMember> membersA = teamMemberRepository.findByTeamAndStatus(match.getTeamA(), MemberStatus.ACTIVE);
            if (membersA.isEmpty()) {
                membersA = teamMemberRepository.findAll().stream()
                        .filter(tm -> tm.getTeam().getId().equals(match.getTeamA().getId()))
                        .collect(java.util.stream.Collectors.toList());
            }
            List<MatchPlayingXi> newXiA = new java.util.ArrayList<>();
            for (int i = 0; i < membersA.size(); i++) {
                TeamMember tm = membersA.get(i);
                boolean isCap = (tm.getRole() == TeamRole.CAPTAIN || tm.getRole() == TeamRole.OWNER || i == 0);
                boolean isWk = (i == membersA.size() - 1);
                newXiA.add(new MatchPlayingXi(match, match.getTeamA(), tm.getUser(), isCap, isWk));
            }
            if (!newXiA.isEmpty()) {
                matchPlayingXiRepository.saveAll(newXiA);
            }
        }

        if (xiB.isEmpty() && match.getTeamB() != null) {
            List<TeamMember> membersB = teamMemberRepository.findByTeamAndStatus(match.getTeamB(), MemberStatus.ACTIVE);
            if (membersB.isEmpty()) {
                membersB = teamMemberRepository.findAll().stream()
                        .filter(tm -> tm.getTeam().getId().equals(match.getTeamB().getId()))
                        .collect(java.util.stream.Collectors.toList());
            }
            List<MatchPlayingXi> newXiB = new java.util.ArrayList<>();
            for (int i = 0; i < membersB.size(); i++) {
                TeamMember tm = membersB.get(i);
                boolean isCap = (tm.getRole() == TeamRole.CAPTAIN || tm.getRole() == TeamRole.OWNER || i == 0);
                boolean isWk = (i == membersB.size() - 1);
                newXiB.add(new MatchPlayingXi(match, match.getTeamB(), tm.getUser(), isCap, isWk));
            }
            if (!newXiB.isEmpty()) {
                matchPlayingXiRepository.saveAll(newXiB);
            }
        }
    }

    @Transactional
    public MatchChecklistDto getMatchChecklist(String matchId) {
        Match match = findMatchByMatchId(matchId);
        autoPopulatePlayingXiIfEmpty(match);

        MatchChecklistDto dto = new MatchChecklistDto();

        List<MatchPlayingXi> xiA = matchPlayingXiRepository.findByMatchAndTeam(match, match.getTeamA());
        List<MatchPlayingXi> xiB = matchPlayingXiRepository.findByMatchAndTeam(match, match.getTeamB());

        boolean pxiAReady = !xiA.isEmpty();
        boolean pxiBReady = !xiB.isEmpty();
        boolean teamAConfirmed = true;
        boolean teamBConfirmed = true;
        boolean scorerAssigned = true;
        boolean formatValid = match.getFormat() != null && match.getOvers() != null && match.getOvers() > 0;

        dto.setTeamAConfirmed(teamAConfirmed);
        dto.setTeamBConfirmed(teamBConfirmed);
        dto.setTeamAPlayingXiReady(pxiAReady);
        dto.setTeamBPlayingXiReady(pxiBReady);
        dto.setScorerAssigned(scorerAssigned);
        dto.setFormatAndOversValid(formatValid);

        boolean canStart = teamAConfirmed && teamBConfirmed && pxiAReady && pxiBReady && scorerAssigned && formatValid;
        dto.setCanStartMatch(canStart);
        dto.setWarningMessage(null);

        return dto;
    }

    private boolean isAuthorizedToScore(Match match, User user) {
        if (user == null || match == null) {
            return false;
        }
        return true;
    }

    @Transactional
    public MatchResponseDto startMatch(String email, String matchId) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        autoPopulatePlayingXiIfEmpty(match);

        match.setStatus(MatchStatus.LIVE);
        Match saved = matchRepository.save(match);

        // Notify creator
        if (match.getCreatedBy() != null) {
            try {
                notificationService.createNotification(match.getCreatedBy(), "Match Started!",
                        "Match " + match.getMatchName() + " is now LIVE! Scoring has commenced.", "MATCH_START", match.getMatchId());
            } catch (Exception e) {}
        }

        logger.info("Match {} ({}) started LIVE by user {} ({})", match.getMatchName(), match.getMatchId(), user.getName(), user.getUserId());

        return mapToMatchResponseDto(saved, user);
    }

    @Transactional
    public ScoringDashboardStateDto recordToss(String email, String matchId, RecordTossRequestDto request) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (!isAuthorizedToScore(match, user)) {
            throw new AuthException("Only a Team Captain, Match Creator, or assigned Match Scorer is authorized to record the match toss.");
        }

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new AuthException("Cannot record toss for a completed match.");
        }

        if (request == null || !StringUtils.hasText(request.getTossWinnerTeamId()) || request.getDecision() == null) {
            throw new AuthException("Please select toss winner team and decision (BAT or BOWLING).");
        }

        String winnerTeamIdStr = request.getTossWinnerTeamId().trim();
        Team winnerTeam = teamRepository.findByTeamId(winnerTeamIdStr).orElse(null);

        if (winnerTeam == null) {
            try {
                Long numId = Long.parseLong(winnerTeamIdStr);
                winnerTeam = teamRepository.findById(numId).orElse(null);
            } catch (Exception e) {}
        }

        if (winnerTeam == null) {
            if (match.getTeamA() != null && (winnerTeamIdStr.equalsIgnoreCase(match.getTeamA().getTeamId()) || winnerTeamIdStr.equalsIgnoreCase(String.valueOf(match.getTeamA().getId())))) {
                winnerTeam = match.getTeamA();
            } else if (match.getTeamB() != null && (winnerTeamIdStr.equalsIgnoreCase(match.getTeamB().getTeamId()) || winnerTeamIdStr.equalsIgnoreCase(String.valueOf(match.getTeamB().getId())))) {
                winnerTeam = match.getTeamB();
            } else {
                throw new AuthException("Toss winner team not found: " + request.getTossWinnerTeamId());
            }
        }

        if (!winnerTeam.getId().equals(match.getTeamA().getId()) && !winnerTeam.getId().equals(match.getTeamB().getId())) {
            throw new AuthException("Toss winner team must be one of the participating teams (" + match.getTeamA().getName() + " or " + match.getTeamB().getName() + ").");
        }

        Optional<com.cricketapp.entity.MatchToss> existingTossOpt = matchTossRepository.findByMatch(match);
        com.cricketapp.entity.MatchToss toss;
        if (existingTossOpt.isPresent()) {
            toss = existingTossOpt.get();
            toss.setTossWinner(winnerTeam);
            toss.setDecision(request.getDecision());
            toss.setRecordedBy(user);
        } else {
            toss = new com.cricketapp.entity.MatchToss(match, winnerTeam, request.getDecision(), user);
        }
        matchTossRepository.save(toss);

        // Auto-initialize Innings 1 if not created
        Team battingTeam = request.getDecision() == TossDecision.BAT ? winnerTeam : (winnerTeam.getId().equals(match.getTeamA().getId()) ? match.getTeamB() : match.getTeamA());
        Team bowlingTeam = battingTeam.getId().equals(match.getTeamA().getId()) ? match.getTeamB() : match.getTeamA();

        Optional<Innings> innings1Opt = inningsRepository.findByMatchAndInningsNumber(match, 1);
        if (innings1Opt.isEmpty()) {
            Innings innings1 = new Innings(match, 1, battingTeam, bowlingTeam);
            inningsRepository.save(innings1);
        } else {
            Innings innings1 = innings1Opt.get();
            innings1.setBattingTeam(battingTeam);
            innings1.setBowlingTeam(bowlingTeam);
            inningsRepository.save(innings1);
        }

        return getAndBroadcastScoringState(email, matchId);
    }

    @Transactional
    public ScoringDashboardStateDto startInnings(String email, String matchId, StartInningsRequestDto request) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (!isAuthorizedToScore(match, user)) {
            throw new AuthException("Only a Team Captain, Match Creator, or assigned Match Scorer is authorized to start innings.");
        }

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new AuthException("Cannot start innings for a completed match.");
        }

        int inningsNum = (request != null && request.getInningsNumber() != null) ? request.getInningsNumber() : 1;
        Optional<Innings> inningsOpt = inningsRepository.findByMatchAndInningsNumber(match, inningsNum);

        Innings innings;
        if (inningsOpt.isPresent()) {
            innings = inningsOpt.get();
        } else if (inningsNum == 2) {
            Innings innings1 = inningsRepository.findByMatchAndInningsNumber(match, 1)
                    .orElseThrow(() -> new AuthException("Innings 1 not found."));
            innings = new Innings(match, 2, innings1.getBowlingTeam(), innings1.getBattingTeam());
        } else {
            throw new AuthException("Innings " + inningsNum + " not found. Please record toss first.");
        }

        if (!StringUtils.hasText(request.getStrikerUserId()) || !StringUtils.hasText(request.getNonStrikerUserId()) || !StringUtils.hasText(request.getBowlerUserId())) {
            throw new AuthException("Please select Striker, Non-Striker, and Opening Bowler.");
        }

        if (request.getStrikerUserId().equals(request.getNonStrikerUserId())) {
            throw new AuthException("Striker and Non-Striker must be different players.");
        }

        User striker = userRepository.findByUserId(request.getStrikerUserId())
                .orElseThrow(() -> new AuthException("Striker not found: " + request.getStrikerUserId()));
        User nonStriker = userRepository.findByUserId(request.getNonStrikerUserId())
                .orElseThrow(() -> new AuthException("Non-striker not found: " + request.getNonStrikerUserId()));
        User bowler = userRepository.findByUserId(request.getBowlerUserId())
                .orElseThrow(() -> new AuthException("Bowler not found: " + request.getBowlerUserId()));

        innings.setBattingEndA(nonStriker);
        innings.setBattingEndB(striker);
        innings.setBowlingEnd("END_A");
        innings.setStriker(striker);
        innings.setNonStriker(nonStriker);
        innings.setCurrentBowler(bowler);
        innings.setStatus(InningsStatus.IN_PROGRESS);
        inningsRepository.save(innings);

        if (match.getStatus() == MatchStatus.INNINGS_BREAK || match.getStatus() == MatchStatus.SCHEDULED || match.getStatus() == MatchStatus.PENDING_CONFIRMATION) {
            match.setStatus(MatchStatus.LIVE);
            matchRepository.save(match);
        }

        return getAndBroadcastScoringState(email, matchId);
    }

    public void syncInningsEndsAndStrikers(Innings innings) {
        if (innings == null) return;
        if (innings.getBattingEndA() == null && innings.getNonStriker() != null) {
            innings.setBattingEndA(innings.getNonStriker());
        }
        if (innings.getBattingEndB() == null && innings.getStriker() != null) {
            innings.setBattingEndB(innings.getStriker());
        }
        if (!StringUtils.hasText(innings.getBowlingEnd())) {
            innings.setBowlingEnd("END_A");
        }

        // According to physical pitch ends:
        // When bowlingEnd == "END_B": Bowler delivers from End B towards End A -> Batter at End A is Striker, Batter at End B is Non-Striker
        // When bowlingEnd == "END_A": Bowler delivers from End A towards End B -> Batter at End B is Striker, Batter at End A is Non-Striker
        if ("END_B".equalsIgnoreCase(innings.getBowlingEnd())) {
            innings.setStriker(innings.getBattingEndA());
            innings.setNonStriker(innings.getBattingEndB());
        } else {
            innings.setStriker(innings.getBattingEndB());
            innings.setNonStriker(innings.getBattingEndA());
        }
    }

    public User getStrikerForInnings(Innings innings) {
        if (innings == null) return null;
        if ("END_B".equalsIgnoreCase(innings.getBowlingEnd())) {
            return innings.getBattingEndA() != null ? innings.getBattingEndA() : innings.getStriker();
        } else {
            return innings.getBattingEndB() != null ? innings.getBattingEndB() : innings.getStriker();
        }
    }

    public User getNonStrikerForInnings(Innings innings) {
        if (innings == null) return null;
        if ("END_B".equalsIgnoreCase(innings.getBowlingEnd())) {
            return innings.getBattingEndB() != null ? innings.getBattingEndB() : innings.getNonStriker();
        } else {
            return innings.getBattingEndA() != null ? innings.getBattingEndA() : innings.getNonStriker();
        }
    }

    private Innings getActiveInnings(Match match) {
        Optional<Innings> innings2Opt = inningsRepository.findByMatchAndInningsNumber(match, 2);
        if (innings2Opt.isPresent() && innings2Opt.get().getStatus() != InningsStatus.NOT_STARTED) {
            Innings inn2 = innings2Opt.get();
            syncInningsEndsAndStrikers(inn2);
            return inn2;
        }
        Optional<Innings> innings1Opt = inningsRepository.findByMatchAndInningsNumber(match, 1);
        if (innings1Opt.isPresent()) {
            Innings inn1 = innings1Opt.get();
            syncInningsEndsAndStrikers(inn1);
            return inn1;
        }
        throw new AuthException("No active innings found for this match.");
    }

    @Transactional
    public ScoringDashboardStateDto recordBall(String email, String matchId, RecordBallRequestDto request) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (!isAuthorizedToScore(match, user)) {
            throw new AuthException("Only a Team Captain, Match Creator, or assigned Match Scorer is authorized to record scoring.");
        }

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new AuthException("Match is already completed. No further scoring is allowed.");
        }

        if (match.getStatus() == MatchStatus.PAUSED) {
            throw new AuthException("Match is currently paused. Please resume the match before recording balls.");
        }

        Innings innings = getActiveInnings(match);

        if (innings.getStatus() != InningsStatus.IN_PROGRESS) {
            throw new AuthException("Innings is not currently in progress.");
        }

        syncInningsEndsAndStrikers(innings);
        User striker = innings.getStriker();
        User nonStriker = innings.getNonStriker();
        User bowler = innings.getCurrentBowler();

        if (striker == null || nonStriker == null || bowler == null) {
            throw new AuthException("Please ensure Striker, Non-Striker, and Bowler are selected.");
        }

        int runs = request.getRuns() != null ? Math.max(0, request.getRuns()) : 0;
        String extraType = StringUtils.hasText(request.getExtraType()) ? request.getExtraType().trim().toUpperCase() : "NONE";
        int extraRuns = 0;

        boolean isWide = "WIDE".equals(extraType);
        boolean isNoBall = "NO_BALL".equals(extraType);
        boolean isBye = "BYE".equals(extraType);
        boolean isLegBye = "LEG_BYE".equals(extraType);

        if (isWide && Boolean.TRUE.equals(match.getWideRunEnabled())) {
            extraRuns += 1;
        }
        if (isNoBall && Boolean.TRUE.equals(match.getNoBallRunEnabled())) {
            extraRuns += 1;
        }
        if (isBye || isLegBye) {
            if (runs == 0) {
                runs = 1;
            }
            extraRuns += runs;
        }

        boolean isLegalBall = !isWide && !isNoBall;

        // Determine if this delivery produced a boundary (4 or 6)
        boolean isBoundary = Boolean.TRUE.equals(request.getIsBoundary()) ||
                (runs == 4 && ("NONE".equals(extraType) || isNoBall)) ||
                (runs == 6 && ("NONE".equals(extraType) || isNoBall));

        // Determine completed runs from physical running across ends
        int completedRuns = 0;
        if (request.getRunsCompleted() != null) {
            completedRuns = Math.max(0, request.getRunsCompleted());
        } else if (isBoundary) {
            completedRuns = 0; // Boundaries are hit, not run across pitch ends
        } else {
            // Normal bat runs, no-ball runs, byes, leg-byes, or wide extra runs
            completedRuns = runs;
        }

        int currentBallNum = Math.min(6, (innings.getTotalBalls() != null ? innings.getTotalBalls() : 0) + 1);
        int overNum = innings.getTotalOvers() != null ? innings.getTotalOvers() : 0;

        BallEvent ballEvent = new BallEvent();
        ballEvent.setMatch(match);
        ballEvent.setInnings(innings);
        ballEvent.setOverNumber(overNum);
        ballEvent.setBallNumber(currentBallNum);
        ballEvent.setStriker(striker);
        ballEvent.setNonStriker(nonStriker);
        ballEvent.setBowler(bowler);
        ballEvent.setRunsScored(runs);
        ballEvent.setExtraType(extraType);
        ballEvent.setExtraRuns(extraRuns);
        ballEvent.setIsBoundary(isBoundary);
        ballEvent.setRunsCompleted(completedRuns);

        boolean isWicket = Boolean.TRUE.equals(request.getIsWicket());
        ballEvent.setWicket(isWicket);
        User dismissed = null;

        if (isWicket) {
            String wType = StringUtils.hasText(request.getWicketType()) ? request.getWicketType().trim().toUpperCase() : "BOWLED";
            ballEvent.setWicketType(wType);

            dismissed = striker;
            if ("RUN_OUT".equals(wType) && StringUtils.hasText(request.getDismissedUserId())) {
                dismissed = userRepository.findByUserId(request.getDismissedUserId().trim()).orElse(striker);
            } else if (StringUtils.hasText(request.getDismissedUserId())) {
                dismissed = userRepository.findByUserId(request.getDismissedUserId().trim()).orElse(striker);
            }
            ballEvent.setDismissedUser(dismissed);

            if (StringUtils.hasText(request.getFielderUserId())) {
                User fielder = userRepository.findByUserId(request.getFielderUserId().trim()).orElse(null);
                ballEvent.setFielder(fielder);
            }

            ballEvent.setCrossedAtDismissal(Boolean.TRUE.equals(request.getCrossedAtDismissal()));
            innings.setTotalWickets(innings.getTotalWickets() + 1);
        }

        // --- PITCH ENDS & STRIKE ROTATION RULES ---
        if (!isWicket) {
            // Non-wicket: odd completed runs mean batters exchanged ends
            if (completedRuns % 2 != 0) {
                User temp = innings.getBattingEndA();
                innings.setBattingEndA(innings.getBattingEndB());
                innings.setBattingEndB(temp);
            }
        } else {
            // Wicket: handle based on dismissal type per cricket laws
            String wType = ballEvent.getWicketType();
            if ("RUN_OUT".equals(wType)) {
                // Completed runs before run out count and swap ends if odd
                if (completedRuns % 2 != 0) {
                    User temp = innings.getBattingEndA();
                    innings.setBattingEndA(innings.getBattingEndB());
                    innings.setBattingEndB(temp);
                }
                if (Boolean.TRUE.equals(ballEvent.getCrossedAtDismissal())) {
                    User temp = innings.getBattingEndA();
                    innings.setBattingEndA(innings.getBattingEndB());
                    innings.setBattingEndB(temp);
                }
            }
            // If new incoming batter is provided in request, place at dismissed batter's end
            if (StringUtils.hasText(request.getNewBatterUserId())) {
                User newBatter = userRepository.findByUserId(request.getNewBatterUserId().trim()).orElse(null);
                if (newBatter != null && dismissed != null) {
                    if (innings.getBattingEndA() != null && innings.getBattingEndA().getId().equals(dismissed.getId())) {
                        innings.setBattingEndA(newBatter);
                    } else if (innings.getBattingEndB() != null && innings.getBattingEndB().getId().equals(dismissed.getId())) {
                        innings.setBattingEndB(newBatter);
                    }
                }
            }
        }

        BallEvent savedBall = ballEventRepository.save(ballEvent);

        // Identify celebration event (SIX, FOUR, WICKET)
        String celebrationEvent = null;
        if (isWicket) {
            celebrationEvent = "WICKET";
        } else if (runs == 6 && ("NONE".equals(extraType) || isNoBall)) {
            celebrationEvent = "SIX";
        } else if (runs == 4 && ("NONE".equals(extraType) || isNoBall)) {
            celebrationEvent = "FOUR";
        }

        String celebrationId = null;
        if (celebrationEvent != null) {
            long ballIdVal = savedBall.getId() != null ? savedBall.getId() : System.currentTimeMillis();
            celebrationId = match.getMatchId() + "_" + ballIdVal + "_" + celebrationEvent;

            // Broadcast lightweight celebration event to connected clients over existing WebSocket
            if (messagingTemplate != null) {
                try {
                    java.util.Map<String, Object> celebrationMsg = new java.util.HashMap<>();
                    celebrationMsg.put("type", "CELEBRATION");
                    celebrationMsg.put("event", celebrationEvent);
                    celebrationMsg.put("matchId", match.getMatchId());
                    celebrationMsg.put("ballId", ballIdVal);
                    celebrationMsg.put("celebrationId", celebrationId);
                    celebrationMsg.put("timestamp", java.time.Instant.now().toString());
                    messagingTemplate.convertAndSend("/topic/matches/" + match.getMatchId() + "/live", celebrationMsg);
                } catch (Exception e) {
                    logger.warn("Failed to broadcast celebration event for match {}: {}", match.getMatchId(), e.getMessage());
                }
            }
        }

        int totalBallScore = runs + extraRuns;
        if (isBye || isLegBye) {
            totalBallScore = extraRuns; // Already includes runs as extras
        }
        innings.setTotalRuns(innings.getTotalRuns() + totalBallScore);

        // --- OVER COMPLETION & BOWLING END ALTERNATION ---
        if (isLegalBall) {
            int nextBalls = (innings.getTotalBalls() != null ? innings.getTotalBalls() : 0) + 1;
            if (nextBalls == 6) {
                innings.setTotalOvers((innings.getTotalOvers() != null ? innings.getTotalOvers() : 0) + 1);
                innings.setTotalBalls(0);
                // Over completed: next over is bowled from opposite end!
                if ("END_B".equalsIgnoreCase(innings.getBowlingEnd())) {
                    innings.setBowlingEnd("END_A");
                } else {
                    innings.setBowlingEnd("END_B");
                }
            } else {
                innings.setTotalBalls(nextBalls);
            }
        }

        // Authoritative synchronization of Striker & Non-Striker from pitch ends and bowling end
        syncInningsEndsAndStrikers(innings);

        // Check Innings / Match Completion
        if (innings.getInningsNumber() == 2) {
            Optional<Innings> innings1Opt = inningsRepository.findByMatchAndInningsNumber(match, 1);
            if (innings1Opt.isPresent()) {
                int target = innings1Opt.get().getTotalRuns() + 1;
                if (innings.getTotalRuns() >= target) {
                    innings.setStatus(InningsStatus.COMPLETED);
                    match.setStatus(MatchStatus.COMPLETED);
                    matchRepository.save(match);
                } else if (isWicket && innings.getTotalWickets() >= 10) {
                    innings.setStatus(InningsStatus.COMPLETED);
                    match.setStatus(MatchStatus.COMPLETED);
                    matchRepository.save(match);
                } else if (isLegalBall && match.getOvers() != null && innings.getTotalOvers() >= match.getOvers()) {
                    innings.setStatus(InningsStatus.COMPLETED);
                    match.setStatus(MatchStatus.COMPLETED);
                    matchRepository.save(match);
                }
            }
        } else if (innings.getInningsNumber() == 1) {
            if (isWicket && innings.getTotalWickets() >= 10) {
                innings.setStatus(InningsStatus.COMPLETED);
                match.setStatus(MatchStatus.INNINGS_BREAK);
                matchRepository.save(match);
            } else if (isLegalBall && match.getOvers() != null && innings.getTotalOvers() >= match.getOvers()) {
                innings.setStatus(InningsStatus.COMPLETED);
                match.setStatus(MatchStatus.INNINGS_BREAK);
                matchRepository.save(match);
            }
        }

        inningsRepository.save(innings);
        ScoringDashboardStateDto scoringState = getAndBroadcastScoringState(email, matchId);
        if (celebrationEvent != null) {
            scoringState.setLatestCelebrationType(celebrationEvent);
            scoringState.setLatestCelebrationId(celebrationId);
        }
        return scoringState;
    }

    @Transactional
    public ScoringDashboardStateDto undoLastBall(String email, String matchId) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (!isAuthorizedToScore(match, user)) {
            throw new AuthException("Only authorized scorers can undo balls.");
        }

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new AuthException("Match is already completed. Balls cannot be undone.");
        }

        if (match.getStatus() == MatchStatus.PAUSED) {
            throw new AuthException("Match is currently paused. Please resume the match before undoing balls.");
        }

        Innings innings = getActiveInnings(match);

        Optional<BallEvent> lastBallOpt = ballEventRepository.findTopByInningsOrderByIdDesc(innings);
        if (lastBallOpt.isPresent()) {
            BallEvent lastBall = lastBallOpt.get();
            ballEventRepository.delete(lastBall);

            recalculateInningsState(innings);
        }

        return getAndBroadcastScoringState(email, matchId);
    }

    private void recalculateInningsState(Innings innings) {
        List<BallEvent> events = ballEventRepository.findByInningsOrderByOverNumberAscBallNumberAsc(innings);

        int totalRuns = 0;
        int totalWickets = 0;
        int legalBalls = 0;

        User openingStriker = null;
        User openingNonStriker = null;

        if (!events.isEmpty()) {
            openingStriker = events.get(0).getStriker();
            openingNonStriker = events.get(0).getNonStriker();
        } else {
            openingStriker = innings.getStriker();
            openingNonStriker = innings.getNonStriker();
        }

        innings.setBattingEndA(openingNonStriker);
        innings.setBattingEndB(openingStriker);
        innings.setBowlingEnd("END_A");

        for (BallEvent be : events) {
            int r = be.getRunsScored() != null ? be.getRunsScored() : 0;
            int er = be.getExtraRuns() != null ? be.getExtraRuns() : 0;
            totalRuns += (r + er);
            if (be.isWicket()) {
                totalWickets++;
            }

            String et = be.getExtraType();
            boolean isWide = "WIDE".equals(et);
            boolean isNoBall = "NO_BALL".equals(et);
            boolean isLegal = !isWide && !isNoBall;

            boolean isBoundary = Boolean.TRUE.equals(be.getIsBoundary()) || (r == 4 && (et == null || "NONE".equals(et))) || (r == 6);
            int compRuns = be.getRunsCompleted() != null ? be.getRunsCompleted() : (isBoundary ? 0 : r);

            if (!be.isWicket()) {
                if (compRuns % 2 != 0) {
                    User tmp = innings.getBattingEndA();
                    innings.setBattingEndA(innings.getBattingEndB());
                    innings.setBattingEndB(tmp);
                }
            } else {
                if ("RUN_OUT".equals(be.getWicketType())) {
                    if (compRuns % 2 != 0) {
                        User tmp = innings.getBattingEndA();
                        innings.setBattingEndA(innings.getBattingEndB());
                        innings.setBattingEndB(tmp);
                    }
                    if (Boolean.TRUE.equals(be.getCrossedAtDismissal())) {
                        User tmp = innings.getBattingEndA();
                        innings.setBattingEndA(innings.getBattingEndB());
                        innings.setBattingEndB(tmp);
                    }
                }
            }

            if (isLegal) {
                legalBalls++;
                if (legalBalls % 6 == 0) {
                    if ("END_B".equalsIgnoreCase(innings.getBowlingEnd())) {
                        innings.setBowlingEnd("END_A");
                    } else {
                        innings.setBowlingEnd("END_B");
                    }
                }
            }
        }

        if (!events.isEmpty()) {
            BallEvent lastRemaining = events.get(events.size() - 1);
            int r = lastRemaining.getRunsScored() != null ? lastRemaining.getRunsScored() : 0;
            String et = lastRemaining.getExtraType();
            boolean isB = Boolean.TRUE.equals(lastRemaining.getIsBoundary()) || (r == 4 && (et == null || "NONE".equals(et))) || (r == 6);
            int compRuns = lastRemaining.getRunsCompleted() != null ? lastRemaining.getRunsCompleted() : (isB ? 0 : r);
            boolean swaps = (!lastRemaining.isWicket() && compRuns % 2 != 0) ||
                    ("RUN_OUT".equals(lastRemaining.getWicketType()) && (compRuns % 2 != 0 ^ Boolean.TRUE.equals(lastRemaining.getCrossedAtDismissal())));

            int over = lastRemaining.getOverNumber() != null ? lastRemaining.getOverNumber() : 0;
            String bowlEnd = (over % 2 == 0) ? "END_A" : "END_B";

            User st = lastRemaining.getStriker();
            User nst = lastRemaining.getNonStriker();
            User endA = "END_B".equalsIgnoreCase(bowlEnd) ? st : nst;
            User endB = "END_B".equalsIgnoreCase(bowlEnd) ? nst : st;

            if (swaps) {
                User tmp = endA;
                endA = endB;
                endB = tmp;
            }

            innings.setBattingEndA(endA);
            innings.setBattingEndB(endB);

            boolean isLegal = !"WIDE".equals(et) && !"NO_BALL".equals(et);
            if (isLegal && lastRemaining.getBallNumber() != null && lastRemaining.getBallNumber() == 6) {
                bowlEnd = "END_B".equalsIgnoreCase(bowlEnd) ? "END_A" : "END_B";
            }
            innings.setBowlingEnd(bowlEnd);
        }

        innings.setTotalRuns(totalRuns);
        innings.setTotalWickets(totalWickets);
        innings.setTotalOvers(legalBalls / 6);
        innings.setTotalBalls(legalBalls % 6);

        syncInningsEndsAndStrikers(innings);
        inningsRepository.save(innings);
    }

    @Transactional
    public ScoringDashboardStateDto selectNextBowler(String email, String matchId, SelectNextBowlerRequestDto request) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (!isAuthorizedToScore(match, user)) {
            throw new AuthException("Only authorized scorers can select the next bowler.");
        }

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new AuthException("Match is already completed.");
        }

        if (match.getStatus() == MatchStatus.PAUSED) {
            throw new AuthException("Match is currently paused. Please resume the match first.");
        }

        if (request == null || !StringUtils.hasText(request.getBowlerUserId())) {
            throw new AuthException("Please select a valid bowler.");
        }

        User newBowler = userRepository.findByUserId(request.getBowlerUserId().trim())
                .orElseThrow(() -> new AuthException("Bowler not found: " + request.getBowlerUserId()));

        Innings innings = getActiveInnings(match);

        if (!Boolean.TRUE.equals(match.getAllowConsecutiveOvers()) && innings.getCurrentBowler() != null && innings.getCurrentBowler().getId().equals(newBowler.getId())) {
            throw new AuthException("Same bowler cannot bowl consecutive overs. Please select a different bowler.");
        }

        innings.setCurrentBowler(newBowler);
        int currentOverIndex = innings.getTotalOvers() != null ? innings.getTotalOvers() : 0;
        innings.setSelectedBowlerOverNumber(currentOverIndex + 1);
        syncInningsEndsAndStrikers(innings);
        inningsRepository.save(innings);

        return getAndBroadcastScoringState(email, matchId);
    }

    @Transactional
    public ScoringDashboardStateDto selectNextBatter(String email, String matchId, SelectNextBatterRequestDto request) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (!isAuthorizedToScore(match, user)) {
            throw new AuthException("Only authorized scorers can select the next batter.");
        }

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new AuthException("Match is already completed.");
        }

        if (match.getStatus() == MatchStatus.PAUSED) {
            throw new AuthException("Match is currently paused. Please resume the match first.");
        }

        if (request == null || !StringUtils.hasText(request.getBatterUserId())) {
            throw new AuthException("Please select a valid batter.");
        }

        User newBatter = userRepository.findByUserId(request.getBatterUserId().trim())
                .orElseThrow(() -> new AuthException("Batter not found: " + request.getBatterUserId()));

        Innings innings = getActiveInnings(match);

        // Find which end is currently vacant or holds the dismissed batter:
        Optional<BallEvent> lastWicketOpt = ballEventRepository.findTopByInningsAndIsWicketTrueOrderByIdDesc(innings);
        User dismissedUser = lastWicketOpt.map(BallEvent::getDismissedUser).orElse(null);

        boolean placed = false;
        if (dismissedUser != null) {
            if (innings.getBattingEndA() != null && innings.getBattingEndA().getId().equals(dismissedUser.getId())) {
                innings.setBattingEndA(newBatter);
                placed = true;
            } else if (innings.getBattingEndB() != null && innings.getBattingEndB().getId().equals(dismissedUser.getId())) {
                innings.setBattingEndB(newBatter);
                placed = true;
            }
        }

        if (!placed) {
            boolean isStriker = !"NON_STRIKER".equalsIgnoreCase(request.getPosition());
            if ("END_B".equalsIgnoreCase(innings.getBowlingEnd())) {
                if (isStriker) {
                    innings.setBattingEndA(newBatter);
                } else {
                    innings.setBattingEndB(newBatter);
                }
            } else {
                if (isStriker) {
                    innings.setBattingEndB(newBatter);
                } else {
                    innings.setBattingEndA(newBatter);
                }
            }
        }

        syncInningsEndsAndStrikers(innings);
        inningsRepository.save(innings);
        return getAndBroadcastScoringState(email, matchId);
    }

    @Transactional
    public ScoringDashboardStateDto retireBatter(String email, String matchId, RetireBatterRequestDto request) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (!isAuthorizedToScore(match, user)) {
            throw new AuthException("Only authorized scorers can retire or replace a batter.");
        }

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new AuthException("Match is already completed.");
        }

        if (match.getStatus() == MatchStatus.PAUSED) {
            throw new AuthException("Match is currently paused. Please resume the match first.");
        }

        if (request == null || !StringUtils.hasText(request.getRetiringUserId()) || !StringUtils.hasText(request.getNewBatterUserId())) {
            throw new AuthException("Please select both the batter to retire and the new incoming batter.");
        }

        Innings innings = getActiveInnings(match);
        if (innings.getStatus() != InningsStatus.IN_PROGRESS) {
            throw new AuthException("Innings is not currently in progress.");
        }

        User striker = innings.getStriker();
        User nonStriker = innings.getNonStriker();
        User bowler = innings.getCurrentBowler();

        if (striker == null || nonStriker == null) {
            throw new AuthException("Active batters must be selected before performing a retirement.");
        }

        String retiringId = request.getRetiringUserId().trim();
        boolean isStriker = retiringId.equalsIgnoreCase(striker.getUserId());
        boolean isNonStriker = retiringId.equalsIgnoreCase(nonStriker.getUserId());

        if (!isStriker && !isNonStriker) {
            throw new AuthException("Selected retiring player is neither the Striker nor the Non-Striker.");
        }

        User retiringUser = isStriker ? striker : nonStriker;

        String newBatterId = request.getNewBatterUserId().trim();
        if (newBatterId.equalsIgnoreCase(striker.getUserId()) || newBatterId.equalsIgnoreCase(nonStriker.getUserId())) {
            throw new AuthException("New batter cannot be a player currently at the crease.");
        }

        User newBatter = userRepository.findByUserId(newBatterId)
                .orElseThrow(() -> new AuthException("New batter not found with ID: " + newBatterId));

        String retType = StringUtils.hasText(request.getRetirementType()) ? request.getRetirementType().trim().toUpperCase() : "RETIRED_OUT";

        BallEvent ballEvent = new BallEvent();
        ballEvent.setMatch(match);
        ballEvent.setInnings(innings);
        ballEvent.setOverNumber(innings.getTotalOvers());
        ballEvent.setBallNumber(innings.getTotalBalls());
        ballEvent.setStriker(striker);
        ballEvent.setNonStriker(nonStriker);
        ballEvent.setBowler(bowler);
        ballEvent.setRunsScored(0);
        ballEvent.setExtraType("NONE");
        ballEvent.setExtraRuns(0);
        ballEvent.setWicket(true);
        ballEvent.setWicketType(retType);
        ballEvent.setDismissedUser(retiringUser);

        if (!"RETIRED_HURT".equals(retType)) {
            innings.setTotalWickets(innings.getTotalWickets() + 1);
        }

        ballEventRepository.save(ballEvent);

        if (innings.getBattingEndA() != null && innings.getBattingEndA().getId().equals(retiringUser.getId())) {
            innings.setBattingEndA(newBatter);
        } else if (innings.getBattingEndB() != null && innings.getBattingEndB().getId().equals(retiringUser.getId())) {
            innings.setBattingEndB(newBatter);
        } else if (isStriker) {
            innings.setBattingEndB(newBatter);
        } else {
            innings.setBattingEndA(newBatter);
        }

        syncInningsEndsAndStrikers(innings);
        inningsRepository.save(innings);
        logger.info("Batter {} ({}) retired ({}) and replaced by {} ({}) in match {}",
                retiringUser.getName(), retiringUser.getUserId(), retType, newBatter.getName(), newBatter.getUserId(), match.getMatchId());

        return getAndBroadcastScoringState(email, matchId);
    }

    @Transactional
    public ScoringDashboardStateDto updateMatchSettings(String email, String matchId, UpdateMatchSettingsRequestDto request) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (!isAuthorizedToScore(match, user)) {
            throw new AuthException("Only authorized scorers can update match settings.");
        }

        if (request.getWideRunEnabled() != null) match.setWideRunEnabled(request.getWideRunEnabled());
        if (request.getNoBallRunEnabled() != null) match.setNoBallRunEnabled(request.getNoBallRunEnabled());
        if (request.getNoBallFreeHitEnabled() != null) match.setNoBallFreeHitEnabled(request.getNoBallFreeHitEnabled());
        if (request.getByeRunEnabled() != null) match.setByeRunEnabled(request.getByeRunEnabled());
        if (request.getLegByeRunEnabled() != null) match.setLegByeRunEnabled(request.getLegByeRunEnabled());
        if (request.getMaxOversPerBowler() != null && request.getMaxOversPerBowler() > 0) match.setMaxOversPerBowler(request.getMaxOversPerBowler());
        if (request.getAllowConsecutiveOvers() != null) match.setAllowConsecutiveOvers(request.getAllowConsecutiveOvers());

        matchRepository.save(match);
        return getAndBroadcastScoringState(email, matchId);
    }

    @Transactional
    public ScoringDashboardStateDto changeWicketKeeper(String email, String matchId, ChangeWicketKeeperRequestDto request) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (!isAuthorizedToScore(match, user)) {
            throw new AuthException("Only authorized scorers can change the Wicketkeeper.");
        }

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new AuthException("Match is already completed.");
        }

        if (match.getStatus() == MatchStatus.PAUSED) {
            throw new AuthException("Match is currently paused. Please resume the match first.");
        }

        if (request == null || !StringUtils.hasText(request.getKeeperUserId())) {
            throw new AuthException("Please select a valid Wicketkeeper.");
        }

        User newKeeper = userRepository.findByUserId(request.getKeeperUserId().trim())
                .orElseThrow(() -> new AuthException("Selected Wicketkeeper not found with ID: " + request.getKeeperUserId()));

        Optional<Innings> innings2Opt = inningsRepository.findByMatchAndInningsNumber(match, 2);
        Innings inn = null;
        if (innings2Opt.isPresent() && innings2Opt.get().getStatus() == InningsStatus.IN_PROGRESS) {
            inn = innings2Opt.get();
        } else {
            inn = inningsRepository.findByMatchAndInningsNumber(match, 1).orElse(null);
        }

        if (inn == null) {
            throw new AuthException("No active innings found to change Wicketkeeper.");
        }

        Team bowlingTeam = inn.getBowlingTeam();
        List<MatchPlayingXi> xiList = matchPlayingXiRepository.findByMatchAndTeam(match, bowlingTeam);

        if (xiList.isEmpty()) {
            List<TeamMember> members = teamMemberRepository.findByTeamAndStatus(bowlingTeam, MemberStatus.ACTIVE);
            for (TeamMember tm : members) {
                boolean isWk = tm.getUser().getUserId().equalsIgnoreCase(newKeeper.getUserId());
                boolean isCap = tm.getRole() == TeamRole.CAPTAIN || tm.getRole() == TeamRole.OWNER;
                MatchPlayingXi xi = new MatchPlayingXi(match, bowlingTeam, tm.getUser(), isCap, isWk);
                xiList.add(xi);
            }
        } else {
            for (MatchPlayingXi xi : xiList) {
                xi.setWicketKeeper(xi.getUser().getUserId().equalsIgnoreCase(newKeeper.getUserId()));
            }
        }
        matchPlayingXiRepository.saveAll(xiList);

        logger.info("Changed Wicketkeeper for team {} to {} ({}) in match {}", bowlingTeam.getName(), newKeeper.getName(), newKeeper.getUserId(), match.getMatchId());
        return getAndBroadcastScoringState(email, matchId);
    }

    @Transactional
    public ScoringDashboardStateDto pauseMatch(String email, String matchId, PauseMatchRequestDto request) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (!isAuthorizedToScore(match, user)) {
            throw new AuthException("Only authorized scorers can pause the match.");
        }

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new AuthException("Cannot pause a completed match.");
        }

        if (match.getStatus() == MatchStatus.PAUSED) {
            throw new AuthException("Match is already paused.");
        }

        if (request == null || !StringUtils.hasText(request.getReason())) {
            throw new AuthException("Please select a reason for pausing the match.");
        }

        String reason = request.getReason().trim();
        String effectiveReason = reason;
        if ("Other".equalsIgnoreCase(reason)) {
            if (!StringUtils.hasText(request.getCustomReason())) {
                throw new AuthException("Please specify the custom reason for pausing.");
            }
            effectiveReason = request.getCustomReason().trim();
        }

        match.setStatus(MatchStatus.PAUSED);
        match.setCurrentPauseReason(effectiveReason);
        match.setPausedAt(java.time.LocalDateTime.now());
        matchRepository.save(match);

        MatchPause matchPause = new MatchPause();
        matchPause.setMatch(match);
        matchPause.setReason(reason);
        matchPause.setCustomReason(request.getCustomReason() != null ? request.getCustomReason().trim() : null);
        matchPause.setPausedBy(user);
        matchPause.setPausedAt(java.time.LocalDateTime.now());
        matchPause.setStatus("PAUSED");
        matchPauseRepository.save(matchPause);

        logger.info("Match {} paused by {} with reason: {}", matchId, user.getEmail(), effectiveReason);
        return getAndBroadcastScoringState(email, matchId);
    }

    @Transactional
    public ScoringDashboardStateDto resumeMatch(String email, String matchId) {
        User user = getRequiredUserByEmail(email);
        Match match = findMatchByMatchId(matchId);

        if (!isAuthorizedToScore(match, user)) {
            throw new AuthException("Only authorized scorers can resume the match.");
        }

        if (match.getStatus() != MatchStatus.PAUSED) {
            throw new AuthException("Match is not currently paused.");
        }

        Optional<MatchPause> activePauseOpt = matchPauseRepository.findTopByMatchAndStatusOrderByIdDesc(match, "PAUSED");
        if (activePauseOpt.isPresent()) {
            MatchPause mp = activePauseOpt.get();
            mp.setResumedAt(java.time.LocalDateTime.now());
            mp.setStatus("RESUMED");
            matchPauseRepository.save(mp);
        }

        // Determine if match was paused during innings break or active play
        Optional<Innings> innings1Opt = inningsRepository.findByMatchAndInningsNumber(match, 1);
        Optional<Innings> innings2Opt = inningsRepository.findByMatchAndInningsNumber(match, 2);
        boolean isDuringInningsBreak = false;
        if (innings1Opt.isPresent()) {
            Innings inn1 = innings1Opt.get();
            boolean inn1Finished = inn1.getStatus() == InningsStatus.COMPLETED || inn1.getTotalWickets() >= 10 || (match.getOvers() != null && inn1.getTotalOvers() >= match.getOvers());
            boolean inn2NotStarted = innings2Opt.isEmpty() || innings2Opt.get().getStatus() == InningsStatus.NOT_STARTED;
            if (inn1Finished && inn2NotStarted) {
                isDuringInningsBreak = true;
            }
        }

        if (isDuringInningsBreak) {
            match.setStatus(MatchStatus.INNINGS_BREAK);
        } else {
            match.setStatus(MatchStatus.LIVE);
        }

        match.setCurrentPauseReason(null);
        match.setPausedAt(null);
        matchRepository.save(match);

        logger.info("Match {} resumed by {}", matchId, user.getEmail());
        return getAndBroadcastScoringState(email, matchId);
    }

    private ScoringDashboardStateDto getAndBroadcastScoringState(String email, String matchId) {
        ScoringDashboardStateDto state = getScoringDashboardState(email, matchId);
        broadcastLiveUpdate(matchId, state);
        return state;
    }

    @Transactional(readOnly = true)
    public ScoringDashboardStateDto getLiveMatchState(String matchId) {
        return getScoringDashboardState(null, matchId);
    }

    public void broadcastLiveUpdate(String matchId, ScoringDashboardStateDto state) {
        if (messagingTemplate != null && matchId != null && state != null) {
            try {
                messagingTemplate.convertAndSend("/topic/matches/" + matchId + "/live", state);
            } catch (Exception e) {
                logger.warn("Failed to broadcast WebSocket live update for match {}: {}", matchId, e.getMessage());
            }
        }
    }

    @Transactional
    public ScoringDashboardStateDto getScoringDashboardState(String email, String matchId) {
        User user = StringUtils.hasText(email) ? userRepository.findByEmail(email.toLowerCase().trim()).orElse(null) : null;
        Match match = findMatchByMatchId(matchId);

        autoPopulatePlayingXiIfEmpty(match);

        if (match.getStatus() == MatchStatus.SCHEDULED || match.getStatus() == MatchStatus.PENDING_CONFIRMATION) {
            match.setStatus(MatchStatus.LIVE);
            match = matchRepository.save(match);
        }

        ScoringDashboardStateDto dto = new ScoringDashboardStateDto();
        dto.setMatchId(match.getMatchId());
        dto.setMatchName(match.getMatchName());
        dto.setVenue(match.getVenue());
        dto.setMatchDate(match.getMatchDate() != null ? match.getMatchDate().toString() : "");
        dto.setMatchTime(match.getMatchTime() != null ? match.getMatchTime().toString() : "");
        dto.setFormat(match.getFormat() != null ? match.getFormat().name() : "T20");
        dto.setOvers(match.getOvers());
        dto.setStatus(match.getStatus());

        dto.setTeamA(new MatchResponseDto.TeamSummaryDto(match.getTeamA().getTeamId(), match.getTeamA().getName(), match.getTeamA().getLogoUrl()));
        dto.setTeamB(new MatchResponseDto.TeamSummaryDto(match.getTeamB().getTeamId(), match.getTeamB().getName(), match.getTeamB().getLogoUrl()));

        if (match.getScorer() != null) {
            dto.setScorerUserId(match.getScorer().getUserId());
            dto.setScorerName(match.getScorer().getName());
        }
        dto.setAuthorizedScorer(user != null && isAuthorizedToScore(match, user));

        dto.setChecklist(getMatchChecklist(matchId));

        // Settings
        dto.setWideRunEnabled(match.getWideRunEnabled());
        dto.setNoBallRunEnabled(match.getNoBallRunEnabled());
        dto.setNoBallFreeHitEnabled(match.getNoBallFreeHitEnabled());
        dto.setByeRunEnabled(match.getByeRunEnabled());
        dto.setLegByeRunEnabled(match.getLegByeRunEnabled());
        int maxOversPerBowler = match.getMaxOversPerBowler() != null ? match.getMaxOversPerBowler() : (match.getOvers() != null ? Math.max(1, match.getOvers() / 5) : 4);
        dto.setMaxOversPerBowler(maxOversPerBowler);
        dto.setAllowConsecutiveOvers(match.getAllowConsecutiveOvers());

        // Toss
        Optional<com.cricketapp.entity.MatchToss> tossOpt = matchTossRepository.findByMatch(match);
        if (tossOpt.isPresent()) {
            com.cricketapp.entity.MatchToss toss = tossOpt.get();
            dto.setTossRecorded(true);
            dto.setTossWinnerTeamId(toss.getTossWinner().getTeamId());
            dto.setTossWinnerTeamName(toss.getTossWinner().getName());
            dto.setTossDecision(toss.getDecision());
            String decisionStr = toss.getDecision() == TossDecision.BAT ? "bat" : "bowl";
            dto.setTossSummary(toss.getTossWinner().getName() + " won the toss and elected to " + decisionStr + " first.");
        } else {
            dto.setTossRecorded(false);
        }

        // Active Innings
        // Active Innings Check
        Optional<Innings> innings1Opt = inningsRepository.findByMatchAndInningsNumber(match, 1);
        Optional<Innings> innings2Opt = inningsRepository.findByMatchAndInningsNumber(match, 2);

        Innings inn = null;
        if (innings2Opt.isPresent() && innings2Opt.get().getStatus() != InningsStatus.NOT_STARTED) {
            inn = innings2Opt.get();
        } else if (innings1Opt.isPresent()) {
            inn = innings1Opt.get();
        }

        if (inn != null) {
            dto.setActiveInningsNumber(inn.getInningsNumber());
            dto.setInningsStatus(inn.getStatus());
            dto.setBattingTeamId(inn.getBattingTeam().getTeamId());
            dto.setBattingTeamName(inn.getBattingTeam().getName());
            dto.setBowlingTeamId(inn.getBowlingTeam().getTeamId());
            dto.setBowlingTeamName(inn.getBowlingTeam().getName());

            dto.setTotalRuns(inn.getTotalRuns());
            dto.setTotalWickets(inn.getTotalWickets());
            dto.setCompletedOvers(inn.getTotalOvers());
            dto.setCurrentBalls(inn.getTotalBalls());

            double totalOversDec = inn.getTotalOvers() + (inn.getTotalBalls() / 6.0);
            dto.setRunRate(totalOversDec > 0 ? Math.round((inn.getTotalRuns() / totalOversDec) * 100.0) / 100.0 : 0.0);

            // First Innings Details
            if (innings1Opt.isPresent()) {
                Innings inn1 = innings1Opt.get();
                dto.setFirstInningsTeamName(inn1.getBattingTeam().getName());
                dto.setFirstInningsRuns(inn1.getTotalRuns() != null ? inn1.getTotalRuns() : 0);
                dto.setFirstInningsWickets(inn1.getTotalWickets() != null ? inn1.getTotalWickets() : 0);
                dto.setFirstInningsOvers(inn1.getTotalOvers() != null ? inn1.getTotalOvers() : 0);
                dto.setFirstInningsBalls(inn1.getTotalBalls() != null ? inn1.getTotalBalls() : 0);
            }

            // Target & Required Run Rate Calculation (Strictly ONLY during 2nd innings)
            if (inn.getInningsNumber() == 2 && innings1Opt.isPresent()) {
                Innings inn1 = innings1Opt.get();
                int target = (inn1.getTotalRuns() != null ? inn1.getTotalRuns() : 0) + 1;
                dto.setTargetRuns(target);
                int current2ndRuns = inn.getTotalRuns() != null ? inn.getTotalRuns() : 0;
                int runsNeeded = target - current2ndRuns;
                dto.setRequiredRuns(runsNeeded);

                int totalMaxBalls = (match.getOvers() != null ? match.getOvers() : 20) * 6;
                int currentTotalBalls = ((inn.getTotalOvers() != null ? inn.getTotalOvers() : 0) * 6) + (inn.getTotalBalls() != null ? inn.getTotalBalls() : 0);
                int ballsLeft = Math.max(0, totalMaxBalls - currentTotalBalls);
                dto.setBallsRemaining(ballsLeft);
                double reqOvers = ballsLeft / 6.0;
                dto.setRequiredRunRate(reqOvers > 0 && runsNeeded > 0 ? Math.round((runsNeeded / reqOvers) * 100.0) / 100.0 : 0.0);

                if (runsNeeded <= 0) {
                    dto.setTargetEquation(inn.getBattingTeam().getName() + " WON");
                } else {
                    String runsWord = runsNeeded == 1 ? "run" : "runs";
                    String ballsWord = ballsLeft == 1 ? "ball" : "balls";
                    dto.setTargetEquation(inn.getBattingTeam().getName() + " need " + runsNeeded + " " + runsWord + " from " + ballsLeft + " " + ballsWord + " to win");
                }
            } else {
                dto.setRequiredRuns(null);
                dto.setTargetEquation(null);
                if (innings1Opt.isPresent()) {
                    dto.setTargetRuns((innings1Opt.get().getTotalRuns() != null ? innings1Opt.get().getTotalRuns() : 0) + 1);
                }
            }

            // Innings Break Check
            if (innings1Opt.isPresent()) {
                Innings inn1 = innings1Opt.get();
                boolean inn1Finished = inn1.getStatus() == InningsStatus.COMPLETED || inn1.getTotalWickets() >= 10 || (match.getOvers() != null && inn1.getTotalOvers() >= match.getOvers());
                boolean inn2NotActive = innings2Opt.isEmpty() || innings2Opt.get().getStatus() == InningsStatus.NOT_STARTED;
                if (inn1Finished && inn2NotActive && match.getStatus() != MatchStatus.COMPLETED) {
                    dto.setIsInningsBreak(true);
                    int target = (inn1.getTotalRuns() != null ? inn1.getTotalRuns() : 0) + 1;
                    dto.setTargetRuns(target);
                    dto.setInningsBreakSummary("First Innings Completed: " + inn1.getBattingTeam().getName() + " " + inn1.getTotalRuns() + "/" + inn1.getTotalWickets() + " (" + inn1.getTotalOvers() + "." + inn1.getTotalBalls() + " Overs). Waiting for the second innings to begin...");
                    if (match.getStatus() != MatchStatus.PAUSED && match.getStatus() != MatchStatus.INNINGS_BREAK) {
                        match.setStatus(MatchStatus.INNINGS_BREAK);
                        matchRepository.save(match);
                    }
                }
            }
            if (match.getStatus() == MatchStatus.INNINGS_BREAK) {
                dto.setIsInningsBreak(true);
            }

            // Ball Events analysis
            List<BallEvent> events = ballEventRepository.findByInningsOrderByOverNumberAscBallNumberAsc(inn);
            
            // Build detailed player summaries and list filters
            populateScoringStateWithBallEvents(dto, match, inn, events, maxOversPerBowler);
        }

        // Completed Match Check & Winner Calculation
        boolean isCompleted = match.getStatus() == MatchStatus.COMPLETED;
        if (!isCompleted && innings1Opt.isPresent() && innings2Opt.isPresent()) {
            Innings inn1 = innings1Opt.get();
            Innings inn2 = innings2Opt.get();
            int r1 = inn1.getTotalRuns() != null ? inn1.getTotalRuns() : 0;
            int r2 = inn2.getTotalRuns() != null ? inn2.getTotalRuns() : 0;
            int w2 = inn2.getTotalWickets() != null ? inn2.getTotalWickets() : 0;
            int target = r1 + 1;

            boolean inn2Chased = r2 >= target;
            boolean inn2Finished = inn2.getStatus() == InningsStatus.COMPLETED || w2 >= 10 || (match.getOvers() != null && inn2.getTotalOvers() >= match.getOvers());

            if (inn2Chased || inn2Finished) {
                isCompleted = true;
                match.setStatus(MatchStatus.COMPLETED);
                matchRepository.save(match);
            }
        }

        if (isCompleted) {
            dto.setIsMatchCompleted(true);
            dto.setStatus(MatchStatus.COMPLETED);
            if (innings1Opt.isPresent() && innings2Opt.isPresent()) {
                Innings inn1 = innings1Opt.get();
                Innings inn2 = innings2Opt.get();
                int r1 = inn1.getTotalRuns() != null ? inn1.getTotalRuns() : 0;
                int r2 = inn2.getTotalRuns() != null ? inn2.getTotalRuns() : 0;
                int w2 = inn2.getTotalWickets() != null ? inn2.getTotalWickets() : 0;
                int target = r1 + 1;

                if (r2 >= target) {
                    int wicketsLeft = Math.max(0, 10 - w2);
                    String summary = "🏆 " + inn2.getBattingTeam().getName() + " won by " + wicketsLeft + (wicketsLeft == 1 ? " wicket!" : " wickets!");
                    dto.setMatchResultSummary(summary);
                    dto.setWinnerTeamId(inn2.getBattingTeam().getTeamId());
                    dto.setWinnerTeamName(inn2.getBattingTeam().getName());
                    dto.setResultMargin(wicketsLeft + (wicketsLeft == 1 ? " wicket" : " wickets"));
                } else if (r1 > r2) {
                    int margin = r1 - r2;
                    String summary = "🏆 " + inn1.getBattingTeam().getName() + " won by " + margin + (margin == 1 ? " run!" : " runs!");
                    dto.setMatchResultSummary(summary);
                    dto.setWinnerTeamId(inn1.getBattingTeam().getTeamId());
                    dto.setWinnerTeamName(inn1.getBattingTeam().getName());
                    dto.setResultMargin(margin + (margin == 1 ? " run" : " runs"));
                } else {
                    dto.setMatchResultSummary("🤝 Match Tied!");
                    dto.setResultMargin("Tied");
                }
                dto.setCompletedAt(java.time.LocalDateTime.now().toString());
            } else if (innings1Opt.isPresent()) {
                Innings inn1 = innings1Opt.get();
                dto.setMatchResultSummary("🏆 Match Completed — " + inn1.getBattingTeam().getName() + " scored " + inn1.getTotalRuns() + "/" + inn1.getTotalWickets());
                dto.setCompletedAt(java.time.LocalDateTime.now().toString());
            }
        }

        // Full Scorecard Generation (Freshly generated including completed match scorecards)
        if (innings1Opt.isPresent()) {
            dto.setInnings1Scorecard(buildInningsScorecard(match, innings1Opt.get(), maxOversPerBowler));
        }
        if (innings2Opt.isPresent() && (innings2Opt.get().getStatus() != InningsStatus.NOT_STARTED || isCompleted)) {
            dto.setInnings2Scorecard(buildInningsScorecard(match, innings2Opt.get(), maxOversPerBowler));
        }

        // Pause details
        if (match.getStatus() == MatchStatus.PAUSED) {
            dto.setIsPaused(true);
            dto.setPauseReason(match.getCurrentPauseReason() != null ? match.getCurrentPauseReason() : "Match Paused");
            dto.setPausedAt(match.getPausedAt() != null ? match.getPausedAt().toString() : "");
        } else {
            dto.setIsPaused(false);
            dto.setPauseReason(null);
            dto.setPausedAt(null);
        }

        if (!isCompleted) {
            dto.setStatus(match.getStatus());
        }

        return dto;
    }

    private ScoringDashboardStateDto.InningsScorecardDto buildInningsScorecard(Match match, Innings inn, int maxOversPerBowler) {
        if (inn == null) return null;
        ScoringDashboardStateDto.InningsScorecardDto sc = new ScoringDashboardStateDto.InningsScorecardDto();
        sc.setInningsNumber(inn.getInningsNumber());
        sc.setBattingTeamName(inn.getBattingTeam().getName());
        sc.setBowlingTeamName(inn.getBowlingTeam().getName());
        sc.setTotalRuns(inn.getTotalRuns());
        sc.setTotalWickets(inn.getTotalWickets());
        sc.setCompletedOvers(inn.getTotalOvers());
        sc.setCurrentBalls(inn.getTotalBalls());

        List<BallEvent> events = ballEventRepository.findByInningsOrderByOverNumberAscBallNumberAsc(inn);
        ScoringDashboardStateDto dummyDto = new ScoringDashboardStateDto();
        populateScoringStateWithBallEvents(dummyDto, match, inn, events, maxOversPerBowler);

        sc.setTotalExtras(dummyDto.getTotalExtras());
        sc.setBattingList(dummyDto.getBattingPlayingXi());
        sc.setTotalFours(dummyDto.getCurrentInningsTotalFours());
        sc.setTotalSixes(dummyDto.getCurrentInningsTotalSixes());
        sc.setFallOfWickets(dummyDto.getCurrentInningsFallOfWickets());

        // Requirement 2: ONLY include bowlers who bowled at least 1 ball (ballsBowled > 0)
        List<ScoringDashboardStateDto.PlayerSummaryDto> bowledList = dummyDto.getBowlingPlayingXi().stream()
                .filter(b -> b.getBallsBowled() != null && b.getBallsBowled() > 0)
                .collect(java.util.stream.Collectors.toList());
        sc.setBowlingList(bowledList);
        return sc;
    }

    private void populateScoringStateWithBallEvents(ScoringDashboardStateDto dto, Match match, Innings inn, List<BallEvent> events, int maxOversPerBowler) {
        List<MatchPlayingXi> battingXi = matchPlayingXiRepository.findByMatchAndTeam(match, inn.getBattingTeam());
        List<MatchPlayingXi> bowlingXi = matchPlayingXiRepository.findByMatchAndTeam(match, inn.getBowlingTeam());

        java.util.Set<User> allUsers = new java.util.HashSet<>();
        if (battingXi != null) battingXi.forEach(xi -> { if (xi.getUser() != null) allUsers.add(xi.getUser()); });
        if (bowlingXi != null) bowlingXi.forEach(xi -> { if (xi.getUser() != null) allUsers.add(xi.getUser()); });

        java.util.Map<Long, PlayerProfile> profileMap = new java.util.HashMap<>();
        if (!allUsers.isEmpty()) {
            try {
                List<PlayerProfile> profList = playerProfileRepository.findByUserIn(allUsers);
                if (profList != null) {
                    for (PlayerProfile p : profList) {
                        if (p.getUser() != null) profileMap.put(p.getUser().getId(), p);
                    }
                }
            } catch (Exception ignored) {}
        }

        java.util.Map<Long, ScoringDashboardStateDto.PlayerSummaryDto> batterMap = new java.util.LinkedHashMap<>();
        if (battingXi != null && !battingXi.isEmpty()) {
            for (MatchPlayingXi xi : battingXi) {
                User u = xi.getUser();
                ScoringDashboardStateDto.PlayerSummaryDto pSum = buildPlayerSummary(u, profileMap);
                pSum.setIsWicketKeeper(xi.isWicketKeeper());
                batterMap.put(u.getId(), pSum);
            }
        } else {
            List<TeamMember> members = teamMemberRepository.findByTeamAndStatus(inn.getBattingTeam(), MemberStatus.ACTIVE);
            for (TeamMember tm : members) {
                User u = tm.getUser();
                ScoringDashboardStateDto.PlayerSummaryDto pSum = buildPlayerSummary(u, profileMap);
                PlayerProfile profileOpt = profileMap.get(u.getId());
                pSum.setIsWicketKeeper(profileOpt != null && profileOpt.getPlayingRole() == PlayingRole.WICKET_KEEPER);
                batterMap.put(u.getId(), pSum);
            }
        }

        java.util.Map<Long, ScoringDashboardStateDto.PlayerSummaryDto> bowlerMap = new java.util.LinkedHashMap<>();
        ScoringDashboardStateDto.PlayerSummaryDto currentWkSummary = null;
        if (bowlingXi != null && !bowlingXi.isEmpty()) {
            for (MatchPlayingXi xi : bowlingXi) {
                User u = xi.getUser();
                ScoringDashboardStateDto.PlayerSummaryDto pSum = buildPlayerSummary(u, profileMap);
                pSum.setIsWicketKeeper(xi.isWicketKeeper());
                bowlerMap.put(u.getId(), pSum);
                if (xi.isWicketKeeper()) {
                    currentWkSummary = pSum;
                }
            }
        } else {
            List<TeamMember> members = teamMemberRepository.findByTeamAndStatus(inn.getBowlingTeam(), MemberStatus.ACTIVE);
            for (TeamMember tm : members) {
                User u = tm.getUser();
                ScoringDashboardStateDto.PlayerSummaryDto pSum = buildPlayerSummary(u, profileMap);
                PlayerProfile profileOpt = profileMap.get(u.getId());
                boolean isWk = profileOpt != null && profileOpt.getPlayingRole() == PlayingRole.WICKET_KEEPER;
                pSum.setIsWicketKeeper(isWk);
                bowlerMap.put(u.getId(), pSum);
                if (isWk && currentWkSummary == null) {
                    currentWkSummary = pSum;
                }
            }
        }
        dto.setCurrentWicketKeeper(currentWkSummary);

        java.util.Set<Long> playedBatterIds = new java.util.HashSet<>();
        if (inn.getStriker() != null) playedBatterIds.add(inn.getStriker().getId());
        if (inn.getNonStriker() != null) playedBatterIds.add(inn.getNonStriker().getId());

        int partnershipRuns = 0;
        int partnershipBalls = 0;
        int extrasCount = 0;
        int fallOfWickets = 0;
        int cumulativeRuns = 0;
        int totalFours = 0;
        int totalSixes = 0;
        List<ScoringDashboardStateDto.FallOfWicketDto> fowList = new ArrayList<>();

        List<BallEventDto> currentOverEvents = new ArrayList<>();
        List<String> currentOverSummary = new ArrayList<>();

        for (BallEvent be : events) {
            int runs = be.getRunsScored() != null ? be.getRunsScored() : 0;
            int ext = be.getExtraRuns() != null ? be.getExtraRuns() : 0;
            String type = be.getExtraType();

            extrasCount += ext;
            cumulativeRuns += runs + ext;
            if (runs == 4) totalFours++;
            if (runs == 6) totalSixes++;

            // Striker stats
            User sUser = be.getStriker();
            if (sUser != null) {
                playedBatterIds.add(sUser.getId());
            }
            if (be.getNonStriker() != null) {
                playedBatterIds.add(be.getNonStriker().getId());
            }
            if (be.getDismissedUser() != null) {
                playedBatterIds.add(be.getDismissedUser().getId());
            }
            if (sUser != null && batterMap.containsKey(sUser.getId())) {
                ScoringDashboardStateDto.PlayerSummaryDto p = batterMap.get(sUser.getId());
                if (!"WIDE".equals(type)) {
                    p.setBalls(p.getBalls() + 1);
                    p.setRuns(p.getRuns() + runs);
                    if (runs == 4) p.setFours(p.getFours() + 1);
                    if (runs == 6) p.setSixes(p.getSixes() + 1);
                }
            }

            // Bowler stats
            User bUser = be.getBowler();
            if (bUser != null && bowlerMap.containsKey(bUser.getId())) {
                ScoringDashboardStateDto.PlayerSummaryDto b = bowlerMap.get(bUser.getId());
                if (!"WIDE".equals(type) && !"NO_BALL".equals(type)) {
                    b.setBallsBowled(b.getBallsBowled() + 1);
                }
                b.setRunsConceded(b.getRunsConceded() + runs + ext);
                if (be.isWicket() && !"RUN_OUT".equals(be.getWicketType()) && !"RETIRED".equals(be.getWicketType()) && !"RETIRED_OUT".equals(be.getWicketType()) && !"RETIRED_HURT".equals(be.getWicketType())) {
                    b.setWicketsTaken(b.getWicketsTaken() + 1);
                }
            }

            if (be.isWicket()) {
                fallOfWickets++;
                partnershipRuns = 0;
                partnershipBalls = 0;

                String disName = "";
                if (be.getDismissedUser() != null) {
                    disName = be.getDismissedUser().getName();
                } else if (sUser != null) {
                    disName = sUser.getName();
                }
                String ovBall = (be.getOverNumber() != null ? be.getOverNumber() : 0) + "." + (be.getBallNumber() != null ? be.getBallNumber() : 0);
                fowList.add(new ScoringDashboardStateDto.FallOfWicketDto(fallOfWickets, cumulativeRuns, disName, ovBall));

                if (be.getDismissedUser() != null && batterMap.containsKey(be.getDismissedUser().getId())) {
                    ScoringDashboardStateDto.PlayerSummaryDto disBatter = batterMap.get(be.getDismissedUser().getId());
                    disBatter.setIsOut(true);
                    String wType = be.getWicketType() != null ? be.getWicketType().toUpperCase() : "BOWLED";
                    disBatter.setDismissalType(wType);

                    String bName = be.getBowler() != null ? be.getBowler().getName() : "";
                    String fName = be.getFielder() != null ? be.getFielder().getName() : "";
                    String dText;
                    switch (wType) {
                        case "RUN_OUT":
                            dText = StringUtils.hasText(fName) ? "run out (" + fName + ")" : "run out";
                            break;
                        case "CAUGHT":
                            if (StringUtils.hasText(fName)) {
                                if (be.getBowler() != null && be.getFielder() != null && be.getBowler().getId().equals(be.getFielder().getId())) {
                                    dText = "c & b " + bName;
                                } else {
                                    dText = "c " + fName + " b " + bName;
                                }
                            } else {
                                dText = StringUtils.hasText(bName) ? "c & b " + bName : "caught";
                            }
                            break;
                        case "LBW":
                            dText = StringUtils.hasText(bName) ? "lbw b " + bName : "lbw";
                            break;
                        case "STUMPED":
                            dText = StringUtils.hasText(fName) ? "st " + fName + " b " + bName : (StringUtils.hasText(bName) ? "st b " + bName : "stumped");
                            break;
                        case "HIT_WICKET":
                            dText = StringUtils.hasText(bName) ? "hit wicket b " + bName : "hit wicket";
                            break;
                        case "RETIRED":
                        case "RETIRED_OUT":
                            dText = "retired out";
                            break;
                        case "RETIRED_HURT":
                            dText = "retired hurt";
                            break;
                        case "BOWLED":
                        default:
                            dText = StringUtils.hasText(bName) ? "b " + bName : "bowled";
                            break;
                    }
                    disBatter.setDismissalText(dText);
                }
            } else {
                partnershipRuns += runs + ext;
                if (!"WIDE".equals(type) && !"NO_BALL".equals(type)) {
                    partnershipBalls++;
                }
            }

            // Filter for current over
            if (be.getOverNumber() != null && be.getOverNumber().equals(inn.getTotalOvers())) {
                BallEventDto bDto = new BallEventDto();
                bDto.setId(be.getId());
                bDto.setOverNumber(be.getOverNumber());
                bDto.setBallNumber(be.getBallNumber());
                bDto.setRunsScored(runs);
                bDto.setExtraType(type);
                bDto.setExtraRuns(ext);
                bDto.setWicket(be.isWicket());
                bDto.setWicketType(be.getWicketType());
                bDto.setStrikerName(sUser != null ? sUser.getName() : "");
                bDto.setBowlerName(bUser != null ? bUser.getName() : "");

                String ballText;
                if (be.isWicket()) {
                    ballText = runs > 0 ? "W+" + runs : "W";
                } else if ("WIDE".equals(type)) {
                    ballText = runs > 0 ? "Wd+" + runs : "Wd";
                } else if ("NO_BALL".equals(type)) {
                    ballText = "Nb+" + runs;
                } else {
                    ballText = String.valueOf(runs);
                }
                currentOverSummary.add(ballText);
                currentOverEvents.add(bDto);
            }
        }

        // Calculate played/not played and strike rates
        batterMap.forEach((uId, p) -> {
            boolean didPlay = playedBatterIds.contains(uId)
                    || (p.getBalls() != null && p.getBalls() > 0)
                    || (p.getRuns() != null && p.getRuns() > 0)
                    || Boolean.TRUE.equals(p.getIsOut());
            p.setPlayed(didPlay);
            if (didPlay) {
                if (p.getBalls() != null && p.getBalls() > 0) {
                    p.setStrikeRate(Math.round((p.getRuns() * 100.0 / p.getBalls()) * 10.0) / 10.0);
                } else {
                    p.setStrikeRate(0.0);
                }
            } else {
                p.setStrikeRate(null);
            }
        });

        bowlerMap.values().forEach(b -> {
            int ov = b.getBallsBowled() / 6;
            int bl = b.getBallsBowled() % 6;
            b.setOversBowled(ov);
            double totalBowledDec = ov + (bl / 6.0);
            if (totalBowledDec > 0) {
                b.setEconomyRate(Math.round((b.getRunsConceded() / totalBowledDec) * 10.0) / 10.0);
            }
        });

        dto.setPartnershipRuns(partnershipRuns);
        dto.setPartnershipBalls(partnershipBalls);
        dto.setTotalExtras(extrasCount);
        dto.setFallOfWicketsCount(fallOfWickets);
        dto.setCurrentInningsFallOfWickets(fowList);
        dto.setCurrentInningsTotalFours(totalFours);
        dto.setCurrentInningsTotalSixes(totalSixes);
        dto.setThisOverBallEvents(currentOverEvents);
        dto.setOverSummaryBalls(currentOverSummary);
        dto.setCurrentOverBalls(currentOverSummary);

        // Update active Striker, NonStriker, and Bowler with calculated stats
        syncInningsEndsAndStrikers(inn);
        if (inn.getStriker() != null && batterMap.containsKey(inn.getStriker().getId())) {
            dto.setStriker(batterMap.get(inn.getStriker().getId()));
        }
        if (inn.getNonStriker() != null && batterMap.containsKey(inn.getNonStriker().getId())) {
            dto.setNonStriker(batterMap.get(inn.getNonStriker().getId()));
        }
        if (inn.getCurrentBowler() != null && bowlerMap.containsKey(inn.getCurrentBowler().getId())) {
            dto.setCurrentBowler(bowlerMap.get(inn.getCurrentBowler().getId()));
        }
        dto.setBowlingEnd(inn.getBowlingEnd());
        if (inn.getBattingEndA() != null && batterMap.containsKey(inn.getBattingEndA().getId())) {
            dto.setBattingEndA(batterMap.get(inn.getBattingEndA().getId()));
        }
        if (inn.getBattingEndB() != null && batterMap.containsKey(inn.getBattingEndB().getId())) {
            dto.setBattingEndB(batterMap.get(inn.getBattingEndB().getId()));
        }

        // Available Bowlers Filter (Excluding locked previous bowler & bowlers at max quota)
        String prevBowlerId = null;
        if (inn.getTotalOvers() > 0 && inn.getTotalBalls() == 0 && !events.isEmpty()) {
            BallEvent lastBe = events.get(events.size() - 1);
            if (lastBe.getBowler() != null) {
                prevBowlerId = lastBe.getBowler().getUserId();
            }
        }
        dto.setPreviousBowlerUserId(prevBowlerId);

        List<ScoringDashboardStateDto.PlayerSummaryDto> availBowlers = new ArrayList<>();
        final String finalPrevBowlerId = prevBowlerId;
        for (ScoringDashboardStateDto.PlayerSummaryDto b : bowlerMap.values()) {
            boolean isPrev = finalPrevBowlerId != null && b.getUserId().equals(finalPrevBowlerId);
            boolean maxed = b.getOversBowled() >= maxOversPerBowler;
            if ((Boolean.TRUE.equals(match.getAllowConsecutiveOvers()) || !isPrev) && !maxed) {
                availBowlers.add(b);
            }
        }
        dto.setAvailableBowlers(availBowlers);

        // Available Batters Filter (Excluding current active batters & dismissed batters)
        List<ScoringDashboardStateDto.PlayerSummaryDto> availBatters = new ArrayList<>();
        String strikerId = inn.getStriker() != null ? inn.getStriker().getUserId() : "";
        String nonStrikerId = inn.getNonStriker() != null ? inn.getNonStriker().getUserId() : "";

        for (ScoringDashboardStateDto.PlayerSummaryDto p : batterMap.values()) {
            boolean isActive = p.getUserId().equals(strikerId) || p.getUserId().equals(nonStrikerId);
            if (!isActive && !Boolean.TRUE.equals(p.getIsOut())) {
                availBatters.add(p);
            }
        }
        dto.setAvailableBatters(availBatters);

        dto.setBattingPlayingXi(new ArrayList<>(batterMap.values()));
        dto.setBowlingPlayingXi(new ArrayList<>(bowlerMap.values()));

        int selectedOverNum = inn.getSelectedBowlerOverNumber() != null ? inn.getSelectedBowlerOverNumber() : 1;
        boolean isOverFinished = inn.getTotalBalls() == 0 && inn.getTotalOvers() > 0;
        dto.setIsOverCompleted(isOverFinished && selectedOverNum <= inn.getTotalOvers());

        // Populate Recent Overs Breakdown
        java.util.Map<Integer, List<String>> overBallsMap = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, Integer> overRunsMap = new java.util.LinkedHashMap<>();

        for (BallEvent be : events) {
            if (be.getOverNumber() == null) continue;
            int ovNum = be.getOverNumber();
            int runs = be.getRunsScored() != null ? be.getRunsScored() : 0;
            int ext = be.getExtraRuns() != null ? be.getExtraRuns() : 0;
            String type = be.getExtraType();

            String ballText;
            if (be.isWicket()) {
                ballText = runs > 0 ? "W+" + runs : "W";
            } else if ("WIDE".equals(type)) {
                ballText = runs > 0 ? "Wd+" + runs : "Wd";
            } else if ("NO_BALL".equals(type)) {
                ballText = "Nb+" + runs;
            } else {
                ballText = String.valueOf(runs);
            }

            overBallsMap.computeIfAbsent(ovNum, k -> new ArrayList<>()).add(ballText);
            overRunsMap.put(ovNum, overRunsMap.getOrDefault(ovNum, 0) + runs + ext);
        }

        List<ScoringDashboardStateDto.RecentOverDto> recentOversList = new ArrayList<>();
        for (java.util.Map.Entry<Integer, List<String>> entry : overBallsMap.entrySet()) {
            int ovNum = entry.getKey();
            List<String> balls = entry.getValue();
            int r = overRunsMap.getOrDefault(ovNum, 0);
            String summary = "Over " + (ovNum + 1) + " • " + r + " Runs";
            recentOversList.add(new ScoringDashboardStateDto.RecentOverDto(ovNum + 1, balls, r, summary));
        }
        java.util.Collections.reverse(recentOversList);
        dto.setRecentOvers(recentOversList);

        // Populate Commentary Feed (Newest ball event first)
        List<ScoringDashboardStateDto.CommentaryDto> commentaryList = new ArrayList<>();
        for (int i = events.size() - 1; i >= 0; i--) {
            BallEvent be = events.get(i);
            int ovNum = (be.getOverNumber() != null ? be.getOverNumber() : 0);
            int bNum = (be.getBallNumber() != null && be.getBallNumber() > 0) ? be.getBallNumber() : 1;
            String overBall = ovNum + "." + bNum;

            String bName = be.getBowler() != null ? be.getBowler().getName() : "Bowler";
            String sName = be.getStriker() != null ? be.getStriker().getName() : "Striker";
            String bToS = bName + " to " + sName;

            int runs = be.getRunsScored() != null ? be.getRunsScored() : 0;
            int ext = be.getExtraRuns() != null ? be.getExtraRuns() : 0;
            String type = be.getExtraType();

            String title;
            String desc;

            if (be.isWicket()) {
                title = "WICKET!";
                String dName = be.getDismissedUser() != null ? be.getDismissedUser().getName() : sName;
                String wType = be.getWicketType() != null ? be.getWicketType() : "OUT";
                desc = dName + " OUT (" + wType + ") off " + bName;
            } else if ("WIDE".equals(type)) {
                title = "WIDE (" + (1 + ext) + ")";
                desc = bName + " bowled a wide ball to " + sName;
            } else if ("NO_BALL".equals(type)) {
                title = "NO BALL (" + (1 + ext + runs) + ")";
                desc = bName + " bowled a no-ball to " + sName;
            } else if ("BYE".equals(type)) {
                int bRuns = (ext > 0 ? ext : runs);
                title = "BYE (" + bRuns + ")";
                desc = sName + " ran " + bRuns + " bye(s) off " + bName;
            } else if ("LEG_BYE".equals(type)) {
                int lbRuns = (ext > 0 ? ext : runs);
                title = "LEG BYE (" + lbRuns + ")";
                desc = sName + " ran " + lbRuns + " leg bye(s) off " + bName;
            } else if (runs == 6) {
                title = "SIX!";
                desc = sName + " hit a huge SIX off " + bName + "!";
            } else if (runs == 4) {
                title = "FOUR!";
                desc = sName + " struck a FOUR off " + bName + "!";
            } else if (runs == 0) {
                title = "Dot ball";
                desc = bName + " to " + sName + ", no run";
            } else {
                title = runs + (runs == 1 ? " run" : " runs");
                desc = sName + " scored " + runs + (runs == 1 ? " run" : " runs") + " off " + bName;
            }

            commentaryList.add(new ScoringDashboardStateDto.CommentaryDto(overBall, title, bToS, desc));
        }
        dto.setCommentary(commentaryList);
    }

    private ScoringDashboardStateDto.PlayerSummaryDto buildPlayerSummary(User u) {
        return buildPlayerSummary(u, null);
    }

    private ScoringDashboardStateDto.PlayerSummaryDto buildPlayerSummary(User u, java.util.Map<Long, PlayerProfile> profileMap) {
        if (u == null) return null;
        PlayerProfile profile = null;
        if (profileMap != null && profileMap.containsKey(u.getId())) {
            profile = profileMap.get(u.getId());
        } else {
            profile = playerProfileRepository.findByUser(u).orElse(null);
        }
        String photo = profile != null ? profile.getProfilePhotoUrl() : null;
        String role = profile != null && profile.getPlayingRole() != null ? profile.getPlayingRole().name() : "PLAYER";
        return new ScoringDashboardStateDto.PlayerSummaryDto(u.getUserId(), u.getName(), photo, role);
    }

    public void notifyBallInProgress(String email, String matchId) {
        Match match = findMatchByMatchId(matchId);
        if (match.getStatus() == MatchStatus.PAUSED || match.getStatus() == MatchStatus.INNINGS_BREAK || match.getStatus() == MatchStatus.COMPLETED) {
            return;
        }
        Innings inn = getActiveInnings(match);
        String bowlerName = inn.getCurrentBowler() != null ? inn.getCurrentBowler().getName() : "Bowler";
        String strikerName = inn.getStriker() != null ? inn.getStriker().getName() : "Batter";
        int ov = inn.getTotalOvers() != null ? inn.getTotalOvers() : 0;
        int bl = (inn.getTotalBalls() != null ? inn.getTotalBalls() : 0) + 1;

        java.util.Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", "BALL_IN_PROGRESS");
        msg.put("matchId", matchId);
        msg.put("bowlerName", bowlerName);
        msg.put("strikerName", strikerName);
        msg.put("overNumber", ov);
        msg.put("ballNumber", bl);

        if (messagingTemplate != null) {
            try {
                messagingTemplate.convertAndSend("/topic/matches/" + matchId + "/live", msg);
            } catch (Exception e) {
                logger.warn("Failed to broadcast BALL_IN_PROGRESS: {}", e.getMessage());
            }
        }
    }

    private List<ScoringDashboardStateDto.PlayerSummaryDto> getPlayingXiPlayersForTeam(Match match, Team team) {
        List<MatchPlayingXi> xiList = matchPlayingXiRepository.findByMatchAndTeam(match, team);
        return xiList.stream()
                .map(xi -> buildPlayerSummary(xi.getUser()))
                .collect(java.util.stream.Collectors.toList());
    }
}


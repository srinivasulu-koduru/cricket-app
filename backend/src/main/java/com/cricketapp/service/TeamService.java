package com.cricketapp.service;

import com.cricketapp.dto.*;
import com.cricketapp.entity.*;
import com.cricketapp.exception.AuthException;
import com.cricketapp.repository.*;
import com.cricketapp.util.TeamIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private static final Logger logger = LoggerFactory.getLogger(TeamService.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif", "image/pjpeg", "image/x-png", "application/octet-stream"
    );

    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final TeamInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final PlayerProfileRepository profileRepository;
    private final MatchRepository matchRepository;
    private final MatchInvitationRepository matchInvitationRepository;
    private final MatchPlayingXiRepository matchPlayingXiRepository;
    private final TeamIdGenerator teamIdGenerator;
    private final EmailService emailService;

    public TeamService(
            TeamRepository teamRepository,
            TeamMemberRepository memberRepository,
            TeamInvitationRepository invitationRepository,
            UserRepository userRepository,
            PlayerProfileRepository profileRepository,
            MatchRepository matchRepository,
            MatchInvitationRepository matchInvitationRepository,
            MatchPlayingXiRepository matchPlayingXiRepository,
            TeamIdGenerator teamIdGenerator,
            EmailService emailService) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.matchRepository = matchRepository;
        this.matchInvitationRepository = matchInvitationRepository;
        this.matchPlayingXiRepository = matchPlayingXiRepository;
        this.teamIdGenerator = teamIdGenerator;
        this.emailService = emailService;
    }

    @Transactional
    public TeamResponseDto createTeam(String email, CreateTeamRequestDto request) {
        User creator = findUserByEmail(email);

        // A player can belong to ONLY ONE team at a time
        List<TeamMember> creatorActiveMemberships = memberRepository.findByUser_EmailAndStatus(creator.getEmail(), MemberStatus.ACTIVE);
        if (!creatorActiveMemberships.isEmpty()) {
            Team existingTeam = creatorActiveMemberships.get(0).getTeam();
            throw new AuthException("You are already a member of team '" + existingTeam.getName() + "'. A player can only belong to one team at a time.");
        }

        if (!StringUtils.hasText(request.getName())) {
            throw new AuthException("Team name cannot be blank");
        }

        String name = request.getName().trim();
        if (name.length() > 100) {
            throw new AuthException("Team name cannot exceed 100 characters");
        }

        String description = request.getDescription() != null ? request.getDescription().trim() : null;
        if (description != null && description.length() > 500) {
            throw new AuthException("Description cannot exceed 500 characters");
        }

        String permanentTeamId = teamIdGenerator.generateUniqueTeamId();
        String joinToken = UUID.randomUUID().toString();

        Team team = new Team(permanentTeamId, name, description, creator, joinToken);
        team = teamRepository.save(team);

        // Creator automatically becomes OWNER
        TeamMember ownerMember = new TeamMember(team, creator, TeamRole.OWNER);
        memberRepository.save(ownerMember);

        return mapToTeamResponseDto(team, creator, TeamRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<TeamResponseDto> getMyTeams(String email) {
        User user = findUserByEmail(email);
        List<TeamMember> activeMemberships = memberRepository.findByUser_EmailAndStatus(user.getEmail(), MemberStatus.ACTIVE);

        return activeMemberships.stream()
                .map(m -> mapToTeamResponseDto(m.getTeam(), user, m.getRole()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeamResponseDto> getAllTeams(String email) {
        User user = findUserByEmail(email);
        List<Team> allTeams = teamRepository.findAll();

        return allTeams.stream().map(t -> {
            Optional<TeamMember> memberOpt = memberRepository.findByTeamAndUser(t, user);
            TeamRole role = memberOpt.filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                    .map(TeamMember::getRole)
                    .orElse(null);
            return mapToTeamResponseDto(t, user, role);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeamResponseDto getTeamDetails(String email, String teamId) {
        User user = findUserByEmail(email);
        Team team = findTeamByTeamId(teamId);

        Optional<TeamMember> memberOpt = memberRepository.findByTeamAndUser(team, user);
        TeamRole currentRole = memberOpt.filter(m -> m.getStatus() == MemberStatus.ACTIVE)
                .map(TeamMember::getRole)
                .orElse(null);

        return mapToTeamResponseDto(team, user, currentRole);
    }

    @Transactional
    public TeamResponseDto updateTeam(String email, String teamId, UpdateTeamRequestDto request) {
        User user = findUserByEmail(email);
        Team team = findTeamByTeamId(teamId);

        verifyOwnerOrCaptain(team, user);

        if (!StringUtils.hasText(request.getName())) {
            throw new AuthException("Team name cannot be blank");
        }

        String name = request.getName().trim();
        if (name.length() > 100) {
            throw new AuthException("Team name cannot exceed 100 characters");
        }

        String description = request.getDescription() != null ? request.getDescription().trim() : null;
        if (description != null && description.length() > 500) {
            throw new AuthException("Description cannot exceed 500 characters");
        }

        team.setName(name);
        team.setDescription(description);
        Team updated = teamRepository.save(team);

        return mapToTeamResponseDto(updated, user, TeamRole.OWNER);
    }

    @Transactional
    public TeamResponseDto uploadTeamLogo(String email, String teamId, MultipartFile file) {
        User user = findUserByEmail(email);
        Team team = findTeamByTeamId(teamId);

        verifyOwnerOrCaptain(team, user);

        if (file == null || file.isEmpty()) {
            throw new AuthException("Please select an image file to upload");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AuthException("Team logo size exceeds maximum limit of 5 MB");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);

        boolean isValidType = (contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase()))
                || (contentType != null && contentType.toLowerCase().startsWith("image/"))
                || isAllowedExtension(extension);

        if (!isValidType) {
            throw new AuthException("Invalid file type. Only JPG, JPEG, PNG, and WEBP image formats are allowed.");
        }

        String newFilename = UUID.randomUUID().toString() + extension;
        File uploadDir = new File("uploads/logos");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        File destFile = new File(uploadDir, newFilename);

        try {
            Files.copy(file.getInputStream(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Failed to save team logo: {}", e.getMessage());
            throw new AuthException("Failed to upload team logo. Please try again.");
        }

        // Delete old logo file if present
        deletePhysicalFile(team.getLogoUrl());

        team.setLogoUrl("/uploads/logos/" + newFilename);
        Team updated = teamRepository.save(team);

        return mapToTeamResponseDto(updated, user, TeamRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponseDto> getTeamMembers(String email, String teamId) {
        Team team = findTeamByTeamId(teamId);
        List<TeamMember> activeMembers = memberRepository.findByTeamAndStatus(team, MemberStatus.ACTIVE);

        return activeMembers.stream().map(m -> {
            User u = m.getUser();
            Optional<PlayerProfile> profileOpt = profileRepository.findByUser(u);
            String photoUrl = profileOpt.map(PlayerProfile::getProfilePhotoUrl).orElse(null);
            String playingRole = profileOpt.map(p -> p.getPlayingRole() != null ? p.getPlayingRole().name() : null).orElse(null);
            String battingStyle = profileOpt.map(p -> p.getBattingStyle() != null ? p.getBattingStyle().name() : null).orElse(null);
            String bowlingStyle = profileOpt.map(p -> p.getBowlingStyle() != null ? p.getBowlingStyle().name() : null).orElse(null);

            return new TeamMemberResponseDto(
                    u.getUserId(),
                    u.getName(),
                    u.getEmail(),
                    photoUrl,
                    m.getRole(),
                    m.getStatus(),
                    m.getJoinedAt(),
                    playingRole,
                    battingStyle,
                    bowlingStyle
            );
        }).collect(Collectors.toList());
    }

    @Transactional
    public TeamInvitationResponseDto invitePlayer(String email, String teamId, String cricketUserId) {
        User caller = findUserByEmail(email);
        Team team = findTeamByTeamId(teamId);

        verifyOwnerOrCaptain(team, caller);

        if (!StringUtils.hasText(cricketUserId)) {
            throw new AuthException("Cricket User ID cannot be blank");
        }

        String normalizedUserId = cricketUserId.trim().toUpperCase();
        User targetUser = userRepository.findByUserId(normalizedUserId)
                .orElseThrow(() -> new AuthException("Player not found with Cricket User ID: " + normalizedUserId));

        // Check if target player is already an active member of ANY team
        List<TeamMember> activeMemberships = memberRepository.findByUser_UserIdAndStatus(normalizedUserId, MemberStatus.ACTIVE);
        if (!activeMemberships.isEmpty()) {
            Team currentTeamOfTarget = activeMemberships.get(0).getTeam();
            if (currentTeamOfTarget.getId().equals(team.getId())) {
                throw new AuthException("Player " + targetUser.getName() + " (" + normalizedUserId + ") is already an active member of this team.");
            } else {
                throw new AuthException("Player " + targetUser.getName() + " (" + normalizedUserId + ") is already a member of team '" + currentTeamOfTarget.getName() + "'. A player can only belong to one team.");
            }
        }

        // Check if invitation is already PENDING
        if (invitationRepository.existsByTeamAndInvitedUserAndStatus(team, targetUser, InvitationStatus.PENDING)) {
            throw new AuthException("An invitation is already pending for player " + normalizedUserId + ".");
        }

        TeamInvitation invitation = new TeamInvitation(team, targetUser, caller);
        invitation = invitationRepository.save(invitation);

        // Send real email invitation to the player's Gmail / email address
        emailService.sendTeamInvitationEmail(
                targetUser.getEmail(),
                targetUser.getName(),
                team.getName(),
                team.getTeamId(),
                caller.getName(),
                team.getDescription(),
                team.getJoinToken()
        );

        return mapToInvitationResponseDto(invitation);
    }

    @Transactional(readOnly = true)
    public List<TeamInvitationResponseDto> getMyInvitations(String email) {
        User user = findUserByEmail(email);
        List<TeamInvitation> pendingInvitations = invitationRepository
                .findByInvitedUser_EmailAndStatusOrderByCreatedAtDesc(user.getEmail(), InvitationStatus.PENDING);

        return pendingInvitations.stream()
                .map(this::mapToInvitationResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse acceptInvitation(String email, Long invitationId) {
        User user = findUserByEmail(email);
        TeamInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new AuthException("Invitation not found"));

        if (!invitation.getInvitedUser().getId().equals(user.getId())) {
            throw new AuthException("Unauthorized invitation request");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new AuthException("Invitation is no longer pending");
        }

        Team team = invitation.getTeam();

        // Check if user is already an active member of another team
        List<TeamMember> userActiveMemberships = memberRepository.findByUser_EmailAndStatus(user.getEmail(), MemberStatus.ACTIVE);
        for (TeamMember m : userActiveMemberships) {
            if (!m.getTeam().getId().equals(team.getId())) {
                throw new AuthException("You are already an active member of team '" + m.getTeam().getName() + "'. A player can only belong to one team at a time. Please leave your current team before joining a new team.");
            }
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        // Create or update member status to ACTIVE
        Optional<TeamMember> existingOpt = memberRepository.findByTeamAndUser(team, user);
        TeamMember member;
        if (existingOpt.isPresent()) {
            member = existingOpt.get();
            member.setStatus(MemberStatus.ACTIVE);
            member.setRole(TeamRole.PLAYER);
        } else {
            member = new TeamMember(team, user, TeamRole.PLAYER);
        }
        memberRepository.save(member);

        return new MessageResponse("Invitation accepted. You are now a member of " + team.getName() + "!");
    }

    @Transactional
    public MessageResponse rejectInvitation(String email, Long invitationId) {
        User user = findUserByEmail(email);
        TeamInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new AuthException("Invitation not found"));

        if (!invitation.getInvitedUser().getId().equals(user.getId())) {
            throw new AuthException("Unauthorized invitation request");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new AuthException("Invitation is no longer pending");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        invitationRepository.save(invitation);

        return new MessageResponse("Invitation rejected.");
    }

    @Transactional
    public MessageResponse cancelInvitation(String email, String teamId, Long invitationId) {
        User caller = findUserByEmail(email);
        Team team = findTeamByTeamId(teamId);
        verifyOwnerOrCaptain(team, caller);

        TeamInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new AuthException("Invitation not found"));

        if (!invitation.getTeam().getId().equals(team.getId())) {
            throw new AuthException("Invitation does not belong to this team");
        }

        invitation.setStatus(InvitationStatus.CANCELLED);
        invitationRepository.save(invitation);

        return new MessageResponse("Invitation cancelled.");
    }

    @Transactional
    public MessageResponse removePlayer(String email, String teamId, String targetCricketUserId) {
        User caller = findUserByEmail(email);
        Team team = findTeamByTeamId(teamId);
        verifyOwnerOrCaptain(team, caller);

        String normalizedTargetId = targetCricketUserId.trim().toUpperCase();
        TeamMember targetMember = memberRepository.findByTeam_TeamIdAndUser_UserId(teamId, normalizedTargetId)
                .orElseThrow(() -> new AuthException("Player is not a member of this team"));

        if (targetMember.getRole() == TeamRole.OWNER) {
            throw new AuthException("Cannot remove the team owner from the team.");
        }

        targetMember.setStatus(MemberStatus.REMOVED);
        memberRepository.save(targetMember);

        return new MessageResponse("Player " + targetMember.getUser().getName() + " removed from team.");
    }

    @Transactional
    public MessageResponse leaveTeam(String email, String teamId) {
        User caller = findUserByEmail(email);
        Team team = findTeamByTeamId(teamId);

        TeamMember member = memberRepository.findByTeamAndUser(team, caller)
                .orElseThrow(() -> new AuthException("You are not a member of this team"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new AuthException("You are not an active member of this team");
        }

        if (member.getRole() == TeamRole.OWNER) {
            throw new AuthException("As the team owner, you cannot leave the team without transferring ownership first.");
        }

        member.setStatus(MemberStatus.LEFT);
        memberRepository.save(member);

        return new MessageResponse("You have left the team " + team.getName() + ".");
    }

    @Transactional
    public MessageResponse deleteTeam(String email, String teamId) {
        User user = findUserByEmail(email);
        Team team = findTeamByTeamId(teamId);

        TeamMember member = memberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new AuthException("You do not have permission to delete this team."));

        if (member.getStatus() != MemberStatus.ACTIVE || member.getRole() != TeamRole.OWNER) {
            throw new AuthException("Only the team owner can delete this team.");
        }

        // Clean up matches, match invitations, and playing XI associated with this team to prevent FK constraint failures
        List<Match> teamMatches = matchRepository.findByTeamAOrTeamB(team, team);
        if (teamMatches != null && !teamMatches.isEmpty()) {
            for (Match m : teamMatches) {
                matchPlayingXiRepository.deleteByMatch(m);
            }
            matchInvitationRepository.deleteByMatchIn(teamMatches);
        }
        matchPlayingXiRepository.deleteByTeam(team);
        matchInvitationRepository.deleteByTeam(team);
        matchRepository.deleteByTeam(team);

        invitationRepository.deleteByTeam(team);
        memberRepository.deleteByTeam(team);
        deletePhysicalFile(team.getLogoUrl());
        teamRepository.delete(team);

        return new MessageResponse("Team " + team.getName() + " (" + team.getTeamId() + ") deleted successfully.");
    }

    @Transactional
    public TeamMemberResponseDto updateMemberRole(String email, String teamId, String targetUserId, UpdateMemberRoleRequestDto request) {
        User caller = findUserByEmail(email);
        Team team = findTeamByTeamId(teamId);

        verifyOwnerOrCaptain(team, caller);

        if (request == null || request.getRole() == null) {
            throw new AuthException("Please specify a valid team role.");
        }

        User targetUser = userRepository.findByUserId(targetUserId.trim())
                .orElseThrow(() -> new AuthException("Target player not found with CRK ID: " + targetUserId));

        TeamMember targetMember = memberRepository.findByTeamAndUser(team, targetUser)
                .orElseThrow(() -> new AuthException("Player " + targetUser.getName() + " is not a member of team " + team.getName()));

        if (targetMember.getStatus() != MemberStatus.ACTIVE) {
            throw new AuthException("Player " + targetUser.getName() + " is not an active member of team " + team.getName());
        }

        if (targetMember.getRole() == TeamRole.OWNER) {
            throw new AuthException("The team owner's role cannot be modified.");
        }

        if (request.getRole() == TeamRole.OWNER) {
            throw new AuthException("To assign team ownership, please use ownership transfer.");
        }

        targetMember.setRole(request.getRole());
        TeamMember saved = memberRepository.save(targetMember);

        Optional<PlayerProfile> profileOpt = profileRepository.findByUser(targetUser);
        String photoUrl = profileOpt.map(PlayerProfile::getProfilePhotoUrl).orElse(null);

        return new TeamMemberResponseDto(
                targetUser.getUserId(),
                targetUser.getName(),
                targetUser.getEmail(),
                photoUrl,
                saved.getRole(),
                saved.getStatus(),
                saved.getJoinedAt()
        );
    }

    @Transactional(readOnly = true)
    public TeamResponseDto getTeamByJoinToken(String joinToken) {
        Team team = teamRepository.findByJoinToken(joinToken.trim())
                .orElseThrow(() -> new AuthException("Invalid or expired shareable team join link"));

        long count = memberRepository.countByTeamAndStatus(team, MemberStatus.ACTIVE);
        return new TeamResponseDto(
                team.getTeamId(),
                team.getName(),
                team.getLogoUrl(),
                team.getDescription(),
                team.getCreatedBy().getUserId(),
                team.getCreatedBy().getName(),
                team.getJoinToken(),
                team.getStatus(),
                count,
                null,
                team.getCreatedAt()
        );
    }

    @Transactional
    public MessageResponse joinTeamByToken(String email, String joinToken) {
        User user = findUserByEmail(email);
        Team team = teamRepository.findByJoinToken(joinToken.trim())
                .orElseThrow(() -> new AuthException("Invalid or expired shareable team join link"));

        // Check if user is already an active member of another team
        List<TeamMember> userActiveMemberships = memberRepository.findByUser_EmailAndStatus(user.getEmail(), MemberStatus.ACTIVE);
        for (TeamMember m : userActiveMemberships) {
            if (!m.getTeam().getId().equals(team.getId())) {
                throw new AuthException("You are already an active member of team '" + m.getTeam().getName() + "'. A player can only belong to one team at a time. Please leave your current team before joining a new team.");
            }
        }

        Optional<TeamMember> existingOpt = memberRepository.findByTeamAndUser(team, user);
        if (existingOpt.isPresent()) {
            TeamMember existing = existingOpt.get();
            if (existing.getStatus() == MemberStatus.ACTIVE) {
                return new MessageResponse("You are already an active member of " + team.getName() + "!");
            }
            existing.setStatus(MemberStatus.ACTIVE);
            memberRepository.save(existing);
        } else {
            TeamMember newMember = new TeamMember(team, user, TeamRole.PLAYER);
            memberRepository.save(newMember);
        }

        return new MessageResponse("You have successfully joined " + team.getName() + "!");
    }

    private User findUserByEmail(String email) {
        String normalized = email.toLowerCase().trim();
        return userRepository.findByEmail(normalized)
                .orElseThrow(() -> new AuthException("User account not found"));
    }

    private Team findTeamByTeamId(String teamId) {
        return teamRepository.findByTeamId(teamId.trim())
                .orElseThrow(() -> new AuthException("Team not found with Team ID: " + teamId));
    }

    private void verifyOwnerOrCaptain(Team team, User user) {
        TeamMember member = memberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new AuthException("You do not have permission to manage this team."));

        if (member.getStatus() != MemberStatus.ACTIVE ||
                (member.getRole() != TeamRole.OWNER && member.getRole() != TeamRole.CAPTAIN)) {
            throw new AuthException("Only the team owner or captain can perform this action.");
        }
    }

    private TeamResponseDto mapToTeamResponseDto(Team team, User currentUser, TeamRole currentRole) {
        long memberCount = memberRepository.countByTeamAndStatus(team, MemberStatus.ACTIVE);
        return new TeamResponseDto(
                team.getTeamId(),
                team.getName(),
                team.getLogoUrl(),
                team.getDescription(),
                team.getCreatedBy().getUserId(),
                team.getCreatedBy().getName(),
                team.getJoinToken(),
                team.getStatus(),
                memberCount,
                currentRole,
                team.getCreatedAt()
        );
    }

    private TeamInvitationResponseDto mapToInvitationResponseDto(TeamInvitation invitation) {
        Team team = invitation.getTeam();
        User target = invitation.getInvitedUser();
        User inviter = invitation.getInvitedBy();

        return new TeamInvitationResponseDto(
                invitation.getId(),
                team.getTeamId(),
                team.getName(),
                team.getLogoUrl(),
                target.getUserId(),
                target.getName(),
                inviter.getUserId(),
                inviter.getName(),
                invitation.getStatus(),
                invitation.getCreatedAt()
        );
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    private boolean isAllowedExtension(String ext) {
        return Arrays.asList(".jpg", ".jpeg", ".png", ".webp").contains(ext);
    }

    private void deletePhysicalFile(String logoUrl) {
        if (StringUtils.hasText(logoUrl) && logoUrl.startsWith("/uploads/logos/")) {
            String filename = logoUrl.substring("/uploads/logos/".length());
            File file = new File("uploads/logos", filename);
            if (file.exists() && file.isFile()) {
                file.delete();
            }
        }
    }
}

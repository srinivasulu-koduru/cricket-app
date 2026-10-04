package com.cricketapp.service;

import com.cricketapp.dto.ProfileResponseDto;
import com.cricketapp.dto.PublicPlayerProfileResponseDto;
import com.cricketapp.dto.UpdateProfileRequestDto;
import com.cricketapp.entity.MemberStatus;
import com.cricketapp.entity.PlayerProfile;
import com.cricketapp.entity.Team;
import com.cricketapp.entity.TeamMember;
import com.cricketapp.entity.User;
import com.cricketapp.exception.AuthException;
import com.cricketapp.repository.PlayerProfileRepository;
import com.cricketapp.repository.TeamMemberRepository;
import com.cricketapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif", "image/pjpeg", "image/x-png", "application/octet-stream"
    );

    private final UserRepository userRepository;
    private final PlayerProfileRepository profileRepository;
    private final TeamMemberRepository memberRepository;
    private final com.cricketapp.repository.MatchRepository matchRepository;
    private final com.cricketapp.repository.InningsRepository inningsRepository;
    private final com.cricketapp.repository.BallEventRepository ballEventRepository;
    private final com.cricketapp.repository.MatchPlayingXiRepository matchPlayingXiRepository;

    public ProfileService(UserRepository userRepository,
                          PlayerProfileRepository profileRepository,
                          TeamMemberRepository memberRepository,
                          com.cricketapp.repository.MatchRepository matchRepository,
                          com.cricketapp.repository.InningsRepository inningsRepository,
                          com.cricketapp.repository.BallEventRepository ballEventRepository,
                          com.cricketapp.repository.MatchPlayingXiRepository matchPlayingXiRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.memberRepository = memberRepository;
        this.matchRepository = matchRepository;
        this.inningsRepository = inningsRepository;
        this.ballEventRepository = ballEventRepository;
        this.matchPlayingXiRepository = matchPlayingXiRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponseDto getProfile(String email) {
        User user = findUserByEmail(email);
        PlayerProfile profile = getOrCreateProfile(user);
        return mapToDto(profile);
    }

    @Transactional
    public ProfileResponseDto updateProfile(String email, UpdateProfileRequestDto request) {
        User user = findUserByEmail(email);
        PlayerProfile profile = getOrCreateProfile(user);

        // Validations
        if (!StringUtils.hasText(request.getName())) {
            throw new AuthException("Full name cannot be blank");
        }
        String trimmedName = request.getName().trim();
        if (trimmedName.length() > 100) {
            throw new AuthException("Full name cannot exceed 100 characters");
        }

        if (request.getDateOfBirth() != null && request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new AuthException("Date of birth cannot be a future date");
        }

        if (request.getLocation() != null && request.getLocation().trim().length() > 100) {
            throw new AuthException("Location cannot exceed 100 characters");
        }

        if (request.getBio() != null && request.getBio().trim().length() > 500) {
            throw new AuthException("Bio cannot exceed 500 characters");
        }

        // Synchronize name on User entity
        user.setName(trimmedName);
        userRepository.save(user);

        // Update profile fields
        profile.setDisplayName(trimmedName);
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setLocation(request.getLocation() != null ? request.getLocation().trim() : null);
        profile.setPlayingRole(request.getPlayingRole());
        profile.setBattingStyle(request.getBattingStyle());
        profile.setBowlingStyle(request.getBowlingStyle());
        profile.setBio(request.getBio() != null ? request.getBio().trim() : null);

        PlayerProfile updated = profileRepository.save(profile);
        return mapToDto(updated);
    }

    @Transactional
    public ProfileResponseDto uploadProfilePhoto(String email, MultipartFile file) {
        User user = findUserByEmail(email);
        PlayerProfile profile = getOrCreateProfile(user);

        if (file == null || file.isEmpty()) {
            throw new AuthException("Please select an image file to upload");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AuthException("Profile photo size exceeds maximum limit of 5 MB");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);

        boolean isValidType = (contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase()))
                || (contentType != null && contentType.toLowerCase().startsWith("image/"))
                || isAllowedExtension(extension);

        if (!isValidType) {
            throw new AuthException("Invalid file type. Only JPG, JPEG, PNG, and WEBP images are allowed.");
        }

        String newFilename = UUID.randomUUID().toString() + extension;
        File uploadDir = new File("uploads/profiles");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        File destFile = new File(uploadDir, newFilename);

        try {
            Files.copy(file.getInputStream(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Failed to save profile photo for user {}: {}", email, e.getMessage());
            throw new AuthException("Failed to upload profile photo. Please try again.");
        }

        deletePhysicalFile(profile.getProfilePhotoUrl());

        profile.setProfilePhotoUrl("/uploads/profiles/" + newFilename);
        PlayerProfile updated = profileRepository.save(profile);
        return mapToDto(updated);
    }

    @Transactional
    public ProfileResponseDto deleteProfilePhoto(String email) {
        User user = findUserByEmail(email);
        PlayerProfile profile = getOrCreateProfile(user);

        if (profile.getProfilePhotoUrl() != null) {
            deletePhysicalFile(profile.getProfilePhotoUrl());
            profile.setProfilePhotoUrl(null);
            profileRepository.save(profile);
        }

        return mapToDto(profile);
    }

    @Transactional(readOnly = true)
    public PublicPlayerProfileResponseDto getPublicPlayerProfile(String userId) {
        String trimmed = userId != null ? userId.trim() : "";
        java.util.Optional<PlayerProfile> profileOpt = profileRepository.findByUser_UserId(trimmed);
        if (profileOpt.isEmpty()) {
            profileOpt = profileRepository.findByUser_UserId(trimmed.toUpperCase());
        }
        if (profileOpt.isEmpty()) {
            profileOpt = profileRepository.findByUser_Email(trimmed.toLowerCase());
        }
        PlayerProfile profile = profileOpt
                .orElseThrow(() -> new AuthException("Player profile not found for Cricket User ID: " + userId));

        User user = profile.getUser();
        PublicPlayerProfileResponseDto response = new PublicPlayerProfileResponseDto(
                user.getUserId(),
                profile.getDisplayName() != null ? profile.getDisplayName() : user.getName(),
                profile.getProfilePhotoUrl(),
                profile.getLocation(),
                profile.getPlayingRole(),
                profile.getBattingStyle(),
                profile.getBowlingStyle(),
                profile.getBio()
        );

        response.setDateOfBirth(profile.getDateOfBirth());
        response.setGender(profile.getGender());

        // Fetch real active team memberships for this player
        List<TeamMember> activeMemberships = memberRepository.findByUser_UserIdAndStatus(user.getUserId(), MemberStatus.ACTIVE);
        List<PublicPlayerProfileResponseDto.PlayerTeamDto> teamDtos = activeMemberships.stream().map(m -> {
            Team t = m.getTeam();
            return new PublicPlayerProfileResponseDto.PlayerTeamDto(
                    t.getTeamId(),
                    t.getName(),
                    t.getLogoUrl(),
                    m.getRole() != null ? m.getRole().name() : "MEMBER",
                    m.getJoinedAt() != null ? m.getJoinedAt().toLocalDate() : null
            );
        }).collect(Collectors.toList());

        response.setTeams(teamDtos);
        populateCareerStats(user, response, null);

        return response;
    }

    @Transactional(readOnly = true)
    public List<PublicPlayerProfileResponseDto> searchPlayers(String query) {
        if (!StringUtils.hasText(query)) {
            return Collections.emptyList();
        }
        String trimmed = query.trim();
        List<PlayerProfile> profiles = profileRepository.searchPlayers(trimmed, PageRequest.of(0, 30));

        return profiles.stream().map(profile -> {
            User user = profile.getUser();
            PublicPlayerProfileResponseDto pDto = new PublicPlayerProfileResponseDto(
                    user.getUserId(),
                    profile.getDisplayName() != null ? profile.getDisplayName() : user.getName(),
                    profile.getProfilePhotoUrl(),
                    profile.getLocation(),
                    profile.getPlayingRole(),
                    profile.getBattingStyle(),
                    profile.getBowlingStyle(),
                    profile.getBio()
            );
            populateCareerStats(user, pDto, null);
            return pDto;
        }).collect(Collectors.toList());
    }

    private User findUserByEmail(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthException("User account not found"));
    }

    private PlayerProfile getOrCreateProfile(User user) {
        return profileRepository.findByUser(user)
                .orElseGet(() -> {
                    PlayerProfile newProfile = new PlayerProfile(user);
                    return profileRepository.save(newProfile);
                });
    }

    private ProfileResponseDto mapToDto(PlayerProfile profile) {
        User user = profile.getUser();
        ProfileResponseDto dto = new ProfileResponseDto(
                user.getUserId(),
                profile.getDisplayName() != null ? profile.getDisplayName() : user.getName(),
                user.getEmail(),
                profile.getProfilePhotoUrl(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getLocation(),
                profile.getPlayingRole(),
                profile.getBattingStyle(),
                profile.getBowlingStyle(),
                profile.getBio()
        );
        populateCareerStats(user, null, dto);
        return dto;
    }

    private void populateCareerStats(User user, PublicPlayerProfileResponseDto publicDto, ProfileResponseDto privateDto) {
        PublicPlayerProfileResponseDto.BattingStatsDto batting = new PublicPlayerProfileResponseDto.BattingStatsDto();
        PublicPlayerProfileResponseDto.BowlingStatsDto bowling = new PublicPlayerProfileResponseDto.BowlingStatsDto();
        PublicPlayerProfileResponseDto.FieldingStatsDto fielding = new PublicPlayerProfileResponseDto.FieldingStatsDto();
        List<PublicPlayerProfileResponseDto.MatchHistoryDto> matchHistory = new java.util.ArrayList<>();

        List<com.cricketapp.entity.Match> completedMatches = matchRepository.findCompletedMatchesForUser(user);

        int totalMatches = completedMatches.size();
        batting.setMatches(totalMatches);
        bowling.setMatches(totalMatches);

        int battingInningsCount = 0;
        int totalBattingRuns = 0;
        int totalBallsFaced = 0;
        int totalFours = 0;
        int totalSixes = 0;
        int totalNotOuts = 0;
        int totalFifties = 0;
        int totalHundreds = 0;
        int highestScoreVal = 0;
        boolean highestScoreNotOut = false;

        int bowlingInningsCount = 0;
        int totalBallsBowled = 0;
        int totalRunsConceded = 0;
        int totalWickets = 0;
        int totalFourWickets = 0;
        int totalFiveWickets = 0;
        int totalMaidens = 0;
        int bestWicketsInnings = -1;
        int bestRunsInnings = 9999;

        int totalCatches = 0;
        int totalRunOuts = 0;
        int totalStumpings = 0;

        for (com.cricketapp.entity.Match match : completedMatches) {
            List<com.cricketapp.entity.Innings> inningsList = inningsRepository.findByMatchOrderByInningsNumberAsc(match);

            int matchRuns = 0;
            int matchBalls = 0;
            int matchWickets = 0;
            int matchBallsBowled = 0;
            int matchCatches = 0;

            for (com.cricketapp.entity.Innings innings : inningsList) {
                List<com.cricketapp.entity.BallEvent> ballEvents = ballEventRepository.findByInningsOrderByOverNumberAscBallNumberAsc(innings);

                boolean playerBattedInInnings = false;
                int innRuns = 0;
                int innBalls = 0;
                boolean innDismissed = false;

                boolean playerBowledInInnings = false;
                int innBallsBowled = 0;
                int innRunsConceded = 0;
                int innWickets = 0;

                java.util.Map<Integer, List<com.cricketapp.entity.BallEvent>> bowlerOverEvents = new java.util.HashMap<>();

                for (com.cricketapp.entity.BallEvent be : ballEvents) {
                    if (be.getStriker() != null && be.getStriker().getId().equals(user.getId())) {
                        playerBattedInInnings = true;
                        innRuns += (be.getRunsScored() != null ? be.getRunsScored() : 0);
                        if (!"WIDE".equalsIgnoreCase(be.getExtraType())) {
                            innBalls++;
                        }
                        if (be.getRunsScored() != null && be.getRunsScored() == 4) {
                            totalFours++;
                        } else if (be.getRunsScored() != null && be.getRunsScored() == 6) {
                            totalSixes++;
                        }
                    }

                    if (be.getNonStriker() != null && be.getNonStriker().getId().equals(user.getId())) {
                        playerBattedInInnings = true;
                    }

                    if (be.isWicket() && be.getDismissedUser() != null && be.getDismissedUser().getId().equals(user.getId())) {
                        playerBattedInInnings = true;
                        innDismissed = true;
                    }

                    if (be.getBowler() != null && be.getBowler().getId().equals(user.getId())) {
                        playerBowledInInnings = true;
                        if (!"WIDE".equalsIgnoreCase(be.getExtraType()) && !"NO_BALL".equalsIgnoreCase(be.getExtraType())) {
                            innBallsBowled++;
                        }
                        int runsOnBall = (be.getRunsScored() != null ? be.getRunsScored() : 0);
                        if (!"BYE".equalsIgnoreCase(be.getExtraType()) && !"LEG_BYE".equalsIgnoreCase(be.getExtraType())) {
                            runsOnBall += (be.getExtraRuns() != null ? be.getExtraRuns() : 0);
                        }
                        innRunsConceded += runsOnBall;

                        if (be.isWicket()) {
                            String wt = be.getWicketType() != null ? be.getWicketType().toUpperCase() : "";
                            if (!wt.contains("RUN_OUT") && !wt.contains("RETIRED") && !wt.contains("TIMED_OUT")) {
                                innWickets++;
                            }
                        }

                        bowlerOverEvents.computeIfAbsent(be.getOverNumber(), k -> new java.util.ArrayList<>()).add(be);
                    }

                    if (be.getFielder() != null && be.getFielder().getId().equals(user.getId())) {
                        String wt = be.getWicketType() != null ? be.getWicketType().toUpperCase() : "";
                        if (wt.contains("CAUGHT")) {
                            totalCatches++;
                            matchCatches++;
                        } else if (wt.contains("STUMPED")) {
                            totalStumpings++;
                        } else if (wt.contains("RUN_OUT")) {
                            totalRunOuts++;
                        }
                    } else if (be.isWicket() && be.getBowler() != null && be.getBowler().getId().equals(user.getId()) && be.getFielder() == null) {
                        String wt = be.getWicketType() != null ? be.getWicketType().toUpperCase() : "";
                        if (wt.contains("CAUGHT")) {
                            totalCatches++;
                            matchCatches++;
                        }
                    }
                }

                if (playerBattedInInnings) {
                    battingInningsCount++;
                    totalBattingRuns += innRuns;
                    totalBallsFaced += innBalls;
                    matchRuns += innRuns;
                    matchBalls += innBalls;

                    if (!innDismissed) {
                        totalNotOuts++;
                    }

                    if (innRuns >= 100) {
                        totalHundreds++;
                    } else if (innRuns >= 50) {
                        totalFifties++;
                    }

                    if (innRuns > highestScoreVal) {
                        highestScoreVal = innRuns;
                        highestScoreNotOut = !innDismissed;
                    } else if (innRuns == highestScoreVal && !innDismissed) {
                        highestScoreNotOut = true;
                    }
                }

                if (playerBowledInInnings) {
                    bowlingInningsCount++;
                    totalBallsBowled += innBallsBowled;
                    totalRunsConceded += innRunsConceded;
                    totalWickets += innWickets;
                    matchWickets += innWickets;
                    matchBallsBowled += innBallsBowled;

                    if (innWickets >= 5) {
                        totalFiveWickets++;
                    } else if (innWickets == 4) {
                        totalFourWickets++;
                    }

                    if (innWickets > bestWicketsInnings || (innWickets == bestWicketsInnings && innRunsConceded < bestRunsInnings)) {
                        bestWicketsInnings = innWickets;
                        bestRunsInnings = innRunsConceded;
                    }

                    for (java.util.Map.Entry<Integer, List<com.cricketapp.entity.BallEvent>> overEntry : bowlerOverEvents.entrySet()) {
                        List<com.cricketapp.entity.BallEvent> overBalls = overEntry.getValue();
                        int legalCount = 0;
                        int overRunsConceded = 0;
                        for (com.cricketapp.entity.BallEvent be : overBalls) {
                            if (!"WIDE".equalsIgnoreCase(be.getExtraType()) && !"NO_BALL".equalsIgnoreCase(be.getExtraType())) {
                                legalCount++;
                            }
                            if (!"BYE".equalsIgnoreCase(be.getExtraType()) && !"LEG_BYE".equalsIgnoreCase(be.getExtraType())) {
                                overRunsConceded += (be.getRunsScored() != null ? be.getRunsScored() : 0) + (be.getExtraRuns() != null ? be.getExtraRuns() : 0);
                            }
                        }
                        if (legalCount == 6 && overRunsConceded == 0) {
                            totalMaidens++;
                        }
                    }
                }
            }

            PublicPlayerProfileResponseDto.MatchHistoryDto mDto = new PublicPlayerProfileResponseDto.MatchHistoryDto();
            mDto.setMatchId(match.getMatchId());
            String nameA = match.getTeamA() != null ? match.getTeamA().getName() : "Team A";
            String nameB = match.getTeamB() != null ? match.getTeamB().getName() : "Team B";
            mDto.setMatchName(nameA + " vs " + nameB);
            mDto.setDate(match.getCreatedAt() != null ? match.getCreatedAt().toLocalDate() : null);
            mDto.setFormat(match.getOvers() != null ? match.getOvers() + " Overs" : "Limited Overs");

            java.util.Optional<com.cricketapp.entity.MatchPlayingXi> xiOpt = matchPlayingXiRepository.findByMatchAndUser(match, user);
            boolean isTeamA = true;
            if (xiOpt.isPresent() && match.getTeamB() != null) {
                isTeamA = !xiOpt.get().getTeam().getId().equals(match.getTeamB().getId());
            } else if (match.getTeamB() != null) {
                boolean inTeamB = memberRepository.findByTeamAndUser(match.getTeamB(), user).isPresent();
                if (inTeamB) isTeamA = false;
            }

            mDto.setTeamName(isTeamA ? nameA : nameB);
            mDto.setOpponentName(isTeamA ? nameB : nameA);
            mDto.setResult(match.getStatus() == com.cricketapp.entity.MatchStatus.COMPLETED ? "Completed" : (match.getStatus() != null ? match.getStatus().name() : "Completed"));

            mDto.setRuns(matchRuns);
            mDto.setBalls(matchBalls);
            mDto.setWickets(matchWickets);
            double matchOvers = (matchBallsBowled / 6) + (matchBallsBowled % 6) / 10.0;
            mDto.setOvers(matchOvers);
            mDto.setCatches(matchCatches);

            matchHistory.add(mDto);
        }

        batting.setInnings(battingInningsCount);
        batting.setRuns(totalBattingRuns);
        batting.setBallsFaced(totalBallsFaced);
        batting.setFours(totalFours);
        batting.setSixes(totalSixes);
        batting.setNotOuts(totalNotOuts);
        batting.setFifties(totalFifties);
        batting.setHundreds(totalHundreds);
        batting.setHighestScore(highestScoreVal > 0 ? (highestScoreVal + (highestScoreNotOut ? "*" : "")) : "0");
        if (battingInningsCount - totalNotOuts > 0) {
            batting.setAverage(Math.round((totalBattingRuns / (double) (battingInningsCount - totalNotOuts)) * 100.0) / 100.0);
        } else if (totalBattingRuns > 0) {
            batting.setAverage((double) totalBattingRuns);
        }
        if (totalBallsFaced > 0) {
            batting.setStrikeRate(Math.round(((totalBattingRuns * 100.0) / totalBallsFaced) * 100.0) / 100.0);
        }

        bowling.setInnings(bowlingInningsCount);
        bowling.setBalls(totalBallsBowled);
        double totalOversVal = (totalBallsBowled / 6) + (totalBallsBowled % 6) / 10.0;
        bowling.setOvers(totalOversVal);
        bowling.setRunsConceded(totalRunsConceded);
        bowling.setWickets(totalWickets);
        bowling.setFourWickets(totalFourWickets);
        bowling.setFiveWickets(totalFiveWickets);
        bowling.setMaidens(totalMaidens);
        if (bestWicketsInnings >= 0) {
            bowling.setBestBowling(bestWicketsInnings + "/" + bestRunsInnings);
        }
        if (totalBallsBowled > 0) {
            bowling.setEconomy(Math.round(((totalRunsConceded * 6.0) / totalBallsBowled) * 100.0) / 100.0);
        }
        if (totalWickets > 0) {
            bowling.setAverage(Math.round((totalRunsConceded / (double) totalWickets) * 100.0) / 100.0);
        }

        fielding.setCatches(totalCatches);
        fielding.setRunOuts(totalRunOuts);
        fielding.setStumpings(totalStumpings);

        // Real Achievements Calculation
        List<PublicPlayerProfileResponseDto.AchievementDto> achievements = new java.util.ArrayList<>();

        if (!completedMatches.isEmpty()) {
            com.cricketapp.entity.Match debutMatch = completedMatches.get(completedMatches.size() - 1);
            java.util.Optional<com.cricketapp.entity.MatchPlayingXi> xiOpt = matchPlayingXiRepository.findByMatchAndUser(debutMatch, user);
            boolean isTeamA = true;
            if (xiOpt.isPresent() && debutMatch.getTeamB() != null) {
                isTeamA = !xiOpt.get().getTeam().getId().equals(debutMatch.getTeamB().getId());
            }
            String oppName = isTeamA
                    ? (debutMatch.getTeamB() != null ? debutMatch.getTeamB().getName() : "Opponent")
                    : (debutMatch.getTeamA() != null ? debutMatch.getTeamA().getName() : "Opponent");
            achievements.add(new PublicPlayerProfileResponseDto.AchievementDto(
                    "Cricket Debut",
                    "Played first competitive match vs " + oppName,
                    "NAVY",
                    debutMatch.getCreatedAt() != null ? debutMatch.getCreatedAt().toLocalDate() : null
            ));
        }

        for (PublicPlayerProfileResponseDto.MatchHistoryDto m : matchHistory) {
            if (m.getRuns() != null && m.getRuns() >= 100) {
                achievements.add(new PublicPlayerProfileResponseDto.AchievementDto(
                        "Centurion",
                        "Scored " + m.getRuns() + " runs vs " + m.getOpponentName(),
                        "GOLD",
                        m.getDate()
                ));
            } else if (m.getRuns() != null && m.getRuns() >= 50) {
                achievements.add(new PublicPlayerProfileResponseDto.AchievementDto(
                        "Half-Century Special",
                        "Scored " + m.getRuns() + " runs vs " + m.getOpponentName(),
                        "EMERALD",
                        m.getDate()
                ));
            }

            if (m.getWickets() != null && m.getWickets() >= 5) {
                achievements.add(new PublicPlayerProfileResponseDto.AchievementDto(
                        "5-Wicket Haul",
                        "Took " + m.getWickets() + " wickets vs " + m.getOpponentName(),
                        "GOLD",
                        m.getDate()
                ));
            } else if (m.getWickets() != null && m.getWickets() == 4) {
                achievements.add(new PublicPlayerProfileResponseDto.AchievementDto(
                        "4-Wicket Haul",
                        "Took 4 wickets vs " + m.getOpponentName(),
                        "NAVY",
                        m.getDate()
                ));
            }

            if (m.getCatches() != null && m.getCatches() >= 3) {
                achievements.add(new PublicPlayerProfileResponseDto.AchievementDto(
                        "Safe Hands",
                        "Took " + m.getCatches() + " catches vs " + m.getOpponentName(),
                        "EMERALD",
                        m.getDate()
                ));
            }

            if (m.getResult() != null && m.getResult().toLowerCase().contains("won") && ( (m.getRuns() != null && m.getRuns() >= 25) || (m.getWickets() != null && m.getWickets() >= 2) )) {
                achievements.add(new PublicPlayerProfileResponseDto.AchievementDto(
                        "Match Winner",
                        "Winning performance vs " + m.getOpponentName(),
                        "GOLD",
                        m.getDate()
                ));
            }
        }

        if (totalBattingRuns >= 500) {
            achievements.add(new PublicPlayerProfileResponseDto.AchievementDto(
                    "500 Career Runs",
                    "Reached 500 total career runs milestone",
                    "GOLD",
                    java.time.LocalDate.now()
            ));
        } else if (totalBattingRuns >= 100) {
            achievements.add(new PublicPlayerProfileResponseDto.AchievementDto(
                    "100 Career Runs",
                    "Reached 100 total career runs milestone",
                    "EMERALD",
                    java.time.LocalDate.now()
            ));
        }

        if (totalWickets >= 25) {
            achievements.add(new PublicPlayerProfileResponseDto.AchievementDto(
                    "25 Career Wickets",
                    "Took 25 total career wickets milestone",
                    "GOLD",
                    java.time.LocalDate.now()
            ));
        } else if (totalWickets >= 10) {
            achievements.add(new PublicPlayerProfileResponseDto.AchievementDto(
                    "10 Career Wickets",
                    "Took 10 total career wickets milestone",
                    "EMERALD",
                    java.time.LocalDate.now()
            ));
        }

        if (publicDto != null) {
            publicDto.setBatting(batting);
            publicDto.setBowling(bowling);
            publicDto.setFielding(fielding);
            publicDto.setMatchHistory(matchHistory);
            publicDto.setRecentMatches(matchHistory);
            publicDto.setAchievements(achievements);
        }
        if (privateDto != null) {
            privateDto.setBatting(batting);
            privateDto.setBowling(bowling);
            privateDto.setFielding(fielding);
            privateDto.setMatchHistory(matchHistory);
            privateDto.setAchievements(achievements);
        }
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

    private void deletePhysicalFile(String photoUrl) {
        if (StringUtils.hasText(photoUrl) && photoUrl.startsWith("/uploads/profiles/")) {
            String filename = photoUrl.substring("/uploads/profiles/".length());
            File file = new File("uploads/profiles", filename);
            if (file.exists() && file.isFile()) {
                file.delete();
            }
        }
    }
}

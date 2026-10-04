package com.cricketapp.dto;

import com.cricketapp.entity.InningsStatus;
import com.cricketapp.entity.MatchStatus;
import com.cricketapp.entity.TossDecision;

import java.util.ArrayList;
import java.util.List;

public class ScoringDashboardStateDto {

    private String matchId;
    private String matchName;
    private String venue;
    private String matchDate;
    private String matchTime;
    private String format;
    private Integer overs;
    private MatchStatus status;

    private MatchResponseDto.TeamSummaryDto teamA;
    private MatchResponseDto.TeamSummaryDto teamB;

    private String scorerUserId;
    private String scorerName;
    private boolean isAuthorizedScorer;

    private MatchChecklistDto checklist;

    // Toss info
    private Boolean tossRecorded;
    private String tossWinnerTeamId;
    private String tossWinnerTeamName;
    private TossDecision tossDecision;
    private String tossSummary;

    // Active Innings info
    private Integer activeInningsNumber;
    private InningsStatus inningsStatus;
    private Boolean isInningsBreak = false;
    private String inningsBreakSummary;
    private String battingTeamId;
    private String battingTeamName;
    private String bowlingTeamId;
    private String bowlingTeamName;

    // First Innings Summary (for Innings Break and 2nd Innings Target calculation)
    private String firstInningsTeamName;
    private Integer firstInningsRuns = 0;
    private Integer firstInningsWickets = 0;
    private Integer firstInningsOvers = 0;
    private Integer firstInningsBalls = 0;

    private Integer totalRuns = 0;
    private Integer totalWickets = 0;
    private Integer completedOvers = 0;
    private Integer currentBalls = 0;
    private Double runRate = 0.0;

    // Additional Match Dashboard Metrics
    private Integer targetRuns;
    private Integer requiredRuns;
    private String targetEquation;
    private Integer partnershipRuns = 0;
    private Integer partnershipBalls = 0;
    private Double last5OversRunRate = 0.0;
    private Double requiredRunRate = 0.0;
    private Integer ballsRemaining = 0;
    private Integer totalExtras = 0;
    private Integer fallOfWicketsCount = 0;
    private Integer currentInningsTotalFours = 0;
    private Integer currentInningsTotalSixes = 0;
    private List<FallOfWicketDto> currentInningsFallOfWickets = new ArrayList<>();

    // Match Pause details
    private Boolean isPaused = false;
    private String pauseReason;
    private String pausedAt;

    // State triggers
    private Boolean isOverCompleted = false;
    private Boolean isWicketPendingNextBatter = false;
    private String previousBowlerUserId;

    // Current Batters, Bowler & Wicket Keeper
    private PlayerSummaryDto striker;
    private PlayerSummaryDto nonStriker;
    private PlayerSummaryDto currentBowler;
    private PlayerSummaryDto currentWicketKeeper;
    private String bowlingEnd = "END_A";
    private PlayerSummaryDto battingEndA;
    private PlayerSummaryDto battingEndB;

    // Celebration event triggers
    private String latestCelebrationType;
    private String latestCelebrationId;

    // Squad XIs & Filtered Lists for Selections
    private List<PlayerSummaryDto> battingPlayingXi = new ArrayList<>();
    private List<PlayerSummaryDto> bowlingPlayingXi = new ArrayList<>();
    private List<PlayerSummaryDto> availableBowlers = new ArrayList<>();
    private List<PlayerSummaryDto> availableBatters = new ArrayList<>();

    // Ball logs
    private List<BallEventDto> thisOverBallEvents = new ArrayList<>();
    private List<String> overSummaryBalls = new ArrayList<>();

    // Recent Overs & Live Commentary for Viewer
    private List<RecentOverDto> recentOvers = new ArrayList<>();
    private List<CommentaryDto> commentary = new ArrayList<>();
    private Long sequenceNumber = System.currentTimeMillis();
    private String updatedAt = java.time.LocalDateTime.now().toString();

    // Match Rules / Settings
    private Boolean wideRunEnabled = true;
    private Boolean noBallRunEnabled = true;
    private Boolean noBallFreeHitEnabled = true;
    private Boolean byeRunEnabled = true;
    private Boolean legByeRunEnabled = true;
    private Integer maxOversPerBowler;
    private Boolean allowConsecutiveOvers = false;

    // Completed Match Details & Full Scorecards
    private Boolean isMatchCompleted = false;
    private String matchResultSummary;
    private String winnerTeamId;
    private String winnerTeamName;
    private String resultMargin;
    private String completedAt;
    private InningsScorecardDto innings1Scorecard;
    private InningsScorecardDto innings2Scorecard;

    public ScoringDashboardStateDto() {
    }

    public static class RecentOverDto {
        private Integer overNumber;
        private List<String> balls = new ArrayList<>();
        private Integer runs = 0;
        private Integer runsConceded;
        private String summaryText;

        public RecentOverDto() {}
        public RecentOverDto(Integer overNumber, List<String> balls, Integer runs, String summaryText) {
            this.overNumber = overNumber;
            this.balls = balls;
            this.runs = runs;
            this.runsConceded = runs;
            this.summaryText = summaryText;
        }

        public Integer getOverNumber() { return overNumber; }
        public void setOverNumber(Integer overNumber) { this.overNumber = overNumber; }
        public List<String> getBalls() { return balls; }
        public void setBalls(List<String> balls) { this.balls = balls; }
        public Integer getRuns() { return runs; }
        public void setRuns(Integer runs) {
            this.runs = runs;
            if (this.runsConceded == null) this.runsConceded = runs;
        }
        public Integer getRunsConceded() { return runsConceded != null ? runsConceded : runs; }
        public void setRunsConceded(Integer runsConceded) { this.runsConceded = runsConceded; }
        public String getSummaryText() { return summaryText; }
        public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    }

    public static class CommentaryDto {
        private String overBall;
        private String overNumber;
        private String title;
        private String badgeText;
        private String bowlerToBatter;
        private String description;

        public CommentaryDto() {}
        public CommentaryDto(String overBall, String title, String bowlerToBatter, String description) {
            this.overBall = overBall;
            this.overNumber = overBall;
            this.title = title;
            this.badgeText = title;
            this.bowlerToBatter = bowlerToBatter;
            this.description = description;
        }

        public String getOverBall() { return overBall; }
        public void setOverBall(String overBall) {
            this.overBall = overBall;
            if (this.overNumber == null) this.overNumber = overBall;
        }
        public String getOverNumber() { return overNumber != null ? overNumber : overBall; }
        public void setOverNumber(String overNumber) { this.overNumber = overNumber; }
        public String getTitle() { return title; }
        public void setTitle(String title) {
            this.title = title;
            if (this.badgeText == null) this.badgeText = title;
        }
        public String getBadgeText() { return badgeText != null ? badgeText : title; }
        public void setBadgeText(String badgeText) { this.badgeText = badgeText; }
        public String getBowlerToBatter() { return bowlerToBatter; }
        public void setBowlerToBatter(String bowlerToBatter) { this.bowlerToBatter = bowlerToBatter; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class InningsScorecardDto {
        private Integer inningsNumber;
        private String battingTeamName;
        private String bowlingTeamName;
        private Integer totalRuns = 0;
        private Integer totalWickets = 0;
        private Integer completedOvers = 0;
        private Integer currentBalls = 0;
        private Integer totalExtras = 0;
        private List<PlayerSummaryDto> battingList = new ArrayList<>();
        private List<PlayerSummaryDto> bowlingList = new ArrayList<>();
        private Integer totalFours = 0;
        private Integer totalSixes = 0;
        private List<FallOfWicketDto> fallOfWickets = new ArrayList<>();

        public InningsScorecardDto() {}

        public Integer getInningsNumber() { return inningsNumber; }
        public void setInningsNumber(Integer inningsNumber) { this.inningsNumber = inningsNumber; }
        public String getBattingTeamName() { return battingTeamName; }
        public void setBattingTeamName(String battingTeamName) { this.battingTeamName = battingTeamName; }
        public String getBowlingTeamName() { return bowlingTeamName; }
        public void setBowlingTeamName(String bowlingTeamName) { this.bowlingTeamName = bowlingTeamName; }
        public Integer getTotalRuns() { return totalRuns; }
        public void setTotalRuns(Integer totalRuns) { this.totalRuns = totalRuns; }
        public Integer getTotalWickets() { return totalWickets; }
        public void setTotalWickets(Integer totalWickets) { this.totalWickets = totalWickets; }
        public Integer getCompletedOvers() { return completedOvers; }
        public void setCompletedOvers(Integer completedOvers) { this.completedOvers = completedOvers; }
        public Integer getCurrentBalls() { return currentBalls; }
        public void setCurrentBalls(Integer currentBalls) { this.currentBalls = currentBalls; }
        public Integer getTotalExtras() { return totalExtras; }
        public void setTotalExtras(Integer totalExtras) { this.totalExtras = totalExtras; }
        public List<PlayerSummaryDto> getBattingList() { return battingList; }
        public void setBattingList(List<PlayerSummaryDto> battingList) { this.battingList = battingList; }
        public List<PlayerSummaryDto> getBowlingList() { return bowlingList; }
        public void setBowlingList(List<PlayerSummaryDto> bowlingList) { this.bowlingList = bowlingList; }
        public Integer getTotalFours() { return totalFours; }
        public void setTotalFours(Integer totalFours) { this.totalFours = totalFours; }
        public Integer getTotalSixes() { return totalSixes; }
        public void setTotalSixes(Integer totalSixes) { this.totalSixes = totalSixes; }
        public List<FallOfWicketDto> getFallOfWickets() { return fallOfWickets; }
        public void setFallOfWickets(List<FallOfWicketDto> fallOfWickets) { this.fallOfWickets = fallOfWickets; }
    }

    public static class FallOfWicketDto {
        private Integer wicketNumber;
        private Integer teamRuns;
        private String playerName;
        private String overBall;

        public FallOfWicketDto() {}
        public FallOfWicketDto(Integer wicketNumber, Integer teamRuns, String playerName, String overBall) {
            this.wicketNumber = wicketNumber;
            this.teamRuns = teamRuns;
            this.playerName = playerName;
            this.overBall = overBall;
        }

        public Integer getWicketNumber() { return wicketNumber; }
        public void setWicketNumber(Integer wicketNumber) { this.wicketNumber = wicketNumber; }
        public Integer getTeamRuns() { return teamRuns; }
        public void setTeamRuns(Integer teamRuns) { this.teamRuns = teamRuns; }
        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
        public String getOverBall() { return overBall; }
        public void setOverBall(String overBall) { this.overBall = overBall; }
    }

    public static class PlayerSummaryDto {
        private String userId;
        private String name;
        private String profilePhotoUrl;
        private String playingRole;
        private String battingStyle;
        private String bowlingStyle;
        private Integer runs = 0;
        private Integer balls = 0;
        private Integer fours = 0;
        private Integer sixes = 0;
        private Double strikeRate = 0.0;
        private Integer oversBowled = 0;
        private Integer ballsBowled = 0;
        private Integer runsConceded = 0;
        private Integer wicketsTaken = 0;
        private Integer maidens = 0;
        private Double economyRate = 0.0;
        private Boolean isOut = false;
        private Boolean isWicketKeeper = false;
        private String dismissalType;
        private String dismissalText;
        private Boolean played = false;
        private Boolean hasBatted = false;

        public PlayerSummaryDto() {
        }

        public PlayerSummaryDto(String userId, String name, String profilePhotoUrl, String playingRole) {
            this.userId = userId;
            this.name = name;
            this.profilePhotoUrl = profilePhotoUrl;
            this.playingRole = playingRole;
            this.battingStyle = "Right-hand bat";
            this.bowlingStyle = "Right-arm fast";
            this.runs = 0;
            this.balls = 0;
            this.fours = 0;
            this.sixes = 0;
            this.strikeRate = 0.0;
            this.oversBowled = 0;
            this.ballsBowled = 0;
            this.runsConceded = 0;
            this.wicketsTaken = 0;
            this.maidens = 0;
            this.economyRate = 0.0;
            this.isOut = false;
            this.isWicketKeeper = false;
            this.played = false;
            this.hasBatted = false;
        }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getProfilePhotoUrl() { return profilePhotoUrl; }
        public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
        public String getPlayingRole() { return playingRole; }
        public void setPlayingRole(String playingRole) { this.playingRole = playingRole; }
        public String getBattingStyle() { return battingStyle; }
        public void setBattingStyle(String battingStyle) { this.battingStyle = battingStyle; }
        public String getBowlingStyle() { return bowlingStyle; }
        public void setBowlingStyle(String bowlingStyle) { this.bowlingStyle = bowlingStyle; }
        public Integer getRuns() { return runs; }
        public void setRuns(Integer runs) { this.runs = runs; }
        public Integer getBalls() { return balls; }
        public void setBalls(Integer balls) { this.balls = balls; }
        public Integer getFours() { return fours; }
        public void setFours(Integer fours) { this.fours = fours; }
        public Integer getSixes() { return sixes; }
        public void setSixes(Integer sixes) { this.sixes = sixes; }
        public Double getStrikeRate() { return strikeRate; }
        public void setStrikeRate(Double strikeRate) { this.strikeRate = strikeRate; }
        public Integer getOversBowled() { return oversBowled; }
        public void setOversBowled(Integer oversBowled) { this.oversBowled = oversBowled; }
        public Integer getBallsBowled() { return ballsBowled; }
        public void setBallsBowled(Integer ballsBowled) { this.ballsBowled = ballsBowled; }
        public Integer getRunsConceded() { return runsConceded; }
        public void setRunsConceded(Integer runsConceded) { this.runsConceded = runsConceded; }
        public Integer getWicketsTaken() { return wicketsTaken; }
        public void setWicketsTaken(Integer wicketsTaken) { this.wicketsTaken = wicketsTaken; }
        public Integer getMaidens() { return maidens; }
        public void setMaidens(Integer maidens) { this.maidens = maidens; }
        public Double getEconomyRate() { return economyRate; }
        public void setEconomyRate(Double economyRate) { this.economyRate = economyRate; }
        public Boolean getIsOut() { return isOut; }
        public void setIsOut(Boolean isOut) { this.isOut = isOut; }
        public Boolean getIsWicketKeeper() { return isWicketKeeper; }
        public void setIsWicketKeeper(Boolean isWicketKeeper) { this.isWicketKeeper = isWicketKeeper; }
        public String getDismissalType() { return dismissalType; }
        public void setDismissalType(String dismissalType) { this.dismissalType = dismissalType; }
        public String getDismissalText() { return dismissalText; }
        public void setDismissalText(String dismissalText) { this.dismissalText = dismissalText; }
        public Boolean getPlayed() { return played != null ? played : false; }
        public void setPlayed(Boolean played) {
            this.played = played;
            this.hasBatted = played;
        }
        public Boolean getHasBatted() { return hasBatted != null ? hasBatted : (played != null ? played : false); }
        public void setHasBatted(Boolean hasBatted) {
            this.hasBatted = hasBatted;
            this.played = hasBatted;
        }
    }

    // Getters and Setters
    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public String getMatchName() { return matchName; }
    public void setMatchName(String matchName) { this.matchName = matchName; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public String getMatchDate() { return matchDate; }
    public void setMatchDate(String matchDate) { this.matchDate = matchDate; }
    public String getMatchTime() { return matchTime; }
    public void setMatchTime(String matchTime) { this.matchTime = matchTime; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public Integer getOvers() { return overs; }
    public void setOvers(Integer overs) { this.overs = overs; }
    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }
    public MatchResponseDto.TeamSummaryDto getTeamA() { return teamA; }
    public void setTeamA(MatchResponseDto.TeamSummaryDto teamA) { this.teamA = teamA; }
    public MatchResponseDto.TeamSummaryDto getTeamB() { return teamB; }
    public void setTeamB(MatchResponseDto.TeamSummaryDto teamB) { this.teamB = teamB; }
    public String getScorerUserId() { return scorerUserId; }
    public void setScorerUserId(String scorerUserId) { this.scorerUserId = scorerUserId; }
    public String getScorerName() { return scorerName; }
    public void setScorerName(String scorerName) { this.scorerName = scorerName; }
    public boolean isAuthorizedScorer() { return isAuthorizedScorer; }
    public void setAuthorizedScorer(boolean authorizedScorer) { isAuthorizedScorer = authorizedScorer; }
    public MatchChecklistDto getChecklist() { return checklist; }
    public void setChecklist(MatchChecklistDto checklist) { this.checklist = checklist; }
    public Boolean getTossRecorded() { return tossRecorded; }
    public void setTossRecorded(Boolean tossRecorded) { this.tossRecorded = tossRecorded; }
    public String getTossWinnerTeamId() { return tossWinnerTeamId; }
    public void setTossWinnerTeamId(String tossWinnerTeamId) { this.tossWinnerTeamId = tossWinnerTeamId; }
    public String getTossWinnerTeamName() { return tossWinnerTeamName; }
    public void setTossWinnerTeamName(String tossWinnerTeamName) { this.tossWinnerTeamName = tossWinnerTeamName; }
    public TossDecision getTossDecision() { return tossDecision; }
    public void setTossDecision(TossDecision tossDecision) { this.tossDecision = tossDecision; }
    public String getTossSummary() { return tossSummary; }
    public void setTossSummary(String tossSummary) { this.tossSummary = tossSummary; }
    public Integer getActiveInningsNumber() { return activeInningsNumber; }
    public void setActiveInningsNumber(Integer activeInningsNumber) { this.activeInningsNumber = activeInningsNumber; }
    public InningsStatus getInningsStatus() { return inningsStatus; }
    public void setInningsStatus(InningsStatus inningsStatus) { this.inningsStatus = inningsStatus; }
    public Boolean getIsInningsBreak() { return isInningsBreak; }
    public void setIsInningsBreak(Boolean isInningsBreak) { this.isInningsBreak = isInningsBreak; }
    public String getInningsBreakSummary() { return inningsBreakSummary; }
    public void setInningsBreakSummary(String inningsBreakSummary) { this.inningsBreakSummary = inningsBreakSummary; }
    public String getBattingTeamId() { return battingTeamId; }
    public void setBattingTeamId(String battingTeamId) { this.battingTeamId = battingTeamId; }
    public String getBattingTeamName() { return battingTeamName; }
    public void setBattingTeamName(String battingTeamName) { this.battingTeamName = battingTeamName; }
    public String getBowlingTeamId() { return bowlingTeamId; }
    public void setBowlingTeamId(String bowlingTeamId) { this.bowlingTeamId = bowlingTeamId; }
    public String getBowlingTeamName() { return bowlingTeamName; }
    public void setBowlingTeamName(String bowlingTeamName) { this.bowlingTeamName = bowlingTeamName; }
    public Integer getTotalRuns() { return totalRuns; }
    public void setTotalRuns(Integer totalRuns) { this.totalRuns = totalRuns; }
    public Integer getTotalWickets() { return totalWickets; }
    public void setTotalWickets(Integer totalWickets) { this.totalWickets = totalWickets; }
    public Integer getCompletedOvers() { return completedOvers; }
    public void setCompletedOvers(Integer completedOvers) { this.completedOvers = completedOvers; }
    public Integer getCurrentBalls() { return currentBalls; }
    public void setCurrentBalls(Integer currentBalls) { this.currentBalls = currentBalls; }
    public Double getRunRate() { return runRate; }
    public void setRunRate(Double runRate) { this.runRate = runRate; }
    public Integer getTargetRuns() { return targetRuns; }
    public void setTargetRuns(Integer targetRuns) { this.targetRuns = targetRuns; }
    public Integer getPartnershipRuns() { return partnershipRuns; }
    public void setPartnershipRuns(Integer partnershipRuns) { this.partnershipRuns = partnershipRuns; }
    public Integer getPartnershipBalls() { return partnershipBalls; }
    public void setPartnershipBalls(Integer partnershipBalls) { this.partnershipBalls = partnershipBalls; }
    public Double getLast5OversRunRate() { return last5OversRunRate; }
    public void setLast5OversRunRate(Double last5OversRunRate) { this.last5OversRunRate = last5OversRunRate; }
    public Double getRequiredRunRate() { return requiredRunRate; }
    public void setRequiredRunRate(Double requiredRunRate) { this.requiredRunRate = requiredRunRate; }
    public Integer getBallsRemaining() { return ballsRemaining; }
    public void setBallsRemaining(Integer ballsRemaining) { this.ballsRemaining = ballsRemaining; }
    public Integer getTotalExtras() { return totalExtras; }
    public void setTotalExtras(Integer totalExtras) { this.totalExtras = totalExtras; }
    public Integer getFallOfWicketsCount() { return fallOfWicketsCount; }
    public void setFallOfWicketsCount(Integer fallOfWicketsCount) { this.fallOfWicketsCount = fallOfWicketsCount; }
    public Boolean getIsOverCompleted() { return isOverCompleted; }
    public void setIsOverCompleted(Boolean overCompleted) { isOverCompleted = overCompleted; }
    public Boolean getIsWicketPendingNextBatter() { return isWicketPendingNextBatter; }
    public void setIsWicketPendingNextBatter(Boolean wicketPendingNextBatter) { isWicketPendingNextBatter = wicketPendingNextBatter; }
    public String getPreviousBowlerUserId() { return previousBowlerUserId; }
    public void setPreviousBowlerUserId(String previousBowlerUserId) { this.previousBowlerUserId = previousBowlerUserId; }
    public PlayerSummaryDto getStriker() { return striker; }
    public void setStriker(PlayerSummaryDto striker) { this.striker = striker; }
    public PlayerSummaryDto getNonStriker() { return nonStriker; }
    public void setNonStriker(PlayerSummaryDto nonStriker) { this.nonStriker = nonStriker; }
    public String getBowlingEnd() { return bowlingEnd != null ? bowlingEnd : "END_A"; }
    public void setBowlingEnd(String bowlingEnd) { this.bowlingEnd = bowlingEnd; }
    public PlayerSummaryDto getBattingEndA() { return battingEndA; }
    public void setBattingEndA(PlayerSummaryDto battingEndA) { this.battingEndA = battingEndA; }
    public PlayerSummaryDto getBattingEndB() { return battingEndB; }
    public void setBattingEndB(PlayerSummaryDto battingEndB) { this.battingEndB = battingEndB; }
    public PlayerSummaryDto getCurrentBowler() { return currentBowler; }
    public void setCurrentBowler(PlayerSummaryDto currentBowler) { this.currentBowler = currentBowler; }
    public PlayerSummaryDto getCurrentWicketKeeper() { return currentWicketKeeper; }
    public void setCurrentWicketKeeper(PlayerSummaryDto currentWicketKeeper) { this.currentWicketKeeper = currentWicketKeeper; }
    public List<PlayerSummaryDto> getBattingPlayingXi() { return battingPlayingXi; }
    public void setBattingPlayingXi(List<PlayerSummaryDto> battingPlayingXi) { this.battingPlayingXi = battingPlayingXi; }
    public List<PlayerSummaryDto> getBowlingPlayingXi() { return bowlingPlayingXi; }
    public void setBowlingPlayingXi(List<PlayerSummaryDto> bowlingPlayingXi) { this.bowlingPlayingXi = bowlingPlayingXi; }
    public List<PlayerSummaryDto> getAvailableBowlers() { return availableBowlers; }
    public void setAvailableBowlers(List<PlayerSummaryDto> availableBowlers) { this.availableBowlers = availableBowlers; }
    public List<PlayerSummaryDto> getAvailableBatters() { return availableBatters; }
    public void setAvailableBatters(List<PlayerSummaryDto> availableBatters) { this.availableBatters = availableBatters; }
    public List<BallEventDto> getThisOverBallEvents() { return thisOverBallEvents; }
    public void setThisOverBallEvents(List<BallEventDto> thisOverBallEvents) { thisOverBallEvents = thisOverBallEvents; }
    public List<String> getOverSummaryBalls() { return overSummaryBalls; }
    public void setOverSummaryBalls(List<String> overSummaryBalls) { this.overSummaryBalls = overSummaryBalls; }
    public List<String> getCurrentOverBalls() { return overSummaryBalls != null ? overSummaryBalls : new ArrayList<>(); }
    public void setCurrentOverBalls(List<String> currentOverBalls) { this.overSummaryBalls = currentOverBalls; }
    public Boolean getWideRunEnabled() { return wideRunEnabled; }
    public void setWideRunEnabled(Boolean wideRunEnabled) { this.wideRunEnabled = wideRunEnabled; }
    public Boolean getNoBallRunEnabled() { return noBallRunEnabled; }
    public void setNoBallRunEnabled(Boolean noBallRunEnabled) { this.noBallRunEnabled = noBallRunEnabled; }
    public Boolean getNoBallFreeHitEnabled() { return noBallFreeHitEnabled; }
    public void setNoBallFreeHitEnabled(Boolean noBallFreeHitEnabled) { this.noBallFreeHitEnabled = noBallFreeHitEnabled; }
    public Boolean getByeRunEnabled() { return byeRunEnabled; }
    public void setByeRunEnabled(Boolean byeRunEnabled) { this.byeRunEnabled = byeRunEnabled; }
    public Boolean getLegByeRunEnabled() { return legByeRunEnabled; }
    public void setLegByeRunEnabled(Boolean legByeRunEnabled) { this.legByeRunEnabled = legByeRunEnabled; }
    public Integer getMaxOversPerBowler() { return maxOversPerBowler; }
    public void setMaxOversPerBowler(Integer maxOversPerBowler) { this.maxOversPerBowler = maxOversPerBowler; }
    public Boolean getAllowConsecutiveOvers() { return allowConsecutiveOvers; }
    public void setAllowConsecutiveOvers(Boolean allowConsecutiveOvers) { this.allowConsecutiveOvers = allowConsecutiveOvers; }

    public Boolean getIsMatchCompleted() { return isMatchCompleted; }
    public void setIsMatchCompleted(Boolean isMatchCompleted) { this.isMatchCompleted = isMatchCompleted; }
    public String getMatchResultSummary() { return matchResultSummary; }
    public void setMatchResultSummary(String matchResultSummary) { this.matchResultSummary = matchResultSummary; }
    public InningsScorecardDto getInnings1Scorecard() { return innings1Scorecard; }
    public void setInnings1Scorecard(InningsScorecardDto innings1Scorecard) { this.innings1Scorecard = innings1Scorecard; }
    public InningsScorecardDto getInnings2Scorecard() { return innings2Scorecard; }
    public void setInnings2Scorecard(InningsScorecardDto innings2Scorecard) { this.innings2Scorecard = innings2Scorecard; }

    public List<RecentOverDto> getRecentOvers() { return recentOvers; }
    public void setRecentOvers(List<RecentOverDto> recentOvers) { this.recentOvers = recentOvers; }
    public List<CommentaryDto> getCommentary() { return commentary; }
    public void setCommentary(List<CommentaryDto> commentary) { this.commentary = commentary; }
    public Long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Long sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getFirstInningsTeamName() { return firstInningsTeamName; }
    public void setFirstInningsTeamName(String firstInningsTeamName) { this.firstInningsTeamName = firstInningsTeamName; }
    public Integer getFirstInningsRuns() { return firstInningsRuns; }
    public void setFirstInningsRuns(Integer firstInningsRuns) { this.firstInningsRuns = firstInningsRuns; }
    public Integer getFirstInningsWickets() { return firstInningsWickets; }
    public void setFirstInningsWickets(Integer firstInningsWickets) { this.firstInningsWickets = firstInningsWickets; }
    public Integer getFirstInningsOvers() { return firstInningsOvers; }
    public void setFirstInningsOvers(Integer firstInningsOvers) { this.firstInningsOvers = firstInningsOvers; }
    public Integer getFirstInningsBalls() { return firstInningsBalls; }
    public void setFirstInningsBalls(Integer firstInningsBalls) { this.firstInningsBalls = firstInningsBalls; }

    public Integer getRequiredRuns() { return requiredRuns; }
    public void setRequiredRuns(Integer requiredRuns) { this.requiredRuns = requiredRuns; }
    public String getTargetEquation() { return targetEquation; }
    public void setTargetEquation(String targetEquation) { this.targetEquation = targetEquation; }

    public String getWinnerTeamId() { return winnerTeamId; }
    public void setWinnerTeamId(String winnerTeamId) { this.winnerTeamId = winnerTeamId; }
    public String getWinnerTeamName() { return winnerTeamName; }
    public void setWinnerTeamName(String winnerTeamName) { this.winnerTeamName = winnerTeamName; }
    public String getResultMargin() { return resultMargin; }
    public void setResultMargin(String resultMargin) { this.resultMargin = resultMargin; }
    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

    public Boolean getIsPaused() { return isPaused != null ? isPaused : false; }
    public void setIsPaused(Boolean isPaused) { this.isPaused = isPaused; }
    public String getPauseReason() { return pauseReason; }
    public void setPauseReason(String pauseReason) { this.pauseReason = pauseReason; }
    public String getPausedAt() { return pausedAt; }
    public void setPausedAt(String pausedAt) { this.pausedAt = pausedAt; }

    public Integer getCurrentInningsTotalFours() { return currentInningsTotalFours; }
    public void setCurrentInningsTotalFours(Integer currentInningsTotalFours) { this.currentInningsTotalFours = currentInningsTotalFours; }
    public Integer getCurrentInningsTotalSixes() { return currentInningsTotalSixes; }
    public void setCurrentInningsTotalSixes(Integer currentInningsTotalSixes) { this.currentInningsTotalSixes = currentInningsTotalSixes; }
    public List<FallOfWicketDto> getCurrentInningsFallOfWickets() { return currentInningsFallOfWickets; }
    public void setCurrentInningsFallOfWickets(List<FallOfWicketDto> currentInningsFallOfWickets) { this.currentInningsFallOfWickets = currentInningsFallOfWickets; }

    public String getLatestCelebrationType() { return latestCelebrationType; }
    public void setLatestCelebrationType(String latestCelebrationType) { this.latestCelebrationType = latestCelebrationType; }
    public String getLatestCelebrationId() { return latestCelebrationId; }
    public void setLatestCelebrationId(String latestCelebrationId) { this.latestCelebrationId = latestCelebrationId; }
}

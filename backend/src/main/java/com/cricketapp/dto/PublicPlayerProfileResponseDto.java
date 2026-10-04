package com.cricketapp.dto;

import com.cricketapp.entity.BattingStyle;
import com.cricketapp.entity.BowlingStyle;
import com.cricketapp.entity.Gender;
import com.cricketapp.entity.PlayingRole;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PublicPlayerProfileResponseDto {
    private String userId;           // Permanent CRKXXXXXX
    private String name;             // Player name
    private String profilePhotoUrl;  // Photo URL
    private String location;
    private PlayingRole playingRole;
    private BattingStyle battingStyle;
    private BowlingStyle bowlingStyle;
    private String bio;
    private LocalDate dateOfBirth;
    private Gender gender;

    private List<PlayerTeamDto> teams = new ArrayList<>();
    private BattingStatsDto batting = new BattingStatsDto();
    private BowlingStatsDto bowling = new BowlingStatsDto();
    private FieldingStatsDto fielding = new FieldingStatsDto();
    private List<MatchHistoryDto> recentMatches = new ArrayList<>();
    private List<MatchHistoryDto> matchHistory = new ArrayList<>();
    private List<AchievementDto> achievements = new ArrayList<>();

    public PublicPlayerProfileResponseDto() {
    }

    public PublicPlayerProfileResponseDto(String userId, String name, String profilePhotoUrl,
                                          String location, PlayingRole playingRole,
                                          BattingStyle battingStyle, BowlingStyle bowlingStyle,
                                          String bio) {
        this.userId = userId;
        this.name = name;
        this.profilePhotoUrl = profilePhotoUrl;
        this.location = location;
        this.playingRole = playingRole;
        this.battingStyle = battingStyle;
        this.bowlingStyle = bowlingStyle;
        this.bio = bio;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public PlayingRole getPlayingRole() {
        return playingRole;
    }

    public void setPlayingRole(PlayingRole playingRole) {
        this.playingRole = playingRole;
    }

    public BattingStyle getBattingStyle() {
        return battingStyle;
    }

    public void setBattingStyle(BattingStyle battingStyle) {
        this.battingStyle = battingStyle;
    }

    public BowlingStyle getBowlingStyle() {
        return bowlingStyle;
    }

    public void setBowlingStyle(BowlingStyle bowlingStyle) {
        this.bowlingStyle = bowlingStyle;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public List<PlayerTeamDto> getTeams() {
        return teams;
    }

    public void setTeams(List<PlayerTeamDto> teams) {
        this.teams = teams;
    }

    public BattingStatsDto getBatting() {
        return batting;
    }

    public void setBatting(BattingStatsDto batting) {
        this.batting = batting;
    }

    public BowlingStatsDto getBowling() {
        return bowling;
    }

    public void setBowling(BowlingStatsDto bowling) {
        this.bowling = bowling;
    }

    public FieldingStatsDto getFielding() {
        return fielding;
    }

    public void setFielding(FieldingStatsDto fielding) {
        this.fielding = fielding;
    }

    public List<MatchHistoryDto> getRecentMatches() {
        return recentMatches;
    }

    public void setRecentMatches(List<MatchHistoryDto> recentMatches) {
        this.recentMatches = recentMatches;
    }

    public List<MatchHistoryDto> getMatchHistory() {
        return matchHistory;
    }

    public void setMatchHistory(List<MatchHistoryDto> matchHistory) {
        this.matchHistory = matchHistory;
    }

    public List<AchievementDto> getAchievements() {
        return achievements;
    }

    public void setAchievements(List<AchievementDto> achievements) {
        this.achievements = achievements;
    }

    // Inner DTO Classes
    public static class PlayerTeamDto {
        private String teamId;
        private String teamName;
        private String logoUrl;
        private String role;
        private LocalDate joinedDate;

        public PlayerTeamDto() {}

        public PlayerTeamDto(String teamId, String teamName, String logoUrl, String role, LocalDate joinedDate) {
            this.teamId = teamId;
            this.teamName = teamName;
            this.logoUrl = logoUrl;
            this.role = role;
            this.joinedDate = joinedDate;
        }

        public String getTeamId() { return teamId; }
        public void setTeamId(String teamId) { this.teamId = teamId; }

        public String getTeamName() { return teamName; }
        public void setTeamName(String teamName) { this.teamName = teamName; }

        public String getLogoUrl() { return logoUrl; }
        public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public LocalDate getJoinedDate() { return joinedDate; }
        public void setJoinedDate(LocalDate joinedDate) { this.joinedDate = joinedDate; }
    }

    public static class BattingStatsDto {
        private int matches = 0;
        private int innings = 0;
        private int runs = 0;
        private String highestScore = "0";
        private Double average = null;
        private Double strikeRate = null;
        private int fifties = 0;
        private int hundreds = 0;
        private int fours = 0;
        private int sixes = 0;
        private int ballsFaced = 0;
        private int notOuts = 0;

        public BattingStatsDto() {}

        public int getMatches() { return matches; }
        public void setMatches(int matches) { this.matches = matches; }

        public int getInnings() { return innings; }
        public void setInnings(int innings) { this.innings = innings; }

        public int getRuns() { return runs; }
        public void setRuns(int runs) { this.runs = runs; }

        public String getHighestScore() { return highestScore; }
        public void setHighestScore(String highestScore) { this.highestScore = highestScore; }

        public Double getAverage() { return average; }
        public void setAverage(Double average) { this.average = average; }

        public Double getStrikeRate() { return strikeRate; }
        public void setStrikeRate(Double strikeRate) { this.strikeRate = strikeRate; }

        public int getFifties() { return fifties; }
        public void setFifties(int fifties) { this.fifties = fifties; }

        public int getHundreds() { return hundreds; }
        public void setHundreds(int hundreds) { this.hundreds = hundreds; }

        public int getFours() { return fours; }
        public void setFours(int fours) { this.fours = fours; }

        public int getSixes() { return sixes; }
        public void setSixes(int sixes) { this.sixes = sixes; }

        public int getBallsFaced() { return ballsFaced; }
        public void setBallsFaced(int ballsFaced) { this.ballsFaced = ballsFaced; }

        public int getNotOuts() { return notOuts; }
        public void setNotOuts(int notOuts) { this.notOuts = notOuts; }
    }

    public static class BowlingStatsDto {
        private int matches = 0;
        private int innings = 0;
        private double overs = 0.0;
        private int balls = 0;
        private int runsConceded = 0;
        private int wickets = 0;
        private String bestBowling = null;
        private Double economy = null;
        private Double average = null;
        private int fourWickets = 0;
        private int fiveWickets = 0;
        private int maidens = 0;

        public BowlingStatsDto() {}

        public int getMatches() { return matches; }
        public void setMatches(int matches) { this.matches = matches; }

        public int getInnings() { return innings; }
        public void setInnings(int innings) { this.innings = innings; }

        public double getOvers() { return overs; }
        public void setOvers(double overs) { this.overs = overs; }

        public int getBalls() { return balls; }
        public void setBalls(int balls) { this.balls = balls; }

        public int getRunsConceded() { return runsConceded; }
        public void setRunsConceded(int runsConceded) { this.runsConceded = runsConceded; }

        public int getWickets() { return wickets; }
        public void setWickets(int wickets) { this.wickets = wickets; }

        public String getBestBowling() { return bestBowling; }
        public void setBestBowling(String bestBowling) { this.bestBowling = bestBowling; }

        public Double getEconomy() { return economy; }
        public void setEconomy(Double economy) { this.economy = economy; }

        public Double getAverage() { return average; }
        public void setAverage(Double average) { this.average = average; }

        public int getFourWickets() { return fourWickets; }
        public void setFourWickets(int fourWickets) { this.fourWickets = fourWickets; }

        public int getFiveWickets() { return fiveWickets; }
        public void setFiveWickets(int fiveWickets) { this.fiveWickets = fiveWickets; }

        public int getMaidens() { return maidens; }
        public void setMaidens(int maidens) { this.maidens = maidens; }
    }

    public static class FieldingStatsDto {
        private int catches = 0;
        private int runOuts = 0;
        private int stumpings = 0;

        public FieldingStatsDto() {}

        public int getCatches() { return catches; }
        public void setCatches(int catches) { this.catches = catches; }

        public int getRunOuts() { return runOuts; }
        public void setRunOuts(int runOuts) { this.runOuts = runOuts; }

        public int getStumpings() { return stumpings; }
        public void setStumpings(int stumpings) { this.stumpings = stumpings; }
    }

    public static class MatchHistoryDto {
        private String matchId;
        private String matchName;
        private LocalDate date;
        private String format;
        private String teamName;
        private String opponentName;
        private String result;
        private Integer runs;
        private Integer balls;
        private Integer wickets;
        private Double overs;
        private Integer catches;

        public MatchHistoryDto() {}

        public String getMatchId() { return matchId; }
        public void setMatchId(String matchId) { this.matchId = matchId; }

        public String getMatchName() { return matchName; }
        public void setMatchName(String matchName) { this.matchName = matchName; }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public String getTeamName() { return teamName; }
        public void setTeamName(String teamName) { this.teamName = teamName; }

        public String getOpponentName() { return opponentName; }
        public void setOpponentName(String opponentName) { this.opponentName = opponentName; }

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }

        public Integer getRuns() { return runs; }
        public void setRuns(Integer runs) { this.runs = runs; }

        public Integer getBalls() { return balls; }
        public void setBalls(Integer balls) { this.balls = balls; }

        public Integer getWickets() { return wickets; }
        public void setWickets(Integer wickets) { this.wickets = wickets; }

        public Double getOvers() { return overs; }
        public void setOvers(Double overs) { this.overs = overs; }

        public Integer getCatches() { return catches; }
        public void setCatches(Integer catches) { this.catches = catches; }
    }

    public static class AchievementDto {
        private String title;
        private String description;
        private String type;
        private LocalDate date;

        public AchievementDto() {}

        public AchievementDto(String title, String description, String type, LocalDate date) {
            this.title = title;
            this.description = description;
            this.type = type;
            this.date = date;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
    }
}

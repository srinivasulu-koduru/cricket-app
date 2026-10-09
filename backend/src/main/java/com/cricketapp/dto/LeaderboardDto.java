package com.cricketapp.dto;

import java.util.List;

public class LeaderboardDto {

    private String category;
    private String timeframe;
    private List<PlayerRankingDto> rankings;

    public LeaderboardDto() {}

    public LeaderboardDto(String category, String timeframe, List<PlayerRankingDto> rankings) {
        this.category = category;
        this.timeframe = timeframe;
        this.rankings = rankings;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public List<PlayerRankingDto> getRankings() { return rankings; }
    public void setRankings(List<PlayerRankingDto> rankings) { this.rankings = rankings; }

    public static class PlayerRankingDto {
        private int rank;
        private String userId;
        private String name;
        private String profilePhotoUrl;
        private String teamName;
        private int matches;
        
        // Batting stats
        private int runs;
        private double average;
        private double strikeRate;
        private int sixes;
        private int fours;
        
        // Bowling stats
        private int wickets;
        private double economy;
        private int maidens;
        
        // Dynamic sorting value
        private double value;

        public PlayerRankingDto() {}

        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getProfilePhotoUrl() { return profilePhotoUrl; }
        public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
        
        public String getTeamName() { return teamName; }
        public void setTeamName(String teamName) { this.teamName = teamName; }
        
        public int getMatches() { return matches; }
        public void setMatches(int matches) { this.matches = matches; }
        
        public int getRuns() { return runs; }
        public void setRuns(int runs) { this.runs = runs; }
        
        public double getAverage() { return average; }
        public void setAverage(double average) { this.average = average; }
        
        public double getStrikeRate() { return strikeRate; }
        public void setStrikeRate(double strikeRate) { this.strikeRate = strikeRate; }
        
        public int getSixes() { return sixes; }
        public void setSixes(int sixes) { this.sixes = sixes; }
        
        public int getFours() { return fours; }
        public void setFours(int fours) { this.fours = fours; }
        
        public int getWickets() { return wickets; }
        public void setWickets(int wickets) { this.wickets = wickets; }
        
        public double getEconomy() { return economy; }
        public void setEconomy(double economy) { this.economy = economy; }
        
        public int getMaidens() { return maidens; }
        public void setMaidens(int maidens) { this.maidens = maidens; }
        
        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
    }
}

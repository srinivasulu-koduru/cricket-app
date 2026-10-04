package com.cricketapp.dto;

import com.cricketapp.entity.BattingStyle;
import com.cricketapp.entity.BowlingStyle;
import com.cricketapp.entity.Gender;
import com.cricketapp.entity.PlayingRole;

import java.time.LocalDate;

public class ProfileResponseDto {
    private String userId;           // Permanent CRKXXXXXX
    private String name;             // Display name or full name
    private String email;            // Registered email
    private String profilePhotoUrl;  // Photo URL
    private LocalDate dateOfBirth;
    private Gender gender;
    private String location;
    private PlayingRole playingRole;
    private BattingStyle battingStyle;
    private BowlingStyle bowlingStyle;
    private String bio;

    private PublicPlayerProfileResponseDto.BattingStatsDto batting = new PublicPlayerProfileResponseDto.BattingStatsDto();
    private PublicPlayerProfileResponseDto.BowlingStatsDto bowling = new PublicPlayerProfileResponseDto.BowlingStatsDto();
    private PublicPlayerProfileResponseDto.FieldingStatsDto fielding = new PublicPlayerProfileResponseDto.FieldingStatsDto();
    private java.util.List<PublicPlayerProfileResponseDto.MatchHistoryDto> matchHistory = new java.util.ArrayList<>();
    private java.util.List<PublicPlayerProfileResponseDto.AchievementDto> achievements = new java.util.ArrayList<>();

    public ProfileResponseDto() {
    }

    public ProfileResponseDto(String userId, String name, String email, String profilePhotoUrl,
                              LocalDate dateOfBirth, Gender gender, String location,
                              PlayingRole playingRole, BattingStyle battingStyle,
                              BowlingStyle bowlingStyle, String bio) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.profilePhotoUrl = profilePhotoUrl;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
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

    public PublicPlayerProfileResponseDto.BattingStatsDto getBatting() { return batting; }
    public void setBatting(PublicPlayerProfileResponseDto.BattingStatsDto batting) { this.batting = batting; }

    public PublicPlayerProfileResponseDto.BowlingStatsDto getBowling() { return bowling; }
    public void setBowling(PublicPlayerProfileResponseDto.BowlingStatsDto bowling) { this.bowling = bowling; }

    public PublicPlayerProfileResponseDto.FieldingStatsDto getFielding() { return fielding; }
    public void setFielding(PublicPlayerProfileResponseDto.FieldingStatsDto fielding) { this.fielding = fielding; }

    public java.util.List<PublicPlayerProfileResponseDto.MatchHistoryDto> getMatchHistory() { return matchHistory; }
    public void setMatchHistory(java.util.List<PublicPlayerProfileResponseDto.MatchHistoryDto> matchHistory) { this.matchHistory = matchHistory; }

    public java.util.List<PublicPlayerProfileResponseDto.AchievementDto> getAchievements() { return achievements; }
    public void setAchievements(java.util.List<PublicPlayerProfileResponseDto.AchievementDto> achievements) { this.achievements = achievements; }
}

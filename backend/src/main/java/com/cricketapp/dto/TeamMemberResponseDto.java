package com.cricketapp.dto;

import com.cricketapp.entity.MemberStatus;
import com.cricketapp.entity.TeamRole;

import java.time.LocalDateTime;

public class TeamMemberResponseDto {

    private String userId;           // Permanent CRKXXXXXX
    private String name;             // Member full name
    private String email;            // Email
    private String profilePhotoUrl;  // Photo URL from PlayerProfile
    private TeamRole role;           // OWNER, CAPTAIN, PLAYER
    private MemberStatus status;     // ACTIVE, LEFT, REMOVED
    private LocalDateTime joinedAt;
    private String playingRole;      // BATTER, BOWLER, ALL_ROUNDER, WICKET_KEEPER
    private String battingStyle;     // RIGHT_HAND_BAT, LEFT_HAND_BAT
    private String bowlingStyle;     // RIGHT_ARM_FAST, etc.

    public TeamMemberResponseDto() {
    }

    public TeamMemberResponseDto(String userId, String name, String email, String profilePhotoUrl,
                                 TeamRole role, MemberStatus status, LocalDateTime joinedAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.profilePhotoUrl = profilePhotoUrl;
        this.role = role;
        this.status = status;
        this.joinedAt = joinedAt;
    }

    public TeamMemberResponseDto(String userId, String name, String email, String profilePhotoUrl,
                                 TeamRole role, MemberStatus status, LocalDateTime joinedAt,
                                 String playingRole, String battingStyle, String bowlingStyle) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.profilePhotoUrl = profilePhotoUrl;
        this.role = role;
        this.status = status;
        this.joinedAt = joinedAt;
        this.playingRole = playingRole;
        this.battingStyle = battingStyle;
        this.bowlingStyle = bowlingStyle;
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

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public void setStatus(MemberStatus status) {
        this.status = status;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getPlayingRole() {
        return playingRole;
    }

    public void setPlayingRole(String playingRole) {
        this.playingRole = playingRole;
    }

    public String getBattingStyle() {
        return battingStyle;
    }

    public void setBattingStyle(String battingStyle) {
        this.battingStyle = battingStyle;
    }

    public String getBowlingStyle() {
        return bowlingStyle;
    }

    public void setBowlingStyle(String bowlingStyle) {
        this.bowlingStyle = bowlingStyle;
    }
}

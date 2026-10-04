package com.cricketapp.dto;

import com.cricketapp.entity.TeamRole;
import com.cricketapp.entity.TeamStatus;

import java.time.LocalDateTime;

public class TeamResponseDto {

    private String teamId;
    private String name;
    private String logoUrl;
    private String description;
    private String createdByUserId;
    private String createdByName;
    private String joinToken;
    private TeamStatus status;
    private long memberCount;
    private TeamRole currentUserRole;
    private LocalDateTime createdAt;

    public TeamResponseDto() {
    }

    public TeamResponseDto(String teamId, String name, String logoUrl, String description,
                           String createdByUserId, String createdByName, String joinToken,
                           TeamStatus status, long memberCount, TeamRole currentUserRole,
                           LocalDateTime createdAt) {
        this.teamId = teamId;
        this.name = name;
        this.logoUrl = logoUrl;
        this.description = description;
        this.createdByUserId = createdByUserId;
        this.createdByName = createdByName;
        this.joinToken = joinToken;
        this.status = status;
        this.memberCount = memberCount;
        this.currentUserRole = currentUserRole;
        this.createdAt = createdAt;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getJoinToken() {
        return joinToken;
    }

    public void setJoinToken(String joinToken) {
        this.joinToken = joinToken;
    }

    public TeamStatus getStatus() {
        return status;
    }

    public void setStatus(TeamStatus status) {
        this.status = status;
    }

    public long getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(long memberCount) {
        this.memberCount = memberCount;
    }

    public TeamRole getCurrentUserRole() {
        return currentUserRole;
    }

    public void setCurrentUserRole(TeamRole currentUserRole) {
        this.currentUserRole = currentUserRole;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

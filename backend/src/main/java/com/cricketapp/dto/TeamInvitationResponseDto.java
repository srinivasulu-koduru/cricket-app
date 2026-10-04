package com.cricketapp.dto;

import com.cricketapp.entity.InvitationStatus;

import java.time.LocalDateTime;

public class TeamInvitationResponseDto {

    private Long invitationId;
    private String teamId;
    private String teamName;
    private String teamLogoUrl;
    private String invitedUserId;
    private String invitedUserName;
    private String invitedByUserId;
    private String invitedByName;
    private InvitationStatus status;
    private LocalDateTime createdAt;

    public TeamInvitationResponseDto() {
    }

    public TeamInvitationResponseDto(Long invitationId, String teamId, String teamName,
                                     String teamLogoUrl, String invitedUserId, String invitedUserName,
                                     String invitedByUserId, String invitedByName,
                                     InvitationStatus status, LocalDateTime createdAt) {
        this.invitationId = invitationId;
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamLogoUrl = teamLogoUrl;
        this.invitedUserId = invitedUserId;
        this.invitedUserName = invitedUserName;
        this.invitedByUserId = invitedByUserId;
        this.invitedByName = invitedByName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(Long invitationId) {
        this.invitationId = invitationId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamLogoUrl() {
        return teamLogoUrl;
    }

    public void setTeamLogoUrl(String teamLogoUrl) {
        this.teamLogoUrl = teamLogoUrl;
    }

    public String getInvitedUserId() {
        return invitedUserId;
    }

    public void setInvitedUserId(String invitedUserId) {
        this.invitedUserId = invitedUserId;
    }

    public String getInvitedUserName() {
        return invitedUserName;
    }

    public void setInvitedUserName(String invitedUserName) {
        this.invitedUserName = invitedUserName;
    }

    public String getInvitedByUserId() {
        return invitedByUserId;
    }

    public void setInvitedByUserId(String invitedByUserId) {
        this.invitedByUserId = invitedByUserId;
    }

    public String getInvitedByName() {
        return invitedByName;
    }

    public void setInvitedByName(String invitedByName) {
        this.invitedByName = invitedByName;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

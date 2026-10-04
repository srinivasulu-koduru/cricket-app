package com.cricketapp.dto;

import com.cricketapp.entity.MatchFormat;
import com.cricketapp.entity.MatchInvitationStatus;
import com.cricketapp.entity.MatchStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class MatchInvitationResponseDto {

    private Long invitationId;
    private String matchId;
    private String matchName;

    private String invitingTeamId;
    private String invitingTeamName;
    private String invitingTeamLogoUrl;

    private String opponentTeamId;
    private String opponentTeamName;
    private String opponentTeamLogoUrl;

    private String invitedByUserId;
    private String invitedByName;

    private String invitedCaptainUserId;
    private String invitedCaptainName;

    private LocalDate matchDate;
    private LocalTime matchTime;
    private String venue;
    private MatchFormat format;
    private Integer overs;
    private String description;

    private MatchInvitationStatus invitationStatus;
    private MatchStatus matchStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MatchInvitationResponseDto() {
    }

    public Long getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(Long invitationId) {
        this.invitationId = invitationId;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getMatchName() {
        return matchName;
    }

    public void setMatchName(String matchName) {
        this.matchName = matchName;
    }

    public String getInvitingTeamId() {
        return invitingTeamId;
    }

    public void setInvitingTeamId(String invitingTeamId) {
        this.invitingTeamId = invitingTeamId;
    }

    public String getInvitingTeamName() {
        return invitingTeamName;
    }

    public void setInvitingTeamName(String invitingTeamName) {
        this.invitingTeamName = invitingTeamName;
    }

    public String getInvitingTeamLogoUrl() {
        return invitingTeamLogoUrl;
    }

    public void setInvitingTeamLogoUrl(String invitingTeamLogoUrl) {
        this.invitingTeamLogoUrl = invitingTeamLogoUrl;
    }

    public String getOpponentTeamId() {
        return opponentTeamId;
    }

    public void setOpponentTeamId(String opponentTeamId) {
        this.opponentTeamId = opponentTeamId;
    }

    public String getOpponentTeamName() {
        return opponentTeamName;
    }

    public void setOpponentTeamName(String opponentTeamName) {
        this.opponentTeamName = opponentTeamName;
    }

    public String getOpponentTeamLogoUrl() {
        return opponentTeamLogoUrl;
    }

    public void setOpponentTeamLogoUrl(String opponentTeamLogoUrl) {
        this.opponentTeamLogoUrl = opponentTeamLogoUrl;
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

    public String getInvitedCaptainUserId() {
        return invitedCaptainUserId;
    }

    public void setInvitedCaptainUserId(String invitedCaptainUserId) {
        this.invitedCaptainUserId = invitedCaptainUserId;
    }

    public String getInvitedCaptainName() {
        return invitedCaptainName;
    }

    public void setInvitedCaptainName(String invitedCaptainName) {
        this.invitedCaptainName = invitedCaptainName;
    }

    public LocalDate getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(LocalDate matchDate) {
        this.matchDate = matchDate;
    }

    public LocalTime getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(LocalTime matchTime) {
        this.matchTime = matchTime;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public MatchFormat getFormat() {
        return format;
    }

    public void setFormat(MatchFormat format) {
        this.format = format;
    }

    public Integer getOvers() {
        return overs;
    }

    public void setOvers(Integer overs) {
        this.overs = overs;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MatchInvitationStatus getInvitationStatus() {
        return invitationStatus;
    }

    public void setInvitationStatus(MatchInvitationStatus invitationStatus) {
        this.invitationStatus = invitationStatus;
    }

    public MatchStatus getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(MatchStatus matchStatus) {
        this.matchStatus = matchStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

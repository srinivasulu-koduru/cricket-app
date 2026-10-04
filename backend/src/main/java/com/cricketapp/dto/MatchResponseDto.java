package com.cricketapp.dto;

import com.cricketapp.entity.MatchFormat;
import com.cricketapp.entity.MatchInvitationStatus;
import com.cricketapp.entity.MatchStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class MatchResponseDto {

    private String matchId;
    private String matchName;

    private TeamSummaryDto teamA;
    private TeamSummaryDto teamB;

    private String createdByUserId;
    private String createdByName;

    private String scorerUserId;
    private String scorerName;
    private Boolean isScorer;

    private LocalDate matchDate;
    private LocalTime matchTime;
    private String venue;
    private MatchFormat format;
    private Integer overs;
    private String description;
    private MatchStatus status;

    private MatchInvitationStatus invitationStatus;
    private Long invitationId;
    private String invitedCaptainUserId;
    private String invitedCaptainName;

    private Boolean isCreator;
    private Boolean isInvitedCaptain;
    private Boolean isCaptain;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MatchResponseDto() {
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

    public TeamSummaryDto getTeamA() {
        return teamA;
    }

    public void setTeamA(TeamSummaryDto teamA) {
        this.teamA = teamA;
    }

    public TeamSummaryDto getTeamB() {
        return teamB;
    }

    public void setTeamB(TeamSummaryDto teamB) {
        this.teamB = teamB;
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

    public String getScorerUserId() {
        return scorerUserId;
    }

    public void setScorerUserId(String scorerUserId) {
        this.scorerUserId = scorerUserId;
    }

    public String getScorerName() {
        return scorerName;
    }

    public void setScorerName(String scorerName) {
        this.scorerName = scorerName;
    }

    public Boolean getIsScorer() {
        return isScorer;
    }

    public void setIsScorer(Boolean isScorer) {
        this.isScorer = isScorer;
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

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public MatchInvitationStatus getInvitationStatus() {
        return invitationStatus;
    }

    public void setInvitationStatus(MatchInvitationStatus invitationStatus) {
        this.invitationStatus = invitationStatus;
    }

    public Long getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(Long invitationId) {
        this.invitationId = invitationId;
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

    public Boolean getIsCreator() {
        return isCreator;
    }

    public void setIsCreator(Boolean isCreator) {
        this.isCreator = isCreator;
    }

    public Boolean getIsInvitedCaptain() {
        return isInvitedCaptain;
    }

    public void setIsInvitedCaptain(Boolean isInvitedCaptain) {
        this.isInvitedCaptain = isInvitedCaptain;
    }

    public Boolean getIsCaptain() {
        return isCaptain;
    }

    public void setIsCaptain(Boolean isCaptain) {
        this.isCaptain = isCaptain;
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

    public static class TeamSummaryDto {
        private String teamId;
        private String name;
        private String logoUrl;

        public TeamSummaryDto() {
        }

        public TeamSummaryDto(String teamId, String name, String logoUrl) {
            this.teamId = teamId;
            this.name = name;
            this.logoUrl = logoUrl;
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
    }
}

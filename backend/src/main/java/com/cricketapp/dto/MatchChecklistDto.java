package com.cricketapp.dto;

public class MatchChecklistDto {
    private boolean teamAConfirmed;
    private boolean teamBConfirmed;
    private boolean teamAPlayingXiReady;
    private boolean teamBPlayingXiReady;
    private boolean scorerAssigned;
    private boolean formatAndOversValid;
    private boolean canStartMatch;
    private String warningMessage;

    public MatchChecklistDto() {
    }

    public boolean isTeamAConfirmed() {
        return teamAConfirmed;
    }

    public void setTeamAConfirmed(boolean teamAConfirmed) {
        this.teamAConfirmed = teamAConfirmed;
    }

    public boolean isTeamBConfirmed() {
        return teamBConfirmed;
    }

    public void setTeamBConfirmed(boolean teamBConfirmed) {
        this.teamBConfirmed = teamBConfirmed;
    }

    public boolean isTeamAPlayingXiReady() {
        return teamAPlayingXiReady;
    }

    public void setTeamAPlayingXiReady(boolean teamAPlayingXiReady) {
        this.teamAPlayingXiReady = teamAPlayingXiReady;
    }

    public boolean isTeamBPlayingXiReady() {
        return teamBPlayingXiReady;
    }

    public void setTeamBPlayingXiReady(boolean teamBPlayingXiReady) {
        this.teamBPlayingXiReady = teamBPlayingXiReady;
    }

    public boolean isScorerAssigned() {
        return scorerAssigned;
    }

    public void setScorerAssigned(boolean scorerAssigned) {
        this.scorerAssigned = scorerAssigned;
    }

    public boolean isFormatAndOversValid() {
        return formatAndOversValid;
    }

    public void setFormatAndOversValid(boolean formatAndOversValid) {
        this.formatAndOversValid = formatAndOversValid;
    }

    public boolean isCanStartMatch() {
        return canStartMatch;
    }

    public void setCanStartMatch(boolean canStartMatch) {
        this.canStartMatch = canStartMatch;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }
}

package com.cricketapp.dto;

import java.util.ArrayList;
import java.util.List;

public class TeamPlayingXiResponseDto {

    private String teamId;
    private String teamName;
    private Boolean isCaptainOfTeam;
    private Boolean isSaved;
    private List<PlayingXiPlayerDto> players = new ArrayList<>();

    public TeamPlayingXiResponseDto() {
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

    public Boolean getIsCaptainOfTeam() {
        return isCaptainOfTeam;
    }

    public void setIsCaptainOfTeam(Boolean isCaptainOfTeam) {
        this.isCaptainOfTeam = isCaptainOfTeam;
    }

    public Boolean getIsSaved() {
        return isSaved;
    }

    public void setIsSaved(Boolean isSaved) {
        this.isSaved = isSaved;
    }

    public List<PlayingXiPlayerDto> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayingXiPlayerDto> players) {
        this.players = players;
    }
}

package com.cricketapp.dto;

import com.cricketapp.entity.MatchFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public class CreateMatchRequestDto {

    private String matchName;
    private String teamAId;
    private String teamBId;
    private LocalDate matchDate;
    private LocalTime matchTime;
    private String venue;
    private MatchFormat format;
    private Integer overs;
    private String description;

    public CreateMatchRequestDto() {
    }

    public CreateMatchRequestDto(String matchName, String teamAId, String teamBId,
                                 LocalDate matchDate, LocalTime matchTime, String venue,
                                 MatchFormat format, Integer overs, String description) {
        this.matchName = matchName;
        this.teamAId = teamAId;
        this.teamBId = teamBId;
        this.matchDate = matchDate;
        this.matchTime = matchTime;
        this.venue = venue;
        this.format = format;
        this.overs = overs;
        this.description = description;
    }

    public String getMatchName() {
        return matchName;
    }

    public void setMatchName(String matchName) {
        this.matchName = matchName;
    }

    public String getTeamAId() {
        return teamAId;
    }

    public void setTeamAId(String teamAId) {
        this.teamAId = teamAId;
    }

    public String getTeamBId() {
        return teamBId;
    }

    public void setTeamBId(String teamBId) {
        this.teamBId = teamBId;
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
}

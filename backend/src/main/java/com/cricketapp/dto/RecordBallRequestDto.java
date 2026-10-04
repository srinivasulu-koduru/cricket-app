package com.cricketapp.dto;

public class RecordBallRequestDto {
    private Integer runs = 0;
    private String extraType; // NONE, WIDE, NO_BALL, BYE, LEG_BYE
    private Integer extraRuns = 0;
    private Boolean isWicket = false;
    private String wicketType; // BOWLED, CAUGHT, LBW, RUN_OUT, STUMPED, HIT_WICKET, RETIRED
    private String dismissedUserId;
    private String fielderUserId;
    private Boolean isBoundary = false;
    private Integer runsCompleted;
    private Boolean crossedAtDismissal = false;
    private String newBatterUserId;

    public RecordBallRequestDto() {}

    public Integer getRuns() { return runs; }
    public void setRuns(Integer runs) { this.runs = runs; }
    public String getExtraType() { return extraType; }
    public void setExtraType(String extraType) { this.extraType = extraType; }
    public Integer getExtraRuns() { return extraRuns; }
    public void setExtraRuns(Integer extraRuns) { this.extraRuns = extraRuns; }
    public Boolean getIsWicket() { return isWicket; }
    public void setIsWicket(Boolean wicket) { isWicket = wicket; }
    public String getWicketType() { return wicketType; }
    public void setWicketType(String wicketType) { this.wicketType = wicketType; }
    public String getDismissedUserId() { return dismissedUserId; }
    public void setDismissedUserId(String dismissedUserId) { this.dismissedUserId = dismissedUserId; }
    public String getFielderUserId() { return fielderUserId; }
    public void setFielderUserId(String fielderUserId) { this.fielderUserId = fielderUserId; }
    public Boolean getIsBoundary() { return isBoundary; }
    public void setIsBoundary(Boolean boundary) { isBoundary = boundary; }
    public Integer getRunsCompleted() { return runsCompleted; }
    public void setRunsCompleted(Integer runsCompleted) { this.runsCompleted = runsCompleted; }
    public Boolean getCrossedAtDismissal() { return crossedAtDismissal; }
    public void setCrossedAtDismissal(Boolean crossedAtDismissal) { this.crossedAtDismissal = crossedAtDismissal; }
    public String getNewBatterUserId() { return newBatterUserId; }
    public void setNewBatterUserId(String newBatterUserId) { this.newBatterUserId = newBatterUserId; }
}

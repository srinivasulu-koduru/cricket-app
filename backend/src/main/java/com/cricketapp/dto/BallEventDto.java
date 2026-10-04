package com.cricketapp.dto;

public class BallEventDto {
    private Long id;
    private Integer overNumber;
    private Integer ballNumber;
    private String strikerUserId;
    private String strikerName;
    private String bowlerUserId;
    private String bowlerName;
    private Integer runsScored;
    private String extraType;
    private Integer extraRuns;
    private boolean isWicket;
    private String wicketType;
    private String dismissedUserId;
    private String summaryText;

    public BallEventDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getOverNumber() { return overNumber; }
    public void setOverNumber(Integer overNumber) { this.overNumber = overNumber; }
    public Integer getBallNumber() { return ballNumber; }
    public void setBallNumber(Integer ballNumber) { this.ballNumber = ballNumber; }
    public String getStrikerUserId() { return strikerUserId; }
    public void setStrikerUserId(String strikerUserId) { this.strikerUserId = strikerUserId; }
    public String getStrikerName() { return strikerName; }
    public void setStrikerName(String strikerName) { this.strikerName = strikerName; }
    public String getBowlerUserId() { return bowlerUserId; }
    public void setBowlerUserId(String bowlerUserId) { this.bowlerUserId = bowlerUserId; }
    public String getBowlerName() { return bowlerName; }
    public void setBowlerName(String bowlerName) { this.bowlerName = bowlerName; }
    public Integer getRunsScored() { return runsScored; }
    public void setRunsScored(Integer runsScored) { this.runsScored = runsScored; }
    public String getExtraType() { return extraType; }
    public void setExtraType(String extraType) { this.extraType = extraType; }
    public Integer getExtraRuns() { return extraRuns; }
    public void setExtraRuns(Integer extraRuns) { this.extraRuns = extraRuns; }
    public boolean isWicket() { return isWicket; }
    public void setWicket(boolean wicket) { isWicket = wicket; }
    public String getWicketType() { return wicketType; }
    public void setWicketType(String wicketType) { this.wicketType = wicketType; }
    public String getDismissedUserId() { return dismissedUserId; }
    public void setDismissedUserId(String dismissedUserId) { this.dismissedUserId = dismissedUserId; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
}

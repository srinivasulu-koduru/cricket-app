package com.cricketapp.dto;

public class SelectNextBowlerRequestDto {
    private String bowlerUserId;

    public SelectNextBowlerRequestDto() {}

    public String getBowlerUserId() { return bowlerUserId; }
    public void setBowlerUserId(String bowlerUserId) { this.bowlerUserId = bowlerUserId; }
}

package com.cricketapp.dto;

public class SelectNextBatterRequestDto {
    private String batterUserId;
    private String position; // STRIKER or NON_STRIKER

    public SelectNextBatterRequestDto() {}

    public String getBatterUserId() { return batterUserId; }
    public void setBatterUserId(String batterUserId) { this.batterUserId = batterUserId; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
}

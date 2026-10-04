package com.cricketapp.dto;

public class ChangeWicketKeeperRequestDto {
    private String keeperUserId;

    public ChangeWicketKeeperRequestDto() {}

    public ChangeWicketKeeperRequestDto(String keeperUserId) {
        this.keeperUserId = keeperUserId;
    }

    public String getKeeperUserId() {
        return keeperUserId;
    }

    public void setKeeperUserId(String keeperUserId) {
        this.keeperUserId = keeperUserId;
    }
}

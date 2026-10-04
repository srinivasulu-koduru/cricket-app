package com.cricketapp.dto;

public class InvitePlayerRequestDto {

    private String cricketUserId;

    public InvitePlayerRequestDto() {
    }

    public InvitePlayerRequestDto(String cricketUserId) {
        this.cricketUserId = cricketUserId;
    }

    public String getCricketUserId() {
        return cricketUserId;
    }

    public void setCricketUserId(String cricketUserId) {
        this.cricketUserId = cricketUserId;
    }
}

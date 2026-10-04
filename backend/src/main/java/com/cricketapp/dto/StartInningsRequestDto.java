package com.cricketapp.dto;

public class StartInningsRequestDto {
    private Integer inningsNumber;
    private String strikerUserId;
    private String nonStrikerUserId;
    private String bowlerUserId;

    public StartInningsRequestDto() {
    }

    public Integer getInningsNumber() {
        return inningsNumber;
    }

    public void setInningsNumber(Integer inningsNumber) {
        this.inningsNumber = inningsNumber;
    }

    public String getStrikerUserId() {
        return strikerUserId;
    }

    public void setStrikerUserId(String strikerUserId) {
        this.strikerUserId = strikerUserId;
    }

    public String getNonStrikerUserId() {
        return nonStrikerUserId;
    }

    public void setNonStrikerUserId(String nonStrikerUserId) {
        this.nonStrikerUserId = nonStrikerUserId;
    }

    public String getBowlerUserId() {
        return bowlerUserId;
    }

    public void setBowlerUserId(String bowlerUserId) {
        this.bowlerUserId = bowlerUserId;
    }
}

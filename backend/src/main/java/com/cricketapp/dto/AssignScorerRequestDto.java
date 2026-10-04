package com.cricketapp.dto;

public class AssignScorerRequestDto {

    private String scorerUserId;

    public AssignScorerRequestDto() {
    }

    public AssignScorerRequestDto(String scorerUserId) {
        this.scorerUserId = scorerUserId;
    }

    public String getScorerUserId() {
        return scorerUserId;
    }

    public void setScorerUserId(String scorerUserId) {
        this.scorerUserId = scorerUserId;
    }
}

package com.cricketapp.dto;

import com.cricketapp.entity.TossDecision;

public class RecordTossRequestDto {
    private String tossWinnerTeamId;
    private TossDecision decision;

    public RecordTossRequestDto() {
    }

    public RecordTossRequestDto(String tossWinnerTeamId, TossDecision decision) {
        this.tossWinnerTeamId = tossWinnerTeamId;
        this.decision = decision;
    }

    public String getTossWinnerTeamId() {
        return tossWinnerTeamId;
    }

    public void setTossWinnerTeamId(String tossWinnerTeamId) {
        this.tossWinnerTeamId = tossWinnerTeamId;
    }

    public TossDecision getDecision() {
        return decision;
    }

    public void setDecision(TossDecision decision) {
        this.decision = decision;
    }
}

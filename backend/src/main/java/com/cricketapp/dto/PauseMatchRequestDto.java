package com.cricketapp.dto;

public class PauseMatchRequestDto {

    private String reason;
    private String customReason;

    public PauseMatchRequestDto() {
    }

    public PauseMatchRequestDto(String reason, String customReason) {
        this.reason = reason;
        this.customReason = customReason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCustomReason() {
        return customReason;
    }

    public void setCustomReason(String customReason) {
        this.customReason = customReason;
    }
}

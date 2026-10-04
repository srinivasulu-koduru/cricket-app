package com.cricketapp.dto;

public class RetireBatterRequestDto {
    private String retiringUserId;
    private String newBatterUserId;
    private String retirementType;

    public RetireBatterRequestDto() {}

    public RetireBatterRequestDto(String retiringUserId, String newBatterUserId, String retirementType) {
        this.retiringUserId = retiringUserId;
        this.newBatterUserId = newBatterUserId;
        this.retirementType = retirementType;
    }

    public String getRetiringUserId() {
        return retiringUserId;
    }

    public void setRetiringUserId(String retiringUserId) {
        this.retiringUserId = retiringUserId;
    }

    public String getNewBatterUserId() {
        return newBatterUserId;
    }

    public void setNewBatterUserId(String newBatterUserId) {
        this.newBatterUserId = newBatterUserId;
    }

    public String getRetirementType() {
        return retirementType;
    }

    public void setRetirementType(String retirementType) {
        this.retirementType = retirementType;
    }
}

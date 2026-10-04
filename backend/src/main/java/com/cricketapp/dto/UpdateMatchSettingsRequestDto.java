package com.cricketapp.dto;

public class UpdateMatchSettingsRequestDto {
    private Boolean wideRunEnabled;
    private Boolean noBallRunEnabled;
    private Boolean noBallFreeHitEnabled;
    private Boolean byeRunEnabled;
    private Boolean legByeRunEnabled;
    private Integer maxOversPerBowler;
    private Boolean allowConsecutiveOvers;

    public UpdateMatchSettingsRequestDto() {}

    public Boolean getWideRunEnabled() { return wideRunEnabled; }
    public void setWideRunEnabled(Boolean wideRunEnabled) { this.wideRunEnabled = wideRunEnabled; }
    public Boolean getNoBallRunEnabled() { return noBallRunEnabled; }
    public void setNoBallRunEnabled(Boolean noBallRunEnabled) { this.noBallRunEnabled = noBallRunEnabled; }
    public Boolean getNoBallFreeHitEnabled() { return noBallFreeHitEnabled; }
    public void setNoBallFreeHitEnabled(Boolean noBallFreeHitEnabled) { this.noBallFreeHitEnabled = noBallFreeHitEnabled; }
    public Boolean getByeRunEnabled() { return byeRunEnabled; }
    public void setByeRunEnabled(Boolean byeRunEnabled) { this.byeRunEnabled = byeRunEnabled; }
    public Boolean getLegByeRunEnabled() { return legByeRunEnabled; }
    public void setLegByeRunEnabled(Boolean legByeRunEnabled) { this.legByeRunEnabled = legByeRunEnabled; }
    public Integer getMaxOversPerBowler() { return maxOversPerBowler; }
    public void setMaxOversPerBowler(Integer maxOversPerBowler) { this.maxOversPerBowler = maxOversPerBowler; }
    public Boolean getAllowConsecutiveOvers() { return allowConsecutiveOvers; }
    public void setAllowConsecutiveOvers(Boolean allowConsecutiveOvers) { this.allowConsecutiveOvers = allowConsecutiveOvers; }
}

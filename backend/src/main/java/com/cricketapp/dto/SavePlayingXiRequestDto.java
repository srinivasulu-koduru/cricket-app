package com.cricketapp.dto;

import java.util.ArrayList;
import java.util.List;

public class SavePlayingXiRequestDto {

    private List<String> playerUserIds = new ArrayList<>();
    private String wicketKeeperUserId;
    private String captainUserId;

    public SavePlayingXiRequestDto() {
    }

    public List<String> getPlayerUserIds() {
        return playerUserIds;
    }

    public void setPlayerUserIds(List<String> playerUserIds) {
        this.playerUserIds = playerUserIds;
    }

    public String getWicketKeeperUserId() {
        return wicketKeeperUserId;
    }

    public void setWicketKeeperUserId(String wicketKeeperUserId) {
        this.wicketKeeperUserId = wicketKeeperUserId;
    }

    public String getCaptainUserId() {
        return captainUserId;
    }

    public void setCaptainUserId(String captainUserId) {
        this.captainUserId = captainUserId;
    }
}

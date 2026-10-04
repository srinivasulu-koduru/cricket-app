package com.cricketapp.dto;

import com.cricketapp.entity.BattingStyle;
import com.cricketapp.entity.BowlingStyle;
import com.cricketapp.entity.Gender;
import com.cricketapp.entity.PlayingRole;

import java.time.LocalDate;

public class UpdateProfileRequestDto {

    private String name;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String location;
    private PlayingRole playingRole;
    private BattingStyle battingStyle;
    private BowlingStyle bowlingStyle;
    private String bio;

    public UpdateProfileRequestDto() {
    }

    public UpdateProfileRequestDto(String name, LocalDate dateOfBirth, Gender gender, String location,
                                   PlayingRole playingRole, BattingStyle battingStyle,
                                   BowlingStyle bowlingStyle, String bio) {
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.location = location;
        this.playingRole = playingRole;
        this.battingStyle = battingStyle;
        this.bowlingStyle = bowlingStyle;
        this.bio = bio;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public PlayingRole getPlayingRole() {
        return playingRole;
    }

    public void setPlayingRole(PlayingRole playingRole) {
        this.playingRole = playingRole;
    }

    public BattingStyle getBattingStyle() {
        return battingStyle;
    }

    public void setBattingStyle(BattingStyle battingStyle) {
        this.battingStyle = battingStyle;
    }

    public BowlingStyle getBowlingStyle() {
        return bowlingStyle;
    }

    public void setBowlingStyle(BowlingStyle bowlingStyle) {
        this.bowlingStyle = bowlingStyle;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}

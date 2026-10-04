package com.cricketapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_playing_xi", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"match_id", "team_id", "user_id"})
})
public class MatchPlayingXi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_captain", nullable = false)
    private boolean isCaptain = false;

    @Column(name = "is_wicket_keeper", nullable = false)
    private boolean isWicketKeeper = false;

    @Column(name = "batting_order")
    private Integer battingOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public MatchPlayingXi() {
    }

    public MatchPlayingXi(Match match, Team team, User user, boolean isCaptain, boolean isWicketKeeper) {
        this.match = match;
        this.team = team;
        this.user = user;
        this.isCaptain = isCaptain;
        this.isWicketKeeper = isWicketKeeper;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isCaptain() {
        return isCaptain;
    }

    public void setCaptain(boolean captain) {
        isCaptain = captain;
    }

    public boolean isWicketKeeper() {
        return isWicketKeeper;
    }

    public void setWicketKeeper(boolean wicketKeeper) {
        isWicketKeeper = wicketKeeper;
    }

    public Integer getBattingOrder() {
        return battingOrder;
    }

    public void setBattingOrder(Integer battingOrder) {
        this.battingOrder = battingOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

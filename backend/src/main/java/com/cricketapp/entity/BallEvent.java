package com.cricketapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ball_events")
public class BallEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", referencedColumnName = "id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "innings_id", referencedColumnName = "id", nullable = false)
    private Innings innings;

    @Column(name = "over_number", nullable = false)
    private Integer overNumber;

    @Column(name = "ball_number", nullable = false)
    private Integer ballNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "striker_user_id", referencedColumnName = "id", nullable = false)
    private User striker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "non_striker_user_id", referencedColumnName = "id", nullable = false)
    private User nonStriker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bowler_user_id", referencedColumnName = "id", nullable = false)
    private User bowler;

    @Column(name = "runs_scored", nullable = false)
    private Integer runsScored = 0;

    @Column(name = "extra_type", length = 30)
    private String extraType; // NONE, WIDE, NO_BALL, BYE, LEG_BYE

    @Column(name = "extra_runs", nullable = false)
    private Integer extraRuns = 0;

    @Column(name = "is_wicket", nullable = false)
    private boolean isWicket = false;

    @Column(name = "wicket_type", length = 50)
    private String wicketType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dismissed_user_id", referencedColumnName = "id")
    private User dismissedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fielder_user_id", referencedColumnName = "id")
    private User fielder;

    @Column(name = "is_boundary")
    private Boolean isBoundary = false;

    @Column(name = "runs_completed")
    private Integer runsCompleted = 0;

    @Column(name = "crossed_at_dismissal")
    private Boolean crossedAtDismissal = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BallEvent() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public Innings getInnings() {
        return innings;
    }

    public void setInnings(Innings innings) {
        this.innings = innings;
    }

    public Integer getOverNumber() {
        return overNumber;
    }

    public void setOverNumber(Integer overNumber) {
        this.overNumber = overNumber;
    }

    public Integer getBallNumber() {
        return ballNumber;
    }

    public void setBallNumber(Integer ballNumber) {
        this.ballNumber = ballNumber;
    }

    public User getStriker() {
        return striker;
    }

    public void setStriker(User striker) {
        this.striker = striker;
    }

    public User getNonStriker() {
        return nonStriker;
    }

    public void setNonStriker(User nonStriker) {
        this.nonStriker = nonStriker;
    }

    public User getBowler() {
        return bowler;
    }

    public void setBowler(User bowler) {
        this.bowler = bowler;
    }

    public Integer getRunsScored() {
        return runsScored;
    }

    public void setRunsScored(Integer runsScored) {
        this.runsScored = runsScored;
    }

    public String getExtraType() {
        return extraType;
    }

    public void setExtraType(String extraType) {
        this.extraType = extraType;
    }

    public Integer getExtraRuns() {
        return extraRuns;
    }

    public void setExtraRuns(Integer extraRuns) {
        this.extraRuns = extraRuns;
    }

    public boolean isWicket() {
        return isWicket;
    }

    public void setWicket(boolean wicket) {
        isWicket = wicket;
    }

    public String getWicketType() {
        return wicketType;
    }

    public void setWicketType(String wicketType) {
        this.wicketType = wicketType;
    }

    public User getDismissedUser() {
        return dismissedUser;
    }

    public void setDismissedUser(User dismissedUser) {
        this.dismissedUser = dismissedUser;
    }

    public User getFielder() {
        return fielder;
    }

    public void setFielder(User fielder) {
        this.fielder = fielder;
    }

    public Boolean getIsBoundary() {
        return isBoundary;
    }

    public void setIsBoundary(Boolean boundary) {
        isBoundary = boundary;
    }

    public Integer getRunsCompleted() {
        return runsCompleted;
    }

    public void setRunsCompleted(Integer runsCompleted) {
        this.runsCompleted = runsCompleted;
    }

    public Boolean getCrossedAtDismissal() {
        return crossedAtDismissal;
    }

    public void setCrossedAtDismissal(Boolean crossedAtDismissal) {
        this.crossedAtDismissal = crossedAtDismissal;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

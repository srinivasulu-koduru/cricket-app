package com.cricketapp.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", unique = true, nullable = false, length = 20)
    private String matchId;

    @Column(name = "match_name", nullable = false, length = 150)
    private String matchName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_a_id", nullable = false)
    private Team teamA;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_b_id", nullable = false)
    private Team teamB;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "scorer_user_id")
    private User scorer;

    @Column(name = "match_date", nullable = false)
    private LocalDate matchDate;

    @Column(name = "match_time", nullable = false)
    private LocalTime matchTime;

    @Column(name = "venue", nullable = false, length = 150)
    private String venue;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 20)
    private MatchFormat format;

    @Column(name = "overs", nullable = false)
    private Integer overs;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MatchStatus status = MatchStatus.SCHEDULED;

    @Column(name = "wide_run_enabled", nullable = false)
    private Boolean wideRunEnabled = true;

    @Column(name = "no_ball_run_enabled", nullable = false)
    private Boolean noBallRunEnabled = false;

    @Column(name = "no_ball_free_hit_enabled", nullable = false)
    private Boolean noBallFreeHitEnabled = false;

    @Column(name = "bye_run_enabled", nullable = false)
    private Boolean byeRunEnabled = true;

    @Column(name = "leg_bye_run_enabled", nullable = false)
    private Boolean legByeRunEnabled = true;

    @Column(name = "max_overs_per_bowler")
    private Integer maxOversPerBowler;

    @Column(name = "allow_consecutive_overs", nullable = false)
    private Boolean allowConsecutiveOvers = false;

    @Column(name = "current_pause_reason", length = 200)
    private String currentPauseReason;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Match() {
    }

    public Match(String matchId, String matchName, Team teamA, Team teamB, User createdBy,
                 LocalDate matchDate, LocalTime matchTime, String venue,
                 MatchFormat format, Integer overs, String description) {
        this.matchId = matchId;
        this.matchName = matchName;
        this.teamA = teamA;
        this.teamB = teamB;
        this.createdBy = createdBy;
        this.matchDate = matchDate;
        this.matchTime = matchTime;
        this.venue = venue;
        this.format = format;
        this.overs = overs;
        this.description = description;
        this.status = MatchStatus.PENDING_CONFIRMATION;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = MatchStatus.PENDING_CONFIRMATION;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getMatchName() {
        return matchName;
    }

    public void setMatchName(String matchName) {
        this.matchName = matchName;
    }

    public Team getTeamA() {
        return teamA;
    }

    public void setTeamA(Team teamA) {
        this.teamA = teamA;
    }

    public Team getTeamB() {
        return teamB;
    }

    public void setTeamB(Team teamB) {
        this.teamB = teamB;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDate getMatchDate() {
        return matchDate;
    }

    public void setMatchDate(LocalDate matchDate) {
        this.matchDate = matchDate;
    }

    public LocalTime getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(LocalTime matchTime) {
        this.matchTime = matchTime;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public MatchFormat getFormat() {
        return format;
    }

    public void setFormat(MatchFormat format) {
        this.format = format;
    }

    public Integer getOvers() {
        return overs;
    }

    public void setOvers(Integer overs) {
        this.overs = overs;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public User getScorer() {
        return scorer;
    }

    public void setScorer(User scorer) {
        this.scorer = scorer;
    }

    public Boolean getWideRunEnabled() { return wideRunEnabled != null ? wideRunEnabled : true; }
    public void setWideRunEnabled(Boolean wideRunEnabled) { this.wideRunEnabled = wideRunEnabled; }

    public Boolean getNoBallRunEnabled() { return noBallRunEnabled != null ? noBallRunEnabled : true; }
    public void setNoBallRunEnabled(Boolean noBallRunEnabled) { this.noBallRunEnabled = noBallRunEnabled; }

    public Boolean getNoBallFreeHitEnabled() { return noBallFreeHitEnabled != null ? noBallFreeHitEnabled : true; }
    public void setNoBallFreeHitEnabled(Boolean noBallFreeHitEnabled) { this.noBallFreeHitEnabled = noBallFreeHitEnabled; }

    public Boolean getByeRunEnabled() { return byeRunEnabled != null ? byeRunEnabled : true; }
    public void setByeRunEnabled(Boolean byeRunEnabled) { this.byeRunEnabled = byeRunEnabled; }

    public Boolean getLegByeRunEnabled() { return legByeRunEnabled != null ? legByeRunEnabled : true; }
    public void setLegByeRunEnabled(Boolean legByeRunEnabled) { this.legByeRunEnabled = legByeRunEnabled; }

    public Integer getMaxOversPerBowler() { return maxOversPerBowler; }
    public void setMaxOversPerBowler(Integer maxOversPerBowler) { this.maxOversPerBowler = maxOversPerBowler; }

    public Boolean getAllowConsecutiveOvers() { return allowConsecutiveOvers != null ? allowConsecutiveOvers : false; }
    public void setAllowConsecutiveOvers(Boolean allowConsecutiveOvers) { this.allowConsecutiveOvers = allowConsecutiveOvers; }

    public String getCurrentPauseReason() { return currentPauseReason; }
    public void setCurrentPauseReason(String currentPauseReason) { this.currentPauseReason = currentPauseReason; }

    public LocalDateTime getPausedAt() { return pausedAt; }
    public void setPausedAt(LocalDateTime pausedAt) { this.pausedAt = pausedAt; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

package com.cricketapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "innings")
public class Innings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", referencedColumnName = "id", nullable = false)
    private Match match;

    @Column(name = "innings_number", nullable = false)
    private Integer inningsNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batting_team_id", referencedColumnName = "id", nullable = false)
    private Team battingTeam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bowling_team_id", referencedColumnName = "id", nullable = false)
    private Team bowlingTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "striker_user_id", referencedColumnName = "id")
    private User striker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "non_striker_user_id", referencedColumnName = "id")
    private User nonStriker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batting_end_a_user_id", referencedColumnName = "id")
    private User battingEndA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batting_end_b_user_id", referencedColumnName = "id")
    private User battingEndB;

    @Column(name = "bowling_end", length = 10)
    private String bowlingEnd = "END_A";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_bowler_user_id", referencedColumnName = "id")
    private User currentBowler;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InningsStatus status = InningsStatus.NOT_STARTED;

    @Column(name = "total_runs", nullable = false)
    private Integer totalRuns = 0;

    @Column(name = "total_wickets", nullable = false)
    private Integer totalWickets = 0;

    @Column(name = "total_overs", nullable = false)
    private Integer totalOvers = 0;

    @Column(name = "total_balls", nullable = false)
    private Integer totalBalls = 0;

    @Column(name = "selected_bowler_over_number", nullable = false)
    private Integer selectedBowlerOverNumber = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Innings() {
    }

    public Innings(Match match, Integer inningsNumber, Team battingTeam, Team bowlingTeam) {
        this.match = match;
        this.inningsNumber = inningsNumber;
        this.battingTeam = battingTeam;
        this.bowlingTeam = bowlingTeam;
        this.status = InningsStatus.NOT_STARTED;
        this.totalRuns = 0;
        this.totalWickets = 0;
        this.totalOvers = 0;
        this.totalBalls = 0;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public Integer getInningsNumber() {
        return inningsNumber;
    }

    public void setInningsNumber(Integer inningsNumber) {
        this.inningsNumber = inningsNumber;
    }

    public Team getBattingTeam() {
        return battingTeam;
    }

    public void setBattingTeam(Team battingTeam) {
        this.battingTeam = battingTeam;
    }

    public Team getBowlingTeam() {
        return bowlingTeam;
    }

    public void setBowlingTeam(Team bowlingTeam) {
        this.bowlingTeam = bowlingTeam;
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

    public User getCurrentBowler() {
        return currentBowler;
    }

    public void setCurrentBowler(User currentBowler) {
        this.currentBowler = currentBowler;
    }

    public InningsStatus getStatus() {
        return status;
    }

    public void setStatus(InningsStatus status) {
        this.status = status;
    }

    public Integer getTotalRuns() {
        return totalRuns;
    }

    public void setTotalRuns(Integer totalRuns) {
        this.totalRuns = totalRuns;
    }

    public Integer getTotalWickets() {
        return totalWickets;
    }

    public void setTotalWickets(Integer totalWickets) {
        this.totalWickets = totalWickets;
    }

    public Integer getTotalOvers() {
        return totalOvers;
    }

    public void setTotalOvers(Integer totalOvers) {
        this.totalOvers = totalOvers;
    }

    public Integer getTotalBalls() {
        return totalBalls;
    }

    public void setTotalBalls(Integer totalBalls) {
        this.totalBalls = totalBalls;
    }

    public Integer getSelectedBowlerOverNumber() {
        return selectedBowlerOverNumber;
    }

    public void setSelectedBowlerOverNumber(Integer selectedBowlerOverNumber) {
        this.selectedBowlerOverNumber = selectedBowlerOverNumber;
    }

    public User getBattingEndA() {
        return battingEndA;
    }

    public void setBattingEndA(User battingEndA) {
        this.battingEndA = battingEndA;
    }

    public User getBattingEndB() {
        return battingEndB;
    }

    public void setBattingEndB(User battingEndB) {
        this.battingEndB = battingEndB;
    }

    public String getBowlingEnd() {
        return bowlingEnd != null ? bowlingEnd : "END_A";
    }

    public void setBowlingEnd(String bowlingEnd) {
        this.bowlingEnd = bowlingEnd;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

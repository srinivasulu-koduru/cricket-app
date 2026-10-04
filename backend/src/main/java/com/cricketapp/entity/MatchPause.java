package com.cricketapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_pauses")
public class MatchPause {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", referencedColumnName = "id", nullable = false)
    private Match match;

    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    @Column(name = "custom_reason", length = 255)
    private String customReason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paused_by_user_id", referencedColumnName = "id", nullable = false)
    private User pausedBy;

    @Column(name = "paused_at", nullable = false)
    private LocalDateTime pausedAt;

    @Column(name = "resumed_at")
    private LocalDateTime resumedAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PAUSED";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public MatchPause() {
    }

    public MatchPause(Match match, String reason, String customReason, User pausedBy, LocalDateTime pausedAt) {
        this.match = match;
        this.reason = reason;
        this.customReason = customReason;
        this.pausedBy = pausedBy;
        this.pausedAt = pausedAt;
        this.status = "PAUSED";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.pausedAt == null) {
            this.pausedAt = LocalDateTime.now();
        }
    }

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

    public User getPausedBy() {
        return pausedBy;
    }

    public void setPausedBy(User pausedBy) {
        this.pausedBy = pausedBy;
    }

    public LocalDateTime getPausedAt() {
        return pausedAt;
    }

    public void setPausedAt(LocalDateTime pausedAt) {
        this.pausedAt = pausedAt;
    }

    public LocalDateTime getResumedAt() {
        return resumedAt;
    }

    public void setResumedAt(LocalDateTime resumedAt) {
        this.resumedAt = resumedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

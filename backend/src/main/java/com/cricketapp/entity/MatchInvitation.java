package com.cricketapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "match_invitations")
public class MatchInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inviting_team_id", nullable = false)
    private Team invitingTeam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "opponent_team_id", nullable = false)
    private Team opponentTeam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invited_captain_id", nullable = false)
    private User invitedCaptain;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private User invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MatchInvitationStatus status = MatchInvitationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public MatchInvitation() {
    }

    public MatchInvitation(Match match, Team invitingTeam, Team opponentTeam,
                           User invitedCaptain, User invitedBy) {
        this.match = match;
        this.invitingTeam = invitingTeam;
        this.opponentTeam = opponentTeam;
        this.invitedCaptain = invitedCaptain;
        this.invitedBy = invitedBy;
        this.status = MatchInvitationStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = MatchInvitationStatus.PENDING;
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

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public Team getInvitingTeam() {
        return invitingTeam;
    }

    public void setInvitingTeam(Team invitingTeam) {
        this.invitingTeam = invitingTeam;
    }

    public Team getOpponentTeam() {
        return opponentTeam;
    }

    public void setOpponentTeam(Team opponentTeam) {
        this.opponentTeam = opponentTeam;
    }

    public User getInvitedCaptain() {
        return invitedCaptain;
    }

    public void setInvitedCaptain(User invitedCaptain) {
        this.invitedCaptain = invitedCaptain;
    }

    public User getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(User invitedBy) {
        this.invitedBy = invitedBy;
    }

    public MatchInvitationStatus getStatus() {
        return status;
    }

    public void setStatus(MatchInvitationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}

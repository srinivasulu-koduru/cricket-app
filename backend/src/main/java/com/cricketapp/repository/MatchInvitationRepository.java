package com.cricketapp.repository;

import com.cricketapp.entity.Match;
import com.cricketapp.entity.MatchInvitation;
import com.cricketapp.entity.MatchInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchInvitationRepository extends JpaRepository<MatchInvitation, Long> {
    Optional<MatchInvitation> findByMatch(Match match);
    Optional<MatchInvitation> findByMatch_MatchId(String matchId);

    List<MatchInvitation> findByInvitedCaptain_EmailAndStatusOrderByCreatedAtDesc(String email, MatchInvitationStatus status);
    List<MatchInvitation> findByInvitedCaptain_EmailOrderByCreatedAtDesc(String email);

    boolean existsByMatchAndStatus(Match match, MatchInvitationStatus status);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM MatchInvitation mi WHERE mi.invitingTeam = :team OR mi.opponentTeam = :team")
    void deleteByTeam(@org.springframework.data.repository.query.Param("team") com.cricketapp.entity.Team team);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM MatchInvitation mi WHERE mi.match IN :matches")
    void deleteByMatchIn(@org.springframework.data.repository.query.Param("matches") List<Match> matches);
}

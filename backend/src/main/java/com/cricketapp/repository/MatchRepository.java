package com.cricketapp.repository;

import com.cricketapp.entity.Match;
import com.cricketapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    Optional<Match> findByMatchId(String matchId);
    boolean existsByMatchId(String matchId);

    List<Match> findByCreatedByOrderByCreatedAtDesc(User user);
    List<Match> findByStatusOrderByCreatedAtDesc(com.cricketapp.entity.MatchStatus status);

    @Query("SELECT DISTINCT m FROM Match m WHERE m.createdBy = :user OR m.teamA IN (SELECT tm.team FROM TeamMember tm WHERE tm.user = :user AND tm.status = com.cricketapp.entity.MemberStatus.ACTIVE) OR m.teamB IN (SELECT tm.team FROM TeamMember tm WHERE tm.user = :user AND tm.status = com.cricketapp.entity.MemberStatus.ACTIVE) ORDER BY m.createdAt DESC")
    List<Match> findMyMatches(@Param("user") User user);

    @Query("SELECT DISTINCT m FROM Match m WHERE m.status = com.cricketapp.entity.MatchStatus.COMPLETED AND (m IN (SELECT xi.match FROM MatchPlayingXi xi WHERE xi.user = :user) OR m.teamA IN (SELECT tm.team FROM TeamMember tm WHERE tm.user = :user) OR m.teamB IN (SELECT tm.team FROM TeamMember tm WHERE tm.user = :user)) ORDER BY m.createdAt DESC")
    List<Match> findCompletedMatchesForUser(@Param("user") User user);

    List<Match> findByTeamAOrTeamB(com.cricketapp.entity.Team teamA, com.cricketapp.entity.Team teamB);

    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Match m WHERE m.teamA = :team OR m.teamB = :team")
    void deleteByTeam(@Param("team") com.cricketapp.entity.Team team);
}

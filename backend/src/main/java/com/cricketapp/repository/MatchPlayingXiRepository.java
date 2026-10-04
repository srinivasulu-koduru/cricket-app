package com.cricketapp.repository;

import com.cricketapp.entity.Match;
import com.cricketapp.entity.MatchPlayingXi;
import com.cricketapp.entity.Team;
import com.cricketapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchPlayingXiRepository extends JpaRepository<MatchPlayingXi, Long> {

    List<MatchPlayingXi> findByMatchAndTeam(Match match, Team team);

    List<MatchPlayingXi> findByMatch(Match match);

    Optional<MatchPlayingXi> findByMatchAndUser(Match match, User user);

    void deleteByMatchAndTeam(Match match, Team team);

    void deleteByMatch(Match match);

    void deleteByTeam(Team team);
}

package com.cricketapp.repository;

import com.cricketapp.entity.Match;
import com.cricketapp.entity.MatchPause;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchPauseRepository extends JpaRepository<MatchPause, Long> {
    Optional<MatchPause> findTopByMatchAndStatusOrderByIdDesc(Match match, String status);
    List<MatchPause> findByMatchOrderByPausedAtDesc(Match match);
}

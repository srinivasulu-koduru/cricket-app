package com.cricketapp.repository;

import com.cricketapp.entity.Match;
import com.cricketapp.entity.MatchScorer;
import com.cricketapp.entity.ScorerStatus;
import com.cricketapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchScorerRepository extends JpaRepository<MatchScorer, Long> {
    Optional<MatchScorer> findByMatchAndStatus(Match match, ScorerStatus status);
    List<MatchScorer> findByScorerAndStatus(User scorer, ScorerStatus status);
    Optional<MatchScorer> findByMatchAndScorer(Match match, User scorer);
    List<MatchScorer> findByMatchOrderByAssignedAtDesc(Match match);
}

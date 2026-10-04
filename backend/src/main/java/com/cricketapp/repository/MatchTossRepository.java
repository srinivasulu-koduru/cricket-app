package com.cricketapp.repository;

import com.cricketapp.entity.Match;
import com.cricketapp.entity.MatchToss;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchTossRepository extends JpaRepository<MatchToss, Long> {
    Optional<MatchToss> findByMatch(Match match);
}

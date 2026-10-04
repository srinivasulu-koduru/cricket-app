package com.cricketapp.repository;

import com.cricketapp.entity.Innings;
import com.cricketapp.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InningsRepository extends JpaRepository<Innings, Long> {
    Optional<Innings> findByMatchAndInningsNumber(Match match, Integer inningsNumber);
    List<Innings> findByMatchOrderByInningsNumberAsc(Match match);
}

package com.cricketapp.repository;

import com.cricketapp.entity.BallEvent;
import com.cricketapp.entity.Innings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BallEventRepository extends JpaRepository<BallEvent, Long> {
    List<BallEvent> findByInningsOrderByOverNumberAscBallNumberAsc(Innings innings);
    Optional<BallEvent> findTopByInningsOrderByIdDesc(Innings innings);
    Optional<BallEvent> findTopByInningsAndIsWicketTrueOrderByIdDesc(Innings innings);
}

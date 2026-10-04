package com.cricketapp.repository;

import com.cricketapp.entity.PlayerProfile;
import com.cricketapp.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {
    Optional<PlayerProfile> findByUser(User user);
    List<PlayerProfile> findByUserIn(java.util.Collection<User> users);
    Optional<PlayerProfile> findByUser_UserId(String userId);
    Optional<PlayerProfile> findByUser_Email(String email);

    @Query("SELECT p FROM PlayerProfile p JOIN p.user u WHERE " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.displayName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.userId) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<PlayerProfile> searchPlayers(@Param("query") String query, Pageable pageable);
}

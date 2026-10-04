package com.cricketapp.repository;

import com.cricketapp.entity.MemberStatus;
import com.cricketapp.entity.Team;
import com.cricketapp.entity.TeamMember;
import com.cricketapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    Optional<TeamMember> findByTeamAndUser(Team team, User user);
    Optional<TeamMember> findByTeam_TeamIdAndUser_Email(String teamId, String email);
    Optional<TeamMember> findByTeam_TeamIdAndUser_UserId(String teamId, String userId);
    
    List<TeamMember> findByUser_EmailAndStatus(String email, MemberStatus status);
    List<TeamMember> findByUser_UserIdAndStatus(String userId, MemberStatus status);
    List<TeamMember> findByTeamAndStatus(Team team, MemberStatus status);
    List<TeamMember> findByTeam_TeamIdAndStatus(String teamId, MemberStatus status);

    boolean existsByTeamAndUserAndStatus(Team team, User user, MemberStatus status);
    long countByTeamAndStatus(Team team, MemberStatus status);
    void deleteByTeam(Team team);
}

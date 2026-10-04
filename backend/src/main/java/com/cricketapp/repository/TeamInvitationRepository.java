package com.cricketapp.repository;

import com.cricketapp.entity.InvitationStatus;
import com.cricketapp.entity.Team;
import com.cricketapp.entity.TeamInvitation;
import com.cricketapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
    List<TeamInvitation> findByInvitedUser_EmailAndStatusOrderByCreatedAtDesc(String email, InvitationStatus status);
    Optional<TeamInvitation> findByTeamAndInvitedUserAndStatus(Team team, User invitedUser, InvitationStatus status);
    List<TeamInvitation> findByTeam_TeamIdAndStatus(String teamId, InvitationStatus status);
    boolean existsByTeamAndInvitedUserAndStatus(Team team, User invitedUser, InvitationStatus status);
    void deleteByTeam(Team team);
}

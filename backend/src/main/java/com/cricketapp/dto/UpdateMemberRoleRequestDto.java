package com.cricketapp.dto;

import com.cricketapp.entity.TeamRole;

public class UpdateMemberRoleRequestDto {
    private TeamRole role;

    public UpdateMemberRoleRequestDto() {
    }

    public UpdateMemberRoleRequestDto(TeamRole role) {
        this.role = role;
    }

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }
}

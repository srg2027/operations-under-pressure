package com.newtonite.operations.team.dto;

import com.newtonite.operations.team.TeamRole;
import jakarta.validation.constraints.NotNull;

public class AddMemberRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Role is required")
    private TeamRole role = TeamRole.MEMBER;

    public AddMemberRequest() {
    }

    public AddMemberRequest(Long userId, TeamRole role) {
        this.userId = userId;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }
}

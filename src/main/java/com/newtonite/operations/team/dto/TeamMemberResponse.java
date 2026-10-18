package com.newtonite.operations.team.dto;

import com.newtonite.operations.team.TeamMembership;
import com.newtonite.operations.team.TeamRole;

import java.time.Instant;

public class TeamMemberResponse {

    private Long membershipId;
    private Long userId;
    private String displayName;
    private String email;
    private TeamRole role;
    private Instant joinedAt;

    public TeamMemberResponse() {
    }

    public TeamMemberResponse(Long membershipId, Long userId, String displayName, String email, TeamRole role, Instant joinedAt) {
        this.membershipId = membershipId;
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public static TeamMemberResponse fromEntity(TeamMembership membership) {
        if (membership == null) {
            return null;
        }
        return new TeamMemberResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getDisplayName(),
                membership.getUser().getEmail(),
                membership.getRole(),
                membership.getCreatedAt()
        );
    }

    public Long getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(Long membershipId) {
        this.membershipId = membershipId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}

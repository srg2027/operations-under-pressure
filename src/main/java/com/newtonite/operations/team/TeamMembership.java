package com.newtonite.operations.team;

import com.newtonite.operations.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity representing a user's membership and assigned role within a team.
 */
@Entity
@Table(
        name = "team_memberships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_team_memberships_team_user",
                        columnNames = {"team_id", "user_id"}
                )
        }
)
public class TeamMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Team is required")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
            name = "team_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_team_memberships_team")
    )
    private Team team;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_team_memberships_user")
    )
    private User user;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private TeamRole role = TeamRole.MEMBER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Default constructor required by the JPA specification.
     */
    public TeamMembership() {
    }

    /**
     * Convenience constructor for adding a user to a team with a specific role.
     *
     * @param team Target team.
     * @param user Member user.
     * @param role Assigned team role (defaults to MEMBER if null).
     */
    public TeamMembership(Team team, User user, TeamRole role) {
        this.team = team;
        this.user = user;
        this.role = role != null ? role : TeamRole.MEMBER;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // --- Object Identity ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamMembership that = (TeamMembership) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "TeamMembership{" +
                "id=" + id +
                ", team=" + (team != null ? team.getId() : null) +
                ", user=" + (user != null ? user.getId() : null) +
                ", role=" + role +
                ", createdAt=" + createdAt +
                '}';
    }
}

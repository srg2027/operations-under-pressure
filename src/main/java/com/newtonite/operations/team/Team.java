package com.newtonite.operations.team;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity representing an operational team in the Operations Under Pressure application.
 */
@Entity
@Table(
        name = "teams",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_teams_name", columnNames = "name")
        }
)
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Team name is required")
    @Size(max = 100, message = "Team name must not exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Default constructor required by the JPA specification.
     */
    public Team() {
    }

    /**
     * Convenience constructor for initializing a new team.
     *
     * @param name        Unique team name.
     * @param description Optional description of the team's operational scope.
     */
    public Team(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // --- Object Identity ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Team team = (Team) o;
        return id != null && Objects.equals(id, team.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

package com.newtonite.operations.team.dto;

import com.newtonite.operations.team.Team;

import java.time.Instant;

public class TeamResponse {

    private Long id;
    private String name;
    private String description;
    private Instant createdAt;

    public TeamResponse() {
    }

    public TeamResponse(Long id, String name, String description, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public static TeamResponse fromEntity(Team team) {
        if (team == null) {
            return null;
        }
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

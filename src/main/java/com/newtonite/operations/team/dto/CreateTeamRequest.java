package com.newtonite.operations.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateTeamRequest {

    @NotBlank(message = "Team name is required")
    @Size(max = 100, message = "Team name must not exceed 100 characters")
    private String name;

    private String description;

    public CreateTeamRequest() {
    }

    public CreateTeamRequest(String name, String description) {
        this.name = name;
        this.description = description;
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
}

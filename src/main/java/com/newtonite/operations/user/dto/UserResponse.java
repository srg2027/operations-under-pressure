package com.newtonite.operations.user.dto;

import com.newtonite.operations.user.User;

import java.time.Instant;

public class UserResponse {

    private Long id;
    private String displayName;
    private String email;
    private boolean active;
    private Instant createdAt;

    public UserResponse() {
    }

    public UserResponse(Long id, String displayName, String email, boolean active, Instant createdAt) {
        this.id = id;
        this.displayName = displayName;
        this.email = email;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static UserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

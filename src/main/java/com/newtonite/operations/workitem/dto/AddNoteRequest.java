package com.newtonite.operations.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AddNoteRequest {

    @NotBlank(message = "Note cannot be blank")
    @Size(max = 2000, message = "Note cannot exceed 2000 characters")
    private String note;

    @NotNull(message = "Actor user ID is required")
    private Long actorId;

    public AddNoteRequest() {
    }

    public AddNoteRequest(String note, Long actorId) {
        this.note = note;
        this.actorId = actorId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }
}

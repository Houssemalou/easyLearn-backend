package com.free.easyLearn.dto.room;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ParticipantActionRequest {

    @NotNull(message = "Room ID is required")
    private UUID roomId;

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    private Boolean muted; // For mute action

    private String message; // For ping message
}
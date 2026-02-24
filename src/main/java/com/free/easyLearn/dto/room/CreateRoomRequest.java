package com.free.easyLearn.dto.room;

import com.free.easyLearn.entity.Room;
import com.free.easyLearn.entity.Student;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CreateRoomRequest {

    @NotBlank(message = "Room name is required")
    @Size(min = 3, max = 255)
    private String name;

    @NotBlank(message = "Language is required")
    private String language;

    @NotNull(message = "Level is required")
    private Student.LanguageLevel level;

    @NotBlank(message = "Objective is required")
    private String objective;

    @NotNull(message = "Scheduled time is required")
    @Future(message = "Scheduled time must be in the future")
    private LocalDateTime scheduledAt;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 480, message = "Duration cannot exceed 8 hours")
    private Integer duration;

    @Min(value = 1, message = "Max students must be at least 1")
    @Max(value = 100, message = "Max students cannot exceed 100")
    private Integer maxStudents = 30;

    @NotNull(message = "Animator type is required")
    private Room.AnimatorType animatorType;

    @NotNull(message = "Professor ID is required")
    private UUID professorId;

    private List<UUID> invitedStudents;
}
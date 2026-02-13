package com.free.easyLearn.dto.room;

import com.free.easyLearn.entity.Room;
import com.free.easyLearn.entity.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDTO {
    private UUID id;
    private String name;
    private String language;
    private Student.LanguageLevel level;
    private String objective;
    private LocalDateTime scheduledAt;
    private Integer duration;
    private Integer maxStudents;
    private Room.RoomStatus status;
    private Room.AnimatorType animatorType;
    private UUID professorId;
    private String professorName;
    private String livekitRoomName;
    private List<UUID> invitedStudents;
    private List<UUID> joinedStudents;
    private Integer participantsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
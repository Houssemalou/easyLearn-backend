package com.free.easyLearn.dto.room;

import com.free.easyLearn.entity.Room;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateRoomRequest {

    @Size(min = 3, max = 255)
    private String name;

    private String objective;

    private LocalDateTime scheduledAt;

    @Min(value = 15)
    @Max(value = 480)
    private Integer duration;

    @Min(value = 1)
    @Max(value = 100)
    private Integer maxStudents;

    private Room.RoomStatus status;

    private List<UUID> invitedStudents;
}
package com.free.easyLearn.dto.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDTO {
    private UUID id;
    private UUID roomId;
    private UUID studentId;
    private String studentName;
    private String studentAvatar;
    private Boolean invited;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private Boolean isMuted;
    private Boolean isCameraOn;
    private Boolean isScreenSharing;
    private Boolean handRaised;
    private Boolean isPinged;
    private LocalDateTime pingedAt;
}
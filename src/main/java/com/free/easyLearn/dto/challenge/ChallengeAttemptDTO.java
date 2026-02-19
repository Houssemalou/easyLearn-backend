package com.free.easyLearn.dto.challenge;

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
public class ChallengeAttemptDTO {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentAvatar;
    private UUID challengeId;
    private Integer attempts;
    private Integer pointsEarned;
    private Boolean isCorrect;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}

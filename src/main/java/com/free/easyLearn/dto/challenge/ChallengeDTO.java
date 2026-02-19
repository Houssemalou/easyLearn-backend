package com.free.easyLearn.dto.challenge;

import com.free.easyLearn.entity.Challenge;
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
public class ChallengeDTO {
    private UUID id;
    private UUID professorId;
    private String professorName;
    private Challenge.ChallengeSubject subject;
    private Challenge.ChallengeDifficulty difficulty;
    private String title;
    private String question;
    private List<String> options;
    private Integer correctAnswer;
    private Integer basePoints;
    private String imageUrl;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private long participantCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

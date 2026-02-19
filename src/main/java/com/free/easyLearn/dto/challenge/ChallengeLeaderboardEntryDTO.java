package com.free.easyLearn.dto.challenge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeLeaderboardEntryDTO {
    private Integer rank;
    private UUID studentId;
    private String studentName;
    private String studentAvatar;
    private Long totalPoints;
    private Long challengesCompleted;
    private Long perfectAnswers;
}

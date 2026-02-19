package com.free.easyLearn.dto.challenge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeStatsDTO {
    private long totalChallenges;
    private long activeChallenges;
    private long totalParticipants;
    private double averageScore;
    private double successRate;
}

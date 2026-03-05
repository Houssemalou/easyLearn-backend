package com.free.easyLearn.dto.challenge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGameStatsDTO {
    private long totalPoints;
    private int level;
    private long pointsToNextLevel;
    private int streak; // consecutive days with a correct answer
    private long challengesCompleted;
    private long totalActiveChallenges;
}

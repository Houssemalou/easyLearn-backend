package com.free.easyLearn.dto.challenge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerResponse {
    private Boolean isCorrect;
    private Integer correctAnswer; // only revealed on final attempt
    private Integer pointsEarned;
    private Integer attemptNumber;
    private Boolean isFinalAttempt;
}

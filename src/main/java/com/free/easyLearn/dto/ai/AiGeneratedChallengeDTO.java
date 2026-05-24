package com.free.easyLearn.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGeneratedChallengeDTO {
    private String subject;
    private String difficulty;
    private String title;
    private String question;
    private List<String> options;
    private int correctAnswer;
    private int basePoints;
    private String targetLevel;
    private int expiresIn;
}

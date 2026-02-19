package com.free.easyLearn.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionDTO {
    private UUID id;
    private String question;
    private List<String> options;
    private Integer correctAnswer;
    private Integer points;
    private Integer orderIndex;
}

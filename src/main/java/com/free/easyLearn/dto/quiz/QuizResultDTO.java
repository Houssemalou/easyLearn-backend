package com.free.easyLearn.dto.quiz;

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
public class QuizResultDTO {
    private UUID id;
    private UUID quizId;
    private String quizTitle;
    private UUID studentId;
    private String studentName;
    private Integer score;
    private Integer totalQuestions;
    private Boolean passed;
    private LocalDateTime completedAt;
    private List<QuizAnswerDTO> answers;
    private String studentAvatar;
    private String sessionName;
    private String language;
}

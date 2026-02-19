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
public class QuizDTO {
    private UUID id;
    private String title;
    private String description;
    private String language;
    private Integer timeLimit;
    private Integer passingScore;
    private Boolean isPublished;
    private UUID sessionId;
    private UUID createdBy;
    private String createdByName;
    private List<QuizQuestionDTO> questions;
    private LocalDateTime createdAt;
}

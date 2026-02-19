package com.free.easyLearn.dto.evaluation;

import com.free.easyLearn.entity.Student;
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
public class EvaluationDTO {
    private UUID id;
    private UUID studentId;
    private String studentName;
    private String studentAvatar;
    private UUID professorId;
    private String professorName;
    private String language;
    private Integer pronunciation;
    private Integer grammar;
    private Integer vocabulary;
    private Integer fluency;
    private Integer overallScore;
    private Student.LanguageLevel assignedLevel;
    private Student.LanguageLevel previousLevel;
    private String feedback;
    private List<String> strengths;
    private List<String> areasToImprove;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

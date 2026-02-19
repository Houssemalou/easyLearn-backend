package com.free.easyLearn.dto.evaluation;

import com.free.easyLearn.entity.Student;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateEvaluationRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotBlank(message = "Language is required")
    private String language;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer pronunciation;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer grammar;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer vocabulary;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer fluency;

    private Student.LanguageLevel assignedLevel;

    private String feedback;

    private List<String> strengths;

    private List<String> areasToImprove;
}

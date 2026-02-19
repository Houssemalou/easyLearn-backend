package com.free.easyLearn.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateQuizRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String language;

    private UUID sessionId;

    private Integer timeLimit;

    private Integer passingScore;

    @NotNull
    @Size(min = 1)
    @Valid
    private List<CreateQuizQuestionRequest> questions;
}

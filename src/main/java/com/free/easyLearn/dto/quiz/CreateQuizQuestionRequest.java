package com.free.easyLearn.dto.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuizQuestionRequest {

    @NotBlank
    private String question;

    @NotNull
    @Size(min = 2)
    private List<String> options;

    @NotNull
    private Integer correctAnswer;

    private Integer points;
}

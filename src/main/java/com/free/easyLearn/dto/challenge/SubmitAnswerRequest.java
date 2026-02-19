package com.free.easyLearn.dto.challenge;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {

    @NotNull(message = "Challenge ID is required")
    private UUID challengeId;

    @NotNull(message = "Selected answer is required")
    @Min(value = 0, message = "Answer must be between 0 and 3")
    @Max(value = 3, message = "Answer must be between 0 and 3")
    private Integer selectedAnswer;
}

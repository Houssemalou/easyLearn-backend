package com.free.easyLearn.dto.challenge;

import com.free.easyLearn.entity.Challenge;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChallengeRequest {

    @NotNull(message = "Subject is required")
    private Challenge.ChallengeSubject subject;

    @NotNull(message = "Difficulty is required")
    private Challenge.ChallengeDifficulty difficulty;

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 255)
    private String title;

    @NotBlank(message = "Question is required")
    private String question;

    @NotNull(message = "Options are required")
    @Size(min = 4, max = 4, message = "Exactly 4 options are required")
    private List<String> options;

    @NotNull(message = "Correct answer index is required")
    @Min(value = 0, message = "Correct answer must be between 0 and 3")
    @Max(value = 3, message = "Correct answer must be between 0 and 3")
    private Integer correctAnswer;

    @NotNull(message = "Base points are required")
    @Min(value = 10, message = "Base points must be at least 10")
    @Max(value = 200, message = "Base points cannot exceed 200")
    private Integer basePoints;

    private String imageUrl;

    @NotNull(message = "Expiration time is required")
    @Min(value = 1, message = "Expires in must be at least 1 hour")
    @Max(value = 168, message = "Expires in cannot exceed 168 hours (7 days)")
    private Integer expiresIn; // hours
}

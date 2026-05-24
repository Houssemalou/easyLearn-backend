package com.free.easyLearn.dto.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateRequest {
    @NotBlank(message = "fileBase64 is required")
    private String fileBase64;

    @NotBlank(message = "mimeType is required")
    private String mimeType;

    @NotBlank(message = "subject is required")
    private String subject;

    @NotBlank(message = "level is required")
    private String level;

    @NotNull(message = "count is required")
    @Min(value = 1, message = "count must be at least 1")
    @Max(value = 10, message = "count cannot exceed 10")
    private Integer count;
}

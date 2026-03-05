package com.free.easyLearn.dto.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateAccessTokenRequest {

    @NotNull(message = "Role is required")
    private String role; // "STUDENT", "PROFESSOR", "ADMIN"

    @Min(value = 1, message = "Count must be at least 1")
    @Max(value = 100, message = "Count must be at most 100")
    private Integer count = 1;
}
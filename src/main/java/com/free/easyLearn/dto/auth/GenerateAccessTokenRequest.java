package com.free.easyLearn.dto.auth;

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
}
package com.free.easyLearn.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateAccessTokenResponse {

    private String token;
    private String role;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
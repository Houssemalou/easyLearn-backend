package com.free.easyLearn.dto.auth;

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
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String type = "Bearer";
    private UUID userId;
    private String email;
    private String name;
    private String role;
    private String bio;
    private String avatar;
    private List<String> languages;
    private String specialization;
    private LocalDateTime joinedAt;
    private Long expiresIn; // milliseconds
}

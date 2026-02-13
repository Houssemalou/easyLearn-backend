package com.free.easyLearn.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StudentRegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;


    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "uniqueCode is required")
    @Size(min = 2, max = 100, message = "UniqueCode must be between 2 and 100 characters")
    private String uniqueCode;

    @NotBlank(message = "Nickname is required")
    @Size(min = 2, max = 100, message = "Nickname must be between 2 and 100 characters")
    private String nickname;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    private String avatar;

    @NotNull(message = "Level is required")
    private String level; // A1, A2, B1, B2

    @NotBlank(message = "Access token is required")
    private String accessToken;
}
package com.free.easyLearn.dto.professor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateProfessorRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255)
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6)
    private String password;
    
    private String bio;
    
    @NotEmpty(message = "At least one language is required")
    private List<String> languages;
    
    @NotBlank(message = "Specialization is required")
    private String specialization;
}

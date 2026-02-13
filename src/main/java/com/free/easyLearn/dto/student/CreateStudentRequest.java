package com.free.easyLearn.dto.student;


import com.free.easyLearn.entity.Student;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateStudentRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255)
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6)
    private String password;
    
    @NotBlank(message = "Nickname is required")
    @Size(min = 2, max = 100)
    private String nickname;
    
    private String bio;
    
    @NotNull(message = "Level is required")
    private Student.LanguageLevel level;
}

package com.free.easyLearn.dto.student;

import com.free.easyLearn.entity.Student;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateStudentRequest {
    
    @Size(min = 2, max = 255)
    private String name;
    
    @Size(min = 2, max = 100)
    private String nickname;
    
    private String bio;
    
    private String avatar;
    
    private Student.LanguageLevel level;
}

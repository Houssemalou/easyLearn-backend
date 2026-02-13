package com.free.easyLearn.dto.evaluation;

import com.free.easyLearn.entity.Student;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentLevelRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "New level is required")
    private Student.LanguageLevel newLevel;

    private String reason;
}

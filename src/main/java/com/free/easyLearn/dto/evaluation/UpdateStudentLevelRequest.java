package com.free.easyLearn.dto.evaluation;

import com.free.easyLearn.entity.Student;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

package com.free.easyLearn.dto.student;

import com.free.easyLearn.entity.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {
    private UUID id;
    private String name;
    private String email;
    private String avatar;
    private String nickname;
    private String bio;
    private Student.LanguageLevel level;
    private LocalDateTime joinedAt;
    private Integer totalSessions;
    private BigDecimal hoursLearned;
    private StudentSkillsDTO skills;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

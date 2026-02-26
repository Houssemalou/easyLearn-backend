package com.free.easyLearn.dto.professor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessorDTO {
    private UUID id;
    private String name;
    private String email;
    private String avatar;
    private String bio;
    private List<String> languages;
    private String specialization;
    private LocalDateTime joinedAt;
    private Integer totalSessions;
    private BigDecimal rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private java.util.UUID createdBy;
}

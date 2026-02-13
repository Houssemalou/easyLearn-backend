package com.free.easyLearn.dto.student;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentSkillsDTO {
    private Integer pronunciation;
    private Integer grammar;
    private Integer vocabulary;
    private Integer fluency;
}

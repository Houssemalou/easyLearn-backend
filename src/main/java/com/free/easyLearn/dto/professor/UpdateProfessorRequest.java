package com.free.easyLearn.dto.professor;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateProfessorRequest {

    @Size(min = 2, max = 255)
    private String name;

    private String bio;

    private String avatar;

    private List<String> languages;

    private String specialization;
}

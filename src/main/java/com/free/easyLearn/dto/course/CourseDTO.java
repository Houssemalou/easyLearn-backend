package com.free.easyLearn.dto.course;

import com.free.easyLearn.entity.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private UUID id;
    private String name;
    private Student.LanguageLevel level;
    private String description;
    private String fileName;
    private String objectKey;
    private Long fileSize;
    private String contentType;
    private UUID professorId;
    private String fileUrl;
    private List<String> fileNames;
    private List<String> objectKeys;
    private List<String> fileUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

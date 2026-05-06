package com.free.easyLearn.dto.document;

import com.free.easyLearn.entity.LearningDocument;
import com.free.easyLearn.entity.Student;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningDocumentDTO {
    private UUID id;
    private String title;
    private LearningDocument.DocumentCategory category;
    private LearningDocument.DocumentSubject subject;
    private Student.LanguageLevel level;
    private String description;

    private String fileName;
    private String objectKey;
    private String contentType;
    private Long fileSize;
    private String fileUrl;

    private String correctionFileName;
    private String correctionObjectKey;
    private String correctionContentType;
    private Long correctionFileSize;
    private String correctionFileUrl;
    private LocalDateTime correctionAvailableAt;

    private Boolean isPublished;
    private UUID professorId;
    private UUID professorUserId;
    private String professorName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

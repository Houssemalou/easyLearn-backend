package com.free.easyLearn.dto.document;

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
public class LearningDocumentCommentDTO {
    private UUID id;
    private UUID documentId;
    private UUID professorId;
    private UUID professorUserId;
    private String professorName;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

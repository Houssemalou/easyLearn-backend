package com.free.easyLearn.dto.document;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentAccessDTO {
    private UUID id;
    private UUID documentId;
    private UUID studentId;
    private String studentName;
    private String studentAvatar;
    private String level;
    private LocalDateTime accessedAt;
    private LocalDateTime completedAt;
    private Integer readTimeSeconds;
}

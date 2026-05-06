package com.free.easyLearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "learning_documents")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject", length = 40)
    private DocumentSubject subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Student.LanguageLevel level;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "correction_file_name")
    private String correctionFileName;

    @Column(name = "correction_object_key", unique = true)
    private String correctionObjectKey;

    @Column(name = "correction_content_type")
    private String correctionContentType;

    @Column(name = "correction_file_size")
    private Long correctionFileSize;

    @Column(name = "correction_available_at")
    private LocalDateTime correctionAvailableAt;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum DocumentCategory {
        COURSE,
        HOMEWORK,
        EXERCISE
    }

    public enum DocumentSubject {
        ARABIC,
        FRENCH,
        ENGLISH,
        MATHEMATICS,
        SCIENCE,
        HISTORY_GEOGRAPHY,
        CIVIC_EDUCATION,
        ISLAMIC_EDUCATION,
        TECHNOLOGY,
        ARTS,
        OTHER
    }
}

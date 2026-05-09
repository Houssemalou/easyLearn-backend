package com.free.easyLearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_access", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "student_id"}),
       indexes = {
           @Index(name = "idx_document_access_document", columnList = "document_id"),
           @Index(name = "idx_document_access_student", columnList = "student_id"),
           @Index(name = "idx_document_access_accessed_at", columnList = "accessed_at")
       })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private LearningDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @CreatedDate
    @Column(name = "accessed_at", nullable = false, updatable = false)
    private LocalDateTime accessedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "read_time_seconds")
    private Integer readTimeSeconds;
}

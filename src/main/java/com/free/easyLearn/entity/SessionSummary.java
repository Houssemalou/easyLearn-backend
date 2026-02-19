package com.free.easyLearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "session_summaries")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "key_topics", columnDefinition = "jsonb")
    private List<String> keyTopics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vocabulary_covered", columnDefinition = "jsonb")
    private List<String> vocabularyCovered;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grammar_points", columnDefinition = "jsonb")
    private List<String> grammarPoints;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strengths", columnDefinition = "jsonb")
    private List<String> strengths;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "areas_to_improve", columnDefinition = "jsonb")
    private List<String> areasToImprove;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendations", columnDefinition = "jsonb")
    private List<String> recommendations;

    @Column(name = "next_session_focus", columnDefinition = "TEXT")
    private String nextSessionFocus;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "pronunciation_score")
    private Integer pronunciationScore;

    @Column(name = "grammar_score")
    private Integer grammarScore;

    @Column(name = "vocabulary_score")
    private Integer vocabularyScore;

    @Column(name = "fluency_score")
    private Integer fluencyScore;

    @Column(name = "participation_score")
    private Integer participationScore;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

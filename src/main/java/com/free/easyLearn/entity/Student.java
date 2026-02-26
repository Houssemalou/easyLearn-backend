package com.free.easyLearn.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "students")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String nickname;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LanguageLevel level;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "total_sessions")
    private Integer totalSessions = 0;

    @Column(name = "hours_learned", precision = 10, scale = 2)
    private BigDecimal hoursLearned = BigDecimal.ZERO;

    @OneToOne(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private StudentSkills skills;

    @Column(name = "unique_code", nullable = false, unique = true)
    private String uniqueCode;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public enum LanguageLevel {
        A1,
        A2,
        B1,
        B2,
        C1,
        C2,
        YEAR1,
        YEAR2,
        YEAR3,
        YEAR4,
        YEAR5,
        YEAR6,
        YEAR7,
        YEAR8,
        YEAR9
    }
}

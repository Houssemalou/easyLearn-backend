package com.free.easyLearn.entity;

import com.free.easyLearn.converter.StringListJsonConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "quiz_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Convert(converter = StringListJsonConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> options;

    @Column(name = "correct_answer", nullable = false)
    private Integer correctAnswer;

    @Column
    @Builder.Default
    private Integer points = 1;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}

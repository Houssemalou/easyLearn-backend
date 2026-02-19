package com.free.easyLearn.repository;

import com.free.easyLearn.entity.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, UUID> {

    List<QuizResult> findByStudentId(UUID studentId);

    List<QuizResult> findByQuizId(UUID quizId);

    Optional<QuizResult> findByQuizIdAndStudentId(UUID quizId, UUID studentId);
}

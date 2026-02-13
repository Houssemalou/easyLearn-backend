package com.free.easyLearn.repository;

import com.free.easyLearn.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    List<Evaluation> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    List<Evaluation> findByProfessorIdOrderByCreatedAtDesc(UUID professorId);

    List<Evaluation> findByStudentIdAndLanguageOrderByCreatedAtDesc(UUID studentId, String language);

    @Query("SELECT e FROM Evaluation e WHERE e.student.user.id = :userId ORDER BY e.createdAt DESC")
    List<Evaluation> findByStudentUserId(@Param("userId") UUID userId);

    @Query("SELECT e FROM Evaluation e WHERE e.professor.user.id = :userId ORDER BY e.createdAt DESC")
    List<Evaluation> findByProfessorUserId(@Param("userId") UUID userId);

    // Stats queries
    @Query("SELECT AVG(e.overallScore) FROM Evaluation e")
    Double getAverageScore();

    @Query("SELECT AVG(e.overallScore) FROM Evaluation e WHERE e.professor.id = :professorId")
    Double getAverageScoreByProfessorId(@Param("professorId") UUID professorId);

    @Query("SELECT AVG(e.overallScore) FROM Evaluation e WHERE e.student.id = :studentId")
    Double getAverageScoreByStudentId(@Param("studentId") UUID studentId);

    long countByProfessorId(UUID professorId);

    long countByStudentId(UUID studentId);
}


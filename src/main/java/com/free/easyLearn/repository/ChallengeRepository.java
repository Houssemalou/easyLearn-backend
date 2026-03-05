package com.free.easyLearn.repository;

import com.free.easyLearn.entity.Challenge;
import com.free.easyLearn.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {

    @Query("SELECT c FROM Challenge c WHERE c.isActive = true AND c.expiresAt > :now ORDER BY c.createdAt DESC")
    List<Challenge> findActiveChallenges(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Challenge c WHERE c.isActive = true AND c.expiresAt > :now " +
            "AND (c.targetLevel IS NULL OR c.targetLevel = :level) " +
            "AND c.professor.createdBy.id = :createdById " +
            "ORDER BY c.createdAt DESC")
    List<Challenge> findActiveChallengesByLevelAndCreatedBy(
            @Param("now") LocalDateTime now,
            @Param("level") Student.LanguageLevel level,
            @Param("createdById") UUID createdById);

    @Query("SELECT c FROM Challenge c WHERE c.isActive = true AND c.expiresAt > :now " +
            "AND (c.targetLevel IS NULL OR c.targetLevel = :level) " +
            "AND c.professor.user.id = :professorUserId " +
            "ORDER BY c.createdAt DESC")
    List<Challenge> findActiveChallengesByLevelAndProfessor(
            @Param("now") LocalDateTime now,
            @Param("level") Student.LanguageLevel level,
            @Param("professorUserId") UUID professorUserId);

    @Query("SELECT c FROM Challenge c WHERE c.professor.user.id = :userId ORDER BY c.createdAt DESC")
    List<Challenge> findByProfessorUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(c) FROM Challenge c WHERE c.professor.user.id = :userId")
    long countByProfessorUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(c) FROM Challenge c WHERE c.professor.user.id = :userId AND c.isActive = true AND c.expiresAt > :now")
    long countActiveChallengesByProfessorUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}

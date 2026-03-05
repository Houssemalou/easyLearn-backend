package com.free.easyLearn.repository;

import com.free.easyLearn.entity.ChallengeAttempt;
import com.free.easyLearn.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChallengeAttemptRepository extends JpaRepository<ChallengeAttempt, UUID> {

    Optional<ChallengeAttempt> findByChallengeIdAndStudentId(UUID challengeId, UUID studentId);

    @Query("SELECT a FROM ChallengeAttempt a WHERE a.student.user.id = :userId ORDER BY a.createdAt DESC")
    List<ChallengeAttempt> findByStudentUserId(@Param("userId") UUID userId);

    List<ChallengeAttempt> findByChallengeIdOrderByPointsEarnedDesc(UUID challengeId);

    long countByChallengeId(UUID challengeId);

    // Leaderboard: aggregate by student
    @Query("SELECT a.student.id, a.student.user.name, a.student.user.avatar, " +
            "SUM(a.pointsEarned), COUNT(a), " +
            "SUM(CASE WHEN a.isCorrect = true AND a.attempts = 1 THEN 1 ELSE 0 END) " +
            "FROM ChallengeAttempt a " +
            "WHERE a.isCorrect = true " +
            "GROUP BY a.student.id, a.student.user.name, a.student.user.avatar " +
            "ORDER BY SUM(a.pointsEarned) DESC")
    List<Object[]> getLeaderboard();

    // Leaderboard filtered by student level
    @Query("SELECT a.student.id, a.student.user.name, a.student.user.avatar, " +
            "SUM(a.pointsEarned), COUNT(a), " +
            "SUM(CASE WHEN a.isCorrect = true AND a.attempts = 1 THEN 1 ELSE 0 END) " +
            "FROM ChallengeAttempt a " +
            "WHERE a.isCorrect = true AND a.student.level = :level " +
            "GROUP BY a.student.id, a.student.user.name, a.student.user.avatar " +
            "ORDER BY SUM(a.pointsEarned) DESC")
    List<Object[]> getLeaderboardByLevel(@Param("level") Student.LanguageLevel level);

    // Student stats: total points earned by a student
    @Query("SELECT COALESCE(SUM(a.pointsEarned), 0) FROM ChallengeAttempt a WHERE a.student.id = :studentId AND a.isCorrect = true")
    long sumPointsByStudentId(@Param("studentId") UUID studentId);

    // Student stats: count of challenges completed correctly by a student
    @Query("SELECT COUNT(a) FROM ChallengeAttempt a WHERE a.student.id = :studentId AND a.isCorrect = true")
    long countCorrectByStudentId(@Param("studentId") UUID studentId);

    // Student stats: distinct dates of correct answers (for streak calculation)
    @Query("SELECT DISTINCT CAST(a.completedAt AS date) FROM ChallengeAttempt a WHERE a.student.id = :studentId AND a.isCorrect = true AND a.completedAt IS NOT NULL ORDER BY CAST(a.completedAt AS date) DESC")
    List<java.sql.Date> findDistinctCorrectDatesByStudentId(@Param("studentId") UUID studentId);

    // Professor stats: average points earned across all attempts on professor's challenges
    @Query("SELECT AVG(a.pointsEarned) FROM ChallengeAttempt a WHERE a.challenge.professor.user.id = :userId")
    Double getAveragePointsByProfessorUserId(@Param("userId") UUID userId);

    // Professor stats: distinct participants on professor's challenges
    @Query("SELECT COUNT(DISTINCT a.student.id) FROM ChallengeAttempt a WHERE a.challenge.professor.user.id = :userId")
    long countDistinctParticipantsByProfessorUserId(@Param("userId") UUID userId);

    // Professor stats: correct attempts count
    @Query("SELECT COUNT(a) FROM ChallengeAttempt a WHERE a.challenge.professor.user.id = :userId AND a.isCorrect = true")
    long countCorrectByProfessorUserId(@Param("userId") UUID userId);

    // Professor stats: total attempts count
    @Query("SELECT COUNT(a) FROM ChallengeAttempt a WHERE a.challenge.professor.user.id = :userId")
    long countTotalAttemptsByProfessorUserId(@Param("userId") UUID userId);
}

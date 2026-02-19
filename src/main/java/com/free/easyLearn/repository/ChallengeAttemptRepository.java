package com.free.easyLearn.repository;

import com.free.easyLearn.entity.ChallengeAttempt;
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

package com.free.easyLearn.service;

import com.free.easyLearn.dto.challenge.*;
import com.free.easyLearn.entity.Challenge;
import com.free.easyLearn.entity.ChallengeAttempt;
import com.free.easyLearn.entity.Professor;
import com.free.easyLearn.entity.Student;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.exception.ResourceNotFoundException;
import com.free.easyLearn.repository.ChallengeAttemptRepository;
import com.free.easyLearn.repository.ChallengeRepository;
import com.free.easyLearn.repository.ProfessorRepository;
import com.free.easyLearn.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChallengeService {

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private ChallengeAttemptRepository attemptRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private StudentRepository studentRepository;

    // ========== Professor Methods ==========

    @Transactional
    public ChallengeDTO createChallenge(UUID professorUserId, CreateChallengeRequest request) {
        Professor professor = professorRepository.findByUserId(professorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        Challenge challenge = Challenge.builder()
                .professor(professor)
                .subject(request.getSubject())
                .difficulty(request.getDifficulty())
                .title(request.getTitle())
                .question(request.getQuestion())
                .options(request.getOptions())
                .correctAnswer(request.getCorrectAnswer())
                .basePoints(request.getBasePoints())
                .imageUrl(request.getImageUrl())
                .expiresAt(LocalDateTime.now().plusHours(request.getExpiresIn()))
                .isActive(true)
                .build();

        challenge = challengeRepository.save(challenge);
        return mapToDTO(challenge, 0);
    }

    public List<ChallengeDTO> getMyChallenges(UUID professorUserId) {
        List<Challenge> challenges = challengeRepository.findByProfessorUserId(professorUserId);
        return challenges.stream()
                .map(c -> mapToDTO(c, attemptRepository.countByChallengeId(c.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteChallenge(UUID professorUserId, UUID challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found"));

        if (!challenge.getProfessor().getUser().getId().equals(professorUserId)) {
            throw new BadRequestException("You can only delete your own challenges");
        }

        challengeRepository.delete(challenge);
    }

    public ChallengeStatsDTO getChallengeStats(UUID professorUserId) {
        long totalChallenges = challengeRepository.countByProfessorUserId(professorUserId);
        long activeChallenges = challengeRepository.countActiveChallengesByProfessorUserId(professorUserId, LocalDateTime.now());
        long totalParticipants = attemptRepository.countDistinctParticipantsByProfessorUserId(professorUserId);
        Double avgScore = attemptRepository.getAveragePointsByProfessorUserId(professorUserId);
        long correctCount = attemptRepository.countCorrectByProfessorUserId(professorUserId);
        long totalAttempts = attemptRepository.countTotalAttemptsByProfessorUserId(professorUserId);

        double successRate = totalAttempts > 0 ? Math.round((double) correctCount / totalAttempts * 100.0) : 0;

        return ChallengeStatsDTO.builder()
                .totalChallenges(totalChallenges)
                .activeChallenges(activeChallenges)
                .totalParticipants(totalParticipants)
                .averageScore(avgScore != null ? Math.round(avgScore) : 0)
                .successRate(successRate)
                .build();
    }

    public List<ChallengeAttemptDTO> getChallengeAttempts(UUID professorUserId, UUID challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found"));

        if (!challenge.getProfessor().getUser().getId().equals(professorUserId)) {
            throw new BadRequestException("You can only view attempts on your own challenges");
        }

        List<ChallengeAttempt> attempts = attemptRepository.findByChallengeIdOrderByPointsEarnedDesc(challengeId);
        return attempts.stream()
                .map(this::mapToAttemptDTO)
                .collect(Collectors.toList());
    }

    // ========== Student Methods ==========

    public List<ChallengeStudentDTO> getActiveChallenges() {
        List<Challenge> challenges = challengeRepository.findActiveChallenges(LocalDateTime.now());
        return challenges.stream()
                .map(this::mapToStudentDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SubmitAnswerResponse submitAnswer(UUID studentUserId, SubmitAnswerRequest request) {
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Challenge challenge = challengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new ResourceNotFoundException("Challenge not found"));

        if (!challenge.getIsActive() || challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This challenge is no longer active");
        }

        // Find or create attempt
        ChallengeAttempt attempt = attemptRepository
                .findByChallengeIdAndStudentId(challenge.getId(), student.getId())
                .orElse(null);

        if (attempt == null) {
            attempt = ChallengeAttempt.builder()
                    .challenge(challenge)
                    .student(student)
                    .attempts(0)
                    .pointsEarned(0)
                    .isCorrect(false)
                    .build();
        }

        // Validate
        if (attempt.getIsCorrect()) {
            throw new BadRequestException("You already answered this challenge correctly");
        }
        if (attempt.getAttempts() >= 2) {
            throw new BadRequestException("Maximum attempts reached for this challenge");
        }

        // Process attempt
        int newAttemptCount = attempt.getAttempts() + 1;
        attempt.setAttempts(newAttemptCount);

        boolean correct = request.getSelectedAnswer().equals(challenge.getCorrectAnswer());
        boolean isFinalAttempt = newAttemptCount >= 2 || correct;

        if (correct) {
            int points = calculatePoints(challenge.getBasePoints(), challenge.getDifficulty(), newAttemptCount);
            attempt.setIsCorrect(true);
            attempt.setPointsEarned(points);
            attempt.setCompletedAt(LocalDateTime.now());
        } else if (isFinalAttempt) {
            attempt.setCompletedAt(LocalDateTime.now());
        }

        attempt = attemptRepository.save(attempt);

        return SubmitAnswerResponse.builder()
                .isCorrect(correct)
                .correctAnswer(isFinalAttempt ? challenge.getCorrectAnswer() : null)
                .pointsEarned(attempt.getPointsEarned())
                .attemptNumber(newAttemptCount)
                .isFinalAttempt(isFinalAttempt)
                .build();
    }

    public List<ChallengeAttemptDTO> getMyAttempts(UUID studentUserId) {
        List<ChallengeAttempt> attempts = attemptRepository.findByStudentUserId(studentUserId);
        return attempts.stream()
                .map(this::mapToAttemptDTO)
                .collect(Collectors.toList());
    }

    // ========== Common Methods ==========

    public List<ChallengeLeaderboardEntryDTO> getLeaderboard() {
        List<Object[]> rows = attemptRepository.getLeaderboard();
        List<ChallengeLeaderboardEntryDTO> leaderboard = new ArrayList<>();

        int rank = 1;
        for (Object[] row : rows) {
            leaderboard.add(ChallengeLeaderboardEntryDTO.builder()
                    .rank(rank++)
                    .studentId((UUID) row[0])
                    .studentName((String) row[1])
                    .studentAvatar((String) row[2])
                    .totalPoints((Long) row[3])
                    .challengesCompleted((Long) row[4])
                    .perfectAnswers((Long) row[5])
                    .build());
        }

        return leaderboard;
    }

    // ========== Private Helpers ==========

    private int calculatePoints(int basePoints, Challenge.ChallengeDifficulty difficulty, int attempt) {
        double multiplier;
        switch (difficulty) {
            case easy:
                multiplier = 1.0;
                break;
            case medium:
                multiplier = 1.5;
                break;
            case hard:
                multiplier = 2.0;
                break;
            default:
                multiplier = 1.0;
        }

        int totalPoints = (int) Math.round(basePoints * multiplier);

        if (attempt == 1) return totalPoints;
        if (attempt == 2) return (int) Math.round(totalPoints / 2.0);
        return 0;
    }

    private ChallengeDTO mapToDTO(Challenge challenge, long participantCount) {
        return ChallengeDTO.builder()
                .id(challenge.getId())
                .professorId(challenge.getProfessor().getId())
                .professorName(challenge.getProfessor().getUser().getName())
                .subject(challenge.getSubject())
                .difficulty(challenge.getDifficulty())
                .title(challenge.getTitle())
                .question(challenge.getQuestion())
                .options(challenge.getOptions())
                .correctAnswer(challenge.getCorrectAnswer())
                .basePoints(challenge.getBasePoints())
                .imageUrl(challenge.getImageUrl())
                .expiresAt(challenge.getExpiresAt())
                .isActive(challenge.getIsActive())
                .participantCount(participantCount)
                .createdAt(challenge.getCreatedAt())
                .updatedAt(challenge.getUpdatedAt())
                .build();
    }

    private ChallengeStudentDTO mapToStudentDTO(Challenge challenge) {
        return ChallengeStudentDTO.builder()
                .id(challenge.getId())
                .professorId(challenge.getProfessor().getId())
                .professorName(challenge.getProfessor().getUser().getName())
                .subject(challenge.getSubject())
                .difficulty(challenge.getDifficulty())
                .title(challenge.getTitle())
                .question(challenge.getQuestion())
                .options(challenge.getOptions())
                .basePoints(challenge.getBasePoints())
                .imageUrl(challenge.getImageUrl())
                .expiresAt(challenge.getExpiresAt())
                .isActive(challenge.getIsActive())
                .createdAt(challenge.getCreatedAt())
                .build();
    }

    private ChallengeAttemptDTO mapToAttemptDTO(ChallengeAttempt attempt) {
        return ChallengeAttemptDTO.builder()
                .id(attempt.getId())
                .studentId(attempt.getStudent().getId())
                .studentName(attempt.getStudent().getUser().getName())
                .studentAvatar(attempt.getStudent().getUser().getAvatar())
                .challengeId(attempt.getChallenge().getId())
                .attempts(attempt.getAttempts())
                .pointsEarned(attempt.getPointsEarned())
                .isCorrect(attempt.getIsCorrect())
                .completedAt(attempt.getCompletedAt())
                .createdAt(attempt.getCreatedAt())
                .build();
    }
}

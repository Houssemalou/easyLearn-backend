package com.free.easyLearn.service;

import com.free.easyLearn.dto.stats.AdminStatsDTO;
import com.free.easyLearn.dto.stats.ProfessorStatsDTO;
import com.free.easyLearn.dto.stats.StudentStatsDTO;
import com.free.easyLearn.entity.*;
import com.free.easyLearn.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatsService {

    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final EvaluationRepository evaluationRepository;

    // ===========================
    // ADMIN STATS
    // ===========================
    public AdminStatsDTO getAdminStats() {
        log.info("Fetching admin statistics");

        long totalStudents = studentRepository.count();
        long totalProfessors = professorRepository.count();
        long activeRooms = roomRepository.countByStatus(Room.RoomStatus.LIVE);
        long scheduledSessions = roomRepository.countByStatus(Room.RoomStatus.SCHEDULED);
        long completedSessions = roomRepository.countByStatus(Room.RoomStatus.COMPLETED);
        long totalEvaluations = evaluationRepository.count();
        Double avgScore = evaluationRepository.getAverageScore();

        // Live rooms
        List<Room> liveRooms = roomRepository.findByStatus(Room.RoomStatus.LIVE);
        List<AdminStatsDTO.RoomSummaryDTO> liveRoomDTOs = liveRooms.stream()
                .map(this::toAdminRoomSummary)
                .collect(Collectors.toList());

        // Upcoming sessions (limit 5)
        List<Room> upcomingRooms = roomRepository.findByStatus(Room.RoomStatus.SCHEDULED);
        List<AdminStatsDTO.RoomSummaryDTO> upcomingDTOs = upcomingRooms.stream()
                .limit(5)
                .map(this::toAdminRoomSummary)
                .collect(Collectors.toList());

        // Recent students (limit 6)
        List<Student> recentStudents = studentRepository.findRecentStudents(PageRequest.of(0, 6));
        List<AdminStatsDTO.StudentSummaryDTO> recentStudentDTOs = recentStudents.stream()
                .map(this::toStudentSummary)
                .collect(Collectors.toList());

        // Level distribution
        List<Object[]> levelCounts = studentRepository.countByLevel();
        List<AdminStatsDTO.LevelDistributionDTO> levelDist = levelCounts.stream()
                .map(row -> AdminStatsDTO.LevelDistributionDTO.builder()
                        .level(row[0].toString())
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());

        return AdminStatsDTO.builder()
                .totalStudents(totalStudents)
                .totalProfessors(totalProfessors)
                .activeRooms(activeRooms)
                .scheduledSessions(scheduledSessions)
                .completedSessions(completedSessions)
                .totalEvaluations(totalEvaluations)
                .averageEvaluationScore(avgScore != null ? avgScore : 0.0)
                .liveRooms(liveRoomDTOs)
                .upcomingSessions(upcomingDTOs)
                .recentStudents(recentStudentDTOs)
                .levelDistribution(levelDist)
                .build();
    }

    // ===========================
    // PROFESSOR STATS
    // ===========================
    public ProfessorStatsDTO getProfessorStats(UUID professorId) {
        log.info("Fetching professor statistics for professorId: {}", professorId);

        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new RuntimeException("Professor not found"));

        // Rooms by professor
        List<Room> allRooms = roomRepository.findAllByProfessorId(professorId);
        List<Room> liveRooms = allRooms.stream()
                .filter(r -> r.getStatus() == Room.RoomStatus.LIVE)
                .collect(Collectors.toList());
        long upcomingSessions = allRooms.stream()
                .filter(r -> r.getStatus() == Room.RoomStatus.SCHEDULED)
                .count();
        long completedSessions = allRooms.stream()
                .filter(r -> r.getStatus() == Room.RoomStatus.COMPLETED)
                .count();

        // Unique students across all rooms
        Set<UUID> uniqueStudentIds = new HashSet<>();
        for (Room room : allRooms) {
            List<RoomParticipant> participants = roomParticipantRepository.findByRoomId(room.getId());
            participants.forEach(p -> {
                if (p.getStudent() != null) {
                    uniqueStudentIds.add(p.getStudent().getId());
                }
            });
        }

        // Evaluations
        long totalEvaluations = evaluationRepository.countByProfessorId(professorId);
        Double avgEvalScore = evaluationRepository.getAverageScoreByProfessorId(professorId);

        // Live rooms DTOs
        List<ProfessorStatsDTO.RoomSummaryDTO> liveRoomDTOs = liveRooms.stream()
                .map(this::toProfRoomSummary)
                .collect(Collectors.toList());

        // Upcoming sessions list (limit 5)
        List<ProfessorStatsDTO.RoomSummaryDTO> upcomingDTOs = allRooms.stream()
                .filter(r -> r.getStatus() == Room.RoomStatus.SCHEDULED)
                .sorted(Comparator.comparing(Room::getScheduledAt))
                .limit(5)
                .map(this::toProfRoomSummary)
                .collect(Collectors.toList());

        // My students details: only include students that were created by the same admin/user
        UUID professorCreatedById = professor.getCreatedBy() != null ? professor.getCreatedBy().getId() : null;

        List<ProfessorStatsDTO.StudentSummaryDTO> myStudents = uniqueStudentIds.stream()
                .map(studentRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(s -> {
                    if (professorCreatedById == null) return false;
                    return s.getCreatedBy() != null && professorCreatedById.equals(s.getCreatedBy().getId());
                })
                .limit(10)
                .map(s -> ProfessorStatsDTO.StudentSummaryDTO.builder()
                        .id(s.getId().toString())
                        .name(s.getUser().getName())
                        .avatar(s.getUser().getAvatar())
                        .level(s.getLevel().name())
                        .nickname(s.getNickname())
                        .build())
                .collect(Collectors.toList());

        return ProfessorStatsDTO.builder()
                .totalStudents(myStudents.size())
                .upcomingSessions(upcomingSessions)
                .completedSessions(completedSessions)
                .rating(professor.getRating())
                .totalEvaluations(totalEvaluations)
                .averageEvaluationScore(avgEvalScore != null ? avgEvalScore : 0.0)
                .liveRooms(liveRoomDTOs)
                .upcomingSessionsList(upcomingDTOs)
                .myStudents(myStudents)
                .build();
    }

    // ===========================
    // STUDENT STATS
    // ===========================
    public StudentStatsDTO getStudentStats(UUID studentId) {
        log.info("Fetching student statistics for studentId: {}", studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Rooms for student
        List<Room> allRooms = roomRepository.findAllByStudentId(studentId);
        List<Room> liveRooms = allRooms.stream()
                .filter(r -> r.getStatus() == Room.RoomStatus.LIVE)
                .collect(Collectors.toList());
        long upcomingSessions = allRooms.stream()
                .filter(r -> r.getStatus() == Room.RoomStatus.SCHEDULED)
                .count();
        long completedSessions = allRooms.stream()
                .filter(r -> r.getStatus() == Room.RoomStatus.COMPLETED)
                .count();

        // Skills
        StudentSkills skills = student.getSkills();
        StudentStatsDTO.SkillsDTO skillsDTO;
        double overallProgress;
        if (skills != null) {
            skillsDTO = StudentStatsDTO.SkillsDTO.builder()
                    .pronunciation(skills.getPronunciation())
                    .grammar(skills.getGrammar())
                    .vocabulary(skills.getVocabulary())
                    .fluency(skills.getFluency())
                    .build();
            overallProgress = (skills.getPronunciation() + skills.getGrammar() +
                    skills.getVocabulary() + skills.getFluency()) / 4.0;
        } else {
            skillsDTO = StudentStatsDTO.SkillsDTO.builder()
                    .pronunciation(0).grammar(0).vocabulary(0).fluency(0).build();
            overallProgress = 0;
        }

        // Live rooms
        List<StudentStatsDTO.RoomSummaryDTO> liveRoomDTOs = liveRooms.stream()
                .map(this::toStudentRoomSummary)
                .collect(Collectors.toList());

        // Upcoming sessions (limit 5)
        List<StudentStatsDTO.RoomSummaryDTO> upcomingDTOs = allRooms.stream()
                .filter(r -> r.getStatus() == Room.RoomStatus.SCHEDULED)
                .sorted(Comparator.comparing(Room::getScheduledAt))
                .limit(5)
                .map(this::toStudentRoomSummary)
                .collect(Collectors.toList());

        // Recent evaluations (limit 5)
        List<Evaluation> evaluations = evaluationRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        List<StudentStatsDTO.EvaluationSummaryDTO> recentEvals = evaluations.stream()
                .limit(5)
                .map(e -> StudentStatsDTO.EvaluationSummaryDTO.builder()
                        .id(e.getId().toString())
                        .language(e.getLanguage())
                        .overallScore(e.getOverallScore())
                        .assignedLevel(e.getAssignedLevel() != null ? e.getAssignedLevel().name() : null)
                        .professorName(e.getProfessor().getUser().getName())
                        .createdAt(e.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());

        return StudentStatsDTO.builder()
                .level(student.getLevel().name())
                .hoursLearned(student.getHoursLearned())
                .totalSessions(student.getTotalSessions())
                .upcomingSessions(upcomingSessions)
                .completedSessions(completedSessions)
                .overallProgress(overallProgress)
                .skills(skillsDTO)
                .liveRooms(liveRoomDTOs)
                .upcomingSessionsList(upcomingDTOs)
                .recentEvaluations(recentEvals)
                .build();
    }

    // ===========================
    // MAPPERS
    // ===========================

    private AdminStatsDTO.RoomSummaryDTO toAdminRoomSummary(Room room) {
        long participantCount = roomParticipantRepository.countByRoomIdAndJoinedAtIsNotNull(room.getId());
        return AdminStatsDTO.RoomSummaryDTO.builder()
                .id(room.getId().toString())
                .name(room.getName())
                .language(room.getLanguage())
                .level(room.getLevel().name())
                .scheduledAt(room.getScheduledAt().toString())
                .duration(room.getDuration())
                .maxStudents(room.getMaxStudents())
                .participantCount(participantCount)
                .status(room.getStatus().name())
                .professorName(room.getProfessor() != null ? room.getProfessor().getUser().getName() : null)
                .build();
    }

    private AdminStatsDTO.StudentSummaryDTO toStudentSummary(Student student) {
        double avgSkill = 0;
        if (student.getSkills() != null) {
            StudentSkills sk = student.getSkills();
            avgSkill = (sk.getPronunciation() + sk.getGrammar() + sk.getVocabulary() + sk.getFluency()) / 4.0;
        }
        return AdminStatsDTO.StudentSummaryDTO.builder()
                .id(student.getId().toString())
                .name(student.getUser().getName())
                .email(student.getUser().getEmail())
                .avatar(student.getUser().getAvatar())
                .level(student.getLevel().name())
                .totalSessions(student.getTotalSessions())
                .averageSkill(avgSkill)
                .build();
    }

    private ProfessorStatsDTO.RoomSummaryDTO toProfRoomSummary(Room room) {
        long participantCount = roomParticipantRepository.countByRoomIdAndJoinedAtIsNotNull(room.getId());
        return ProfessorStatsDTO.RoomSummaryDTO.builder()
                .id(room.getId().toString())
                .name(room.getName())
                .language(room.getLanguage())
                .level(room.getLevel().name())
                .scheduledAt(room.getScheduledAt().toString())
                .duration(room.getDuration())
                .maxStudents(room.getMaxStudents())
                .participantCount(participantCount)
                .status(room.getStatus().name())
                .build();
    }

    private StudentStatsDTO.RoomSummaryDTO toStudentRoomSummary(Room room) {
        return StudentStatsDTO.RoomSummaryDTO.builder()
                .id(room.getId().toString())
                .name(room.getName())
                .language(room.getLanguage())
                .level(room.getLevel().name())
                .objective(room.getObjective())
                .scheduledAt(room.getScheduledAt().toString())
                .duration(room.getDuration())
                .status(room.getStatus().name())
                .professorName(room.getProfessor() != null ? room.getProfessor().getUser().getName() : null)
                .build();
    }
}

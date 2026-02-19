package com.free.easyLearn.service;

import com.free.easyLearn.dto.summary.CreateSessionSummaryRequest;
import com.free.easyLearn.dto.summary.SessionSummaryDTO;
import com.free.easyLearn.entity.Professor;
import com.free.easyLearn.entity.Room;
import com.free.easyLearn.entity.SessionSummary;
import com.free.easyLearn.entity.Student;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.exception.ResourceNotFoundException;
import com.free.easyLearn.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SessionSummaryService {

    @Autowired
    private SessionSummaryRepository summaryRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private RoomParticipantRepository roomParticipantRepository;

    @Transactional
    public SessionSummaryDTO createOrUpdateSummary(UUID userId, CreateSessionSummaryRequest request) {
        Room room = roomRepository.findById(UUID.fromString(request.getRoomId()))
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.getRoomId()));

        Professor professor = professorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        // Verify professor is assigned to this room
        if (room.getProfessor() != null && !room.getProfessor().getId().equals(professor.getId())) {
            throw new BadRequestException("You are not the professor of this session");
        }

        // Check if summary already exists
        SessionSummary summary = summaryRepository.findByRoomId(room.getId())
                .orElse(SessionSummary.builder()
                        .room(room)
                        .professor(professor)
                        .build());

        // Update summary fields
        summary.setSummary(request.getSummary());
        summary.setKeyTopics(request.getKeyTopics());
        summary.setVocabularyCovered(request.getVocabularyCovered());
        summary.setGrammarPoints(request.getGrammarPoints());
        summary.setStrengths(request.getStrengths());
        summary.setAreasToImprove(request.getAreasToImprove());
        summary.setRecommendations(request.getRecommendations());
        summary.setNextSessionFocus(request.getNextSessionFocus());
        summary.setOverallScore(request.getOverallScore());
        summary.setPronunciationScore(request.getPronunciationScore());
        summary.setGrammarScore(request.getGrammarScore());
        summary.setVocabularyScore(request.getVocabularyScore());
        summary.setFluencyScore(request.getFluencyScore());
        summary.setParticipationScore(request.getParticipationScore());

        summary = summaryRepository.save(summary);
        return mapToDTO(summary);
    }

    public SessionSummaryDTO getSummaryByRoomId(UUID roomId) {
        SessionSummary summary = summaryRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Summary not found for room: " + roomId));
        return mapToDTO(summary);
    }

    public List<SessionSummaryDTO> getSummariesByRoomIds(List<UUID> roomIds) {
        List<SessionSummary> summaries = summaryRepository.findByRoomIdIn(roomIds);
        return summaries.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<SessionSummaryDTO> getSummariesByProfessor(UUID userId) {
        Professor professor = professorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        List<SessionSummary> summaries = summaryRepository.findByProfessorId(professor.getId());
        return summaries.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<SessionSummaryDTO> getSummariesForStudent(UUID userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        // Find all rooms where the student participated
        List<UUID> roomIds = roomParticipantRepository.findByStudentId(student.getId())
                .stream()
                .map(participant -> participant.getRoom().getId())
                .collect(Collectors.toList());

        // Get summaries for these rooms
        if (roomIds.isEmpty()) {
            return List.of();
        }

        List<SessionSummary> summaries = summaryRepository.findByRoomIdIn(roomIds);
        return summaries.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private SessionSummaryDTO mapToDTO(SessionSummary summary) {
        return SessionSummaryDTO.builder()
                .id(summary.getId())
                .roomId(summary.getRoom().getId())
                .roomName(summary.getRoom().getName())
                .professorId(summary.getProfessor().getId())
                .professorName(summary.getProfessor().getUser().getName())
                .summary(summary.getSummary())
                .keyTopics(summary.getKeyTopics())
                .vocabularyCovered(summary.getVocabularyCovered())
                .grammarPoints(summary.getGrammarPoints())
                .strengths(summary.getStrengths())
                .areasToImprove(summary.getAreasToImprove())
                .recommendations(summary.getRecommendations())
                .nextSessionFocus(summary.getNextSessionFocus())
                .overallScore(summary.getOverallScore())
                .pronunciationScore(summary.getPronunciationScore())
                .grammarScore(summary.getGrammarScore())
                .vocabularyScore(summary.getVocabularyScore())
                .fluencyScore(summary.getFluencyScore())
                .participationScore(summary.getParticipationScore())
                .createdAt(summary.getCreatedAt())
                .updatedAt(summary.getUpdatedAt())
                .build();
    }
}


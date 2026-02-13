package com.free.easyLearn.service;

import com.free.easyLearn.dto.evaluation.CreateEvaluationRequest;
import com.free.easyLearn.dto.evaluation.EvaluationDTO;
import com.free.easyLearn.dto.evaluation.UpdateStudentLevelRequest;
import com.free.easyLearn.dto.student.StudentDTO;
import com.free.easyLearn.dto.student.StudentSkillsDTO;
import com.free.easyLearn.entity.*;
import com.free.easyLearn.exception.ResourceNotFoundException;
import com.free.easyLearn.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvaluationService {

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Transactional
    public EvaluationDTO createEvaluation(UUID professorUserId, CreateEvaluationRequest request) {
        Professor professor = professorRepository.findByUserId(professorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        // Calculate overall score (weighted average)
        int overallScore = (int) Math.round(
            request.getPronunciation() * 0.25 +
            request.getGrammar() * 0.25 +
            request.getVocabulary() * 0.25 +
            request.getFluency() * 0.25
        );

        Student.LanguageLevel previousLevel = student.getLevel();

        Evaluation evaluation = Evaluation.builder()
                .student(student)
                .professor(professor)
                .language(request.getLanguage())
                .pronunciation(request.getPronunciation())
                .grammar(request.getGrammar())
                .vocabulary(request.getVocabulary())
                .fluency(request.getFluency())
                .overallScore(overallScore)
                .assignedLevel(request.getAssignedLevel())
                .feedback(request.getFeedback())
                .strengths(request.getStrengths())
                .areasToImprove(request.getAreasToImprove())
                .build();

        evaluation = evaluationRepository.save(evaluation);

        // If professor assigned a new level, update the student
        if (request.getAssignedLevel() != null && request.getAssignedLevel() != previousLevel) {
            student.setLevel(request.getAssignedLevel());
            studentRepository.save(student);
        }

        return mapToDTO(evaluation, previousLevel);
    }

    public List<EvaluationDTO> getEvaluationsByProfessor(UUID professorUserId) {
        List<Evaluation> evaluations = evaluationRepository.findByProfessorUserId(professorUserId);
        return evaluations.stream()
                .map(e -> mapToDTO(e, null))
                .collect(Collectors.toList());
    }

    public List<EvaluationDTO> getEvaluationsForStudent(UUID studentUserId) {
        List<Evaluation> evaluations = evaluationRepository.findByStudentUserId(studentUserId);
        return evaluations.stream()
                .map(e -> mapToDTO(e, null))
                .collect(Collectors.toList());
    }

    public List<EvaluationDTO> getEvaluationsForStudentByLanguage(UUID studentUserId, String language) {
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        List<Evaluation> evaluations = evaluationRepository.findByStudentIdAndLanguageOrderByCreatedAtDesc(student.getId(), language);
        return evaluations.stream()
                .map(e -> mapToDTO(e, null))
                .collect(Collectors.toList());
    }

    @Transactional
    public StudentDTO updateStudentLevel(UUID professorUserId, UpdateStudentLevelRequest request) {
        // Verify the professor exists
        professorRepository.findByUserId(professorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor not found"));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        student.setLevel(request.getNewLevel());
        student = studentRepository.save(student);

        return mapStudentToDTO(student);
    }

    public List<StudentDTO> getAllStudents() {
        List<Student> students = studentRepository.findAll();
        return students.stream()
                .map(this::mapStudentToDTO)
                .collect(Collectors.toList());
    }

    private EvaluationDTO mapToDTO(Evaluation evaluation, Student.LanguageLevel previousLevel) {
        return EvaluationDTO.builder()
                .id(evaluation.getId())
                .studentId(evaluation.getStudent().getId())
                .studentName(evaluation.getStudent().getUser().getName())
                .studentAvatar(evaluation.getStudent().getUser().getAvatar())
                .professorId(evaluation.getProfessor().getId())
                .professorName(evaluation.getProfessor().getUser().getName())
                .language(evaluation.getLanguage())
                .pronunciation(evaluation.getPronunciation())
                .grammar(evaluation.getGrammar())
                .vocabulary(evaluation.getVocabulary())
                .fluency(evaluation.getFluency())
                .overallScore(evaluation.getOverallScore())
                .assignedLevel(evaluation.getAssignedLevel())
                .previousLevel(previousLevel)
                .feedback(evaluation.getFeedback())
                .strengths(evaluation.getStrengths())
                .areasToImprove(evaluation.getAreasToImprove())
                .createdAt(evaluation.getCreatedAt())
                .updatedAt(evaluation.getUpdatedAt())
                .build();
    }

    private StudentDTO mapStudentToDTO(Student student) {
        StudentSkillsDTO skillsDto = null;
        if (student.getSkills() != null) {
            skillsDto = StudentSkillsDTO.builder()
                    .pronunciation(student.getSkills().getPronunciation())
                    .grammar(student.getSkills().getGrammar())
                    .vocabulary(student.getSkills().getVocabulary())
                    .fluency(student.getSkills().getFluency())
                    .build();
        }

        return StudentDTO.builder()
                .id(student.getId())
                .name(student.getUser() != null ? student.getUser().getName() : null)
                .email(student.getUser() != null ? student.getUser().getEmail() : null)
                .avatar(student.getUser() != null ? student.getUser().getAvatar() : null)
                .nickname(student.getNickname())
                .bio(student.getBio())
                .level(student.getLevel())
                .joinedAt(student.getJoinedAt())
                .totalSessions(student.getTotalSessions())
                .hoursLearned(student.getHoursLearned())
                .skills(skillsDto)
                .createdAt(student.getCreatedAt())
                .updatedAt(student.getUpdatedAt())
                .build();
    }
}


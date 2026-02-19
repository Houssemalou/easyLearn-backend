package com.free.easyLearn.controller;

import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.dto.common.PageResponse;
import com.free.easyLearn.dto.quiz.*;
import com.free.easyLearn.entity.Professor;
import com.free.easyLearn.entity.Student;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.exception.ResourceNotFoundException;
import com.free.easyLearn.repository.ProfessorRepository;
import com.free.easyLearn.repository.StudentRepository;
import com.free.easyLearn.service.QuizService;
import com.free.easyLearn.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quizzes")
@Tag(name = "Quizzes", description = "Gestion des quiz pour professeurs et étudiants")
@SecurityRequirement(name = "bearerAuth")
public class QuizController {

    @Autowired
    private QuizService quizService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @PostMapping
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Créer un quiz", description = "Le professeur crée un nouveau quiz")
    public ResponseEntity<ApiResponse<QuizDTO>> createQuiz(
            @Valid @RequestBody CreateQuizRequest request) {
        User user = getCurrentUser();
        Professor professor = professorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Professor profile not found"));
        QuizDTO quiz = quizService.createQuiz(request, professor.getId());
        return ResponseEntity.ok(ApiResponse.success("Quiz créé avec succès", quiz));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Liste des quiz", description = "Récupère les quiz avec filtres et pagination")
    public ResponseEntity<ApiResponse<PageResponse<QuizDTO>>> getQuizzes(
            @RequestParam(required = false) UUID sessionId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Boolean isPublished,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        Page<QuizDTO> quizzes = quizService.getQuizzes(
                sessionId, language, isPublished, createdBy, search, page, size, sortBy, sortOrder
        );

        PageResponse<QuizDTO> response = PageResponse.<QuizDTO>builder()
                .data(quizzes.getContent())
                .total(quizzes.getTotalElements())
                .page(page)
                .limit(size)
                .totalPages(quizzes.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Détails d'un quiz", description = "Récupère un quiz par son ID")
    public ResponseEntity<ApiResponse<QuizDTO>> getQuizById(@PathVariable UUID id) {
        QuizDTO quiz = quizService.getQuizById(id);
        return ResponseEntity.ok(ApiResponse.success(quiz));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Publier un quiz", description = "Rend le quiz disponible pour les étudiants")
    public ResponseEntity<ApiResponse<QuizDTO>> publishQuiz(@PathVariable UUID id) {
        QuizDTO quiz = quizService.publishQuiz(id);
        return ResponseEntity.ok(ApiResponse.success("Quiz publié avec succès", quiz));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Soumettre un quiz", description = "L'étudiant soumet ses réponses au quiz")
    public ResponseEntity<ApiResponse<QuizResultDTO>> submitQuiz(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitQuizRequest request) {
        User user = getCurrentUser();
        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        QuizResultDTO result = quizService.submitQuiz(id, request, student.getId());
        return ResponseEntity.ok(ApiResponse.success("Quiz soumis avec succès", result));
    }

    @GetMapping("/{id}/results")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Résultats d'un quiz", description = "Voir tous les résultats des étudiants sur un quiz")
    public ResponseEntity<ApiResponse<List<QuizResultDTO>>> getQuizResults(@PathVariable UUID id) {
        List<QuizResultDTO> results = quizService.getQuizResults(id);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/student/{studentId}/results")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Résultats d'un étudiant", description = "Récupère tous les résultats de quiz d'un étudiant")
    public ResponseEntity<ApiResponse<List<QuizResultDTO>>> getStudentResults(@PathVariable UUID studentId) {
        List<QuizResultDTO> results = quizService.getStudentQuizResults(studentId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Supprimer un quiz", description = "Le professeur supprime un de ses quiz")
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(@PathVariable UUID id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.ok(ApiResponse.success("Quiz supprimé avec succès", null));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return roomService.getUserByEmail(email);
    }
}

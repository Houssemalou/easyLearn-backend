package com.free.easyLearn.controller;

import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.dto.evaluation.CreateEvaluationRequest;
import com.free.easyLearn.dto.evaluation.EvaluationDTO;
import com.free.easyLearn.dto.evaluation.UpdateStudentLevelRequest;
import com.free.easyLearn.dto.student.StudentDTO;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.service.EvaluationService;
import com.free.easyLearn.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@Tag(name = "Evaluations", description = "Gestion des évaluations des étudiants")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private RoomService roomService;

    // ==========================================
    // Professor endpoints
    // ==========================================

    @PostMapping
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Créer une évaluation",
            description = "Le professeur évalue un étudiant sur une langue donnée")
    public ResponseEntity<ApiResponse<EvaluationDTO>> createEvaluation(
            @Valid @RequestBody CreateEvaluationRequest request) {
        User user = getCurrentUser();
        EvaluationDTO evaluation = evaluationService.createEvaluation(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Évaluation créée avec succès", evaluation));
    }

    @GetMapping("/my-evaluations")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Mes évaluations - Professeur",
            description = "Récupérer toutes les évaluations créées par le professeur connecté")
    public ResponseEntity<ApiResponse<List<EvaluationDTO>>> getMyCreatedEvaluations() {
        User user = getCurrentUser();
        List<EvaluationDTO> evaluations = evaluationService.getEvaluationsByProfessor(user.getId());
        return ResponseEntity.ok(ApiResponse.success(evaluations));
    }

    @GetMapping("/students")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Liste des étudiants",
            description = "Récupérer la liste de tous les étudiants pour évaluation")
    public ResponseEntity<ApiResponse<List<StudentDTO>>> getAllStudents() {
        List<StudentDTO> students = evaluationService.getAllStudents();
        return ResponseEntity.ok(ApiResponse.success(students));
    }

    @PutMapping("/update-level")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Changer le niveau d'un étudiant",
            description = "Le professeur change le niveau CECR d'un étudiant (A1→C2)")
    public ResponseEntity<ApiResponse<StudentDTO>> updateStudentLevel(
            @Valid @RequestBody UpdateStudentLevelRequest request) {
        User user = getCurrentUser();
        StudentDTO student = evaluationService.updateStudentLevel(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Niveau mis à jour avec succès", student));
    }

    // ==========================================
    // Student endpoints
    // ==========================================

    @GetMapping("/my-results")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Mes évaluations - Étudiant",
            description = "Récupérer toutes les évaluations reçues par l'étudiant connecté")
    public ResponseEntity<ApiResponse<List<EvaluationDTO>>> getMyEvaluations() {
        User user = getCurrentUser();
        List<EvaluationDTO> evaluations = evaluationService.getEvaluationsForStudent(user.getId());
        return ResponseEntity.ok(ApiResponse.success(evaluations));
    }

    @GetMapping("/my-results/{language}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Mes évaluations par langue",
            description = "Filtrer les évaluations par langue")
    public ResponseEntity<ApiResponse<List<EvaluationDTO>>> getMyEvaluationsByLanguage(
            @PathVariable String language) {
        User user = getCurrentUser();
        List<EvaluationDTO> evaluations = evaluationService.getEvaluationsForStudentByLanguage(user.getId(), language);
        return ResponseEntity.ok(ApiResponse.success(evaluations));
    }

    // ==========================================
    // Helper
    // ==========================================

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return roomService.getUserByEmail(email);
    }
}

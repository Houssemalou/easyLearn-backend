package com.free.easyLearn.controller;

import com.free.easyLearn.dto.challenge.*;
import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.service.ChallengeService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/challenges")
@Tag(name = "Challenges", description = "Gestion des défis (challenges) pour professeurs et étudiants")
public class ChallengeController {

    @Autowired
    private ChallengeService challengeService;

    @Autowired
    private RoomService roomService;

    // ==========================================
    // Professor endpoints
    // ==========================================

    @PostMapping
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Créer un défi", description = "Le professeur crée un nouveau défi pour les étudiants")
    public ResponseEntity<ApiResponse<ChallengeDTO>> createChallenge(
            @Valid @RequestBody CreateChallengeRequest request) {
        User user = getCurrentUser();
        ChallengeDTO challenge = challengeService.createChallenge(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Défi créé avec succès", challenge));
    }

    @GetMapping("/my-challenges")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Mes défis", description = "Récupérer tous les défis créés par le professeur connecté")
    public ResponseEntity<ApiResponse<List<ChallengeDTO>>> getMyChallenges() {
        User user = getCurrentUser();
        List<ChallengeDTO> challenges = challengeService.getMyChallenges(user.getId());
        return ResponseEntity.ok(ApiResponse.success(challenges));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Supprimer un défi", description = "Le professeur supprime un de ses défis")
    public ResponseEntity<ApiResponse<Void>> deleteChallenge(@PathVariable UUID id) {
        User user = getCurrentUser();
        challengeService.deleteChallenge(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Défi supprimé avec succès", null));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Statistiques des défis", description = "Statistiques agrégées pour le dashboard professeur")
    public ResponseEntity<ApiResponse<ChallengeStatsDTO>> getChallengeStats() {
        User user = getCurrentUser();
        ChallengeStatsDTO stats = challengeService.getChallengeStats(user.getId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/{id}/attempts")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Voir les tentatives", description = "Voir toutes les tentatives des étudiants sur un défi")
    public ResponseEntity<ApiResponse<List<ChallengeAttemptDTO>>> getChallengeAttempts(@PathVariable UUID id) {
        User user = getCurrentUser();
        List<ChallengeAttemptDTO> attempts = challengeService.getChallengeAttempts(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(attempts));
    }

    // ==========================================
    // Student endpoints
    // ==========================================

    @GetMapping("/active")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Défis actifs", description = "Récupérer tous les défis actifs et non expirés")
    public ResponseEntity<ApiResponse<List<ChallengeStudentDTO>>> getActiveChallenges() {
        List<ChallengeStudentDTO> challenges = challengeService.getActiveChallenges();
        return ResponseEntity.ok(ApiResponse.success(challenges));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Soumettre une réponse", description = "L'étudiant soumet sa réponse à un défi")
    public ResponseEntity<ApiResponse<SubmitAnswerResponse>> submitAnswer(
            @Valid @RequestBody SubmitAnswerRequest request) {
        User user = getCurrentUser();
        SubmitAnswerResponse response = challengeService.submitAnswer(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Réponse soumise", response));
    }

    @GetMapping("/my-attempts")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Mes tentatives", description = "Historique des tentatives de l'étudiant connecté")
    public ResponseEntity<ApiResponse<List<ChallengeAttemptDTO>>> getMyAttempts() {
        User user = getCurrentUser();
        List<ChallengeAttemptDTO> attempts = challengeService.getMyAttempts(user.getId());
        return ResponseEntity.ok(ApiResponse.success(attempts));
    }

    // ==========================================
    // Common endpoints
    // ==========================================

    @GetMapping("/leaderboard")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Classement", description = "Classement global des étudiants sur les défis")
    public ResponseEntity<ApiResponse<List<ChallengeLeaderboardEntryDTO>>> getLeaderboard() {
        List<ChallengeLeaderboardEntryDTO> leaderboard = challengeService.getLeaderboard();
        return ResponseEntity.ok(ApiResponse.success(leaderboard));
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

package com.free.easyLearn.controller;

import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.dto.summary.CreateSessionSummaryRequest;
import com.free.easyLearn.dto.summary.SessionSummaryDTO;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.service.RoomService;
import com.free.easyLearn.service.SessionSummaryService;
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
@RequestMapping("/api/session-summaries")
@Tag(name = "Session Summaries", description = "Gestion des résumés de session")
public class SessionSummaryController {

    @Autowired
    private SessionSummaryService summaryService;
    
    @Autowired
    private RoomService roomService;

    @PostMapping
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Créer ou mettre à jour un résumé de session", 
               description = "Le professeur crée un résumé pour une session")
    public ResponseEntity<ApiResponse<SessionSummaryDTO>> createOrUpdateSummary(
            @Valid @RequestBody CreateSessionSummaryRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = roomService.getUserByEmail(email);
        
        SessionSummaryDTO summary = summaryService.createOrUpdateSummary(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Summary créé avec succès", summary));
    }

    @GetMapping("/room/{roomId}")
    @Operation(summary = "Récupérer le résumé d'une session", 
               description = "Récupérer le résumé d'une session par son ID")
    public ResponseEntity<ApiResponse<SessionSummaryDTO>> getSummaryByRoom(
            @PathVariable UUID roomId) {
        SessionSummaryDTO summary = summaryService.getSummaryByRoomId(roomId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
    
    @PostMapping("/by-rooms")
    @Operation(summary = "Récupérer les résumés de plusieurs sessions", 
               description = "Récupérer les résumés de plusieurs sessions par leurs IDs")
    public ResponseEntity<ApiResponse<List<SessionSummaryDTO>>> getSummariesByRooms(
            @RequestBody List<UUID> roomIds) {
        List<SessionSummaryDTO> summaries = summaryService.getSummariesByRoomIds(roomIds);
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }
    
    @GetMapping("/my-summaries")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Mes résumés de session", 
               description = "Récupérer tous les résumés créés par le professeur connecté")
    public ResponseEntity<ApiResponse<List<SessionSummaryDTO>>> getMySummaries() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = roomService.getUserByEmail(email);
        
        List<SessionSummaryDTO> summaries = summaryService.getSummariesByProfessor(user.getId());
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }
    
    @GetMapping("/my-sessions")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Mes résumés de sessions - Étudiant", 
               description = "Récupérer tous les résumés des sessions auxquelles l'étudiant a participé")
    public ResponseEntity<ApiResponse<List<SessionSummaryDTO>>> getMySessionSummaries() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = roomService.getUserByEmail(email);
        
        List<SessionSummaryDTO> summaries = summaryService.getSummariesForStudent(user.getId());
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }
}

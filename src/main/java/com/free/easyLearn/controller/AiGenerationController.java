package com.free.easyLearn.controller;

import com.free.easyLearn.dto.ai.AiGenerateRequest;
import com.free.easyLearn.dto.ai.AiGenerateResponse;
import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.service.AiGenerationService;
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

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Generation", description = "Génération de défis par intelligence artificielle")
public class AiGenerationController {

    @Autowired
    private AiGenerationService aiGenerationService;

    @Autowired
    private RoomService roomService;

    @PostMapping("/generate-challenges")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Générer des défis avec l'IA",
               description = "Le professeur envoie un fichier cours, l'IA génère des QCM. Rate limité à 15 requêtes/minute et 1500/jour.")
    public ResponseEntity<ApiResponse<AiGenerateResponse>> generateChallenges(
            @Valid @RequestBody AiGenerateRequest request) {
        User user = getCurrentUser();
        AiGenerateResponse response = aiGenerationService.generateChallenges(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Défis générés avec succès", response));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return roomService.getUserByEmail(email);
    }
}

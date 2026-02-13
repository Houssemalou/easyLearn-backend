package com.free.easyLearn.controller;

import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.dto.common.PageResponse;
import com.free.easyLearn.dto.professor.CreateProfessorRequest;
import com.free.easyLearn.dto.professor.ProfessorDTO;
import com.free.easyLearn.dto.professor.UpdateProfessorRequest;
import com.free.easyLearn.service.ProfessorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/professors")
@Tag(name = "Professors", description = "Gestion des professeurs")
@SecurityRequirement(name = "bearerAuth")
public class ProfessorController {

    @Autowired
    private ProfessorService professorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un professeur", description = "Crée un nouveau professeur avec compte utilisateur")
    public ResponseEntity<ApiResponse<ProfessorDTO>> createProfessor(
            @Valid @RequestBody CreateProfessorRequest request
    ) {
        ProfessorDTO professor = professorService.createProfessor(request);
        return ResponseEntity.ok(ApiResponse.success("Professor created successfully", professor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    @Operation(summary = "Modifier un professeur", description = "Met à jour les informations d'un professeur")
    public ResponseEntity<ApiResponse<ProfessorDTO>> updateProfessor(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProfessorRequest request
    ) {
        ProfessorDTO professor = professorService.updateProfessor(id, request);
        return ResponseEntity.ok(ApiResponse.success("Professor updated successfully", professor));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un professeur", description = "Supprime un professeur et son compte utilisateur")
    public ResponseEntity<ApiResponse<Void>> deleteProfessor(@PathVariable UUID id) {
        professorService.deleteProfessor(id);
        return ResponseEntity.ok(ApiResponse.success("Professor deleted successfully", null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détails d'un professeur", description = "Récupère les informations détaillées d'un professeur")
    public ResponseEntity<ApiResponse<ProfessorDTO>> getProfessorById(@PathVariable UUID id) {
        ProfessorDTO professor = professorService.getProfessorById(id);
        return ResponseEntity.ok(ApiResponse.success(professor));
    }

    @GetMapping
    @Operation(summary = "Liste des professeurs", description = "Récupère la liste de tous les professeurs avec pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProfessorDTO>>> getProfessors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "rating") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        Page<ProfessorDTO> professors = professorService.getProfessors(page, size, sortBy, sortOrder);

        PageResponse<ProfessorDTO> response = PageResponse.<ProfessorDTO>builder()
                .data(professors.getContent())
                .total(professors.getTotalElements())
                .page(page)
                .limit(size)
                .totalPages(professors.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

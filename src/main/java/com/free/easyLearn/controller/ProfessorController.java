package com.free.easyLearn.controller;

import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.dto.common.PageResponse;
import com.free.easyLearn.dto.professor.CreateProfessorRequest;
import com.free.easyLearn.dto.professor.ProfessorDTO;
import com.free.easyLearn.dto.professor.UpdateProfessorRequest;
import com.free.easyLearn.dto.room.RoomDTO;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.repository.UserRepository;
import com.free.easyLearn.service.ProfessorService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/professors")
@Tag(name = "Professors", description = "Gestion des professeurs")
@SecurityRequirement(name = "bearerAuth")
public class ProfessorController {

    @Autowired
    private ProfessorService professorService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomService roomService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Mon profil professeur", description = "Récupère le profil du professeur connecté")
    public ResponseEntity<ApiResponse<ProfessorDTO>> getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        ProfessorDTO professor = professorService.getProfessorByUserId(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Professor profile retrieved", professor));
    }

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

    @GetMapping("/created-by/{createdById}")
    @Operation(summary = "Liste des professeurs par créateur", description = "Récupère la liste des professeurs créés par un admin (created_by) avec pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProfessorDTO>>> getProfessorsByCreator(
            @PathVariable UUID createdById,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "rating") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        Page<ProfessorDTO> professors = professorService.getProfessorsByAdmin(createdById, page, size, sortBy, sortOrder);

        PageResponse<ProfessorDTO> response = PageResponse.<ProfessorDTO>builder()
                .data(professors.getContent())
                .total(professors.getTotalElements())
                .page(page)
                .limit(size)
                .totalPages(professors.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
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

    @GetMapping("/{userId}/sessions")
    @Operation(summary = "Sessions d'un professeur", description = "Récupère les sessions d'un professeur par son userId")
    public ResponseEntity<ApiResponse<PageResponse<RoomDTO>>> getProfessorSessions(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "scheduledAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        Page<RoomDTO> rooms = roomService.getMyRooms(userId, User.UserRole.PROFESSOR, page, size, sortBy, sortOrder);

        PageResponse<RoomDTO> response = PageResponse.<RoomDTO>builder()
                .data(rooms.getContent())
                .total(rooms.getTotalElements())
                .page(page)
                .limit(size)
                .totalPages(rooms.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

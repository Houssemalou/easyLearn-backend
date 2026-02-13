package com.free.easyLearn.controller;

import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.dto.common.PageResponse;
import com.free.easyLearn.dto.room.*;
import com.free.easyLearn.entity.Room;
import com.free.easyLearn.entity.Student;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms", description = "Gestion des sessions/salles de cours")
@SecurityRequirement(name = "bearerAuth")
public class RoomController {

    @Autowired
    private RoomService roomService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            String email = ((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal()).getUsername();
            // In a real scenario, you'd fetch the user from database here
            // For now, we'll handle this in the service layer
            return null; // Will be handled in service
        }
        return null;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    @Operation(summary = "Créer une nouvelle room", description = "Crée une session de cours avec LiveKit")
    public ResponseEntity<ApiResponse<RoomDTO>> createRoom(
            @Valid @RequestBody CreateRoomRequest request
    ) {
        RoomDTO room = roomService.createRoom(request);
        return ResponseEntity.ok(ApiResponse.success("Room created successfully", room));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    @Operation(summary = "Modifier une room", description = "Met à jour les informations d'une session")
    public ResponseEntity<ApiResponse<RoomDTO>> updateRoom(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoomRequest request
    ) {
        RoomDTO room = roomService.updateRoom(id, request);
        return ResponseEntity.ok(ApiResponse.success("Room updated successfully", room));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    @Operation(summary = "Supprimer une room", description = "Supprime une session et sa room LiveKit")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable UUID id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success("Room deleted successfully", null));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détails d'une room", description = "Récupère les informations détaillées d'une session")
    public ResponseEntity<ApiResponse<RoomDTO>> getRoomById(@PathVariable UUID id) {
        RoomDTO room = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success(room));
    }

    @GetMapping
    @Operation(summary = "Liste des rooms", description = "Récupère la liste de toutes les sessions avec pagination")
    public ResponseEntity<ApiResponse<PageResponse<RoomDTO>>> getRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "scheduledAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        Page<RoomDTO> rooms = roomService.getRooms(page, size, sortBy, sortOrder);

        PageResponse<RoomDTO> response = PageResponse.<RoomDTO>builder()
                .data(rooms.getContent())
                .total(rooms.getTotalElements())
                .page(page)
                .limit(size)
                .totalPages(rooms.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-sessions")
    @Operation(summary = "Mes sessions", description = "Récupère les sessions auxquelles je suis invité/assigné")
    public ResponseEntity<ApiResponse<PageResponse<RoomDTO>>> getMySessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "scheduledAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        // Get user details from authentication
        User user = roomService.getUserByEmail(email);
        
        Page<RoomDTO> rooms = roomService.getMyRooms(user.getId(), user.getRole(), page, size, sortBy, sortOrder);

        PageResponse<RoomDTO> response = PageResponse.<RoomDTO>builder()
                .data(rooms.getContent())
                .total(rooms.getTotalElements())
                .page(page)
                .limit(size)
                .totalPages(rooms.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    @Operation(summary = "Démarrer une session", description = "Passe la room en mode LIVE")
    public ResponseEntity<ApiResponse<Void>> startRoom(@PathVariable UUID id) {
        roomService.startRoom(id);
        return ResponseEntity.ok(ApiResponse.success("Room started", null));
    }

    @GetMapping("/{id}/can-join")
    @Operation(summary = "Vérifier si je peux rejoindre", description = "Vérifie si l'utilisateur peut rejoindre la session")
    public ResponseEntity<ApiResponse<Boolean>> canJoinRoom(@PathVariable UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = roomService.getUserByEmail(email);
        
        try {
            boolean canJoin = roomService.canJoinRoom(id, user.getId(), user.getRole());
            return ResponseEntity.ok(ApiResponse.success(canJoin));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage(), false));
        }
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "Rejoindre une session", description = "Enregistre que l'utilisateur a rejoint la session")
    public ResponseEntity<ApiResponse<Void>> joinRoom(@PathVariable UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = roomService.getUserByEmail(email);
        
        // Validate can join (will throw exception if not allowed)
        roomService.canJoinRoom(id, user.getId(), user.getRole());
        
        // Record join
        roomService.recordJoin(id, user.getId(), user.getRole());
        
        return ResponseEntity.ok(ApiResponse.success("Joined room successfully", null));
    }

    @PostMapping("/{id}/end")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    @Operation(summary = "Terminer une session", description = "Termine la session et déclenche la génération du résumé")
    public ResponseEntity<ApiResponse<Void>> endRoom(@PathVariable UUID id) {
        roomService.endRoom(id);
        return ResponseEntity.ok(ApiResponse.success("Room ended, summary will be generated", null));
    }

    @PostMapping("/{id}/leave")
    @Operation(summary = "Quitter une session", description = "Enregistre le départ d'un utilisateur. " +
            "Si plus personne n'est dans la salle, le statut passe à COMPLETED.")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(@PathVariable UUID id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = roomService.getUserByEmail(email);

        roomService.leaveRoom(id, user.getId(), user.getRole());
        return ResponseEntity.ok(ApiResponse.success("Left room successfully", null));
    }

    @GetMapping("/{id}/participants")
    @Operation(summary = "Liste des participants", description = "Récupère tous les participants d'une room")
    public ResponseEntity<ApiResponse<List<ParticipantDTO>>> getRoomParticipants(@PathVariable UUID id) {
        List<ParticipantDTO> participants = roomService.getRoomParticipants(id);
        return ResponseEntity.ok(ApiResponse.success(participants));
    }

    @PostMapping("/participants/mute")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    @Operation(
        summary = "Muter/Démuter un participant",
        description = "Permet au professeur de couper le micro d'un étudiant"
    )
    public ResponseEntity<ApiResponse<ParticipantDTO>> muteParticipant(
            @Valid @RequestBody ParticipantActionRequest request
    ) {
        ParticipantDTO participant = roomService.muteParticipant(request);
        return ResponseEntity.ok(ApiResponse.success("Participant mute status updated", participant));
    }

    @PostMapping("/participants/ping")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    @Operation(
        summary = "Pinger un participant",
        description = "Envoie une notification d'attention à un étudiant"
    )
    public ResponseEntity<ApiResponse<ParticipantDTO>> pingParticipant(
            @Valid @RequestBody ParticipantActionRequest request
    ) {
        ParticipantDTO participant = roomService.pingParticipant(request);
        return ResponseEntity.ok(ApiResponse.success("Participant pinged successfully", participant));
    }

    @DeleteMapping("/participants/ping")
    @Operation(summary = "Effacer le ping", description = "L'étudiant acquitte le ping")
    public ResponseEntity<ApiResponse<Void>> clearPing(
            @RequestParam UUID roomId,
            @RequestParam UUID studentId
    ) {
        roomService.clearPing(roomId, studentId);
        return ResponseEntity.ok(ApiResponse.success("Ping cleared", null));
    }
}
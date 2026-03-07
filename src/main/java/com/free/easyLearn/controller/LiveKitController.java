package com.free.easyLearn.controller;

import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.dto.livekit.LiveKitTokenRequest;
import com.free.easyLearn.dto.livekit.LiveKitTokenResponse;
import com.free.easyLearn.dto.livekit.WebhookEvent;
import com.free.easyLearn.entity.SessionRecording;
import com.free.easyLearn.service.LiveKitService;
import com.free.easyLearn.service.LiveKitRecordingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/livekit")
@Tag(name = "LiveKit", description = "Endpoints pour la gestion des sessions vidéo LiveKit")
@SecurityRequirement(name = "bearerAuth")
public class LiveKitController {

    @Autowired
    private LiveKitService liveKitService;

    @Autowired
    private LiveKitRecordingService recordingService;

    @PostMapping("/token")
    @Operation(
            summary = "Générer un token d'accès LiveKit",
            description = "Génère un token JWT pour permettre à un utilisateur de rejoindre une room LiveKit. " +
                    "Le token contient les permissions pour publier/recevoir audio/vidéo et données."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token généré avec succès",
                    content = @Content(schema = @Schema(implementation = LiveKitTokenResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Room ou utilisateur introuvable"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Non authentifié"
            )
    })
    public ResponseEntity<ApiResponse<LiveKitTokenResponse>> generateToken(
            @Valid @RequestBody LiveKitTokenRequest request
    ) {
        LiveKitTokenResponse response = liveKitService.generateToken(
                request.getRoomId(),
                request.getUserId()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

//    @PostMapping("/webhook")
//    @Operation(
//            summary = "Webhook LiveKit",
//            description = "Endpoint pour recevoir les événements webhook de LiveKit (participant rejoint/quitte, etc.)",
//            security = {}
//    )
//    @ApiResponses(value = {
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "200",
//                    description = "Webhook traité avec succès"
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "401",
//                    description = "Signature invalide"
//            )
//    })
//    public ResponseEntity<String> handleWebhook(
//            @Parameter(description = "Token de signature du webhook")
//            @RequestHeader("Authorization") String token,
//            @RequestBody String body
//    ) {
//        boolean isValid = liveKitService.validateWebhook(token, body);
//        if (isValid) {
//            // Process webhook event
//            return ResponseEntity.ok("Webhook processed");
//        } else {
//            return ResponseEntity.status(401).body("Invalid signature");
//        }
//    }

    @GetMapping("/recordings/{roomName}")
    @Operation(
            summary = "Récupérer les enregistrements d'une room",
            description = "Retourne la liste des enregistrements (URL) pour une room LiveKit donnée par son livekitRoomName."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Liste des enregistrements"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Aucun enregistrement trouvé"
            )
    })
    public ResponseEntity<ApiResponse<List<SessionRecording>>> getRecordingsByRoom(
            @PathVariable String roomName
    ) {
        List<SessionRecording> recordings = recordingService.getRecordingsByRoomName(roomName);

        // Filter out expired recordings (older than 3 days)
        LocalDateTime expirationThreshold = LocalDateTime.now().minusDays(3);
        recordings = recordings.stream()
                .filter(r -> r.getCreatedAt() == null || r.getCreatedAt().isAfter(expirationThreshold))
                .collect(Collectors.toList());

        // Replace raw MinIO URLs with time-limited presigned URLs (valid 60 min)
        recordings.forEach(r -> {
            String presigned = recordingService.generatePresignedUrl(r.getRecordingUrl(), 60);
            r.setRecordingUrl(presigned);
        });

        return ResponseEntity.ok(ApiResponse.success(recordings));
    }

    @GetMapping("/recordings/{roomName}/download/{recordingId}")
    @Operation(
            summary = "Télécharger un enregistrement",
            description = "Génère une URL de téléchargement pour un enregistrement spécifique (valide 60 min)."
    )
    public ResponseEntity<?> downloadRecording(
            @PathVariable String roomName,
            @PathVariable Long recordingId
    ) {
        List<SessionRecording> recordings = recordingService.getRecordingsByRoomName(roomName);
        SessionRecording target = recordings.stream()
                .filter(r -> r.getId().equals(recordingId))
                .findFirst()
                .orElse(null);

        if (target == null) {
            return ResponseEntity.notFound().build();
        }

        // Check if expired
        if (target.getCreatedAt() != null && target.getCreatedAt().plusDays(3).isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Recording has expired and is no longer available."));
        }

        String downloadUrl = recordingService.generatePresignedDownloadUrl(target.getRecordingUrl(), 60);
        return ResponseEntity.ok(ApiResponse.success(Map.of("downloadUrl", downloadUrl)));
    }
}


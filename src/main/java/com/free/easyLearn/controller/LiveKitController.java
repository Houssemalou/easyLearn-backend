package com.free.easyLearn.controller;

import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.dto.livekit.LiveKitTokenRequest;
import com.free.easyLearn.dto.livekit.LiveKitTokenResponse;
import com.free.easyLearn.service.LiveKitService;
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

@RestController
@RequestMapping("/api/livekit")
@Tag(name = "LiveKit", description = "Endpoints pour la gestion des sessions vidéo LiveKit")
@SecurityRequirement(name = "bearerAuth")
public class LiveKitController {

    @Autowired
    private LiveKitService liveKitService;

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

    @PostMapping("/webhook")
    @Operation(
            summary = "Webhook LiveKit",
            description = "Endpoint pour recevoir les événements webhook de LiveKit (participant rejoint/quitte, etc.)",
            security = {}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Webhook traité avec succès"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Signature invalide"
            )
    })
    public ResponseEntity<String> handleWebhook(
            @Parameter(description = "Token de signature du webhook")
            @RequestHeader("Authorization") String token,
            @RequestBody String body
    ) {
        boolean isValid = liveKitService.validateWebhook(token, body);
        if (isValid) {
            // Process webhook event
            return ResponseEntity.ok("Webhook processed");
        } else {
            return ResponseEntity.status(401).body("Invalid signature");
        }
    }
}
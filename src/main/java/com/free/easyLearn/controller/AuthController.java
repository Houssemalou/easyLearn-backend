package com.free.easyLearn.controller;
import com.free.easyLearn.dto.auth.*;
import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.entity.AccessToken;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.repository.UserRepository;
import com.free.easyLearn.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.free.easyLearn.exception.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints pour l'authentification et la gestion des tokens")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    @Operation(
        summary = "Inscription d'un nouvel utilisateur",
        description = "Crée un nouveau compte utilisateur (Admin, Professeur ou Étudiant) et retourne un token JWT"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Inscription réussie",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Email déjà utilisé ou données invalides"
        )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }

    @PostMapping("/register/student")
    @Operation(
        summary = "Inscription d'un étudiant",
        description = "Crée un nouveau compte étudiant avec profil complet et retourne un token JWT"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Inscription étudiante réussie",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Token invalide, email déjà utilisé ou données invalides"
        )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> registerStudent(@Valid @RequestBody StudentRegisterRequest request) {
        AuthResponse response = authService.registerStudent(request);
        return ResponseEntity.ok(ApiResponse.success("Student registration successful", response));
    }

    @PostMapping("/register/professor")
    @Operation(
        summary = "Inscription d'un professeur",
        description = "Crée un nouveau compte professeur avec profil complet et retourne un token JWT"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Inscription professeur réussie",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Token invalide, email déjà utilisé ou données invalides"
        )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> registerProfessor(@Valid @RequestBody ProfessorRegisterRequest request) {
        AuthResponse response = authService.registerProfessor(request);
        return ResponseEntity.ok(ApiResponse.success("Professor registration successful", response));
    }

    @PostMapping("/register/admin")
    @Operation(
        summary = "Inscription d'un administrateur",
        description = "Crée un nouveau compte administrateur et retourne un token JWT"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Inscription admin réussie",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Token invalide, email déjà utilisé ou données invalides"
        )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> registerAdmin(@Valid @RequestBody AdminRegisterRequest request) {
        AuthResponse response = authService.registerAdmin(request);
        return ResponseEntity.ok(ApiResponse.success("Admin registration successful", response));
    }

    @PostMapping("/login")
    @Operation(
        summary = "Connexion utilisateur",
        description = "Authentifie un utilisateur avec email et mot de passe, retourne un token JWT"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Connexion réussie",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Email ou mot de passe incorrect"
        )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Rafraîchir le token JWT",
        description = "Génère un nouveau token JWT à partir d'un refresh token valide"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Token rafraîchi avec succès",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Refresh token invalide ou expiré"
        )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }
    
    @GetMapping("/me")
    @Operation(
        summary = "Obtenir les informations de l'utilisateur connecté",
        description = "Retourne les informations de l'utilisateur authentifié via le token JWT"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Informations utilisateur récupérées",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Token invalide ou expiré"
        )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser() {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        AuthResponse response = authService.getCurrentUser(user.getId());
        return ResponseEntity.ok(ApiResponse.success("User info retrieved", response));
    }
    
    @PostMapping("/logout")
    @Operation(
        summary = "Déconnexion utilisateur",
        description = "Invalide le refresh token de l'utilisateur"
    )
    public ResponseEntity<ApiResponse<Void>> logout() {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        authService.logout(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/generate-access-token")
    @Operation(
        summary = "Générer un token d'accès",
        description = "Génère un nouveau token d'accès pour l'inscription (réservé aux admins)"
    )
    public ResponseEntity<ApiResponse<GenerateAccessTokenResponse>> generateAccessToken(
            @Valid @RequestBody GenerateAccessTokenRequest request
    ) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        AccessToken.UserRole role = AccessToken.UserRole.valueOf(request.getRole().toUpperCase());
        com.free.easyLearn.entity.AccessToken token = authService.generateAccessToken(role, user);

        GenerateAccessTokenResponse response = GenerateAccessTokenResponse.builder()
                .token(token.getToken())
                .role(token.getRole().name())
                .expiresAt(token.getExpiresAt())
                .createdAt(token.getCreatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Access token generated", response));
    }

    @GetMapping("/access-tokens/{role}")
    @Operation(
        summary = "Lister les tokens d'accès disponibles",
        description = "Retourne la liste des tokens d'accès non utilisés pour un rôle donné"
    )
    public ResponseEntity<ApiResponse<java.util.List<GenerateAccessTokenResponse>>> getAvailableAccessTokens(
            @PathVariable String role
    ) {
        AccessToken.UserRole tokenRole = AccessToken.UserRole.valueOf(role.toUpperCase());
        java.util.List<com.free.easyLearn.entity.AccessToken> tokens = authService.getAvailableAccessTokens(tokenRole);

        java.util.List<GenerateAccessTokenResponse> responses = tokens.stream()
                .map(token -> GenerateAccessTokenResponse.builder()
                        .token(token.getToken())
                        .role(token.getRole().name())
                        .expiresAt(token.getExpiresAt())
                        .createdAt(token.getCreatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Available access tokens retrieved", responses));
    }
}

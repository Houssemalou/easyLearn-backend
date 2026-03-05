package com.free.easyLearn.controller;

import com.free.easyLearn.dto.auth.*;
import com.free.easyLearn.dto.common.ApiResponse;
import com.free.easyLearn.entity.AccessToken;
import com.free.easyLearn.entity.PremiumToken;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.repository.UserRepository;
import com.free.easyLearn.security.CookieUtil;
import com.free.easyLearn.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints pour l'authentification et la gestion des tokens")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CookieUtil cookieUtil;

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

    @GetMapping("/verify-email")
    @Operation(
            summary = "Vérifier l'email",
            description = "Vérifie l'adresse email d'un professeur via le token envoyé par email"
    )
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email vérifié avec succès", null));
    }

    @PostMapping("/resend-verification")
    @Operation(
            summary = "Renvoyer l'email de vérification",
            description = "Renvoie un email de vérification à l'adresse spécifiée"
    )
    public ResponseEntity<ApiResponse<Void>> resendVerificationEmail(@RequestParam String email) {
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Email de vérification renvoyé", null));
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
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletResponse httpResponse) {
        AuthResponse response = authService.login(request);

        // Set tokens as HttpOnly cookies
        cookieUtil.addAccessTokenCookie(httpResponse, response.getToken());
        cookieUtil.addRefreshTokenCookie(httpResponse, response.getRefreshToken());

        // Strip tokens from the JSON response body (security: tokens should only live in cookies)
        response.setToken(null);
        response.setRefreshToken(null);

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
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@RequestBody(required = false) RefreshTokenRequest request,
                                                                  HttpServletRequest httpRequest,
                                                                  HttpServletResponse httpResponse) {
        // Try to get refresh token from HttpOnly cookie first, then from request body
        String refreshToken = cookieUtil.getRefreshTokenFromCookies(httpRequest);
        if (refreshToken == null && request != null && request.getRefreshToken() != null) {
            refreshToken = request.getRefreshToken();
        }
        if (refreshToken == null) {
            throw new BadRequestException("Refresh token is required");
        }

        AuthResponse response = authService.refreshToken(refreshToken);

        // Set new tokens as HttpOnly cookies
        cookieUtil.addAccessTokenCookie(httpResponse, response.getToken());
        cookieUtil.addRefreshTokenCookie(httpResponse, response.getRefreshToken());

        // Strip tokens from the JSON response body
        response.setToken(null);
        response.setRefreshToken(null);

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

        // Try email first, then uniqueCode for students
        User user = userRepository.findByEmail(username).orElse(null);
        if (user == null) {
            com.free.easyLearn.entity.Student student =
                    authService.findStudentByUniqueCode(username);
            user = student.getUser();
        }

        AuthResponse response = authService.getCurrentUser(user.getId());
        return ResponseEntity.ok(ApiResponse.success("User info retrieved", response));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Déconnexion utilisateur",
            description = "Invalide le refresh token de l'utilisateur"
    )
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse httpResponse) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        authService.logout(user.getId());

        // Clear HttpOnly cookies
        cookieUtil.clearTokenCookies(httpResponse);

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/generate-access-token")
    @Operation(
            summary = "Générer des tokens d'accès",
            description = "Génère un ou plusieurs tokens d'accès pour l'inscription (réservé aux admins). Paramètre 'count' pour spécifier le nombre."
    )
    public ResponseEntity<ApiResponse<java.util.List<GenerateAccessTokenResponse>>> generateAccessToken(
            @Valid @RequestBody GenerateAccessTokenRequest request
    ) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        AccessToken.UserRole role = AccessToken.UserRole.valueOf(request.getRole().toUpperCase());
        int count = request.getCount() != null ? request.getCount() : 1;

        java.util.List<com.free.easyLearn.entity.AccessToken> tokens = authService.generateAccessTokens(role, count, user);

        java.util.List<GenerateAccessTokenResponse> responses = tokens.stream()
                .map(token -> GenerateAccessTokenResponse.builder()
                        .token(token.getToken())
                        .role(token.getRole().name())
                        .expiresAt(token.getExpiresAt())
                        .createdAt(token.getCreatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(count + " access token(s) generated", responses));
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

    // ===== Premium Token Endpoints =====

    @PostMapping("/generate-premium-token")
    @Operation(
            summary = "Générer des tokens premium",
            description = "Génère un ou plusieurs tokens premium pour l'accès au chatbot IA (réservé aux admins). Paramètre 'count' pour spécifier le nombre."
    )
    public ResponseEntity<ApiResponse<java.util.List<GenerateAccessTokenResponse>>> generatePremiumToken(
            @RequestBody(required = false) java.util.Map<String, Object> request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        int count = 1;
        if (request != null && request.containsKey("count")) {
            count = Math.max(1, Math.min(100, ((Number) request.get("count")).intValue()));
        }

        java.util.List<PremiumToken> tokens = authService.generatePremiumTokens(count, user);

        java.util.List<GenerateAccessTokenResponse> responses = tokens.stream()
                .map(token -> GenerateAccessTokenResponse.builder()
                        .token(token.getToken())
                        .role("PREMIUM")
                        .expiresAt(token.getExpiresAt())
                        .createdAt(token.getCreatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(count + " premium token(s) generated", responses));
    }

    @GetMapping("/premium-tokens")
    @Operation(
            summary = "Lister les tokens premium disponibles",
            description = "Retourne la liste des tokens premium non utilisés"
    )
    public ResponseEntity<ApiResponse<java.util.List<GenerateAccessTokenResponse>>> getAvailablePremiumTokens() {
        java.util.List<PremiumToken> tokens = authService.getAvailablePremiumTokens();

        java.util.List<GenerateAccessTokenResponse> responses = tokens.stream()
                .map(token -> GenerateAccessTokenResponse.builder()
                        .token(token.getToken())
                        .role("PREMIUM")
                        .expiresAt(token.getExpiresAt())
                        .createdAt(token.getCreatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Available premium tokens retrieved", responses));
    }

    @PostMapping("/activate-premium")
    @Operation(
            summary = "Activer un token premium",
            description = "L'élève active un token premium pour accéder au chatbot IA pendant 30 jours"
    )
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> activatePremiumToken(
            @RequestBody java.util.Map<String, String> request
    ) {
        String tokenStr = request.get("token");
        if (tokenStr == null || tokenStr.isBlank()) {
            throw new BadRequestException("Token is required");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // Resolve user: try email first, then uniqueCode
        User user = userRepository.findByEmail(username).orElse(null);
        if (user == null) {
            com.free.easyLearn.entity.Student student = authService.findStudentByUniqueCode(username);
            user = student.getUser();
        }

        java.time.LocalDateTime premiumExpiry = authService.activatePremiumToken(tokenStr, user.getId());

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("premiumExpiresAt", premiumExpiry.toString());

        return ResponseEntity.ok(ApiResponse.success("Premium activé avec succès pour 30 jours", data));
    }

    @GetMapping("/premium-status")
    @Operation(
            summary = "Vérifier le statut premium",
            description = "Vérifie si l'élève a un abonnement premium actif"
    )
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getPremiumStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepository.findByEmail(username).orElse(null);
        if (user == null) {
            com.free.easyLearn.entity.Student student = authService.findStudentByUniqueCode(username);
            user = student.getUser();
        }

        boolean isActive = authService.isStudentPremiumActive(user.getId());

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("isPremium", isActive);

        return ResponseEntity.ok(ApiResponse.success("Premium status retrieved", data));
    }
}

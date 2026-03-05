package com.free.easyLearn.service;

import com.free.easyLearn.dto.auth.*;
import com.free.easyLearn.entity.AccessToken;
import com.free.easyLearn.entity.PremiumToken;
import com.free.easyLearn.entity.Professor;
import com.free.easyLearn.entity.Student;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.repository.AccessTokenRepository;
import com.free.easyLearn.repository.PremiumTokenRepository;
import com.free.easyLearn.repository.ProfessorRepository;
import com.free.easyLearn.repository.StudentRepository;
import com.free.easyLearn.repository.UserRepository;
import com.free.easyLearn.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Autowired
    private PremiumTokenRepository premiumTokenRepository;

    // Inject EmailService for verification emails
    @Autowired
    private EmailService emailService;

    @Transactional
    public AuthResponse registerStudent(StudentRegisterRequest request) {
        // Validate access token
        AccessToken accessToken = accessTokenRepository.findByToken(request.getAccessToken().trim())
                .orElseThrow(() -> new BadRequestException("Invalid access token"));

        if (accessToken.getIsUsed() || accessToken.getExpiresAt().isBefore(LocalDateTime.now()) ||
                !accessToken.getRole().equals(AccessToken.UserRole.STUDENT)) {
            throw new BadRequestException("Invalid or expired access token");
        }

        // Mark token as used and link to user
        accessToken.setIsUsed(true);
        accessToken.setUsedAt(LocalDateTime.now());
        // usedBy will be set after we create the User (because we need the User reference)

        // Check if uniqueCode already exists
        if (studentRepository.findByUniqueCode(request.getUniqueCode()).isPresent()) {
            throw new BadRequestException("Unique code already in use");
        }

        // Create new user
        User user = User.builder()
                .name(request.getName())
                .email(null) // Students don't have email
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.UserRole.STUDENT)
                .avatar(request.getAvatar())
                .isActive(true)
                .build();

        // Link createdBy on user to the admin who generated the token
        user.setCreatedBy(accessToken.getCreatedBy());

        user = userRepository.save(user);

        // Create student profile and set createdBy to the admin who generated the token
        Student student = Student.builder()
                .user(user)
                .nickname(request.getNickname())
                .bio(request.getBio())
                .level(Student.LanguageLevel.valueOf(request.getLevel().toUpperCase()))
                .uniqueCode(request.getUniqueCode())
                .joinedAt(LocalDateTime.now())
                .createdBy(accessToken.getCreatedBy())
                .build();

        studentRepository.save(student);

        // Now link token.usedBy
        accessToken.setUsedBy(user);
        accessTokenRepository.save(accessToken);

        // Also set user's accessToken reference for bidirectional mapping
        user.setAccessToken(accessToken);
        userRepository.save(user);

        // Generate tokens using uniqueCode as username for students
        String token = tokenProvider.generateTokenFromUsername(request.getUniqueCode());
        String refreshToken = tokenProvider.generateRefreshToken(request.getUniqueCode());

        log.info("Student registered successfully: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .build();
    }

    @Transactional
    public AuthResponse registerProfessor(ProfessorRegisterRequest request) {
        // Validate access token
        AccessToken accessToken = accessTokenRepository.findByToken(request.getAccessToken().trim())
                .orElseThrow(() -> new BadRequestException("Invalid access token"));

        if (accessToken.getIsUsed() || accessToken.getExpiresAt().isBefore(LocalDateTime.now()) ||
                !accessToken.getRole().equals(AccessToken.UserRole.PROFESSOR)) {
            throw new BadRequestException("Invalid or expired access token");
        }

        // Mark token as used and link to user
        accessToken.setIsUsed(true);
        accessToken.setUsedAt(LocalDateTime.now());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        // Create new user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.UserRole.PROFESSOR)
                .avatar(request.getAvatar())
                .isActive(true)
                .build();

        // Link createdBy on user to the admin who generated the token
        user.setCreatedBy(accessToken.getCreatedBy());

        user = userRepository.save(user);

        // Create professor profile and set createdBy to the admin who generated the token
        Professor professor = Professor.builder()
                .user(user)
                .bio(request.getBio())
                .languages(request.getLanguages())
                .specialization(request.getSpecialization())
                .joinedAt(LocalDateTime.now())
                .createdBy(accessToken.getCreatedBy())
                .build();

        professorRepository.save(professor);

        // Now link token.usedBy
        accessToken.setUsedBy(user);
        accessTokenRepository.save(accessToken);

        // Also set user's accessToken reference for bidirectional mapping
        user.setAccessToken(accessToken);

        // Generate email verification token and send verification email
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        user.setEmailVerified(false);
        userRepository.save(user);

        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationToken);
            log.info("Verification email sent to professor: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to professor: {}", user.getEmail(), e);
        }

        log.info("Professor registered successfully: {}", user.getEmail());

        // Return response WITHOUT JWT tokens — professor must verify email first
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public AuthResponse registerAdmin(AdminRegisterRequest request) {
        // Validate access token
        AccessToken accessToken = accessTokenRepository.findByToken(request.getAccessToken().trim())
                .orElseThrow(() -> new BadRequestException("Invalid access token"));

        if (accessToken.getIsUsed() || accessToken.getExpiresAt().isBefore(LocalDateTime.now()) ||
                !accessToken.getRole().equals(AccessToken.UserRole.ADMIN)) {
            throw new BadRequestException("Invalid or expired access token");
        }

        // Mark token as used and link to user
        accessToken.setIsUsed(true);
        accessToken.setUsedAt(LocalDateTime.now());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        // Create new user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.UserRole.ADMIN)
                .isActive(true)
                .build();

        // Link createdBy on user to the admin who generated the token
        user.setCreatedBy(accessToken.getCreatedBy());

        user = userRepository.save(user);

        // Now link token.usedBy
        accessToken.setUsedBy(user);
        accessTokenRepository.save(accessToken);

        // Also set user's accessToken reference for bidirectional mapping
        user.setAccessToken(accessToken);

        // Generate email verification token and send verification email
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        user.setEmailVerified(false);
        userRepository.save(user);

        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationToken);
            log.info("Verification email sent to admin: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to admin: {}", user.getEmail(), e);
        }

        log.info("Admin registered successfully: {}", user.getEmail());

        // Return response WITHOUT JWT tokens — admin must verify email first
        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }

    // Keep the old method for backward compatibility (simple registration)
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        // Create new user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.UserRole.valueOf(request.getRole().toUpperCase()))
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // Generate tokens
        String token = tokenProvider.generateTokenFromUsername(user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Get user details - try email first, then uniqueCode
        User user = userRepository.findByEmail(request.getUsername())
                .orElseGet(() -> {
                    // If not found by email, try by uniqueCode
                    Student student = studentRepository.findByUniqueCode(request.getUsername())
                            .orElseThrow(() -> new BadRequestException("User not found"));
                    return student.getUser();
                });

        // Block login for professors and admins who haven't verified their email
        if ((user.getRole() == User.UserRole.PROFESSOR || user.getRole() == User.UserRole.ADMIN)
                && !Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Veuillez vérifier votre adresse email avant de vous connecter. Consultez votre boîte de réception.");
        }

        // Check if the user's access token has expired (1 year validity)
        // If expired, deactivate user and block login
        AccessToken userAccessToken = user.getAccessToken();
        if (userAccessToken != null && userAccessToken.getExpiresAt() != null
                && userAccessToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            user.setIsActive(false);
            userRepository.save(user);
            log.warn("User {} access token expired. Account deactivated.", user.getId());
            throw new BadRequestException("Votre token d'accès a expiré. Votre compte est désactivé. Veuillez contacter l'administrateur pour obtenir un nouveau token.");
        }

        // Check if user is active
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BadRequestException("Votre compte est inactif. Veuillez contacter l'administrateur pour obtenir un nouveau token d'accès.");
        }

        // Generate tokens
        String token = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(request.getUsername());

        log.info("User logged in successfully: {}, role: {}", request.getUsername(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .expiresIn(tokenProvider.getJwtExpirationMs())
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (tokenProvider.validateToken(refreshToken)) {
            String username = tokenProvider.getUsernameFromToken(refreshToken);
            User user = userRepository.findByEmail(username)
                    .orElseGet(() -> {
                        // If not found by email, try by uniqueCode
                        Student student = studentRepository.findByUniqueCode(username)
                                .orElseThrow(() -> new BadRequestException("User not found"));
                        return student.getUser();
                    });

            String newToken = tokenProvider.generateTokenFromUsername(username);
            String newRefreshToken = tokenProvider.generateRefreshToken(username);

            log.info("Token refreshed for user: {}", username);

            return AuthResponse.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole().name())
                    .expiresIn(tokenProvider.getJwtExpirationMs())
                    .build();
        } else {
            log.warn("Invalid refresh token attempt");
            throw new BadRequestException("Invalid refresh token");
        }
    }

    public AuthResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        return AuthResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public AccessToken generateAccessToken(AccessToken.UserRole role, User createdBy) {
        // Generate unique token with role prefix
        String token;
        do {
            String randomPart = UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12);
            token = role.name() + "_" + randomPart;
        } while (accessTokenRepository.existsByTokenAndIsUsedFalseAndExpiresAtAfter(token, LocalDateTime.now()));

        AccessToken accessToken = AccessToken.builder()
                .token(token)
                .role(role)
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusYears(1)) // Tokens valid for 1 year
                .createdBy(createdBy)
                .build();

        return accessTokenRepository.save(accessToken);
    }

    @Transactional
    public List<AccessToken> generateAccessTokens(AccessToken.UserRole role, int count, User createdBy) {
        List<AccessToken> tokens = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            tokens.add(generateAccessToken(role, createdBy));
        }
        return tokens;
    }

    public List<AccessToken> getAvailableAccessTokens(AccessToken.UserRole role) {
        return accessTokenRepository.findByRoleAndIsUsedFalseAndExpiresAtAfter(role, LocalDateTime.now());
    }

    public Student findStudentByUniqueCode(String uniqueCode) {
        return studentRepository.findByUniqueCode(uniqueCode)
                .orElseThrow(() -> new BadRequestException("Student not found with code: " + uniqueCode));
    }

    // --- Email verification methods required by AuthController ---

    public void verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Invalid verification token");
        }

        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        if (user.getEmailVerificationTokenExpiry() != null && user.getEmailVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);
        userRepository.save(user);

        log.info("Email verified for user {}", user.getEmail());
    }

    public void resendVerificationEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found with email: " + email));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BadRequestException("Email already verified");
        }

        String verificationToken = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(24);

        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationTokenExpiry(expiry);
        userRepository.save(user);

        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getName(), verificationToken);
            log.info("Verification email resent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to resend verification email to {}", user.getEmail(), e);
            throw new BadRequestException("Failed to send verification email");
        }
    }

    public void logout(UUID userId) {
        // TODO: Invalider les refresh tokens de l'utilisateur en base
        // Pour l'instant, le client doit simplement supprimer ses tokens localement
        log.info("User logged out: {}", userId);
    }

    // ===== Premium Token Management =====

    @Transactional
    public PremiumToken generatePremiumToken(User createdBy) {
        String token;
        do {
            String randomPart = UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12);
            token = "PREMIUM_" + randomPart;
        } while (premiumTokenRepository.existsByTokenAndIsUsedFalseAndExpiresAtAfter(token, LocalDateTime.now()));

        PremiumToken premiumToken = PremiumToken.builder()
                .token(token)
                .isUsed(false)
                .durationDays(30)
                .expiresAt(LocalDateTime.now().plusYears(1)) // Token itself valid for 1 year (unused)
                .createdBy(createdBy)
                .build();

        return premiumTokenRepository.save(premiumToken);
    }

    @Transactional
    public List<PremiumToken> generatePremiumTokens(int count, User createdBy) {
        List<PremiumToken> tokens = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            tokens.add(generatePremiumToken(createdBy));
        }
        return tokens;
    }

    public List<PremiumToken> getAvailablePremiumTokens() {
        return premiumTokenRepository.findByIsUsedFalseAndExpiresAtAfter(LocalDateTime.now());
    }

    @Transactional
    public LocalDateTime activatePremiumToken(String tokenStr, UUID studentUserId) {
        PremiumToken premiumToken = premiumTokenRepository.findByToken(tokenStr.trim())
                .orElseThrow(() -> new BadRequestException("Token premium invalide"));

        if (premiumToken.getIsUsed() || premiumToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token premium invalide ou expiré");
        }

        User user = userRepository.findById(studentUserId)
                .orElseThrow(() -> new BadRequestException("Utilisateur non trouvé"));

        if (user.getRole() != User.UserRole.STUDENT) {
            throw new BadRequestException("Seuls les élèves peuvent activer un token premium");
        }

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Profil élève non trouvé"));

        // Mark token as used
        premiumToken.setIsUsed(true);
        premiumToken.setUsedAt(LocalDateTime.now());
        premiumToken.setUsedBy(user);
        premiumTokenRepository.save(premiumToken);

        // Set premium expiry on student (30 days from now)
        LocalDateTime premiumExpiry = LocalDateTime.now().plusDays(premiumToken.getDurationDays());
        student.setPremiumExpiresAt(premiumExpiry);
        studentRepository.save(student);

        log.info("Premium token activated for student {}. Expires at {}", student.getId(), premiumExpiry);
        return premiumExpiry;
    }

    public boolean isStudentPremiumActive(UUID studentUserId) {
        User user = userRepository.findById(studentUserId)
                .orElseThrow(() -> new BadRequestException("Utilisateur non trouvé"));

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Profil élève non trouvé"));

        return student.getPremiumExpiresAt() != null
                && student.getPremiumExpiresAt().isAfter(LocalDateTime.now());
    }

    // Reactivate a user with a new access token (admin operation)
    @Transactional
    public void reactivateUserWithToken(UUID userId, String newAccessTokenStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Utilisateur non trouvé"));

        AccessToken newToken = accessTokenRepository.findByToken(newAccessTokenStr.trim())
                .orElseThrow(() -> new BadRequestException("Token d'accès invalide"));

        if (newToken.getIsUsed() || newToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token d'accès invalide ou expiré");
        }

        // Mark new token as used by this user
        newToken.setIsUsed(true);
        newToken.setUsedAt(LocalDateTime.now());
        newToken.setUsedBy(user);
        accessTokenRepository.save(newToken);

        // Reactivate user
        user.setIsActive(true);
        user.setAccessToken(newToken);
        userRepository.save(user);

        log.info("User {} reactivated with new access token", userId);
    }
}

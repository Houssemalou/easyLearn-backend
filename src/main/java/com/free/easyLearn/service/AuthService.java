package com.free.easyLearn.service;

import com.free.easyLearn.dto.auth.*;
import com.free.easyLearn.entity.AccessToken;
import com.free.easyLearn.entity.Professor;
import com.free.easyLearn.entity.Student;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.repository.AccessTokenRepository;
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
        userRepository.save(user);

        // Generate tokens
        String token = tokenProvider.generateTokenFromUsername(user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        log.info("Professor registered successfully: {}", user.getEmail());

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
        userRepository.save(user);

        // Generate tokens
        String token = tokenProvider.generateTokenFromUsername(user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        log.info("Admin registered successfully: {}", user.getEmail());

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
                .expiresAt(LocalDateTime.now().plusDays(30)) // Tokens valid for 30 days
                .createdBy(createdBy)
                .build();

        return accessTokenRepository.save(accessToken);
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
}

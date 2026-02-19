package com.free.easyLearn.config;

import com.free.easyLearn.entity.AccessToken;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.repository.AccessTokenRepository;
import com.free.easyLearn.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
public class DataInitializer implements ApplicationRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createDefaultAdmin();
    }

    private void createDefaultAdmin() {
        if (userRepository.existsByRole(User.UserRole.ADMIN)) {
            log.info("==========================================================");
            log.info("          EasyLearn Backend - Startup Complete             ");
            log.info("==========================================================");
            log.info("  Admin account already exists, skipping creation.");
            log.info("==========================================================");
            return;
        }

        User admin = User.builder()
                .name("Admin")
                .email("admin@easylearn.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(User.UserRole.ADMIN)
                .isActive(true)
                .build();

        admin = userRepository.save(admin);

        // Create default access tokens for each role
        createAccessToken(AccessToken.UserRole.STUDENT, admin);
        createAccessToken(AccessToken.UserRole.PROFESSOR, admin);
        createAccessToken(AccessToken.UserRole.ADMIN, admin);

        log.info("==========================================================");
        log.info("          EasyLearn Backend - Startup Complete             ");
        log.info("==========================================================");
        log.info("  Default Admin Account Created:");
        log.info("  ----------------------");
        log.info("  Email    : admin@easylearn.com");
        log.info("  Password : admin123");
        log.info("==========================================================");
    }

    private String createAccessToken(AccessToken.UserRole role, User createdBy) {
        String randomPart = UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12);
        String token = role.name() + "_" + randomPart;

        AccessToken accessToken = AccessToken.builder()
                .token(token)
                .role(role)
                .isUsed(false)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .createdBy(createdBy)
                .build();

        accessTokenRepository.save(accessToken);
        return token;
    }

}

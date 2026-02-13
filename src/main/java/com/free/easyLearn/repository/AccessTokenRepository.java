package com.free.easyLearn.repository;

import com.free.easyLearn.entity.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccessTokenRepository extends JpaRepository<AccessToken, UUID> {

    Optional<AccessToken> findByToken(String token);

    List<AccessToken> findByRoleAndIsUsedFalseAndExpiresAtAfter(AccessToken.UserRole role, LocalDateTime now);

    boolean existsByTokenAndIsUsedFalseAndExpiresAtAfter(String token, LocalDateTime now);
}
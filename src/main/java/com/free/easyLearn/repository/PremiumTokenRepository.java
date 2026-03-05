package com.free.easyLearn.repository;

import com.free.easyLearn.entity.PremiumToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PremiumTokenRepository extends JpaRepository<PremiumToken, UUID> {

    Optional<PremiumToken> findByToken(String token);

    List<PremiumToken> findByIsUsedFalseAndExpiresAtAfter(LocalDateTime now);

    boolean existsByTokenAndIsUsedFalseAndExpiresAtAfter(String token, LocalDateTime now);

    List<PremiumToken> findByUsedBy_Id(UUID userId);
}

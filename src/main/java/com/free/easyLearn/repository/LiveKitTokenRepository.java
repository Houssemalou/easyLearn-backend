package com.free.easyLearn.repository;

import com.free.easyLearn.entity.LiveKitToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LiveKitTokenRepository extends JpaRepository<LiveKitToken, UUID> {

    List<LiveKitToken> findByUserIdAndRoomIdAndExpiresAtAfter(
            UUID userId,
            UUID roomId,
            LocalDateTime now
    );

    void deleteByExpiresAtBefore(LocalDateTime now);
}

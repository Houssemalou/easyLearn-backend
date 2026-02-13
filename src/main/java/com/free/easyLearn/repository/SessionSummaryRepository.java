package com.free.easyLearn.repository;

import com.free.easyLearn.entity.SessionSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionSummaryRepository extends JpaRepository<SessionSummary, UUID> {

    Optional<SessionSummary> findByRoomId(UUID roomId);
    
    List<SessionSummary> findByProfessorId(UUID professorId);
    
    List<SessionSummary> findByRoomIdIn(List<UUID> roomIds);
}

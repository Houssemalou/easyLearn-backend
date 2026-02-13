package com.free.easyLearn.repository;

import com.free.easyLearn.entity.RoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, UUID> {

    List<RoomParticipant> findByRoomId(UUID roomId);

    List<RoomParticipant> findByStudentId(UUID studentId);

    Optional<RoomParticipant> findByRoomIdAndStudentId(UUID roomId, UUID studentId);

    long countByRoomIdAndJoinedAtIsNotNull(UUID roomId);

    /**
     * Count participants who joined but haven't left yet (still in the room)
     */
    long countByRoomIdAndJoinedAtIsNotNullAndLeftAtIsNull(UUID roomId);
}

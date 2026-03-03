package com.free.easyLearn.repository;

import com.free.easyLearn.entity.SessionRecording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRecordingRepository extends JpaRepository<SessionRecording, Long> {

    /**
     * Find all recordings for a room by its LiveKit room name.
     */
    List<SessionRecording> findByRoomName(String roomName);
}

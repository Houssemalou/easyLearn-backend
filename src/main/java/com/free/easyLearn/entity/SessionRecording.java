package com.free.easyLearn.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SessionRecording Entity
 * Stores the recording URL for each LiveKit room.
 */
@Entity
@Table(name = "session_recordings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionRecording {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_name", nullable = false)
    private String roomName;

    @Column(name = "recording_url", nullable = false, length = 1024)
    private String recordingUrl;
}

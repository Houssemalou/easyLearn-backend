package com.free.easyLearn.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SessionRecording Entity
 * Stores the recording URL for each LiveKit room.
 * Recordings are automatically deleted 3 days after creation.
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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PostLoad
    protected void onLoad() {
        if (this.createdAt != null) {
            this.expiresAt = this.createdAt.plusDays(3);
        }
    }
}

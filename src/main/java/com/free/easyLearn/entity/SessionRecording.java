//package com.free.easyLearn.entity;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import java.time.LocalDateTime;
//
/// **
// * SessionRecording Entity
// * Stores metadata for LiveKit session recordings stored in MinIO
// */
//@Entity
//@Table(name = "session_recordings")
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class SessionRecording {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "room_id", nullable = false)
//    private Room room;
//
//    @Column(name = "livekit_recording_id", unique = true, nullable = false)
//    private String livekitRecordingId;
//
//    @Column(name = "file_name", nullable = false)
//    private String fileName;
//
//    @Column(name = "minio_bucket", nullable = false)
//    private String minioBucket;
//
//    @Column(name = "minio_object_key", nullable = false)
//    private String minioObjectKey;
//
//    @Column(name = "duration_seconds")
//    private Integer durationSeconds;
//
//    @Column(name = "file_size_bytes")
//    private Long fileSizeBytes;
//
//    @Column(name = "format")
//    private String format; // mp4, webm, etc.
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "status", nullable = false)
//    private RecordingStatus status;
//
//    @Column(name = "started_at", nullable = false)
//    private LocalDateTime startedAt;
//
//    @Column(name = "completed_at")
//    private LocalDateTime completedAt;
//
//    @Column(name = "error_message", columnDefinition = "TEXT")
//    private String errorMessage;
//
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//
//    @PrePersist
//    protected void onCreate() {
//        createdAt = LocalDateTime.now();
//        updatedAt = LocalDateTime.now();
//        if (status == null) {
//            status = RecordingStatus.RECORDING;
//        }
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        updatedAt = LocalDateTime.now();
//    }
//
//    /**
//     * Recording status enum
//     */
//    public enum RecordingStatus {
//        RECORDING,      // Currently recording
//        PROCESSING,     // Being uploaded to MinIO
//        COMPLETED,      // Available for viewing
//        FAILED          // Recording or upload failed
//    }
//}

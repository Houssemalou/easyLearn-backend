//package com.free.easyLearn.service;
//
//import com.lingua.hub.dto.SessionRecordingDTO;
//import com.lingua.hub.entity.Room;
//import com.lingua.hub.entity.SessionRecording;
//import com.lingua.hub.entity.SessionRecording.RecordingStatus;
//import com.free.easyLearn.repository.RoomRepository;
//import com.free.easyLearn.repository.SessionRecordingRepository;
//import io.minio.*;
//import io.minio.http.Method;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.InputStream;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//import java.util.stream.Collectors;
//
/// **
// * Service for managing session recordings with MinIO storage
// */
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class RecordingService {
//
//    private final SessionRecordingRepository recordingRepository;
//    private final RoomRepository roomRepository;
//    private final MinioClient minioClient;
//
//    @Value("${minio.bucket-name}")
//    private String bucketName;
//
//    @Value("${minio.url}")
//    private String minioUrl;
//
//    /**
//     * Start a new recording session
//     */
//    @Transactional
//    public SessionRecordingDTO startRecording(Long roomId, String livekitRecordingId) {
//        Room room = roomRepository.findById(roomId)
//                .orElseThrow(() -> new RuntimeException("Room not found: " + roomId));
//
//        // Create recording entity
//        SessionRecording recording = SessionRecording.builder()
//                .room(room)
//                .livekitRecordingId(livekitRecordingId)
//                .fileName(generateFileName(room, livekitRecordingId))
//                .minioBucket(bucketName)
//                .minioObjectKey(generateObjectKey(room, livekitRecordingId))
//                .format("mp4")
//                .status(RecordingStatus.RECORDING)
//                .startedAt(LocalDateTime.now())
//                .build();
//
//        recording = recordingRepository.save(recording);
//        log.info("Started recording for room {} with LiveKit ID: {}", roomId, livekitRecordingId);
//
//        return convertToDTO(recording);
//    }
//
//    /**
//     * Upload recording file to MinIO
//     */
//    @Transactional
//    public SessionRecordingDTO uploadRecording(String livekitRecordingId,
//                                               InputStream fileStream,
//                                               long fileSize,
//                                               Integer durationSeconds) {
//        SessionRecording recording = recordingRepository.findByLivekitRecordingId(livekitRecordingId)
//                .orElseThrow(() -> new RuntimeException("Recording not found: " + livekitRecordingId));
//
//        try {
//            // Update status to processing
//            recording.setStatus(RecordingStatus.PROCESSING);
//            recordingRepository.save(recording);
//
//            // Ensure bucket exists
//            ensureBucketExists();
//
//            // Upload to MinIO
//            minioClient.putObject(
//                PutObjectArgs.builder()
//                    .bucket(bucketName)
//                    .object(recording.getMinioObjectKey())
//                    .stream(fileStream, fileSize, -1)
//                    .contentType("video/mp4")
//                    .build()
//            );
//
//            // Update recording metadata
//            recording.setStatus(RecordingStatus.COMPLETED);
//            recording.setCompletedAt(LocalDateTime.now());
//            recording.setFileSizeBytes(fileSize);
//            recording.setDurationSeconds(durationSeconds);
//            recording = recordingRepository.save(recording);
//
//            log.info("Successfully uploaded recording {} to MinIO", livekitRecordingId);
//            return convertToDTO(recording);
//
//        } catch (Exception e) {
//            log.error("Failed to upload recording to MinIO", e);
//            recording.setStatus(RecordingStatus.FAILED);
//            recording.setErrorMessage(e.getMessage());
//            recordingRepository.save(recording);
//            throw new RuntimeException("Failed to upload recording", e);
//        }
//    }
//
//    /**
//     * Upload recording from MultipartFile (for manual uploads)
//     */
//    @Transactional
//    public SessionRecordingDTO uploadRecordingFile(String livekitRecordingId, MultipartFile file) {
//        try {
//            return uploadRecording(
//                livekitRecordingId,
//                file.getInputStream(),
//                file.getSize(),
//                null // Duration will be extracted separately if needed
//            );
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to upload recording file", e);
//        }
//    }
//
//    /**
//     * Get recording by ID with playback URL
//     */
//    @Transactional(readOnly = true)
//    public SessionRecordingDTO getRecording(Long id) {
//        SessionRecording recording = recordingRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Recording not found: " + id));
//
//        return convertToDTOWithUrl(recording);
//    }
//
//    /**
//     * Get all recordings for a room
//     */
//    @Transactional(readOnly = true)
//    public List<SessionRecordingDTO> getRoomRecordings(Long roomId) {
//        List<SessionRecording> recordings = recordingRepository.findByRoomIdOrderByStartedAtDesc(roomId);
//        return recordings.stream()
//                .map(this::convertToDTOWithUrl)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Get all recordings for a student (rooms they participated in)
//     */
//    @Transactional(readOnly = true)
//    public List<SessionRecordingDTO> getStudentRecordings(Long studentId) {
//        List<SessionRecording> recordings = recordingRepository.findCompletedRecordingsByStudentId(studentId);
//        return recordings.stream()
//                .map(this::convertToDTOWithUrl)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Get all completed recordings (for admin)
//     */
//    @Transactional(readOnly = true)
//    public Page<SessionRecordingDTO> getAllCompletedRecordings(Pageable pageable) {
//        Page<SessionRecording> recordings = recordingRepository.findByStatusOrderByStartedAtDesc(
//                RecordingStatus.COMPLETED, pageable);
//        return recordings.map(this::convertToDTOWithUrl);
//    }
//
//    /**
//     * Delete recording
//     */
//    @Transactional
//    public void deleteRecording(Long id) {
//        SessionRecording recording = recordingRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Recording not found: " + id));
//
//        // Delete from MinIO
//        try {
//            minioClient.removeObject(
//                RemoveObjectArgs.builder()
//                    .bucket(bucketName)
//                    .object(recording.getMinioObjectKey())
//                    .build()
//            );
//            log.info("Deleted recording {} from MinIO", recording.getMinioObjectKey());
//        } catch (Exception e) {
//            log.error("Failed to delete recording from MinIO", e);
//            // Continue with database deletion even if MinIO deletion fails
//        }
//
//        // Delete from database
//        recordingRepository.delete(recording);
//        log.info("Deleted recording {} from database", id);
//    }
//
//    /**
//     * Generate pre-signed URL for viewing recording
//     */
//    public String generatePlaybackUrl(String objectKey) {
//        try {
//            return minioClient.getPresignedObjectUrl(
//                GetPresignedObjectUrlArgs.builder()
//                    .method(Method.GET)
//                    .bucket(bucketName)
//                    .object(objectKey)
//                    .expiry(24, TimeUnit.HOURS) // URL valid for 24 hours
//                    .build()
//            );
//        } catch (Exception e) {
//            log.error("Failed to generate playback URL", e);
//            throw new RuntimeException("Failed to generate playback URL", e);
//        }
//    }
//
//    /**
//     * Ensure MinIO bucket exists
//     */
//    private void ensureBucketExists() {
//        try {
//            boolean found = minioClient.bucketExists(
//                BucketExistsArgs.builder().bucket(bucketName).build()
//            );
//
//            if (!found) {
//                minioClient.makeBucket(
//                    MakeBucketArgs.builder().bucket(bucketName).build()
//                );
//                log.info("Created MinIO bucket: {}", bucketName);
//            }
//        } catch (Exception e) {
//            log.error("Failed to ensure bucket exists", e);
//            throw new RuntimeException("Failed to create bucket", e);
//        }
//    }
//
//    /**
//     * Generate unique filename for recording
//     */
//    private String generateFileName(Room room, String livekitRecordingId) {
//        return String.format("room_%d_%s_%s.mp4",
//            room.getId(),
//            room.getName().replaceAll("[^a-zA-Z0-9]", "_"),
//            livekitRecordingId);
//    }
//
//    /**
//     * Generate MinIO object key (path in bucket)
//     */
//    private String generateObjectKey(Room room, String livekitRecordingId) {
//        return String.format("recordings/%d/%s.mp4",
//            room.getId(),
//            livekitRecordingId);
//    }
//
//    /**
//     * Convert entity to DTO
//     */
//    private SessionRecordingDTO convertToDTO(SessionRecording recording) {
//        return SessionRecordingDTO.builder()
//                .id(recording.getId())
//                .roomId(recording.getRoom().getId())
//                .roomName(recording.getRoom().getName())
//                .livekitRecordingId(recording.getLivekitRecordingId())
//                .fileName(recording.getFileName())
//                .durationSeconds(recording.getDurationSeconds())
//                .fileSizeBytes(recording.getFileSizeBytes())
//                .format(recording.getFormat())
//                .status(recording.getStatus())
//                .startedAt(recording.getStartedAt())
//                .completedAt(recording.getCompletedAt())
//                .errorMessage(recording.getErrorMessage())
//                .build();
//    }
//
//    /**
//     * Convert entity to DTO with playback URL
//     */
//    private SessionRecordingDTO convertToDTOWithUrl(SessionRecording recording) {
//        SessionRecordingDTO dto = convertToDTO(recording);
//
//        // Add playback URL if recording is completed
//        if (recording.getStatus() == RecordingStatus.COMPLETED) {
//            dto.setPlaybackUrl(generatePlaybackUrl(recording.getMinioObjectKey()));
//        }
//
//        return dto;
//    }
//}

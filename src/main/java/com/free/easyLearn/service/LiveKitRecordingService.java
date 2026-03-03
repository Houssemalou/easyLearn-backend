package com.free.easyLearn.service;

import com.free.easyLearn.entity.SessionRecording;
import com.free.easyLearn.repository.SessionRecordingRepository;
import io.livekit.server.EgressServiceClient;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitEgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiveKitRecordingService {

    private static final Logger log = LoggerFactory.getLogger(LiveKitRecordingService.class);

    private final EgressServiceClient egressClient;
    private final RoomServiceClient roomServiceClient;
    private final SessionRecordingRepository sessionRecordingRepository;

    // Track active egress per room: roomName -> egressId
    private final Map<String, String> activeEgressMap = new ConcurrentHashMap<>();

    // S3/MinIO configuration
    @Value("${livekit.s3.access-key:minioadmin}")
    private String s3AccessKey;

    @Value("${livekit.s3.secret-key:minioadmin}")
    private String s3SecretKey;

    @Value("${livekit.s3.endpoint:http://91.134.137.202:9000}")
    private String s3Endpoint;

    @Value("${livekit.s3.bucket:livekit-recordings}")
    private String s3Bucket;

    @Value("${livekit.s3.region:us-east-1}")
    private String s3Region;

    public LiveKitRecordingService(EgressServiceClient egressClient,
                                    RoomServiceClient roomServiceClient,
                                    SessionRecordingRepository sessionRecordingRepository) {
        this.egressClient = egressClient;
        this.roomServiceClient = roomServiceClient;
        this.sessionRecordingRepository = sessionRecordingRepository;
    }

    /**
     * Start a RoomCompositeEgress recording for the given room.
     * The recording is saved as MP4 to MinIO (S3-compatible).
     */
    public String startRecording(String roomName) {
        try {
            // Check if already recording
            if (activeEgressMap.containsKey(roomName)) {
                String existingId = activeEgressMap.get(roomName);
                log.warn("Recording already active for room '{}', egressId: {}", roomName, existingId);
                return existingId;
            }

            // Build S3 upload config pointing to MinIO
            LivekitEgress.S3Upload s3Upload = LivekitEgress.S3Upload.newBuilder()
                    .setAccessKey(s3AccessKey)
                    .setSecret(s3SecretKey)
                    .setEndpoint(s3Endpoint)
                    .setBucket(s3Bucket)
                    .setRegion(s3Region)
                    .setForcePathStyle(true)
                    .build();

            // Build file output with S3 destination
            LivekitEgress.EncodedFileOutput fileOutput = LivekitEgress.EncodedFileOutput.newBuilder()
                    .setFileType(LivekitEgress.EncodedFileType.MP4)
                    .setFilepath(roomName + "/{room_name}-{time}.mp4")
                    .setS3(s3Upload)
                    .build();

            log.info("Starting RoomCompositeEgress for room '{}' -> s3://{}/{}", roomName, s3Bucket, roomName);

            // Use the SDK high-level method
            Call<LivekitEgress.EgressInfo> call = egressClient.startRoomCompositeEgress(
                    roomName,
                    fileOutput,
                    "grid",  // layout
                    null,    // optionsPreset
                    null,    // optionsAdvanced
                    false,   // audioOnly
                    false    // videoOnly
            );

            Response<LivekitEgress.EgressInfo> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                LivekitEgress.EgressInfo egressInfo = response.body();
                String egressId = egressInfo.getEgressId();
                activeEgressMap.put(roomName, egressId);
                log.info("Recording started for room '{}'. EgressId: {}, Status: {}",
                        roomName, egressId, egressInfo.getStatus());
                return egressId;
            } else {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "unknown";
                log.error("Failed to start recording for room '{}'. HTTP {}: {}",
                        roomName, response.code(), errorBody);
                return null;
            }
        } catch (Exception e) {
            log.error("Exception starting recording for room '{}': {}", roomName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Stop the active recording for the given room.
     */
    public void stopRecording(String roomName) {
        try {
            String egressId = activeEgressMap.get(roomName);

            if (egressId == null) {
                // Try to find active egress from LiveKit directly
                egressId = findActiveEgressForRoom(roomName);
                if (egressId == null) {
                    log.warn("No active recording found for room '{}'. Nothing to stop.", roomName);
                    return;
                }
            }

            log.info("Stopping recording for room '{}', egressId: {}", roomName, egressId);

            Call<LivekitEgress.EgressInfo> call = egressClient.stopEgress(egressId);
            Response<LivekitEgress.EgressInfo> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                LivekitEgress.EgressInfo egressInfo = response.body();
                log.info("Recording stop requested for room '{}'. EgressId: {}, Status: {}",
                        roomName, egressInfo.getEgressId(), egressInfo.getStatus());
            } else {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "unknown";
                log.error("Failed to stop recording for room '{}'. HTTP {}: {}", roomName, response.code(), errorBody);
            }

            activeEgressMap.remove(roomName);
        } catch (Exception e) {
            log.error("Exception stopping recording for room '{}': {}", roomName, e.getMessage(), e);
            activeEgressMap.remove(roomName);
        }
    }

    /**
     * Handle egress lifecycle events from webhooks.
     */
    public void handleEgressEvent(LivekitEgress.EgressInfo egressInfo) {
        String egressId = egressInfo.getEgressId();
        String roomName = egressInfo.getRoomName();
        LivekitEgress.EgressStatus status = egressInfo.getStatus();

        log.info("Egress event - Room: '{}', EgressId: {}, Status: {}", roomName, egressId, status);

        switch (status) {
            case EGRESS_COMPLETE:
                log.info("Recording COMPLETE for room '{}'. File uploaded to MinIO.", roomName);
                if (egressInfo.getFileResultsCount() > 0) {
                    for (LivekitEgress.FileInfo fi : egressInfo.getFileResultsList()) {
                        log.info("  -> File: {}, Size: {} bytes, Location: {}", fi.getFilename(), fi.getSize(), fi.getLocation());
                        saveRecordingUrl(roomName, fi.getLocation());
                    }
                }
                activeEgressMap.remove(roomName);
                break;

            case EGRESS_FAILED:
                log.error("Recording FAILED for room '{}'. EgressId: {}, Error: {}",
                        roomName, egressId, egressInfo.getError());
                activeEgressMap.remove(roomName);
                break;

            case EGRESS_ACTIVE:
                log.info("Recording ACTIVE for room '{}'. EgressId: {}", roomName, egressId);
                activeEgressMap.put(roomName, egressId);
                break;

            case EGRESS_STARTING:
                log.info("Recording STARTING for room '{}'. EgressId: {}", roomName, egressId);
                break;

            case EGRESS_ENDING:
                log.info("Recording ENDING for room '{}'. EgressId: {}", roomName, egressId);
                break;

            default:
                log.debug("Unhandled egress status '{}' for room '{}'", status, roomName);
                break;
        }
    }

    /**
     * Query LiveKit for any active egress in a room.
     */
    private String findActiveEgressForRoom(String roomName) {
        try {
            // listEgress(roomName, egressId, active) — pass null for egressId, true for active
            Call<List<LivekitEgress.EgressInfo>> call = egressClient.listEgress(roomName, null, true);
            Response<List<LivekitEgress.EgressInfo>> response = call.execute();

            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                String egressId = response.body().get(0).getEgressId();
                log.info("Found active egress '{}' for room '{}'", egressId, roomName);
                return egressId;
            }
        } catch (Exception e) {
            log.error("Error listing egress for room '{}': {}", roomName, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Check if there is an active recording for a room.
     */
    public boolean isRecording(String roomName) {
        return activeEgressMap.containsKey(roomName);
    }

    /**
     * Save the recording URL to the database after egress completes.
     */
    private void saveRecordingUrl(String roomName, String recordingUrl) {
        try {
            SessionRecording recording = SessionRecording.builder()
                    .roomName(roomName)
                    .recordingUrl(recordingUrl)
                    .build();
            sessionRecordingRepository.save(recording);
            log.info("Saved recording URL for room '{}': {}", roomName, recordingUrl);
        } catch (Exception e) {
            log.error("Failed to save recording URL for room '{}': {}", roomName, e.getMessage(), e);
        }
    }

    /**
     * Get all recording URLs for a given room name.
     */
    public List<SessionRecording> getRecordingsByRoomName(String roomName) {
        return sessionRecordingRepository.findByRoomName(roomName);
    }
}

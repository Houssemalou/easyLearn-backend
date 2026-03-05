package com.free.easyLearn.service;

import com.free.easyLearn.entity.SessionRecording;
import com.free.easyLearn.repository.SessionRecordingRepository;
import io.livekit.server.*;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import livekit.LivekitEgress;
import livekit.LivekitModels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class LiveKitRecordingService {

    private static final Logger log = LoggerFactory.getLogger(LiveKitRecordingService.class);

    private final EgressServiceClient egressClient;
    private final RoomServiceClient roomServiceClient;
    private final SessionRecordingRepository sessionRecordingRepository;
    private final MinioClient minioClient;

    // Track active egress per room: roomName -> egressId
    private final Map<String, String> activeEgressMap = new ConcurrentHashMap<>();

    // Reverse map: egressId -> roomName (needed because WebEgress events have empty roomName)
    private final Map<String, String> egressToRoomMap = new ConcurrentHashMap<>();

    // LiveKit configuration
    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    // Internal URL for egress Chrome to connect to LiveKit (inside Docker network)
    @Value("${livekit.internal-url:ws://livekit:7880}")
    private String livekitInternalUrl;

    // URL of the recording page (frontend, reachable from egress container)
    @Value("${livekit.recording-page-url:http://host.docker.internal:8080}")
    private String recordingPageUrl;

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
                                    SessionRecordingRepository sessionRecordingRepository,
                                    MinioClient minioClient) {
        this.egressClient = egressClient;
        this.roomServiceClient = roomServiceClient;
        this.sessionRecordingRepository = sessionRecordingRepository;
        this.minioClient = minioClient;
    }

    /**
     * Generate a LiveKit access token for the egress recorder (hidden viewer).
     */
    private String generateRecorderToken(String roomName) {
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setName("Recorder");
        token.setIdentity("egress-recorder-" + roomName);
        token.setTtl(14400); // 4 hours

        // Subscribe-only — no publish, hidden
        token.addGrants(
                new RoomJoin(true),
                new RoomName(roomName),
                new CanPublish(false),
                new CanSubscribe(true),
                new CanPublishData(false),
                new Hidden(true)
        );

        return token.toJwt();
    }

    /**
     * Start a WebEgress recording for the given room.
     * Opens ProfessorLiveRoom in recording mode (Chrome headless), which renders the full
     * session UI (video grid, chat, whiteboard). The output is saved as MP4 to MinIO.
     */
    public String startRecording(String roomName) {
        try {
            // Check if already recording
            if (activeEgressMap.containsKey(roomName)) {
                String existingId = activeEgressMap.get(roomName);
                log.warn("Recording already active for room '{}', egressId: {}", roomName, existingId);
                return existingId;
            }

            // Generate a LiveKit token for the recorder
            String recorderToken = generateRecorderToken(roomName);

            // Build the recording URL: /professor/room/{roomName}/record?token=...&wsUrl=...
            // In recording mode, ProfessorLiveRoom ignores the roomId param and uses token directly
            String url = recordingPageUrl + "/professor/room/" + roomName + "/record"
                    + "?token=" + URLEncoder.encode(recorderToken, StandardCharsets.UTF_8)
                    + "&wsUrl=" + URLEncoder.encode(livekitInternalUrl, StandardCharsets.UTF_8);

            log.info("Starting WebEgress for room '{}' -> {}", roomName, url);

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
                    .setFilepath(roomName + "/{time}.mp4")
                    .setS3(s3Upload)
                    .build();

            log.info("WebEgress URL: {}", url);
            log.info("S3 config: endpoint={}, bucket={}, region={}, path={}/", s3Endpoint, s3Bucket, s3Region, roomName);

            Call<LivekitEgress.EgressInfo> call = egressClient.startWebEgress(url, fileOutput);

            Response<LivekitEgress.EgressInfo> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                LivekitEgress.EgressInfo egressInfo = response.body();
                String egressId = egressInfo.getEgressId();
                activeEgressMap.put(roomName, egressId);
                egressToRoomMap.put(egressId, roomName);
                log.info("WebEgress started for room '{}'. EgressId: {}, Status: {}",
                        roomName, egressId, egressInfo.getStatus());
                return egressId;
            } else {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "unknown";
                log.error("Failed to start WebEgress for room '{}'. HTTP {}: {}",
                        roomName, response.code(), errorBody);
                return null;
            }
        } catch (Exception e) {
            log.error("Exception starting WebEgress for room '{}': {}", roomName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Stop the active recording for the given room.
     */
    public void stopRecording(String roomName) {
        try {
            String egressId = activeEgressMap.get(roomName);

            // Always query LiveKit for active egress (in-memory map may be stale after restart)
            if (egressId == null) {
                egressId = findActiveEgressForRoom(roomName);
            }

            if (egressId == null) {
                log.info("No active recording found for room '{}'. Nothing to stop.", roomName);
                return;
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
                // Egress may already be stopping/completed — this is expected on room_finished
                if (response.code() == 404 || (errorBody != null && errorBody.contains("not found"))) {
                    log.info("Egress '{}' for room '{}' already stopped/completed.", egressId, roomName);
                } else {
                    log.warn("Failed to stop recording for room '{}'. HTTP {}: {}", roomName, response.code(), errorBody);
                }
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

        // WebEgress events have empty roomName — look up from our reverse map
        if (roomName == null || roomName.isEmpty()) {
            roomName = egressToRoomMap.get(egressId);
            log.info("WebEgress event — resolved roomName '{}' from egressId '{}'", roomName, egressId);
        }

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
                egressToRoomMap.remove(egressId);
                break;

            case EGRESS_FAILED:
                log.error("Recording FAILED for room '{}'. EgressId: {}, Error: {}",
                        roomName, egressId, egressInfo.getError());
                activeEgressMap.remove(roomName);
                egressToRoomMap.remove(egressId);
                break;

            case EGRESS_ACTIVE:
                log.info("Recording ACTIVE for room '{}'. EgressId: {}", roomName, egressId);
                if (roomName != null && !roomName.isEmpty()) {
                    activeEgressMap.put(roomName, egressId);
                    egressToRoomMap.put(egressId, roomName);
                }
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
     * Clean up in-memory tracking for a room (called when room finishes).
     * NOTE: Do NOT remove from egressToRoomMap here — it's needed when
     * the egress_ended webhook fires (WebEgress events have empty roomName).
     */
    public void clearRoom(String roomName) {
        activeEgressMap.remove(roomName);
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

    /**
     * Generate a presigned GET URL for a recording stored in MinIO.
     * The URL is valid for the given duration.
     *
     * @param recordingUrl the raw S3/MinIO URL stored in the DB
     *                     (e.g. http://91.134.137.202:9000/livekit-recordings/room-xxx/file.mp4)
     * @param durationMinutes how long the presigned link should be valid
     * @return a presigned URL, or the original URL if generation fails
     */
    public String generatePresignedUrl(String recordingUrl, int durationMinutes) {
        try {
            // Extract the object key from the full URL
            // URL format: http://host:port/bucket/object-key
            URI uri = new URI(recordingUrl);
            String path = uri.getPath(); // e.g. /livekit-recordings/room-xxx/file.mp4

            // Remove leading slash and bucket name to get the object key
            String bucketPrefix = "/" + s3Bucket + "/";
            String objectKey;
            if (path.startsWith(bucketPrefix)) {
                objectKey = path.substring(bucketPrefix.length());
            } else {
                // Fallback: remove just the leading slash
                objectKey = path.startsWith("/") ? path.substring(1) : path;
            }

            String presigned = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(s3Bucket)
                            .object(objectKey)
                            .expiry(durationMinutes, TimeUnit.MINUTES)
                            .build()
            );

            log.info("Generated presigned URL for object '{}' (valid {} min)", objectKey, durationMinutes);
            return presigned;
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for '{}': {}", recordingUrl, e.getMessage(), e);
            return recordingUrl; // fallback to original
        }
    }

    /**
     * Destroy the LiveKit room. This kicks all participants (including the recorder)
     * and triggers the room_finished webhook from LiveKit.
     */
    public void destroyLiveKitRoom(String roomName) {
        try {
            log.info("Destroying LiveKit room '{}'", roomName);
            Call<Void> call = roomServiceClient.deleteRoom(roomName);
            Response<Void> response = call.execute();
            if (response.isSuccessful()) {
                log.info("LiveKit room '{}' destroyed successfully", roomName);
            } else {
                log.warn("Failed to destroy LiveKit room '{}': HTTP {}", roomName, response.code());
            }
        } catch (Exception e) {
            log.error("Error destroying LiveKit room '{}': {}", roomName, e.getMessage(), e);
        }
    }

    /**
     * Count real (non-recorder) participants in a LiveKit room.
     * The egress recorder has identity starting with "egress-recorder-".
     * Returns -1 on error.
     */
    public int countRealParticipants(String roomName) {
        try {
            Call<List<LivekitModels.ParticipantInfo>> call = roomServiceClient.listParticipants(roomName);
            Response<List<LivekitModels.ParticipantInfo>> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                long realCount = response.body().stream()
                        .filter(p -> !p.getIdentity().startsWith("egress-recorder-"))
                        .count();
                log.info("Room '{}' has {} real participants (total: {})",
                        roomName, realCount, response.body().size());
                return (int) realCount;
            } else {
                log.warn("Failed to list participants for room '{}': HTTP {}",
                        roomName, response.code());
                return -1;
            }
        } catch (Exception e) {
            log.error("Error listing participants for room '{}': {}", roomName, e.getMessage(), e);
            return -1;
        }
    }
}

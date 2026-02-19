package com.free.easyLearn.service;

import com.free.easyLearn.config.LiveKitConfig;
import com.free.easyLearn.dto.livekit.LiveKitTokenResponse;
import com.free.easyLearn.entity.LiveKitToken;
import com.free.easyLearn.entity.Room;
import com.free.easyLearn.entity.User;
import com.free.easyLearn.exception.ResourceNotFoundException;
import com.free.easyLearn.repository.LiveKitTokenRepository;
import com.free.easyLearn.repository.RoomRepository;
import com.free.easyLearn.repository.UserRepository;
import io.livekit.server.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class LiveKitService {

    @Autowired
    private LiveKitConfig livekitConfig;

    @Autowired
    private RoomServiceClient roomServiceClient;

    @Autowired
    private LiveKitTokenRepository liveKitTokenRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${livekit.token-expiration}")
    private long tokenExpiration;

    /**
     * Generate LiveKit access token for a user to join a room
     * If the room is SCHEDULED, the first token generation sets it to LIVE
     */
    @Transactional
    public LiveKitTokenResponse generateToken(UUID roomId, UUID userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Create unique LiveKit room name if not exists
        if (room.getLivekitRoomName() == null) {
            room.setLivekitRoomName("room-" + room.getId().toString());
        }

        // First person joining → set room to LIVE
        if (room.getStatus() == Room.RoomStatus.SCHEDULED) {
            room.setStatus(Room.RoomStatus.LIVE);
        }
        roomRepository.save(room);

        System.out.println(user.getRole());

        // Create LiveKit room if it doesn't exist (best effort - room creation or already exists)
        try {
            System.out.println("Creating/Ensuring LiveKit room exists: " + room.getLivekitRoomName());
            createLiveKitRoom(room.getLivekitRoomName(), room.getMaxStudents());
        } catch (Exception e) {
            // Room might already exist, which is fine - ignore error
            System.out.println("LiveKit room creation note: " + e.getMessage());
        }

        // Determine if user is professor (can publish video/audio) or student (can only subscribe)
        boolean canPublish = user.getRole() == User.UserRole.ADMIN ||
                user.getRole() == User.UserRole.PROFESSOR;

        // Create access token
        String identity = user.getName();
        AccessToken token = new AccessToken(
                livekitConfig.getApiKey(),
                livekitConfig.getApiSecret()
        );

        token.setName(user.getName());
        token.setIdentity(identity);
        token.setTtl(tokenExpiration);

        // Add video grants with proper permissions
        token.addGrants(
                new RoomJoin(true),
                new RoomName(room.getLivekitRoomName()),
                new CanPublish(true),
                new CanSubscribe(true),
                new CanPublishData(true)
        );

        String jwt = token.toJwt();

        // Save token to database
        LocalDateTime expiresAt = LocalDateTime.now().plus(tokenExpiration, ChronoUnit.SECONDS);
        LiveKitToken liveKitToken = LiveKitToken.builder()
                .user(user)
                .room(room)
                .token(jwt)
                .identity(identity)
                .expiresAt(expiresAt)
                .build();

        liveKitTokenRepository.save(liveKitToken);
        System.out.println(livekitConfig.getApiKey());

        return LiveKitTokenResponse.builder()
                .token(jwt)
                .identity(identity)
                .roomName(room.getLivekitRoomName())
                .serverUrl(livekitConfig.getLivekitUrl())
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * Create a LiveKit room
     */
    public void createLiveKitRoom(String roomName, int maxParticipants) {
        try {
            // Use the correct API for SDK 0.8.5 - createRoom expects just the room name
            roomServiceClient.createRoom(roomName);
            System.out.println("LiveKit room created successfully: " + roomName);
        } catch (Exception e) {
            // Log error but don't fail - room might already exist
            System.out.println("LiveKit room creation/check: " + e.getMessage());
        }
    }

    /**
     * Delete a LiveKit room
     */
    public void deleteLiveKitRoom(String roomName) {
        try {
            roomServiceClient.deleteRoom(roomName);
        } catch (Exception e) {
            // Log and ignore; deletion is best-effort
            System.err.println("Error deleting LiveKit room: " + e.getMessage());
        }
    }

    /**
     * List all active LiveKit rooms
     */
    public void listRooms() {
        // TODO: Implement room listing
        // This is a placeholder for now
    }

    /**
     * Get LiveKit room info
     */
    public void getRoomInfo(String roomName) {
        // TODO: Implement room info retrieval
        // This is a placeholder for now
    }

    /**
     * List participants in a LiveKit room
     */
    public void listParticipants(String roomName) {
        // TODO: Implement participant listing
        // This is a placeholder for now
    }

    /**
     * Remove participant from room
     */
    public void removeParticipant(String roomName, String identity) {
        // TODO: Implement participant removal
        // This is a placeholder for now
    }

    /**
     * Mute/Unmute participant
     */
    public void muteParticipantTrack(String roomName, String identity, String trackSid, boolean muted) {
        // TODO: Implement participant muting
        // This is a placeholder for now
    }

    /**
     * Clean up expired tokens (runs every hour)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanUpExpiredTokens() {
        liveKitTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    /**
     * Validate webhook signature
     */
    public boolean validateWebhook(String token, String body) {
        WebhookReceiver receiver = new WebhookReceiver(
                livekitConfig.getApiKey(),
                livekitConfig.getApiSecret()
        );
        try {
            receiver.receive(body, token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

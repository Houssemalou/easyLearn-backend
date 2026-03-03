package com.free.easyLearn.controller;

import com.free.easyLearn.service.LiveKitRecordingService;
import com.free.easyLearn.service.RoomService;
import com.free.easyLearn.dto.room.RoomDTO;
import io.livekit.server.WebhookReceiver;
import livekit.LivekitWebhook.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/livekit/webhook")
public class LiveKitWebhookController {

    private static final Logger log = LoggerFactory.getLogger(LiveKitWebhookController.class);

    private final LiveKitRecordingService recordingService;
    private final RoomService roomService;
    private final WebhookReceiver webhookReceiver;

    public LiveKitWebhookController(LiveKitRecordingService service,
                                     RoomService roomService,
                                     WebhookReceiver webhookReceiver) {
        this.recordingService = service;
        this.roomService = roomService;
        this.webhookReceiver = webhookReceiver;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String body,
                                                @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            // Validate and parse the webhook using the SDK
            WebhookEvent event = webhookReceiver.receive(body, authorization);

            String eventType = event.getEvent();
            String roomName = event.hasRoom() ? event.getRoom().getName() : null;

            log.info("LiveKit webhook: event='{}', room='{}'", eventType, roomName);

            if (roomName == null) {
                // Egress events may not have room at top level
                if (event.hasEgressInfo()) {
                    roomName = event.getEgressInfo().getRoomName();
                }
            }

            switch (eventType) {
                // ─── Room Events ───
                case "room_started":
                    log.info("Room started: '{}'", roomName);
                    break;

                case "room_finished":
                    handleRoomFinished(roomName);
                    break;

                // ─── Participant Events ───
                case "participant_joined":
                    handleParticipantJoined(event, roomName);
                    break;

                case "participant_left":
                    log.info("Participant '{}' left room '{}'",
                            event.hasParticipant() ? event.getParticipant().getIdentity() : "?",
                            roomName);
                    break;

                // ─── Egress Events ───
                case "egress_started":
                case "egress_updated":
                case "egress_ended":
                    if (event.hasEgressInfo()) {
                        log.info("Egress event '{}' for room '{}'", eventType, event.getEgressInfo().getRoomName());
                        recordingService.handleEgressEvent(event.getEgressInfo());
                    }
                    break;

                default:
                    log.debug("Unhandled webhook event: '{}'", eventType);
                    break;
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Error processing LiveKit webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }

    /**
     * When a participant joins, auto-start recording if not already active.
     */
    private void handleParticipantJoined(WebhookEvent event, String roomName) {
        String identity = event.hasParticipant() ? event.getParticipant().getIdentity() : "unknown";
        int numParticipants = event.hasRoom() ? event.getRoom().getNumParticipants() : 0;

        log.info("Participant '{}' joined room '{}'. Total: {}", identity, roomName, numParticipants);

        // Auto-start recording when first participant joins
        if (roomName != null && numParticipants >= 1 && !recordingService.isRecording(roomName)) {
            log.info("Auto-starting recording for room '{}'", roomName);
            String egressId = recordingService.startRecording(roomName);
            if (egressId != null) {
                log.info("Recording auto-started for room '{}', egressId: {}", roomName, egressId);
            } else {
                log.warn("Failed to auto-start recording for room '{}'", roomName);
            }
        }
    }

    /**
     * When room finishes, stop recording and mark room as completed.
     */
    private void handleRoomFinished(String roomName) {
        log.info("Room finished: '{}'", roomName);

        // Stop active recording
        if (roomName != null && recordingService.isRecording(roomName)) {
            log.info("Stopping recording for finished room '{}'", roomName);
            recordingService.stopRecording(roomName);
        }

        // Mark room as COMPLETED in our system
        if (roomName != null) {
            try {
                RoomDTO roomDto = roomService.getRoomByLivekitName(roomName);
                if (roomDto != null) {
                    roomService.endRoom(roomDto.getId());
                    log.info("Marked room '{}' as COMPLETED via webhook", roomName);
                }
            } catch (Exception e) {
                log.warn("Could not mark room '{}' as COMPLETED: {}", roomName, e.getMessage());
            }
        }
    }
}

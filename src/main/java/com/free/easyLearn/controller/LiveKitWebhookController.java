package com.free.easyLearn.controller;

import com.free.easyLearn.service.LiveKitRecordingService;
import com.free.easyLearn.service.RoomService;
import com.free.easyLearn.dto.room.RoomDTO;
import io.livekit.server.WebhookReceiver;
import livekit.LivekitWebhook.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
                    handleRoomStarted(roomName);
                    break;

                case "room_finished":
                    handleRoomFinished(roomName);
                    break;

                // ─── Participant Events ───
                case "participant_joined":
                    log.info("Participant '{}' joined room '{}'",
                            event.hasParticipant() ? event.getParticipant().getIdentity() : "?",
                            roomName);
                    break;

                case "participant_left":
                    String leftIdentity = event.hasParticipant() ? event.getParticipant().getIdentity() : "?";
                    log.info("Participant '{}' left room '{}'", leftIdentity, roomName);
                    // If a real user left (not the recorder), check if only recorder remains
                    if (roomName != null && !leftIdentity.startsWith("egress-recorder-")) {
                        handleParticipantLeft(roomName);
                    }
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
            // Always return 200 to prevent LiveKit from retrying
            return ResponseEntity.ok("Error processed");
        }
    }

    /**
     * When room starts, auto-start recording.
     * room_started fires ONCE when the LiveKit room is created (first participant connects).
     */
    private void handleRoomStarted(String roomName) {
        log.info("Room started: '{}' — auto-starting recording", roomName);

        if (roomName == null) return;

        try {
            String egressId = recordingService.startRecording(roomName);
            if (egressId != null) {
                log.info("Recording started for room '{}', egressId: {}", roomName, egressId);
            } else {
                log.warn("Failed to start recording for room '{}'", roomName);
            }
        } catch (Exception e) {
            log.error("Exception starting recording for room '{}': {}", roomName, e.getMessage(), e);
        }
    }

    /**
     * When room finishes, explicitly stop WebEgress and mark room as COMPLETED.
     * WebEgress does NOT auto-stop when the room closes (it's a standalone Chrome session).
     * We must call stopRecording() which sends stopEgress() to LiveKit.
     * LiveKit will then finalize the file, upload to S3, and fire egress_ended webhook.
     */
    private void handleRoomFinished(String roomName) {
        log.info("Room finished: '{}'", roomName);

        if (roomName == null) return;

        // Explicitly stop the WebEgress recording
        try {
            recordingService.stopRecording(roomName);
        } catch (Exception e) {
            log.error("Error stopping recording for room '{}': {}", roomName, e.getMessage(), e);
        }

        // Mark room as COMPLETED (tolerant if already completed)
        try {
            RoomDTO roomDto = roomService.getRoomByLivekitName(roomName);
            if (roomDto != null) {
                roomService.endRoom(roomDto.getId());
                log.info("Marked room '{}' as COMPLETED via webhook", roomName);
            }
        } catch (Exception e) {
            // This is normal if room was already completed by leaveRoom or frontend
            log.info("Room '{}' status update skipped (likely already completed): {}", roomName, e.getMessage());
        }
    }

    /**
     * When a real participant leaves, check if only the recorder remains.
     * If so, stop the recording and destroy the LiveKit room.
     * Destroying the room kicks the recorder, triggers room_finished webhook,
     * which then marks the room as COMPLETED in the DB.
     */
    private void handleParticipantLeft(String roomName) {
        try {
            int realCount = recordingService.countRealParticipants(roomName);
            if (realCount == 0) {
                log.info("No real participants left in room '{}'. Stopping recording and destroying LiveKit room.", roomName);

                // 1) Stop the WebEgress recording first (finalize MP4, upload to MinIO)
                recordingService.stopRecording(roomName);

                // 2) Destroy the LiveKit room — this kicks the recorder and triggers room_finished
                recordingService.destroyLiveKitRoom(roomName);
            }
        } catch (Exception e) {
            log.error("Error handling participant_left for room '{}': {}", roomName, e.getMessage(), e);
        }
    }
}

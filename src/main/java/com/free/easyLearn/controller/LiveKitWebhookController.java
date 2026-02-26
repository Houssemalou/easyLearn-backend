package com.free.easyLearn.controller;

import com.free.easyLearn.service.LiveKitRecordingService;
import livekit.LivekitWebhook.WebhookEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/livekit/webhook")
public class LiveKitWebhookController {

    private static final Logger log = LoggerFactory.getLogger(LiveKitWebhookController.class);

    private final LiveKitRecordingService recordingService;
    private final Map<String, String> activeRecordings = new ConcurrentHashMap<>();

    public LiveKitWebhookController(LiveKitRecordingService service) {
        this.recordingService = service;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody WebhookEvent event) {

        String roomName = null;
        if (event != null && event.hasRoom()) {
            roomName = event.getRoom().getName();
        }

        // Write a simple hook record to the file named 'hooks' in project root for testing
        try {
            Path hooksFile = Paths.get(System.getProperty("user.dir"), "hooks");
            String entry = LocalDateTime.now().toString() + " - " + (roomName != null ? roomName : "<no-room>") + System.lineSeparator();
            Files.write(hooksFile, entry.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("Webhook received - room='{}' written to {}", roomName, hooksFile.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Failed to write webhook hook file: {}", e.getMessage());
        }

        if (roomName != null) {
            switch (event.getEvent()) {

                case "room_started":
                    if (!activeRecordings.containsKey(roomName)) {
                        String egressId = recordingService.startRecording(roomName);
                        activeRecordings.put(roomName, egressId);
                    }
                    break;

                case "room_finished":
                    if (activeRecordings.containsKey(roomName)) {
                        recordingService.stopRecording(activeRecordings.get(roomName));
                        activeRecordings.remove(roomName);
                    }
                    break;
            }
        } else {
            log.warn("Received webhook with no room information: event={}", event != null ? event.getEvent() : "null");
        }

        return ResponseEntity.ok().build();
    }
}

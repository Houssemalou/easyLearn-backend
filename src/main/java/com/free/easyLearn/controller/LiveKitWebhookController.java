package com.free.easyLearn.controller;

import com.free.easyLearn.service.LiveKitRecordingService;
import livekit.LivekitWebhook.WebhookEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/livekit/webhook")
public class LiveKitWebhookController {

    private final LiveKitRecordingService recordingService;
    private final Map<String, String> activeRecordings = new ConcurrentHashMap<>();

    public LiveKitWebhookController(LiveKitRecordingService service) {
        this.recordingService = service;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestBody WebhookEvent event) {

        String roomName = event.getRoom().getName();

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

        return ResponseEntity.ok().build();
    }
}


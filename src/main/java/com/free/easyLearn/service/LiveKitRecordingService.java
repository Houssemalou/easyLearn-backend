package com.free.easyLearn.service;

import io.livekit.server.EgressServiceClient;
import livekit.LivekitEgress;
import org.springframework.stereotype.Service;

@Service
public class LiveKitRecordingService {

    private final EgressServiceClient egressClient;

    public LiveKitRecordingService() {
        this.egressClient = EgressServiceClient.createClient(
                "http://localhost:7880",
                "devkey",
                "secret"
        );
    }

    public String startRecording(String roomName) {

        LivekitEgress.EncodedFileOutput encodedFileOutput = LivekitEgress.EncodedFileOutput.newBuilder()
                .setFileType(LivekitEgress.EncodedFileType.MP4)
                .setFilepath(roomName + ".mp4")
                .build();

        var response = egressClient.startRoomCompositeEgress(
                roomName,
                encodedFileOutput,
                "grid", // layout
                null, // optionsPreset
                null, // optionsAdvanced
                false, // audioOnly
                false // videoOnly
        );

        try {
            LivekitEgress.EgressInfo egressInfo = response.execute().body();
            if (egressInfo != null) {
                return egressInfo.getEgressId();
            } else {
                throw new RuntimeException("Failed to start recording: EgressInfo is null");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to start recording", e);
        }
    }

    public void stopRecording(String egressId) {
        egressClient.stopEgress(
                egressId
        );
    }
}

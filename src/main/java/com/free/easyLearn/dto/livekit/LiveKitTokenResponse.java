package com.free.easyLearn.dto.livekit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveKitTokenResponse {
    private String token;
    private String identity;
    private String roomName;
    private String serverUrl;
    private LocalDateTime expiresAt;
}
package com.free.easyLearn.config;

import io.livekit.server.RoomServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveKitConfig {

    @Value("${livekit.url:wss://easylearn-9lc2pn85.livekit.cloud}")
    private String livekitUrl;

    @Value("${livekit.api-key:APIEZCTfVKs3bGH}")
    private String apiKey;

    @Value("${livekit.api-secret:qzsYKTH3EkKjRflbDXjHpWxuQKsPLBXyuVfkgvKEkuD}")
    private String apiSecret;

    @Bean
    public RoomServiceClient roomServiceClient() {
        String serverUrl = livekitUrl.replace("wss://", "https://");
        return RoomServiceClient.create(serverUrl, apiKey, apiSecret);
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public String getLivekitUrl() {
        return livekitUrl;
    }
}

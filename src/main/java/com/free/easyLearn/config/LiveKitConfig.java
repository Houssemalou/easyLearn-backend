package com.free.easyLearn.config;

import io.livekit.server.EgressServiceClient;
import io.livekit.server.RoomServiceClient;
import io.livekit.server.WebhookReceiver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveKitConfig {

    @Value("${livekit.url}")
    private String livekitUrl;

    @Value("${livekit.api-key}")
    private String apiKey;

    @Value("${livekit.api-secret}")
    private String apiSecret;

    private String getHttpUrl() {
        return livekitUrl.replace("wss://", "https://").replace("ws://", "http://");
    }

    @Bean
    public RoomServiceClient roomServiceClient() {
        return RoomServiceClient.create(getHttpUrl(), apiKey, apiSecret);
    }

    @Bean
    public EgressServiceClient egressServiceClient() {
        return EgressServiceClient.createClient(getHttpUrl(), apiKey, apiSecret);
    }

    @Bean
    public WebhookReceiver webhookReceiver() {
        return new WebhookReceiver(apiKey, apiSecret);
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

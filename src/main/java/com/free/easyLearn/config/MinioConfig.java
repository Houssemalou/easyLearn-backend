package com.free.easyLearn.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO Configuration — reuses the same S3 credentials defined under livekit.s3.*
 */
@Configuration
public class MinioConfig {

    @Value("${livekit.s3.endpoint:http://91.134.137.202:9000}")
    private String minioUrl;

    @Value("${livekit.s3.access-key:minioadmin}")
    private String accessKey;

    @Value("${livekit.s3.secret-key:minioadmin}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }
}

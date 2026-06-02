package com.tomzxy.fbu_chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class MinioConfig {

    // Cú pháp ${TEN_BIEN:gia_tri_mac_dinh} giúp app không bị sập khi test local ngoài Docker
    @Value("${MINIO_ROOT_USER:minioadmin}")
    private String accessKey;

    @Value("${MINIO_ROOT_PASSWORD:minioadmin_password}")
    private String secretKey;

    // Định nghĩa luôn endpoint nội bộ trong mạng Docker (http://minio:9000) hoặc localhost khi test
    @Value("${MINIO_ENDPOINT:http://localhost:9000}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .region(Region.US_EAST_1) // MinIO yêu cầu một vùng giả định
                .forcePathStyle(true)     // Bắt buộc đối với MinIO để nhận diện đúng Bucket dạng path
                .build();
    }
}
package com.tomzxy.fbu_chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final S3Client s3Client;

    @Value("${MINIO_BUCKET_NAME:fbu-rag-assets}")
    private String bucketName;

    @Value("${MINIO_EXTERNAL_URL:http://localhost:9000}")
    private String externalUrl;

    @PostConstruct
    public void init() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("MinIO Bucket '{}' already exists.", bucketName);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("Successfully created MinIO Bucket '{}'.", bucketName);
            } else {
                log.error("Failed to check MinIO bucket status", e);
            }
        }

        allowPublicRead();
    }

    public String uploadFile(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String objectKey = folder + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            String baseUrl = externalUrl.endsWith("/") ? externalUrl.substring(0, externalUrl.length() - 1) : externalUrl;
            return String.format("%s/%s/%s", baseUrl, bucketName, objectKey);
        } catch (IOException e) {
            log.error("Failed to upload file to MinIO S3", e);
            throw new RuntimeException("Lỗi trong quá trình lưu trữ file hình ảnh.");
        }
    }

    public boolean objectExistsByUrl(String url) {
        String objectKey = extractObjectKey(url);
        if (objectKey == null || objectKey.isBlank()) {
            return false;
        }

        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            log.warn("Failed to verify MinIO object '{}': {}", objectKey, e.getMessage());
            return false;
        }
    }

    public void deleteObjectByUrl(String url) {
        String objectKey = extractObjectKey(url);
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
        } catch (S3Exception e) {
            log.warn("Failed to delete MinIO object '{}': {}", objectKey, e.getMessage());
        }
    }

    private void allowPublicRead() {
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucketName);

        try {
            s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                    .bucket(bucketName)
                    .policy(policy)
                    .build());
        } catch (S3Exception e) {
            log.warn("Failed to apply public read policy for MinIO bucket '{}': {}", bucketName, e.getMessage());
        }
    }

    private String extractObjectKey(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String marker = "/" + bucketName + "/";
        int markerIndex = url.indexOf(marker);
        if (markerIndex >= 0) {
            return url.substring(markerIndex + marker.length());
        }

        return url.startsWith(bucketName + "/")
                ? url.substring(bucketName.length() + 1)
                : url;
    }
}

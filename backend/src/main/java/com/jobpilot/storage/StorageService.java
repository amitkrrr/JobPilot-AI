package com.jobpilot.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final S3Client s3Client;
    private final String bucketName;

    public StorageService(S3Client s3Client, @Value("${aws.s3.bucket-name}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @PostConstruct
    public void initializeBucket() {
        try {
            try {
                // Check if the bucket already exists
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            } catch (S3Exception e) {
                // NoSuchBucket throws a 404/S3Exception, so we catch and create it automatically!
                try {
                    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                    log.info("Automated Storage Setup: S3 Bucket '{}' created successfully.", bucketName);
                } catch (Exception createException) {
                    log.error("Failed to automate S3 bucket creation: {}", createException.getMessage());
                }
            }
        } catch (Exception connException) {
            // Fault tolerance: If S3 is temporarily booting up, prevent application crash on startup
            log.warn("Could not connect to local S3 on startup. Will initialize on first upload request. Cause: {}", connException.getMessage());
        }
    }

    public void uploadFile(String key, byte[] bytes, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
    }

    public byte[] downloadFile(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);
        return responseBytes.asByteArray();
    }

    public void deleteFile(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }
}

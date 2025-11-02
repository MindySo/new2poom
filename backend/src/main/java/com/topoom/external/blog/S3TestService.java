package com.topoom.external.blog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3TestService {
    
    private final S3Client s3Client;
    
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;
    
    public String testS3Connection() {
        try {
            log.info("S3 연결 테스트 시작: bucket={}", bucketName);
            
            // 1. 버킷 존재 확인
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            s3Client.headBucket(headBucketRequest);
            log.info("✅ 버킷 접근 성공: {}", bucketName);
            
            // 2. 버킷 내용 조회
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .maxKeys(5)
                    .build();
            
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
            log.info("✅ 버킷 내용 조회 성공: {}개 객체 발견", listResponse.contents().size());
            
            // 3. 테스트 파일 업로드
            String testKey = "test/connection-test-" + System.currentTimeMillis() + ".txt";
            String testContent = "S3 연결 테스트 - " + java.time.LocalDateTime.now();
            
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(testKey)
                    .contentType("text/plain")
                    .build();
            
            s3Client.putObject(putRequest, RequestBody.fromString(testContent));
            log.info("✅ 테스트 파일 업로드 성공: {}", testKey);
            
            return String.format("S3 연결 테스트 성공!\n" +
                    "- 버킷: %s\n" +
                    "- 기존 객체 수: %d개\n" +
                    "- 테스트 파일: %s\n" +
                    "- 테스트 시간: %s", 
                    bucketName, 
                    listResponse.contents().size(),
                    testKey,
                    java.time.LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("❌ S3 연결 실패: {}", e.getMessage(), e);
            return String.format("S3 연결 실패: %s", e.getMessage());
        }
    }
}
package com.topoom.external.blog;

import com.topoom.missingcase.domain.CaseFile;
import com.topoom.missingcase.domain.MissingCase;
import com.topoom.missingcase.repository.CaseFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageDownloadService {

    private final S3Client s3Client;
    private final CaseFileRepository caseFileRepository;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * 이미지 URL 목록을 다운로드하고 S3에 업로드 후 CaseFile로 저장
     */
    @Transactional
    public List<CaseFile> downloadAndUploadImages(MissingCase missingCase, List<String> imageUrls) {
        List<CaseFile> caseFiles = new ArrayList<>();

        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = imageUrls.get(i);
            try {
                log.info("이미지 다운로드 시작: {}", imageUrl);

                // 1. 이미지 다운로드
                byte[] imageData = downloadImage(imageUrl);

                // 2. S3 키 생성
                String s3Key = generateS3Key(missingCase.getId(), i, imageUrl);

                // 3. S3 업로드
                uploadToS3(s3Key, imageData, getContentType(imageUrl));

                // 4. CaseFile 엔티티 생성 및 저장
                CaseFile caseFile = createCaseFile(missingCase, imageUrl, s3Key, imageData.length);
                caseFileRepository.save(caseFile);

                caseFiles.add(caseFile);

                log.info("이미지 저장 완료: {}", s3Key);

            } catch (Exception e) {
                log.error("이미지 처리 실패: {}", imageUrl, e);
                // 실패해도 다음 이미지 계속 처리
            }
        }

        return caseFiles;
    }

    /**
     * URL에서 이미지 다운로드
     */
    private byte[] downloadImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try (InputStream in = connection.getInputStream()) {
            return in.readAllBytes();
        }
    }

    /**
     * S3 키 생성 (missing_case_123/input/appearance/img_0.jpg)
     */
    private String generateS3Key(Long caseId, int index, String imageUrl) {
        String extension = getFileExtension(imageUrl);
        return String.format("missing_case_%d/input/appearance/img_%d.%s",
                caseId, index, extension);
    }

    /**
     * S3에 업로드
     */
    private void uploadToS3(String s3Key, byte[] data, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));
    }

    /**
     * CaseFile 엔티티 생성
     */
    private CaseFile createCaseFile(MissingCase missingCase, String sourceUrl, String s3Key, long fileSize) {
        return CaseFile.builder()
                .missingCase(missingCase)
                .ioRole("INPUT")
                .purpose("APPEARANCE")
                .contentKind("IMAGE")
                .s3Key(s3Key)
                .s3Bucket(bucketName)
                .contentType(getContentType(sourceUrl))
                .sizeBytes(fileSize)
                .sourceUrl(sourceUrl)
                .crawledAt(LocalDateTime.now())
                .build();
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String url) {
        String[] parts = url.split("\\.");
        if (parts.length > 0) {
            String ext = parts[parts.length - 1].toLowerCase();
            // 쿼리 파라미터 제거
            return ext.split("\\?")[0];
        }
        return "jpg";
    }

    /**
     * Content-Type 추출
     */
    private String getContentType(String url) {
        String extension = getFileExtension(url);
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }
}

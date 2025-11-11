package com.topoom.messaging.consumer;

import com.topoom.config.RabbitMQConfig;
import com.topoom.messaging.dto.ClassificationMessage;
import com.topoom.messaging.dto.ClassifiedImage;
import com.topoom.messaging.dto.CrawlingMessage;
import com.topoom.messaging.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Step 2: 이미지 분류 Consumer
 * - crawling-queue에서 메시지 소비
 * - 각 이미지를 서드파티 API로 분류 (FACE, BODY, TEXT_CAPTURE)
 * - classification-queue에 결과 발행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassificationConsumer {

    private final MessageProducer messageProducer;
    // TODO: ImageClassificationService 주입 필요

    @RabbitListener(queues = RabbitMQConfig.CLASSIFICATION_QUEUE, concurrency = "3-5")
    public void consumeClassification(ClassificationMessage message) {
        log.info("소비: classification-queue - requestId={}, blogUrl={}",
            message.getRequestId(), message.getBlogUrl());

        try {
            List<ClassifiedImage> classifiedImages = new ArrayList<>();

            // 각 이미지를 분류 (이미 ClassifiedImage 리스트가 있는 경우)
            if (message.getClassifiedImages() != null && !message.getClassifiedImages().isEmpty()) {
                for (ClassifiedImage img : message.getClassifiedImages()) {
                    try {
                        // 이미지 파일 읽기
                        byte[] imageData = Files.readAllBytes(Paths.get(img.getTempPath()));

                        // TODO: 서드파티 이미지 분류 API 호출
                        // ClassifiedImage.ImageType type = imageClassificationService.classify(imageData);

                        // 임시로 기존 타입 또는 TEXT_CAPTURE로 설정
                        ClassifiedImage.ImageType type = img.getType() != null
                            ? img.getType()
                            : ClassifiedImage.ImageType.TEXT_CAPTURE;

                        classifiedImages.add(ClassifiedImage.builder()
                            .type(type)
                            .tempPath(img.getTempPath())
                            .build());

                    } catch (Exception e) {
                        log.error("이미지 분류 실패: {}", img.getTempPath(), e);
                        throw new RuntimeException("이미지 분류 실패", e); // Retry 트리거
                    }
                }
            }

            // 분류 결과를 메시지에 업데이트
            message.setClassifiedImages(classifiedImages);

            // s3-upload-queue로 발행
            messageProducer.sendToS3UploadQueue(message);

            log.info("이미지 분류 완료: {} - 총 {} 건",
                message.getBlogUrl(), classifiedImages.size());

        } catch (Exception e) {
            log.error("ClassificationConsumer 처리 실패: {}", message.getRequestId(), e);
            throw e; // Retry 및 DLQ 처리
        }
    }
}

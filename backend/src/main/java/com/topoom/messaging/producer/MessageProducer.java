package com.topoom.messaging.producer;

import com.topoom.config.RabbitMQConfig;
import com.topoom.messaging.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 메시지 발행 공통 클래스
 * - RabbitTemplate을 사용하여 각 큐에 메시지 발행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * crawling-queue에 메시지 발행 (기존 - 이미지 크롤링용)
     */
    public void sendToCrawlingQueue(CrawlingMessage message) {
        log.info("발행: crawling-queue - requestId={}, blogUrl={}",
            message.getRequestId(), message.getBlogUrl());
        rabbitTemplate.convertAndSend(RabbitMQConfig.CRAWLING_QUEUE, message);
    }

    /**
     * crawling-queue에 블로그 게시글 크롤링 메시지 발행 (신규)
     */
    public void sendToBlogCrawlingQueue(BlogCrawlingMessage message) {
        log.info("발행: crawling-queue - requestId={}, postUrl={}",
            message.getRequestId(), message.getPostUrl());
        rabbitTemplate.convertAndSend(RabbitMQConfig.CRAWLING_QUEUE, message);
    }

    /**
     * classification-queue에 메시지 발행
     */
    public void sendToClassificationQueue(ClassificationMessage message) {
        log.info("발행: classification-queue - requestId={}, images={}",
            message.getRequestId(), message.getClassifiedImages().size());
        rabbitTemplate.convertAndSend(RabbitMQConfig.CLASSIFICATION_QUEUE, message);
    }

    /**
     * s3-upload-queue에 메시지 발행
     */
    public void sendToS3UploadQueue(ClassificationMessage message) {
        log.info("발행: s3-upload-queue - requestId={}, images={}",
            message.getRequestId(), message.getClassifiedImages().size());
        rabbitTemplate.convertAndSend(RabbitMQConfig.S3_UPLOAD_QUEUE, message);
    }

    /**
     * ocr-request-queue에 메시지 발행
     */
    public void sendToOcrQueue(OcrRequestMessage message) {
        log.info("발행: ocr-request-queue - requestId={}, s3Key={}",
            message.getRequestId(), message.getLastImageS3Key());
        rabbitTemplate.convertAndSend(RabbitMQConfig.OCR_REQUEST_QUEUE, message);
    }

    /**
     * finalize-queue에 메시지 발행
     */
    public void sendToFinalizeQueue(FinalizeMessage message) {
        log.info("발행: finalize-queue - requestId={}, ocrResult={}",
            message.getRequestId(), message.getOcrResult() != null ? "있음" : "없음");
        rabbitTemplate.convertAndSend(RabbitMQConfig.FINALIZE_QUEUE, message);
    }
}

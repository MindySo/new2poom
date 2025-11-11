package com.topoom.messaging.consumer;

import com.topoom.config.RabbitMQConfig;
import com.topoom.external.blog.service.IntegratedBlogCrawlingService;
import com.topoom.messaging.dto.BlogCrawlingMessage;
import com.topoom.messaging.dto.ClassificationMessage;
import com.topoom.messaging.dto.ClassifiedImage;
import com.topoom.messaging.dto.ContactInfo;
import com.topoom.messaging.producer.MessageProducer;
import com.topoom.missingcase.entity.CaseContact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 블로그 게시글 크롤링 Consumer
 * - crawling-queue에서 메시지 소비
 * - 개별 게시글의 이미지 + 연락처 크롤링
 * - classification-queue로 결과 발행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BlogCrawlingConsumer {

    private final IntegratedBlogCrawlingService integratedBlogCrawlingService;
    private final MessageProducer messageProducer;

    @RabbitListener(queues = RabbitMQConfig.CRAWLING_QUEUE, concurrency = "5-10")
    public void consumeBlogCrawling(BlogCrawlingMessage message) {
        log.info("소비: crawling-queue - requestId={}, postUrl={}",
            message.getRequestId(), message.getPostUrl());

        try {
            // 이미지 + 연락처 크롤링 (기존 로직 재사용)
            // 참고: caseId는 나중에 DB 저장 시 생성되므로, 여기서는 null 전달
            Map<String, Object> result = integratedBlogCrawlingService
                .extractAndUploadImagesWithContacts(message.getPostUrl(), null);

            @SuppressWarnings("unchecked")
            List<CaseContact> contacts = (List<CaseContact>) result.get("contacts");

            // TODO: 이미지 분류 로직 추가 필요
            // 현재는 임시로 빈 리스트 생성
            List<ClassifiedImage> classifiedImages = new ArrayList<>();

            // 연락처 정보 변환
            List<ContactInfo> contactInfos = contacts.stream()
                .map(contact -> ContactInfo.builder()
                    .organization(contact.getOrganization())
                    .phoneNumber(contact.getPhoneNumber())
                    .build())
                .collect(Collectors.toList());

            // classification-queue로 발행
            ClassificationMessage classificationMsg = ClassificationMessage.builder()
                .requestId(message.getRequestId())
                .blogUrl(message.getPostUrl())
                .title(message.getTitle())
                .text("") // TODO: 블로그 본문 추출 필요
                .classifiedImages(classifiedImages)
                .contacts(contactInfos)
                .build();

            messageProducer.sendToClassificationQueue(classificationMsg);

            log.info("블로그 크롤링 완료: {} - contacts={}",
                message.getPostUrl(), contactInfos.size());

        } catch (Exception e) {
            log.error("BlogCrawlingConsumer 처리 실패: {}", message.getRequestId(), e);
            throw e; // Retry 및 DLQ 처리
        }
    }
}

package com.topoom.messaging.consumer;

import com.topoom.config.RabbitMQConfig;
import com.topoom.messaging.dto.FinalizeMessage;
import com.topoom.messaging.dto.OcrRequestMessage;
import com.topoom.messaging.exception.OcrResultInvalidException;
import com.topoom.messaging.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OCR 처리 Consumer (핵심!)
 * - ocr-request-queue에서 메시지 소비
 * - OCR API 호출 및 결과 검증
 * - 검증 실패 시 재시도 (RabbitMQ Retry 활용)
 * - 성공 시 finalize-queue로 전달
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OcrConsumer {

    private final MessageProducer messageProducer;

    @Qualifier("ocrWebClient")
    private final WebClient ocrWebClient;

    // 정규식 패턴들 (CaseOcrService와 동일)
    private static final Pattern FIRST_LINE_PATTERN = Pattern.compile(
        "^\\s*([가-힣]+)[:：]?\\s+([가-힣]{2,4})\\s*\\(\\s*(\\d{1,3})\\s*세\\s*\\)\\s*(남|여|남자|여자|남성|여성)",
        Pattern.MULTILINE
    );
    private static final Pattern NAME_PATTERN = Pattern.compile("(?:성명|이름)\\s*[:：]?\\s*([가-힣]{2,4})");
    private static final Pattern AGE_PATTERN = Pattern.compile("(?:나이|연령|당시나이|당시\\s*나이)\\s*[:：]?\\s*(\\d{1,3})");
    private static final Pattern GENDER_PATTERN = Pattern.compile("(?:성별)\\s*[:：]?\\s*(남|여|남성|여성)");
    private static final Pattern OCCURRED_DATE_PATTERN = Pattern.compile("(?:발생일시|실종일시)\\s*[:：]?\\s*(\\d{4})년\\s*(\\d{1,2})월\\s*(\\d{1,2})일");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("(?:실종장소|발생장소|장소)\\s*[:：]?\\s*([^\\n]+?)(?=\\s*키|\\s*신장|\\s*$)");
    private static final Pattern HEIGHT_PATTERN = Pattern.compile("(?:신장|키)\\s*[:：]?\\s*(\\d{2,3})\\s*cm");
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(?:체중|몸무게)\\s*[:：]?\\s*(\\d{2,3})\\s*kg");
    private static final Pattern BODY_TYPE_PATTERN = Pattern.compile("(?:체격|체형)\\s*[:：]?\\s*([가-힣()（）]+)");
    private static final Pattern FACE_SHAPE_PATTERN = Pattern.compile("(?:얼굴형|얼굴)\\s*[:：]?\\s*([가-힣()（）]+)");
    private static final Pattern HAIR_COLOR_PATTERN = Pattern.compile("(?:두발색상|머리색|머리카락색)\\s*[:：]?\\s*([가-힣()（）]+)");
    private static final Pattern HAIR_STYLE_PATTERN = Pattern.compile("(?:두발형태|머리형태|헤어스타일)\\s*[:：]?\\s*([가-힣()（）]+)");
    private static final Pattern CLOTHING_PATTERN = Pattern.compile("(?:착의의상|착의사항|착의|옷차림|의상)\\s*[:：]?\\s*([^\\n]+?)(?=\\s*(?:진행상태|특이사항|특징|기타특징)|\\s*$)");
    private static final Pattern FEATURES_PATTERN = Pattern.compile("(?:특이사항|특징|기타특징)\\s*[:：]?\\s*([^\\n]+)");
    private static final Pattern PROGRESS_STATUS_PATTERN = Pattern.compile("(?:진행상태|상태)\\s*[:：]?\\s*([가-힣]+)");

    @RabbitListener(queues = RabbitMQConfig.OCR_REQUEST_QUEUE, concurrency = "2-3")
    public void consumeOcrRequest(OcrRequestMessage message) {
        log.info("OCR 처리 시작 (재시도 {}회): requestId={}, s3Key={}",
            message.getRetryCount(), message.getRequestId(), message.getLastImageS3Key());

        try {
            // 1. OCR API 호출
            String ocrResult = callOcrApi(message.getLastImageS3Key()).block();

            // 2. 결과 검증 (핵심!)
            if (!isValidOcrResult(ocrResult)) {
                message.setRetryCount(message.getRetryCount() + 1);
                throw new OcrResultInvalidException(
                    String.format("OCR 결과 유효하지 않음 (재시도 %d회): null 또는 빈 값 또는 필수 정보 없음",
                        message.getRetryCount())
                );
            }

            log.info("✅ OCR 결과 검증 성공: requestId={}, 결과 길이={}",
                message.getRequestId(), ocrResult.length());

            // 3. OCR 결과 파싱
            Map<String, Object> parsedData = parseOcrResult(ocrResult);

            // 4. Finalize 큐로 발행 (DB 저장 트리거)
            FinalizeMessage finalizeMsg = FinalizeMessage.builder()
                .requestId(message.getRequestId())
                .blogUrl(message.getPostUrl())
                .title(message.getTitle())
                .text(message.getText())
                .uploadedImages(message.getUploadedImages())
                .contacts(message.getContacts())
                .ocrResult(ocrResult)
                .parsedOcrData(parsedData)
                .build();

            messageProducer.sendToFinalizeQueue(finalizeMsg);

            log.info("✅ OCR 처리 완료, Finalize 큐로 발행: requestId={}", message.getRequestId());

        } catch (OcrResultInvalidException e) {
            // OCR 결과 검증 실패 → 재시도
            log.error("❌ OCR 결과 검증 실패 (재시도 {}회): requestId={}",
                message.getRetryCount(), message.getRequestId(), e);
            throw e; // → RabbitMQ Retry (3번까지)

        } catch (Exception e) {
            log.error("❌ OCR 처리 중 오류: requestId={}", message.getRequestId(), e);
            throw e; // → RabbitMQ Retry
        }
    }

    /**
     * OCR API 호출
     */
    private Mono<String> callOcrApi(String s3Key) {
        if (s3Key == null || s3Key.trim().isEmpty()) {
            log.warn("S3 키가 없음, OCR 스킵");
            return Mono.just("");
        }

        Map<String, String> request = Map.of("s3Key", s3Key);

        return ocrWebClient.post()
            .uri("/s3-direct")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                Boolean success = (Boolean) response.get("success");
                if (Boolean.TRUE.equals(success)) {
                    return (String) response.get("extractedText");
                } else {
                    throw new RuntimeException("OCR API 호출 실패: " + response.get("error"));
                }
            })
            .timeout(Duration.ofSeconds(30))
            .doOnSuccess(result -> log.info("OCR API 호출 성공: s3Key={}, 결과 길이={}",
                s3Key, result != null ? result.length() : 0))
            .doOnError(error -> log.error("OCR API 호출 실패: s3Key={}", s3Key, error));
    }

    /**
     * OCR 결과 검증
     */
    private boolean isValidOcrResult(String ocrResult) {
        // 1차: null 또는 빈 문자열
        if (ocrResult == null || ocrResult.trim().isEmpty()) {
            log.warn("OCR 결과가 null 또는 빈 값");
            return false;
        }

        // 2차: 최소 길이 검증 (너무 짧으면 의미 없음)
        if (ocrResult.length() < 20) {
            log.warn("OCR 결과가 너무 짧음: length={}", ocrResult.length());
            return false;
        }

        // 3차: 필수 패턴 존재 여부
        // 실종자 정보에 최소한 하나의 필드는 있어야 함
        boolean hasEssentialInfo = ocrResult.matches(
            ".*(?:성명|이름|나이|연령|당시나이|성별|남|여|발생일시|실종일시|실종장소).*"
        );

        if (!hasEssentialInfo) {
            log.warn("OCR 결과에 필수 정보 없음");
            return false;
        }

        log.info("OCR 결과 검증 통과: length={}", ocrResult.length());
        return true;
    }

    /**
     * OCR 결과 파싱
     */
    private Map<String, Object> parseOcrResult(String ocrText) {
        Map<String, Object> parsed = new HashMap<>();

        try {
            // 첫 줄 통합 패턴 처리
            Matcher firstLineMatcher = FIRST_LINE_PATTERN.matcher(ocrText);
            if (firstLineMatcher.find()) {
                parsed.put("targetType", firstLineMatcher.group(1));
                parsed.put("personName", firstLineMatcher.group(2));
                parsed.put("age", Integer.parseInt(firstLineMatcher.group(3)));
                parsed.put("gender", normalizeGender(firstLineMatcher.group(4)));
            }

            // 개별 필드 파싱 (fallback)
            Matcher nameMatcher = NAME_PATTERN.matcher(ocrText);
            if (nameMatcher.find() && !parsed.containsKey("personName")) {
                parsed.put("personName", nameMatcher.group(1));
            }

            Matcher ageMatcher = AGE_PATTERN.matcher(ocrText);
            if (ageMatcher.find() && !parsed.containsKey("age")) {
                parsed.put("age", Integer.parseInt(ageMatcher.group(1)));
            }

            Matcher genderMatcher = GENDER_PATTERN.matcher(ocrText);
            if (genderMatcher.find() && !parsed.containsKey("gender")) {
                parsed.put("gender", normalizeGender(genderMatcher.group(1)));
            }

            // 발생일시
            Matcher occurredDateMatcher = OCCURRED_DATE_PATTERN.matcher(ocrText);
            if (occurredDateMatcher.find()) {
                // LocalDateTime은 Serializable이 아니므로 문자열로 저장
                String dateStr = String.format("%s-%s-%s",
                    occurredDateMatcher.group(1),
                    occurredDateMatcher.group(2),
                    occurredDateMatcher.group(3)
                );
                parsed.put("occurredAt", dateStr);
            }

            // 실종장소
            Matcher locationMatcher = LOCATION_PATTERN.matcher(ocrText);
            if (locationMatcher.find()) {
                parsed.put("occurredLocation", locationMatcher.group(1).trim());
            }

            // 신장
            Matcher heightMatcher = HEIGHT_PATTERN.matcher(ocrText);
            if (heightMatcher.find()) {
                parsed.put("heightCm", Integer.parseInt(heightMatcher.group(1)));
            }

            // 체중
            Matcher weightMatcher = WEIGHT_PATTERN.matcher(ocrText);
            if (weightMatcher.find()) {
                parsed.put("weightKg", Integer.parseInt(weightMatcher.group(1)));
            }

            // 체격
            Matcher bodyTypeMatcher = BODY_TYPE_PATTERN.matcher(ocrText);
            if (bodyTypeMatcher.find()) {
                parsed.put("bodyType", bodyTypeMatcher.group(1).trim());
            }

            // 얼굴형
            Matcher faceShapeMatcher = FACE_SHAPE_PATTERN.matcher(ocrText);
            if (faceShapeMatcher.find()) {
                parsed.put("faceShape", faceShapeMatcher.group(1).trim());
            }

            // 두발색상
            Matcher hairColorMatcher = HAIR_COLOR_PATTERN.matcher(ocrText);
            if (hairColorMatcher.find()) {
                parsed.put("hairColor", hairColorMatcher.group(1).trim());
            }

            // 두발형태
            Matcher hairStyleMatcher = HAIR_STYLE_PATTERN.matcher(ocrText);
            if (hairStyleMatcher.find()) {
                parsed.put("hairStyle", hairStyleMatcher.group(1).trim());
            }

            // 착의의상
            Matcher clothingMatcher = CLOTHING_PATTERN.matcher(ocrText);
            if (clothingMatcher.find()) {
                String clothing = clothingMatcher.group(1).trim();
                if (clothing.length() >= 2 && !clothing.matches("^(착의|의상|착의의상|착의사항)$")) {
                    parsed.put("clothingDesc", clothing);
                }
            }

            // 특이사항
            Matcher featuresMatcher = FEATURES_PATTERN.matcher(ocrText);
            if (featuresMatcher.find()) {
                parsed.put("etcFeatures", featuresMatcher.group(1).trim());
            }

            // 진행상태
            Matcher progressStatusMatcher = PROGRESS_STATUS_PATTERN.matcher(ocrText);
            if (progressStatusMatcher.find()) {
                parsed.put("progressStatus", progressStatusMatcher.group(1).trim());
            }

            // 기본값 설정
            parsed.putIfAbsent("targetType", "실종자");
            parsed.putIfAbsent("progressStatus", "신고");

            log.info("OCR 파싱 완료: 필드 수={}, 포함된 필드={}",
                parsed.size(), parsed.keySet());

        } catch (Exception e) {
            log.error("OCR 파싱 중 오류", e);
        }

        return parsed;
    }

    private String normalizeGender(String gender) {
        if (gender.contains("남")) return "남성";
        if (gender.contains("여")) return "여성";
        return gender;
    }
}

package com.topoom.external.scheduler;

import com.topoom.external.blog.BlogCrawler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataUpdateScheduler {

    private final BlogCrawler blogCrawler;

    private static final String SAFE182_BLOG_URL = "https://m.blog.naver.com/safe182pol";

    /**
     * 15분마다 블로그 크롤링 실행
     */
    @Scheduled(cron = "0 */15 * * * *")  // 매 15분마다 (0분, 15분, 30분, 45분)
    public void scheduleRegularCrawling() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("===============================================");
        log.info("⏰ 정기 크롤링 시작 (15분마다): {}", timestamp);
        log.info("===============================================");

        try {
            blogCrawler.crawlBlogMain(SAFE182_BLOG_URL);
            log.info("✅ 정기 크롤링 완료: {}", timestamp);
        } catch (Exception e) {
            log.error("❌ 정기 크롤링 실패: {}", timestamp, e);
        }
    }

    /**
     * 매일 새벽 2시에 전체 크롤링 실행 (대량 처리용)
     */
    @Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
    public void scheduleDailyCrawling() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        log.info("===============================================");
        log.info("🌙 일일 전체 크롤링 시작: {}", timestamp);
        log.info("===============================================");

        try {
            blogCrawler.crawlBlogMain(SAFE182_BLOG_URL);
            log.info("✅ 일일 전체 크롤링 완료: {}", timestamp);
        } catch (Exception e) {
            log.error("❌ 일일 전체 크롤링 실패: {}", timestamp, e);
        }
    }
}

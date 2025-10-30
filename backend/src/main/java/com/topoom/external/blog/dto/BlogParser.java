package com.topoom.external.blog;

import com.topoom.external.blog.dto.CrawledData;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BlogParser {

    /**
     * 게시글 HTML에서 이미지 URL 추출
     */
    public CrawledData parseBlogData(String html, String sourceUrl) {
        try {
            Document doc = Jsoup.parse(html);

            // 게시글 제목 추출
            String title = extractTitle(doc);

            // 이미지 URL 추출
            List<String> imageUrls = extractImageUrls(doc);

            log.info("파싱 완료 - 제목: {}, 이미지 수: {}", title, imageUrls.size());

            return CrawledData.builder()
                    .sourceUrl(sourceUrl)
                    .sourceTitle(title)
                    .crawledAt(LocalDateTime.now())
                    .imageUrls(imageUrls)
                    .build();

        } catch (Exception e) {
            log.error("게시글 파싱 실패: {}", sourceUrl, e);
            throw new RuntimeException("파싱 실패", e);
        }
    }

    /**
     * 제목 추출 (실제 HTML 구조에 맞게 수정 필요)
     */
    private String extractTitle(Document doc) {
        // TODO: 실제 CSS Selector로 수정
        Elements titleElements = doc.select("h1, h2, .title, .post-title");
        return titleElements.isEmpty() ? "제목 없음" : titleElements.first().text();
    }

    /**
     * 이미지 URL 추출
     */
    private List<String> extractImageUrls(Document doc) {
        Elements images = doc.select("img");

        return images.stream()
                .map(img -> img.attr("abs:src"))  // 절대 URL로 변환
                .filter(url -> !url.isEmpty())
                .filter(url -> !url.contains("emoticon"))  // 이모티콘 제외
                .filter(url -> !url.contains("icon"))      // 아이콘 제외
                .distinct()
                .collect(Collectors.toList());
    }
}


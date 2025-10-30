package com.topoom.external.blog;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BlogClient {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final int TIMEOUT = 10000;

    /**
     * 블로그 메인 페이지에서 게시글 URL 목록 추출
     */
    public List<String> fetchPostUrls(String blogMainUrl) {
        try {
            log.info("블로그 메인 페이지 접근: {}", blogMainUrl);

            Document doc = Jsoup.connect(blogMainUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .get();

            // TODO: 실제 HTML 구조에 맞게 CSS Selector 수정 필요
            // 예시: 게시글 링크가 <a class="post-link"> 형태라면
            Elements postLinks = doc.select("a[href*='/PostView']");

            List<String> urls = postLinks.stream()
                    .map(link -> link.attr("abs:href"))
                    .filter(url -> !url.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

            log.info("발견된 게시글 URL 수: {}", urls.size());
            return urls;

        } catch (IOException e) {
            log.error("블로그 메인 페이지 접근 실패: {}", blogMainUrl, e);
            return Collections.emptyList();
        }
    }

    /**
     * 게시글 상세 페이지 HTML 가져오기
     */
    public String fetchPostContent(String postUrl) {
        try {
            log.info("게시글 크롤링: {}", postUrl);

            Document doc = Jsoup.connect(postUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT)
                    .get();

            // 네이버 블로그 모바일 버전은 iframe 없이 바로 접근 가능
            return doc.html();

        } catch (IOException e) {
            log.error("게시글 접근 실패: {}", postUrl, e);
            throw new RuntimeException("게시글 크롤링 실패: " + postUrl, e);
        }
    }
}

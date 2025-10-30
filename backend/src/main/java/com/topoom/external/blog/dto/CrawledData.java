package com.topoom.external.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrawledData {

    // 게시글 정보
    private String sourceUrl;
    private String sourceTitle;
    private LocalDateTime crawledAt;

    // 크롤링한 이미지 URL 목록
    private List<String> imageUrls;
}

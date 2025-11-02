package com.topoom.external.blog.service;

import com.topoom.external.blog.dto.BlogPostInfo;
import com.topoom.external.blog.dto.ExtractedImageInfo;
import com.topoom.external.blog.entity.BlogPost;
import com.topoom.external.blog.repository.BlogPostRepository;
import com.topoom.missingcase.domain.CaseContact;
import com.topoom.missingcase.domain.CaseFile;
import com.topoom.missingcase.repository.CaseContactRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegratedBlogCrawlingService {

    private final S3ImageUploadService s3ImageUploadService;
    private final CaseContactRepository caseContactRepository;
    private final BlogPostRepository blogPostRepository;
    
    private static final int WAIT_TIMEOUT_SECONDS = 10;
    private static final int MAX_PAGES = 50;
    
    // 연락처 정규식 패턴
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(\\d{2,3}[-\\s\\.\\u2010-\\u2015]*\\d{3,4}[-\\s\\.\\u2010-\\u2015]*\\d{4})"
    );
    
    private static final Pattern ORGANIZATION_PATTERN = Pattern.compile(
        "([가-힣]+(?:경찰서|서)|[가-힣]*실종수사팀|[가-힣]*수사팀)"
    );

    /**
     * 전체 크롤링 프로세스 (블로그 목록 → 개별 게시글 처리)
     */
    public Map<String, Object> crawlAndProcessAllPosts(String blogId, String categoryNo) {
        log.info("전체 크롤링 프로세스 시작: blogId={}, categoryNo={}", blogId, categoryNo);
        
        WebDriver driver = null;
        try {
            driver = createChromeDriver();
            
            // 1. 블로그 목록 크롤링
            List<BlogPostInfo> blogPosts = crawlBlogPostList(driver, blogId, categoryNo);
            log.info("블로그 목록 크롤링 완료: {}개 게시글 발견", blogPosts.size());
            
            // 2. 블로그 게시글 정보를 DB에 저장
            List<BlogPost> savedPosts = saveBlogPostsToDatabase(blogPosts);
            log.info("블로그 게시글 DB 저장 완료: {}개 저장", savedPosts.size());
            
            // 3. 각 게시글에서 이미지 및 연락처 추출 (옵션)
            // 실제 운영에서는 필요시에만 실행하도록 분리 가능
            
            Map<String, Object> result = new HashMap<>();
            result.put("blogPosts", blogPosts);
            result.put("savedPosts", savedPosts);
            result.put("totalCount", blogPosts.size());
            result.put("savedCount", savedPosts.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("전체 크롤링 프로세스 실패: blogId={}, categoryNo={}", blogId, categoryNo, e);
            throw new RuntimeException("전체 크롤링 프로세스 실패", e);
        } finally {
            if (driver != null) {
                driver.quit();
                log.info("WebDriver 종료");
            }
        }
    }

    /**
     * 블로그 목록만 크롤링 (기존 SeleniumBlogCrawlingService 로직)
     */
    public List<BlogPostInfo> crawlCategoryPostsWithSelenium(String blogId, String categoryNo) {
        String categoryUrl = String.format(
            "https://blog.naver.com/PostList.naver?blogId=%s&categoryNo=%s", 
            blogId, categoryNo
        );
        
        log.info("Selenium 블로그 크롤링 시작: {}", categoryUrl);
        
        WebDriver driver = null;
        try {
            driver = createChromeDriver();
            driver.get(categoryUrl);
            log.info("페이지 로드 완료");
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));
            
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("postBottomTitleListBody")));
                log.info("postBottomTitleListBody 로딩 완료");
            } catch (Exception e) {
                log.warn("postBottomTitleListBody 로딩 실패, 다른 요소 시도");
            }
            
            Thread.sleep(3000);
            
            List<BlogPostInfo> blogPosts = crawlBlogPostList(driver, blogId, categoryNo);
            
            // 블로그 게시글 정보를 DB에 저장
            List<BlogPost> savedPosts = saveBlogPostsToDatabase(blogPosts);
            log.info("블로그 게시글 DB 저장 완료: {}개 저장", savedPosts.size());
            
            return blogPosts;
            
        } catch (Exception e) {
            log.error("Selenium 크롤링 실패: {}", categoryUrl, e);
            throw new RuntimeException("Selenium 크롤링 실패", e);
        } finally {
            if (driver != null) {
                driver.quit();
                log.info("WebDriver 종료");
            }
        }
    }

    /**
     * 개별 게시글 이미지 및 연락처 추출 (기존 BlogImageProcessingService 로직)
     */
    public Map<String, Object> extractAndUploadImagesWithContacts(String postUrl, Long caseId) {
        log.info("블로그 게시글 이미지 및 연락처 추출 시작: {}", postUrl);
        
        WebDriver driver = null;
        try {
            driver = createChromeDriver();
            driver.get(postUrl);
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIMEOUT_SECONDS));
            
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.className("se-main-container")));
                log.info("블로그 게시글 콘텐츠 로딩 완료");
            } catch (Exception e) {
                log.warn("se-main-container 로딩 실패, 다른 요소 시도");
            }
            
            Thread.sleep(3000);
            
            // 이미지 추출 및 업로드
            List<ExtractedImageInfo> extractedImages = extractImagesFromWebDriver(driver, postUrl);
            log.info("총 {}개 이미지 추출 완료", extractedImages.size());
            
            List<CaseFile> uploadedFiles = new ArrayList<>();
            int imageSuccessCount = 0;
            int imageFailCount = 0;
            
            for (ExtractedImageInfo imageInfo : extractedImages) {
                try {
                    CaseFile caseFile = s3ImageUploadService.downloadAndUploadImage(
                            imageInfo.getImageUrl(), 
                            postUrl, 
                            caseId
                    );
                    uploadedFiles.add(caseFile);
                    imageSuccessCount++;
                    
                    log.info("이미지 업로드 성공 ({}/{}): {}", 
                            imageSuccessCount, extractedImages.size(), imageInfo.getImageUrl());
                    
                } catch (Exception e) {
                    imageFailCount++;
                    log.error("이미지 업로드 실패 ({}/{}): {} - {}", 
                            imageFailCount, extractedImages.size(), imageInfo.getImageUrl(), e.getMessage());
                }
            }
            
            // 연락처 추출 및 저장 (같은 WebDriver 사용)
            List<CaseContact> contacts = new ArrayList<>();
            try {
                contacts = extractAndSaveContacts(driver, postUrl, caseId);
                log.info("연락처 추출 완료: {}개", contacts.size());
            } catch (Exception e) {
                log.error("연락처 추출 실패: {}", postUrl, e);
            }
            
            log.info("전체 처리 완료 - 이미지: 성공 {}개, 실패 {}개 | 연락처: {}개", 
                    imageSuccessCount, imageFailCount, contacts.size());
            
            Map<String, Object> result = new HashMap<>();
            result.put("images", uploadedFiles);
            result.put("contacts", contacts);
            result.put("imageStats", Map.of("success", imageSuccessCount, "fail", imageFailCount));
            result.put("contactCount", contacts.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("블로그 이미지 및 연락처 처리 실패: {}", postUrl, e);
            throw new RuntimeException("블로그 이미지 및 연락처 처리 실패", e);
        } finally {
            if (driver != null) {
                driver.quit();
                log.info("WebDriver 종료");
            }
        }
    }

    /**
     * 이미지만 추출 및 업로드 (호환성 메서드)
     */
    public List<CaseFile> extractAndUploadImages(String postUrl, Long caseId) {
        Map<String, Object> result = extractAndUploadImagesWithContacts(postUrl, caseId);
        return (List<CaseFile>) result.get("images");
    }

    // ===== 내부 메서드들 =====

    /**
     * Chrome WebDriver 생성
     */
    private WebDriver createChromeDriver() {
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        
        return new ChromeDriver(options);
    }

    /**
     * 블로그 목록 크롤링 (WebDriver 재사용)
     */
    private List<BlogPostInfo> crawlBlogPostList(WebDriver driver, String blogId, String categoryNo) {
        List<BlogPostInfo> allPosts = new ArrayList<>();
        int currentPage = 1;
        
        log.info("WebDriver에서 모든 페이지 게시글 정보 추출 시작");
        
        while (currentPage <= MAX_PAGES) {
            log.info("현재 페이지 {}: 게시글 추출 중...", currentPage);
            
            List<BlogPostInfo> currentPagePosts = extractCurrentPagePosts(driver, categoryNo);
            
            if (currentPagePosts.isEmpty()) {
                log.info("페이지 {}에서 게시글을 찾을 수 없습니다. 크롤링 종료.", currentPage);
                break;
            }
            
            allPosts.addAll(currentPagePosts);
            log.info("페이지 {}에서 {}개 게시글 추출 완료", currentPage, currentPagePosts.size());
            
            boolean hasNextPage = clickNextPage(driver);
            if (!hasNextPage) {
                log.info("다음 페이지가 없습니다. 크롤링 완료.");
                break;
            }
            
            currentPage++;
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        log.info("Selenium으로 총 {}개 페이지에서 {}개의 게시글을 크롤링했습니다.", currentPage, allPosts.size());
        return allPosts;
    }

    /**
     * 현재 페이지에서 게시글 추출
     */
    private List<BlogPostInfo> extractCurrentPagePosts(WebDriver driver, String categoryNo) {
        List<BlogPostInfo> posts = new ArrayList<>();
        
        try {
            WebElement postBottomElement = driver.findElement(By.id("postBottomTitleListBody"));
            List<WebElement> rows = postBottomElement.findElements(By.tagName("tr"));
            
            for (WebElement row : rows) {
                BlogPostInfo postInfo = extractFromWebElement(row, categoryNo);
                if (postInfo != null) {
                    posts.add(postInfo);
                }
            }
        } catch (Exception e) {
            try {
                List<WebElement> allRows = driver.findElements(By.cssSelector("tbody tr"));
                
                for (WebElement row : allRows) {
                    BlogPostInfo postInfo = extractFromWebElement(row, categoryNo);
                    if (postInfo != null) {
                        posts.add(postInfo);
                    }
                }
            } catch (Exception e2) {
                log.warn("현재 페이지에서 게시글 추출 실패: {}", e2.getMessage());
            }
        }
        
        return posts;
    }

    /**
     * 다음 페이지 버튼 클릭
     */
    private boolean clickNextPage(WebDriver driver) {
        try {
            log.info("다음 페이지 버튼 탐색 시작...");
            
            String[] nextButtonSelectors = {
                "a.next.pcol2",
                "a.next",
                "a[onclick*='_next_category_param']",
                "a[title='다음 페이지로 이동']",
                "a.btn_next",
                "a[class*='next']",
                "a[title*='다음']",
                ".paging a:last-child"
            };
            
            for (String selector : nextButtonSelectors) {
                try {
                    List<WebElement> buttons = driver.findElements(By.cssSelector(selector));
                    log.info("선택자 '{}': {}개 버튼 발견", selector, buttons.size());
                    
                    for (WebElement button : buttons) {
                        String className = button.getAttribute("class");
                        String onclick = button.getAttribute("onclick");
                        String title = button.getAttribute("title");
                        String text = button.getText().trim();
                        
                        log.info("버튼 발견 - 클래스: {}, onclick: {}, title: {}, text: {}", 
                                className, onclick, title, text);
                        
                        if (className != null && (className.contains("disabled") || className.contains("off"))) {
                            log.info("버튼이 비활성화됨: {}", className);
                            return false;
                        }
                        
                        if (button.isEnabled() && button.isDisplayed()) {
                            log.info("다음 페이지 버튼 클릭 시도: {}", selector);
                            button.click();
                            
                            Thread.sleep(3000);
                            
                            try {
                                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
                                wait.until(ExpectedConditions.presenceOfElementLocated(By.id("postBottomTitleListBody")));
                                log.info("다음 페이지 로딩 완료");
                                return true;
                            } catch (Exception e) {
                                log.warn("페이지 로딩 확인 실패, 계속 진행: {}", e.getMessage());
                                return true;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("선택자 '{}' 시도 실패: {}", selector, e.getMessage());
                }
            }
            
            log.info("다음 페이지 버튼을 찾을 수 없습니다. 마지막 페이지일 수 있습니다.");
            return false;
            
        } catch (Exception e) {
            log.warn("다음 페이지 클릭 중 오류: {}", e.getMessage());
            return false;
        }
    }

    /**
     * WebElement에서 게시글 정보 추출
     */
    private BlogPostInfo extractFromWebElement(WebElement row, String categoryNo) {
        try {
            WebElement titleLink = null;
            try {
                titleLink = row.findElement(By.cssSelector("td.title a"));
            } catch (Exception e) {
                try {
                    titleLink = row.findElement(By.cssSelector(".title a"));
                } catch (Exception e2) {
                    try {
                        titleLink = row.findElement(By.cssSelector("a[href*='PostView']"));
                    } catch (Exception e3) {
                        return null;
                    }
                }
            }
            
            WebElement timeElement = null;
            try {
                timeElement = row.findElement(By.cssSelector("td.date span.date"));
            } catch (Exception e) {
                try {
                    timeElement = row.findElement(By.cssSelector(".date"));
                } catch (Exception e2) {
                    // 시간은 옵션
                }
            }
            
            String title = titleLink.getText().trim();
            String href = titleLink.getAttribute("href");
            String logNo = titleLink.getAttribute("logno");
            String timeAgo = timeElement != null ? timeElement.getText().trim() : "";
            
            log.debug("발견된 게시글: title={}, href={}, logNo={}", title, href, logNo);
            
            if (title.isEmpty() || href.isEmpty()) {
                return null;
            }
            
            if (title.contains("공지") || title.contains("안내") || title.contains("공모전")) {
                log.debug("공지사항 제외: {}", title);
                return null;
            }
            
            String fullUrl = href.startsWith("http") ? href : "https://blog.naver.com" + href;
            
            return BlogPostInfo.builder()
                    .title(title)
                    .postUrl(fullUrl)
                    .logNo(logNo)
                    .timeAgo(timeAgo)
                    .categoryNo(categoryNo)
                    .crawledAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.debug("WebElement 파싱 중 오류: {}", e.getMessage());
            return null;
        }
    }

    /**
     * WebDriver에서 이미지 추출
     */
    private List<ExtractedImageInfo> extractImagesFromWebDriver(WebDriver driver, String postUrl) {
        List<ExtractedImageInfo> images = new ArrayList<>();
        
        try {
            List<WebElement> imageElements = driver.findElements(By.cssSelector(".se-image"));
            log.info("se-image 클래스로 {}개 이미지 요소 발견", imageElements.size());
            
            for (WebElement imageElement : imageElements) {
                try {
                    WebElement imgTag = imageElement.findElement(By.tagName("img"));
                    String imageUrl = imgTag.getAttribute("src");
                    String alt = imgTag.getAttribute("alt");
                    
                    String fullSizeImageUrl = convertToFullSizeUrl(imageUrl);
                    
                    if (fullSizeImageUrl != null && !fullSizeImageUrl.isEmpty() && isValidImageUrl(fullSizeImageUrl)) {
                        log.info("이미지 발견 (원본): {}", fullSizeImageUrl);
                        
                        ExtractedImageInfo imageInfo = ExtractedImageInfo.builder()
                                .imageUrl(fullSizeImageUrl)
                                .altText(alt)
                                .sourcePostUrl(postUrl)
                                .extractedAt(LocalDateTime.now())
                                .build();
                        
                        images.add(imageInfo);
                    }
                } catch (Exception e) {
                    log.warn("이미지 요소 처리 실패: {}", e.getMessage());
                }
            }
            
            log.info("총 {}개 이미지 추출 완료", images.size());
            
        } catch (Exception e) {
            log.error("이미지 추출 중 오류 발생", e);
        }
        
        return images;
    }

    /**
     * 썸네일 이미지 URL을 원본 크기로 변환
     */
    private String convertToFullSizeUrl(String imageUrl) {
        if (imageUrl != null && imageUrl.contains("postfiles.pstatic.net")) {
            if (imageUrl.contains("?type=")) {
                imageUrl = imageUrl.replaceAll("\\?type=w\\d+(_blur)?", "?type=w966");
            }
        }
        return imageUrl;
    }

    /**
     * 유효한 이미지 URL 검증
     */
    private boolean isValidImageUrl(String imageUrl) {
        return imageUrl != null && 
               !imageUrl.isEmpty() && 
               imageUrl.startsWith("http") &&
               imageUrl.contains("postfiles.pstatic.net");
    }

    /**
     * 연락처 추출 및 저장
     */
    private List<CaseContact> extractAndSaveContacts(WebDriver driver, String postUrl, Long caseId) {
        try {
            log.info("연락처 정보 추출 시작: {} (caseId: {})", postUrl, caseId);
            
            String pageTitle = driver.getTitle();
            String pageContent = getPageContent(driver);
            
            List<CaseContact> contacts = extractContactsFromContent(pageContent, postUrl, pageTitle, caseId);
            
            List<CaseContact> savedContacts = new ArrayList<>();
            for (CaseContact contact : contacts) {
                // caseId가 null인 경우 중복 체크를 하지 않고 저장
                if (caseId == null || !caseContactRepository.existsByCaseIdAndPhoneNorm(caseId, contact.getPhoneNorm())) {
                    CaseContact saved = caseContactRepository.save(contact);
                    savedContacts.add(saved);
                    log.info("연락처 저장 완료: {} - {}", saved.getOrganization(), saved.getPhoneNumber());
                } else {
                    log.info("중복 연락처 스킵: {}", contact.getPhoneNumber());
                }
            }
            
            log.info("연락처 추출 완료: 총 {}개 발견, {}개 신규 저장", contacts.size(), savedContacts.size());
            return savedContacts;
            
        } catch (Exception e) {
            log.error("연락처 추출 실패: {}", postUrl, e);
            return new ArrayList<>();
        }
    }

    /**
     * 페이지 콘텐츠 추출
     */
    private String getPageContent(WebDriver driver) {
        try {
            WebElement contentArea = driver.findElement(By.className("se-main-container"));
            return contentArea.getText();
        } catch (Exception e) {
            log.warn("메인 콘텐츠 영역을 찾을 수 없어 전체 페이지에서 추출합니다.");
            return driver.findElement(By.tagName("body")).getText();
        }
    }

    /**
     * 콘텐츠에서 연락처 정보 추출
     */
    private List<CaseContact> extractContactsFromContent(String content, String sourceUrl, String sourceTitle, Long caseId) {
        List<CaseContact> contacts = new ArrayList<>();
        
        log.info("페이지 텍스트 내용 (처음 500자): {}", content.length() > 500 ? content.substring(0, 500) : content);
        
        Matcher phoneMatcher = PHONE_PATTERN.matcher(content);
        
        while (phoneMatcher.find()) {
            String phoneNumber = phoneMatcher.group(1);
            String phoneNorm = phoneNumber.replaceAll("[^0-9]", "");
            
            log.info("전화번호 패턴 매칭됨: '{}' -> 정규화: '{}'", phoneNumber, phoneNorm);
            
            if (phoneNorm.length() < 8 || phoneNorm.length() > 15) {
                log.warn("전화번호 길이 부적절: {} ({}자리)", phoneNorm, phoneNorm.length());
                continue;
            }
            
            String organization = extractOrganizationNearPhone(content, phoneMatcher.start(), phoneMatcher.end());
            
            CaseContact contact = CaseContact.builder()
                    .caseId(caseId)
                    .organization(organization)
                    .phoneNumber(phoneNumber)
                    .phoneNorm(phoneNorm)
                    .sourceUrl(sourceUrl)
                    .sourceTitle(sourceTitle)
                    .crawledAt(LocalDateTime.now())
                    .build();
            
            contacts.add(contact);
            log.info("연락처 발견: {} - {}", organization, phoneNumber);
        }
        
        if (contacts.isEmpty()) {
            log.warn("전화번호 패턴 매칭되지 않음. 다른 패턴으로 시도");
            Pattern simplePattern = Pattern.compile("(\\d{2,3}.{0,3}\\d{3,4}.{0,3}\\d{4})");
            Matcher simpleMatcher = simplePattern.matcher(content);
            while (simpleMatcher.find()) {
                String found = simpleMatcher.group(1);
                log.info("간단한 패턴으로 발견: '{}'", found);
            }
        }
        
        return contacts;
    }

    /**
     * 전화번호 주변에서 기관명 추출
     */
    private String extractOrganizationNearPhone(String content, int phoneStart, int phoneEnd) {
        int searchStart = Math.max(0, phoneStart - 50);
        int searchEnd = Math.min(content.length(), phoneEnd + 50);
        String searchArea = content.substring(searchStart, searchEnd);
        
        Matcher orgMatcher = ORGANIZATION_PATTERN.matcher(searchArea);
        if (orgMatcher.find()) {
            return orgMatcher.group(1);
        }
        
        String[] keywords = {"경찰서", "실종수사팀", "수사팀", "파출소"};
        for (String keyword : keywords) {
            if (searchArea.contains(keyword)) {
                String[] words = searchArea.split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    if (words[i].contains(keyword)) {
                        StringBuilder org = new StringBuilder();
                        int start = Math.max(0, i - 2);
                        for (int j = start; j <= i; j++) {
                            if (j > start) org.append(" ");
                            org.append(words[j]);
                        }
                        return org.toString().trim();
                    }
                }
            }
        }
        
        return "알 수 없음";
    }

    /**
     * BlogPostInfo 리스트를 BlogPost 엔티티로 변환하여 DB에 저장
     */
    private List<BlogPost> saveBlogPostsToDatabase(List<BlogPostInfo> blogPostInfos) {
        List<BlogPost> savedPosts = new ArrayList<>();
        
        for (BlogPostInfo postInfo : blogPostInfos) {
            try {
                // 중복 체크 (URL 기준 - 더 정확함)
                if (!blogPostRepository.existsBySourceUrl(postInfo.getPostUrl())) {
                    BlogPost blogPost = BlogPost.builder()
                            .title(postInfo.getTitle())
                            .sourceUrl(postInfo.getPostUrl())
                            .crawledAt(postInfo.getCrawledAt())
                            .build();
                    
                    BlogPost saved = blogPostRepository.save(blogPost);
                    savedPosts.add(saved);
                    log.info("BlogPost 저장 완료: id={}, title={}, url={}", saved.getId(), saved.getTitle(), saved.getSourceUrl());
                } else {
                    log.info("중복 게시글 스킵 (URL 기준): {}", postInfo.getPostUrl());
                }
            } catch (Exception e) {
                log.error("BlogPost 저장 실패: title={}, url={}", postInfo.getTitle(), postInfo.getPostUrl(), e);
            }
        }
        
        return savedPosts;
    }
}
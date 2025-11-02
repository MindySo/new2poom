package com.topoom.external.blog.service;

import com.topoom.external.blog.dto.ExtractedImageInfo;
import com.topoom.missingcase.domain.CaseFile;
import com.topoom.missingcase.domain.CaseContact;
import com.topoom.missingcase.repository.CaseContactRepository;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
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
public class BlogImageProcessingService {
    
    private final S3ImageUploadService s3ImageUploadService;
    private final CaseContactRepository caseContactRepository;
    
    private static final int WAIT_TIMEOUT_SECONDS = 10;
    
    // 전화번호 정규식 패턴
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(\\d{2,3}[-\\s\\.\\u2010-\\u2015]*\\d{3,4}[-\\s\\.\\u2010-\\u2015]*\\d{4})"
    );
    
    // 기관명 정규식 패턴
    private static final Pattern ORGANIZATION_PATTERN = Pattern.compile(
        "([가-힣]+(?:경찰서|서)|[가-힣]*실종수사팀|[가-힣]*수사팀)"
    );
    
    public Map<String, Object> extractAndUploadImagesWithContacts(String postUrl, Long caseId) {
        log.info("블로그 게시글 이미지 및 연락처 추출 시작: {}", postUrl);
        
        WebDriver driver = null;
        try {
            // WebDriver 생성
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
    
    // 기존 메서드 유지 (호환성)
    public List<CaseFile> extractAndUploadImages(String postUrl, Long caseId) {
        Map<String, Object> result = extractAndUploadImagesWithContacts(postUrl, caseId);
        return (List<CaseFile>) result.get("images");
    }
    
    // ===== 내부 메서드들 =====
    
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
                    
                    // 원본 크기 이미지 URL로 변환
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
    
    private String convertToFullSizeUrl(String imageUrl) {
        if (imageUrl != null && imageUrl.contains("postfiles.pstatic.net")) {
            if (imageUrl.contains("?type=")) {
                imageUrl = imageUrl.replaceAll("\\?type=w\\d+(_blur)?", "?type=w966");
            }
        }
        return imageUrl;
    }
    
    private boolean isValidImageUrl(String imageUrl) {
        return imageUrl != null && 
               !imageUrl.isEmpty() && 
               imageUrl.startsWith("http") &&
               imageUrl.contains("postfiles.pstatic.net");
    }
    
    private List<CaseContact> extractAndSaveContacts(WebDriver driver, String postUrl, Long caseId) {
        try {
            log.info("연락처 정보 추출 시작: {} (caseId: {})", postUrl, caseId);
            
            String pageTitle = driver.getTitle();
            String pageContent = getPageContent(driver);
            
            List<CaseContact> contacts = extractContactsFromContent(pageContent, postUrl, pageTitle, caseId);
            
            List<CaseContact> savedContacts = new ArrayList<>();
            for (CaseContact contact : contacts) {
                if (!caseContactRepository.existsByCaseIdAndPhoneNorm(caseId, contact.getPhoneNorm())) {
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
    
    private String getPageContent(WebDriver driver) {
        try {
            // 메인 콘텐츠 영역에서 텍스트 추출
            WebElement contentArea = driver.findElement(By.className("se-main-container"));
            return contentArea.getText();
        } catch (Exception e) {
            log.warn("메인 콘텐츠 영역을 찾을 수 없어 전체 페이지에서 추출합니다.");
            return driver.findElement(By.tagName("body")).getText();
        }
    }
    
    private List<CaseContact> extractContactsFromContent(String content, String sourceUrl, String sourceTitle, Long caseId) {
        List<CaseContact> contacts = new ArrayList<>();
        
        log.info("페이지 텍스트 내용 (처음 500자): {}", content.length() > 500 ? content.substring(0, 500) : content);
        
        // 전화번호 패턴 매칭
        Matcher phoneMatcher = PHONE_PATTERN.matcher(content);
        
        while (phoneMatcher.find()) {
            String phoneNumber = phoneMatcher.group(1);
            String phoneNorm = phoneNumber.replaceAll("[^0-9]", "");
            
            log.info("전화번호 패턴 매칭됨: '{}' -> 정규화: '{}'", phoneNumber, phoneNorm);
            
            // 전화번호 길이 검증 (8-15자리)
            if (phoneNorm.length() < 8 || phoneNorm.length() > 15) {
                log.warn("전화번호 길이 부적절: {} ({}자리)", phoneNorm, phoneNorm.length());
                continue;
            }
            
            // 전화번호 주변 텍스트에서 기관명 추출
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
            // 더 간단한 패턴으로 재시도
            Pattern simplePattern = Pattern.compile("(\\d{2,3}.{0,3}\\d{3,4}.{0,3}\\d{4})");
            Matcher simpleMatcher = simplePattern.matcher(content);
            while (simpleMatcher.find()) {
                String found = simpleMatcher.group(1);
                log.info("간단한 패턴으로 발견: '{}'", found);
            }
        }
        
        return contacts;
    }
    
    private String extractOrganizationNearPhone(String content, int phoneStart, int phoneEnd) {
        // 전화번호 앞뒤 50자 내에서 기관명 찾기
        int searchStart = Math.max(0, phoneStart - 50);
        int searchEnd = Math.min(content.length(), phoneEnd + 50);
        String searchArea = content.substring(searchStart, searchEnd);
        
        Matcher orgMatcher = ORGANIZATION_PATTERN.matcher(searchArea);
        if (orgMatcher.find()) {
            return orgMatcher.group(1);
        }
        
        // 기관명 패턴이 없으면 일반적인 키워드 찾기
        String[] keywords = {"경찰서", "실종수사팀", "수사팀", "파출소"};
        for (String keyword : keywords) {
            if (searchArea.contains(keyword)) {
                // 키워드 앞의 단어들을 포함해서 추출
                String[] words = searchArea.split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    if (words[i].contains(keyword)) {
                        StringBuilder org = new StringBuilder();
                        // 앞의 2단어 정도까지 포함
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
}
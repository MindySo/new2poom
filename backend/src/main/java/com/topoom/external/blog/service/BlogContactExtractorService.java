package com.topoom.external.blog.service;

import com.topoom.missingcase.domain.CaseContact;
import com.topoom.missingcase.repository.CaseContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogContactExtractorService {
    
    private final CaseContactRepository caseContactRepository;
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(\\d{2,3}[-\\s\\.\\u2010-\\u2015]*\\d{3,4}[-\\s\\.\\u2010-\\u2015]*\\d{4})"
    );
    
    private static final Pattern ORGANIZATION_PATTERN = Pattern.compile(
        "([가-힣]+(?:경찰서|서)|[가-힣]*실종수사팀|[가-힣]*수사팀)"
    );
    
    public List<CaseContact> extractAndSaveContactsFromUrl(String postUrl, Long caseId) {
        // 임시로 새로운 WebDriver 생성해서 사용
        return extractAndSaveContactsWithNewDriver(postUrl, caseId);
    }
    
    public List<CaseContact> extractAndSaveContacts(WebDriver driver, String postUrl, Long caseId) {
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
            throw new RuntimeException("연락처 추출 실패", e);
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
    
    private List<CaseContact> extractAndSaveContactsWithNewDriver(String postUrl, Long caseId) {
        WebDriver driver = null;
        try {
            // WebDriver 설정 (기존 이미지 추출 서비스와 동일)
            io.github.bonigarcia.wdm.WebDriverManager.chromedriver().setup();
            
            org.openqa.selenium.chrome.ChromeOptions options = new org.openqa.selenium.chrome.ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            
            driver = new org.openqa.selenium.chrome.ChromeDriver(options);
            driver.get(postUrl);
            
            // 페이지 로딩 대기
            Thread.sleep(3000);
            
            return extractAndSaveContacts(driver, postUrl, caseId);
            
        } catch (Exception e) {
            log.error("연락처 추출 실패: {}", postUrl, e);
            return new ArrayList<>();
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
package com.topoom.missingcase.service;

import com.topoom.external.openapi.Safe182Client;
import com.topoom.missingcase.domain.MissingCase;
import com.topoom.missingcase.dto.Safe182Response;
import com.topoom.missingcase.repository.MissingCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MissingCaseSyncService {
    private final Safe182Client safe182Client;
    private final MissingCaseRepository missingCaseRepository;

    /**
     * Safe182 API 데이터를 DB로 동기화
     */
    public void syncMissing(int rowSize) {
        Safe182Response response = safe182Client.getMissing(rowSize);

        if (response == null || response.getBody() == null) return;

        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;

        for (Safe182Response.MissingItem item : response.getBody().getItems()) {
            // DB 저장 전, 중복 체크 가능
            MissingCase missingCase = new MissingCase();
            missingCase.setPersonName(item.getName());
            missingCase.setGender(item.getGender());
            missingCase.setOccurredLocation(item.getOccurredLocation());
            missingCase.setNationality(item.getNationality());
            missingCase.setCrawledAt(LocalDateTime.now());
            missingCase.setDeleted(false);

            try {
                if (item.getOccurredAt() != null && !item.getOccurredAt().isEmpty()) {
                    LocalDate date = LocalDate.parse(item.getOccurredAt(), DateTimeFormatter.ISO_DATE);
                    missingCase.setOccurredAt(date.atStartOfDay());
                }
            } catch (Exception e) {
                log.warn("⚠️ 날짜 파싱 실패 [{}]: {}", item.getOccurredAt(), e.getMessage());
            }

            missingCaseRepository.save(missingCase);
        }
    }
}

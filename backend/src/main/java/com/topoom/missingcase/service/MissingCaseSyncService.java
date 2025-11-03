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

        if (response == null || response.getList() == null) return;

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        for (Safe182Response.Safe182Item item : response.getList()) {
            try {
                MissingCase missingCase = new MissingCase();
                missingCase.setPersonName(item.getNm());
                missingCase.setGender(item.getSexdstnDscd());
                missingCase.setNationality(item.getNltyDscd());
                if (item.getAge() != null) {
                    missingCase.setAgeAtTime(item.getAge().shortValue());
                }
                if (item.getAgeNow() != null) {
                    missingCase.setCurrentAge(item.getAgeNow().shortValue());
                }
                missingCase.setOccurredLocation(item.getOccrAdres());

                // 날짜 변환 (occrde는 yyyyMMdd 형식)
                if (item.getOccrde() != null && item.getOccrde().matches("\\d{8}")) {
                    LocalDate date = LocalDate.parse(item.getOccrde(), dateFormatter);
                    missingCase.setOccurredAt(date.atStartOfDay());
                } else {
                    missingCase.setOccurredAt(null);
                }
                if (item.getHeight() != null) {
                    missingCase.setHeightCm(item.getHeight().shortValue());
                }
                if (item.getBdwgh() != null) {
                    missingCase.setWeightKg(item.getBdwgh().shortValue());
                }
                missingCase.setHairColor(item.getHaircolrDscd());
                missingCase.setFaceShape(item.getFaceshpeDscd());
                missingCase.setBodyType(item.getFrmDscd());
                missingCase.setHairStyle(item.getHairshpeDscd());
                missingCase.setClothingDesc(item.getAlldressingDscd());

                missingCase.setCrawledAt(LocalDateTime.now());
                missingCase.setDeleted(false);

                missingCase.setSourceTitle("실종경보 Open Api");
                missingCase.setSourceUrl("https://www.safe182.go.kr");


                missingCase.setTargetType(mapTargetType(item.getWritingTrgetDscd()));

                missingCaseRepository.save(missingCase);

            } catch (Exception e) {
                log.error("실종자 정보 저장 중 오류 발생: {}", e.getMessage(), e);
            }
        }

    }

    private String mapTargetType(String writingTrgetDscd) {
        if (writingTrgetDscd == null) {
            return "불상";
        }
        switch (writingTrgetDscd) {
            case "010":
                return "아동";
            case "060":
            case "061":
            case "062":
                return "장애";
            case "070":
                return "치매";
            default:
                return "기타";
        }
    }
}

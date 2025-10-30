package com.topoom.external.scheduler;

import com.topoom.external.openapi.Safe182Client;
import com.topoom.missingcase.dto.MissingCaseDto;
import com.topoom.missingcase.repository.MissingCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataUpdateScheduler {

    private final Safe182Client safe182Client;
    private final MissingCaseRepository missingCaseRepository;

    @Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
    public void scheduleDataUpdate() {
        // TODO: 정기적 데이터 수집 스케줄링 로직 구현
        log.info("Starting scheduled data update");
        List<MissingCaseDto.Response> apiCases = safe182Client.fetchMissingCases();

        for (MissingCaseDto.Response apiCase : apiCases) {
            missingCaseRepository.findBySourceId(apiCase.getSourceId())
                    .ifPresentOrElse(
                            existing -> updateEntity(existing, apiCase),
                            () -> missingCaseRepository.save(convertToEntity(apiCase))
                    );
        }
    }
}

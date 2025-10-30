package com.topoom.missingcase.service;

import com.topoom.missingcase.domain.MissingCase;
import com.topoom.missingcase.dto.MissingCaseDto;
import com.topoom.missingcase.repository.MissingCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MissingCaseService {

    private final MissingCaseRepository missingCaseRepository;

    public List<MissingCaseDto.Response> getAllCases() {
        return missingCaseRepository.findAll().stream()
                .map(caseEntity -> new MissingCaseDto.Response(
                        caseEntity.getId(),
                        caseEntity.getPersonName(),
                        caseEntity.getOccurredLocation(),
                        caseEntity.getOccurredAt(),
                        caseEntity.getProgressStatus()
                ))
                .toList();
    }

    public MissingCaseDto.DetailResponse getCaseById(Long id) {
        MissingCase caseEntity = missingCaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 실종 사례가 없습니다."));

        return new MissingCaseDto.DetailResponse(
                caseEntity.getId(),
                caseEntity.getPersonName(),
                caseEntity.getGender(),
                caseEntity.getCurrentAge(),
                caseEntity.getOccurredLocation(),
                caseEntity.getOccurredAt(),
                caseEntity.getProgressStatus(),
                caseEntity.getClothingDesc()
        );
    }
}

package com.topoom.missingcase.repository;

import com.topoom.missingcase.domain.MissingCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MissingCaseRepository extends JpaRepository<MissingCase, Long> {

    // URL로 중복 체크
    boolean existsBySourceUrl(String sourceUrl);
}

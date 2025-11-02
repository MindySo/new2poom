package com.topoom.missingcase.repository;

import com.topoom.missingcase.domain.CaseContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseContactRepository extends JpaRepository<CaseContact, Long> {
    
    List<CaseContact> findByCaseId(Long caseId);
    
    Optional<CaseContact> findByCaseIdAndPhoneNorm(Long caseId, String phoneNorm);
    
    boolean existsByCaseIdAndPhoneNorm(Long caseId, String phoneNorm);
}

package com.topoom.missingcase.repository;

import com.topoom.missingcase.domain.MissingCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MissingCaseRepository extends JpaRepository<MissingCase, Long> {
    List<MissingCase> findByIsDeletedFalse();
}

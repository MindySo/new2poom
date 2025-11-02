package com.topoom.missingcase.domain;

import com.topoom.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_contact")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaseContact extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = true)
    private Long caseId;

    @Column(name = "organization", length = 120)
    private String organization;

    @Column(name = "phone_number", length = 30, nullable = false)
    private String phoneNumber;

    @Column(name = "phone_norm", length = 20)
    private String phoneNorm;

    @Column(name = "source_url", columnDefinition = "TEXT", nullable = false)
    private String sourceUrl;

    @Column(name = "source_title", length = 300, nullable = false)
    private String sourceTitle;

    @Column(name = "crawled_at", nullable = false)
    private LocalDateTime crawledAt;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @PrePersist
    @PreUpdate
    public void normalizePhoneNumber() {
        if (phoneNumber != null) {
            // 숫자만 추출하여 정규화
            this.phoneNorm = phoneNumber.replaceAll("[^0-9]", "");
        }
    }
}

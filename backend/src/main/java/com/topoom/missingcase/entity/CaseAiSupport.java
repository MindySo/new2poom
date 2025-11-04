package com.topoom.missingcase.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "case_ai_support",
        indexes = @Index(name = "ix_ai_support_case", columnList = "case_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseAiSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = true)
    private Long caseId;

    @Column(name = "top1_desc")
    private String top1Desc;

    @Column(name = "top2_desc")
    private String top2Desc;

    @Column(name = "info_items", columnDefinition = "JSON")
    private String infoItems;
}

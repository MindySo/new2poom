package com.topoom.missingcase.domain;

import com.topoom.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_file")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CaseFile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = true)
    private Long caseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "io_role", length = 20, nullable = false)
    private IoRole ioRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 20, nullable = false)
    private Purpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_kind", length = 20, nullable = false)
    private ContentKind contentKind;

    @Column(name = "s3_key", columnDefinition = "TEXT", nullable = false)
    private String s3Key;

    @Column(name = "s3_bucket", length = 128)
    private String s3Bucket;

    @Column(name = "content_type", length = 64)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "width_px")
    private Integer widthPx;

    @Column(name = "height_px")
    private Integer heightPx;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "source_url", columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(name = "crawled_at")
    private LocalDateTime crawledAt;

    public enum IoRole { INPUT, OUTPUT }
    public enum Purpose { BEFORE, APPEARANCE, FACE, FULL_BODY, UNUSABLE, TEXT, ENHANCED, ANALYSIS }
    public enum ContentKind { IMAGE, JSON }
}

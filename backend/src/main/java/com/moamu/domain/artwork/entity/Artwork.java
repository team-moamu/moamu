package com.moamu.domain.artwork.entity;

import java.time.LocalDateTime;

import com.moamu.domain.artwork.enums.Status;
import com.moamu.global.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Artowork
 * Entity
 */

@Entity
@Table(name = "artwork")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Artwork extends BaseEntity {
    // 작품ID
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "artwork_id")
    private Long artworkId;

    // 작품 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.READY;

    // 작품 이름
    @Column(name = "artwork_title")
    private String artworkTitle;

    // S3 저장 키(추후 media_id로 변경 의논 필요)
    @Column(name = "media_id")
    private Long imageKey;

    // 최신 작품 생성 요청 버전
    @Column(name = "task_version", nullable = false)
    private int taskVersion = 1;

    // 작품 생성 AI 모델 버전 (사전 제작 메타데이터)
    @Column(name = "model_version")
    private String modelVersion;

    // 작품 준비 완료 시각
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}

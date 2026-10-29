package com.moamu.domain.schedule.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.moamu.domain.schedule.enums.Status;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    // 회차 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    // 도전 ID
    @Column(name = "challenge_id", nullable = false)
    private Long challengeId;

    // 도전 회차
    @Column(name = "day_no", nullable = false)
    private int dayNo;

    // 인증 날짜
    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    // 인증 가능 시작 시각
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    // 마감 시각
    @Column(name = "deadline_at", nullable = false)
    private LocalDateTime deadlineAt;

    // 회차 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status = Status.UPCOMING;

    // 사용자 인증 시도 횟수 (최대 3회)
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    // 최종 실패 또는 보류 사유
    @Column(name = "result_reason", length = 500)
    private String resultReason;

    // 최종 회차 결과 확정 시각
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    // 동시 수정 방지 버전
    @Version
    @Column(name = "version", nullable = false)
    private int version;

    // 퍼즐 조각 위치 번호
    @Column(name = "piece_index", nullable = false)
    private int pieceIndex;

    @CreatedDate
    @Column(name = "frst_reg_dt", updatable = false)
    private LocalDateTime frstRegDt;

    @LastModifiedDate
    @Column(name = "upd_dt")
    private LocalDateTime updDt;

    public static Schedule create(
            Long challengeId,
            int dayNo,
            LocalDate scheduleDate,
            LocalDateTime startAt,
            LocalDateTime deadlineAt,
            int pieceIndex
    ) {
        Schedule schedule = new Schedule();

        schedule.challengeId = challengeId;
        schedule.dayNo = dayNo;
        schedule.scheduleDate = scheduleDate;
        schedule.startAt = startAt;
        schedule.deadlineAt = deadlineAt;
        schedule.pieceIndex = pieceIndex;

        return schedule;
    }
}

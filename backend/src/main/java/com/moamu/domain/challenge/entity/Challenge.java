package com.moamu.domain.challenge.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.moamu.domain.challenge.enums.Status;
import com.moamu.domain.challenge.enums.Verifiability;
import com.moamu.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "challenge")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Challenge extends BaseEntity {
    // 도전ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="challenge_id")
    private Long challengeId;

    // 회원 ID
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    // 카테고리 ID
    @Column(name = "cate_id", nullable = false)
    private Long cateId;
    
    // 목표 템플릿ID
    @Column(name = "goal_template_id", nullable = false)
    private Long goalTemplateId;

    // 해당 도전에 연결된 작품 ID
    @Column(name = "artwork_id", nullable = false)
    private Long artworkId;

    // 도전명
    @Column(name = "challenge_title", nullable = false)
    private String challengeTitle;

    // AI 생성 목표 문장
    @Column(name = "goal_text", nullable = false, columnDefinition = "TEXT")
    private String goalText;

    // 사용자가 직접 입력한 원본 목표 문장
    @Column(name = "input_goal_text", columnDefinition = "TEXT")
    private String inputGoalText;

    // 사진 인증 가능성 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Verifiability verifiability;

    // 대체 조건 수락 여부 (Y/N)
    @Column(name = "substitute_accepted", nullable = false)
    private char substituteAccepted = 'N';

    // 목표 생성 시 촬영 가이드
    @Column(name = "photo_guide", columnDefinition = "TEXT")
    private String photoGuide;

    // 도전 기간 일 수 (7/14/30)
    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    // 도전 시작일
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // 도전 종료일
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // 도전 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    // 도전 중단 시각
    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    // 도전 생성 당시 사진 인증 범위 안내
    @Column(name = "verification_scope", nullable = false, columnDefinition = "TEXT")
    private String verificationScope;

    // 퍼즐조각 그리드 행 수
    @Column(name = "grid_rows", nullable = false)
    private int gridRows;

    // 퍼즐조각 그리드 열 수
    @Column(name = "grid_cols", nullable = false)
    private int gridCols;

    // 미술관 전시 여부(Y/N)
    @Column(name = "is_exhibition")
    private char isExhibition = 'N';
}

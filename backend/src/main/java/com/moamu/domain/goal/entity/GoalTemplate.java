
package com.moamu.domain.goal.entity;

import com.moamu.domain.goal.enums.Status;
import com.moamu.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "goal_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoalTemplate extends BaseEntity {

    // 도전 템플릿 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_template_id")
    private Long goalTemplateId;

    // 카테고리 ID
    @Column(name = "cate_id", nullable = false)
    private Long cateId;

    // 도전 템플릿 이름
    @Column(name = "goal_name", nullable = false)
    private String goalName;

    // 목표 수행 내용 설명
    @Column(name = "description")
    private String description;

    // 인증 사진 촬영 가이드
    @Column(name = "photo_guide", nullable = false)
    private String photoGuide;

    // 사진으로 확인 가능한 범위와 한계 안내
    @Column(name = "verification_scope")
    private String verificationScope;

    // 도전 템플릿 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    // 목표 목록 표시 순서
    @Column(name = "sort_order")
    private int sortOrder;
}

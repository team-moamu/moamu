package com.moamu.domain.goal.entity;

import com.moamu.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "goal_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoalCategory extends BaseEntity {

    // 카테고리 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cate_id")
    private Long cateId;

    // 부모 카테고리 ID (대분류는 NULL)
    @Column(name = "parent_cate_id")
    private Long parentCateId;

    // 카테고리 코드
    @Column(name = "cate_code", nullable = false, length = 50)
    private String cateCode;

    // 카테고리 표시명
    @Column(name = "cate_name", nullable = false, length = 50)
    private String cateName;

    // 카테고리 생성 출처
    @Column(name = "cate_source", nullable = false, length = 20)
    private String cateSource;

    // 분석 장애 대체 경로용 기본 조건
    @Column(name = "default_conditions", columnDefinition = "TEXT")
    private String defaultConditions;
}

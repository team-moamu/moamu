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
@Table (name = "goal_template_condition")
@Getter 
@NoArgsConstructor (access = AccessLevel.PROTECTED)
public class GoalTemplateCondition extends BaseEntity {

    // 도전 인증 조건 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_template_condition_id")
    private Long goalTemplateConditionId;

    @Column(name = "goal_template_id", nullable = false)
    private Long goalTemplateId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "description", nullable = false, length = 300)
    private String description;
}

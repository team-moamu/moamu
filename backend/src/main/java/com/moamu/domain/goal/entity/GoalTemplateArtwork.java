package com.moamu.domain.goal.entity;

import com.moamu.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * GoalTemplateArtwork
 * 
 * 목표 템플릿과 작품을 매핑해주는 Entity
 */

@Entity 
@Table(name = "goal_template_artwork")
@Getter 
@NoArgsConstructor 
public class GoalTemplateArtwork extends BaseEntity {

    // 목표 템플릿 - 작품 매핑 ID
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_template_artwork_id")
    private Long goalTemplateArtworkId;

    // 목표 템플릿 ID
    @Column(name = "goal_template_id", nullable = false)
    private Long goalTemplateId;

    // 작품ID
    @Column(name = "artwork_id", nullable = false)
    private Long artworkId;

    // 목표 목록 표시 순서
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
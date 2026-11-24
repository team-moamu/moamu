package com.moamu.domain.goal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import com.moamu.domain.goal.entity.GoalTemplateArtwork;

public interface GoalTemplateArtworkRepository extends JpaRepository<GoalTemplateArtwork, Long> {


    @Query("""
            SELECT link.artworkId FROM GoalTemplateArtwork link
            JOIN Artwork artwork ON artwork.artworkId = link.artworkId
            WHERE link.goalTemplateId = :templateId AND link.delYn = 'N' AND artwork.delYn = 'N'
            """)
    List<Long> findActiveArtworkIds(@Param("templateId") Long templateId);

    // 여러 템플릿에 연결된 작품도 후보에는 한 번만 넣는다.
    @Query("""
            SELECT DISTINCT link.artworkId FROM GoalTemplateArtwork link
            JOIN GoalTemplate template ON template.goalTemplateId = link.goalTemplateId
            JOIN Artwork artwork ON artwork.artworkId = link.artworkId
            WHERE template.cateId = :categoryId AND template.delYn = 'N'
                AND link.delYn = 'N' AND artwork.delYn = 'N'
                AND artwork.status = com.moamu.domain.artwork.enums.Status.READY
            """)
    List<Long> findReadyArtworkIdsByCategory(@Param("categoryId") Long categoryId);

    Optional<GoalTemplateArtwork> findByGoalTemplateIdAndArtworkId(Long goalTemplateId, Long artworkId);

    @Modifying
    @Query("UPDATE GoalTemplateArtwork link SET link.delYn = 'N' WHERE link.goalTemplateArtworkId = :linkId")
    void restore(@Param("linkId") Long linkId);
}

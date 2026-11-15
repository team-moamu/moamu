package com.moamu.domain.goal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moamu.domain.goal.entity.GoalCategory;

public interface GoalCategoryRepository extends JpaRepository<GoalCategory, Long> {

    // 활성 상태인 대·중·소분류 경로로 기존 소분류 ID 조회
    @Query("""
            SELECT minor.cateId
            FROM GoalCategory major
            JOIN GoalCategory middle ON middle.parentCateId = major.cateId
            JOIN GoalCategory minor ON minor.parentCateId = middle.cateId
            WHERE major.parentCateId IS NULL
                AND major.delYn = 'N'
                AND middle.delYn = 'N'
                AND minor.delYn = 'N'
                AND major.cateName = :majorName
                AND middle.cateName = :middleName
                AND minor.cateName = :minorName
            """)
    Optional<Long> findActiveMinorId(@Param("majorName") String majorName, @Param("middleName") String middleName, @Param("minorName") String minorName);
}

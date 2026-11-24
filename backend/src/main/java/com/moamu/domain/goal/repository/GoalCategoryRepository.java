package com.moamu.domain.goal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

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
    Optional<Long> findActiveMinorId(@Param("majorName") String majorName, @Param("middleName") String middleName,
            @Param("minorName") String minorName);

    // 삭제된 분류 복구
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE GoalCategory c
            SET c.delYn = 'N', c.updDt = CURRENT_TIMESTAMP
            WHERE c.cateId = :cateId
            """)
    void restore(@Param("cateId") Long cateId);

    // 같은 부모와 이름의 분류는 삭제 상태까지 조회하고 복구·생성을 직렬화
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM GoalCategory c
            WHERE ((:parentId IS NULL AND c.parentCateId IS NULL) OR c.parentCateId = :parentId)
                AND TRIM(c.cateName) = :name
            """)
    Optional<GoalCategory> findForConfirmation(@Param("parentId") Long parentId, @Param("name") String name);

    // 분류 ID로 DB에 저장된 대·중·소분류 이름을 한 번에 가져오는 조회
    interface CategoryNames {
        String getMajorName();

        String getMiddleName();

        String getMinorName();
    }

    @Query("""
            SELECT major.cateName AS majorName, middle.cateName AS middleName, minor.cateName AS minorName
            FROM GoalCategory minor
            JOIN GoalCategory middle ON minor.parentCateId = middle.cateId
            JOIN GoalCategory major ON middle.parentCateId = major.cateId
            WHERE minor.cateId = :categoryId AND major.parentCateId IS NULL
                AND major.delYn = 'N' AND middle.delYn = 'N' AND minor.delYn = 'N'
            """)
    Optional<CategoryNames> findCategoryNames(@Param("categoryId") Long categoryId);
}

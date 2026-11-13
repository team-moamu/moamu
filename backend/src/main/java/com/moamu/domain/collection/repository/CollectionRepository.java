package com.moamu.domain.collection.repository;

import com.moamu.domain.challenge.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Challenge, Long> {

    // 전시할 도전 한 건을 조회
    @Query("""
        SELECT c
        FROM Challenge c
        WHERE c.challengeId = :challengeId
            AND c.userId = :userId
            AND c.delYn = 'N'
        """)
    Optional<Challenge> findOwnedForExhibition(
        @Param("userId") Long userId,
        @Param("challengeId") Long challengeId
    );

    // 사용자의 완료된 도전 목록을 조회
    @Query("""
        SELECT c
        FROM Challenge c
        WHERE c.userId = :userId
            AND c.status = com.moamu.domain.challenge.enums.Status.COMPLETED
            AND c.delYn = 'N'
            AND (:exhibitionStatus IS NULL OR c.isExhibited = :exhibitionStatus)
        ORDER BY
            CASE WHEN :sort = 'name' THEN c.challengeTitle END ASC,
            CASE WHEN :sort = 'latest' THEN c.endDate END DESC,
            CASE WHEN :sort = 'successRate' THEN
                CASE WHEN c.durationDays > 0 THEN
                    (SELECT COUNT(s) * 1.0 FROM Schedule s
                     WHERE s.challengeId = c.challengeId
                         AND s.status = com.moamu.domain.schedule.enums.Status.SUCCESS)
                    / c.durationDays
                ELSE 0.0 END
            END DESC,
            c.challengeId DESC
        """)
    List<Challenge> findOwned(
        @Param("userId") Long userId,
        @Param("exhibitionStatus") Character exhibitionStatus,
        @Param("sort") String sort
    );
}

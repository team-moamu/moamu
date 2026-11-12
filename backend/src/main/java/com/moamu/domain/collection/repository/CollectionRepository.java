package com.moamu.domain.collection.repository;

import com.moamu.domain.challenge.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CollectionRepository extends JpaRepository<Challenge, Long> {

    // 사용자의 완료된 도전 목록을 조회
    @Query("""
        SELECT c
        FROM Challenge c
        WHERE c.userId = :userId
            AND c.status = com.moamu.domain.challenge.enums.Status.COMPLETED
            AND c.delYn = 'N'
        ORDER BY c.endDate DESC, c.challengeId DESC
        """)
    List<Challenge> findOwned(
        @Param("userId") Long userId
    );
}

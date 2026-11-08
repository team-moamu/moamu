package com.moamu.domain.verification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.moamu.domain.schedule.enums.Status;
import com.moamu.domain.verification.dto.VerificationSuccessResponse.Check;
import com.moamu.domain.verification.dto.VerificationSuccessSnapshot;
import com.moamu.domain.verification.entity.Verification;

public interface VerificationSuccessRepository extends JpaRepository<Verification, Long> {

    // 인증 소유자의 도전과 회차, 도전 생성 당시 선택된 작품 정보 조회
    @Query("""
        SELECT new com.moamu.domain.verification.dto.VerificationSuccessSnapshot(
            v.verificationId,
            v.status,
            c.challengeId,
            c.challengeTitle,
            s.scheduleId,
            s.dayNo,
            s.status,
            s.pieceIndex,
            c.gridRows,
            c.gridCols,
            c.artworkId,
            a.artworkTitle,
            m.imageKey
        )
        FROM Verification v
        INNER JOIN Schedule s ON s.scheduleId = v.scheduleId
        INNER JOIN Challenge c ON c.challengeId = s.challengeId
        LEFT JOIN Artwork a ON a.artworkId = c.artworkId
        LEFT JOIN Media m ON m.mediaId = a.mediaId
        WHERE v.verificationId = :verificationId
            AND c.userId = :userId
            AND c.delYn = 'N'
        """)
    Optional<VerificationSuccessSnapshot> findOwned(
        @Param("userId") long userId,
        @Param("verificationId") long verificationId
    );

    // 성공 확정된 회차의 공개 조각을 위치 순서로 조회
    @Query("""
        SELECT s.pieceIndex
        FROM Schedule s
        WHERE s.challengeId = :challengeId
            AND s.status = :status
        ORDER BY s.pieceIndex ASC
        """)
    List<Integer> findRevealed(
        @Param("challengeId") long challengeId,
        @Param("status") Status status
    );

    // 전체 조각 수는 격자 칸 수가 아닌 도전의 전체 회차 수로 계산
    @Query("""
        SELECT COUNT(s)
        FROM Schedule s
        WHERE s.challengeId = :challengeId
        """)
    long countPieces(@Param("challengeId") long challengeId);

    // 해당 인증 시도에 저장된 조건별 설명과 판정 결과 조회
    @Query("""
        SELECT new com.moamu.domain.verification.dto.VerificationSuccessResponse$Check(
            cc.challengeConditionId,
            cc.code,
            cc.description,
            vc.status,
            vc.reason
        )
        FROM VerificationCheck vc
        INNER JOIN ChallengeCondition cc ON cc.challengeConditionId = vc.challengeConditionId
        WHERE vc.verificationId = :verificationId
        ORDER BY cc.challengeConditionId ASC
        """)
    List<Check> findChecks(@Param("verificationId") long verificationId);
}
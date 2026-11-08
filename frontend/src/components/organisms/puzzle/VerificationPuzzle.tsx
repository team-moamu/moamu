import { useState } from 'react'
import type { VerificationSuccessResponse } from '../../../types/Challenge'

interface VerificationPuzzleProps {
  result: VerificationSuccessResponse
}

// 작품 위에 회차별 공개 상태를 덮어 표시. 배정되지 않은 격자 칸은 공개하지 않음
export function VerificationPuzzle({ result }: VerificationPuzzleProps) {
  const [failedImageUrl, setFailedImageUrl] = useState<string | null>(null)
  const revealedPieces = new Set(result.revealedPieceIndices)
  const imageUrl = result.artwork.imageUrl
  const imageAvailable = imageUrl && failedImageUrl !== imageUrl

  return (
    <figure>
      <div
        className="verification-puzzle"
        role="img"
        aria-label={`${result.artwork.title || '작품'} 퍼즐, ${result.totalPieceCount}개 중 ${result.earnedPieceCount}개 공개, 이번 조각 ${result.openedPieceIndex + 1}번`}
      >
        {imageAvailable ? (
          <img src={imageUrl} alt="" onError={() => setFailedImageUrl(imageUrl)} />
        ) : (
          <p className="verification-puzzle-placeholder">작품 이미지를 준비하고 있어요</p>
        )}
        <div className="verification-puzzle-grid" style={{
          gridTemplateColumns: `repeat(${result.gridCols}, 1fr)`,
          gridTemplateRows: `repeat(${result.gridRows}, 1fr)`,
        }}>
          {Array.from({ length: result.gridRows * result.gridCols }, (_, index) => {
            const revealed = index < result.totalPieceCount && revealedPieces.has(index)
            const justOpened = revealed && index === result.openedPieceIndex
            return <span key={index} className={[
              'verification-puzzle-piece',
              revealed ? 'is-revealed' : '',
              justOpened ? 'is-new' : '',
            ].join(' ')}>{justOpened && <span className="verification-puzzle-sparkle" aria-hidden="true">✦</span>}</span>
          })}
        </div>
      </div>
      <figcaption className="verification-puzzle-caption">
        {result.challengeTitle} · {result.earnedPieceCount} / {result.totalPieceCount} 조각
      </figcaption>
    </figure>
  )
}


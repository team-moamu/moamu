import { ArtworkPuzzle } from './ArtworkPuzzle'
import type { VerificationSuccessResponse } from '../../../types/Challenge'

interface VerificationPuzzleProps {
  result: VerificationSuccessResponse
}

// 작품 위에 회차별 공개 상태를 덮어 표시. 배정되지 않은 격자 칸은 공개하지 않음
export function VerificationPuzzle({ result }: VerificationPuzzleProps) {
  return (
    <figure>
      <ArtworkPuzzle
        imageUrl={result.artwork.imageUrl}
        title={result.artwork.title}
        gridRows={result.gridRows}
        gridCols={result.gridCols}
        totalPieceCount={result.totalPieceCount}
        revealedPieceIndices={result.revealedPieceIndices}
        highlightedPieceIndex={result.openedPieceIndex}
      />
      <figcaption className="mt-3.5 mb-[18px] text-center text-[14px] font-semibold">
        {result.challengeTitle} · {result.earnedPieceCount} / {result.totalPieceCount} 조각
      </figcaption>
    </figure>
  )
}


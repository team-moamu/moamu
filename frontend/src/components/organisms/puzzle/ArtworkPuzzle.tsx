import { useState } from 'react'
import './ArtworkPuzzle.css'

interface ArtworkPuzzleProps {
  imageUrl: string | null
  title: string | null
  gridRows: number
  gridCols: number
  totalPieceCount: number
  revealedPieceIndices: number[]
  allPiecesEarned?: boolean
  highlightedPieceIndex?: number
  maskColor?: 'brown' | 'black'
}

// 일일 인증과 도전 완료에서 공통으로 사용하는 작품·공개 위치 렌더링
export function ArtworkPuzzle({
  imageUrl,
  title,
  gridRows,
  gridCols,
  totalPieceCount,
  revealedPieceIndices,
  allPiecesEarned = false,
  highlightedPieceIndex,
  maskColor = 'brown',
}: ArtworkPuzzleProps) {
  const [failedImageUrl, setFailedImageUrl] = useState<string | null>(null)
  const revealedPieces = new Set(revealedPieceIndices)
  const imageAvailable = imageUrl && failedImageUrl !== imageUrl
  const highlightedLabel = highlightedPieceIndex === undefined ? '' : `, 이번 조각 ${highlightedPieceIndex + 1}번`

  return (
    <div className="relative aspect-[1.6] overflow-hidden rounded-lg bg-[#e5dac5]"
      role="img"
      aria-label={`${title || '작품'} 퍼즐, ${totalPieceCount}개 중 ${revealedPieces.size}개 공개${highlightedLabel}`}>
      {imageAvailable ? (
        <img className="h-full w-full object-cover" src={imageUrl} alt="" onError={() => setFailedImageUrl(imageUrl)} />
      ) : (
        <p data-image-placeholder className="absolute inset-0 z-2 grid place-content-center p-3 text-center text-xs leading-6 text-[var(--color-text)] [text-shadow:0_0_4px_white]">작품 이미지를 불러올 수 없어요</p>
      )}
      {/* 전체 획득 시 배정되지 않은 칸과 격자 경계까지 제거하여 원본 작품 전체 표시 */}
      {!allPiecesEarned && (
        <div className="absolute inset-0 grid" style={{
          gridTemplateColumns: `repeat(${gridCols}, 1fr)`,
          gridTemplateRows: `repeat(${gridRows}, 1fr)`,
        }}>
          {Array.from({ length: gridRows * gridCols }, (_, index) => {
            const revealed = index < totalPieceCount && revealedPieces.has(index)
            const justOpened = revealed && index === highlightedPieceIndex
            return (
              <span key={index} data-piece-index={index} data-revealed={revealed} data-new={justOpened}
                className={[
                  'relative',
                  justOpened
                    ? 'z-1 animate-[artwork-piece-reveal_900ms_ease-out] border-[3px] border-[#ffdd78] bg-[#ffc035]/14 shadow-[0_0_12px_#ffce59,inset_0_0_12px_#ffce59] motion-reduce:animate-none'
                    : `border ${maskColor === 'black' ? 'border-[#303030]' : 'border-[#fff8ec]/13'} ${revealed ? 'bg-transparent' : maskColor === 'black' ? 'bg-[#171717]' : 'bg-[#493725]/84'}`,
                ].join(' ')}>
                {justOpened && <span className="absolute -top-[18px] -right-[9px] text-[30px] text-[#fff3b4] [text-shadow:0_0_8px_#ffae29]" aria-hidden="true">✦</span>}
              </span>
            )
          })}
        </div>
      )}
    </div>
  )
}

